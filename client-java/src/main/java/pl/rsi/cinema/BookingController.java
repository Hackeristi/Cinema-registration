package pl.rsi.cinema;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.rsi.cinema.CinemaServerService.MovieDetails;
import pl.rsi.cinema.dto.MovieFromServer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;

public class BookingController {

    // Auth overlay fields
    @FXML
    private VBox authOverlay;
    @FXML
    private VBox authModal;
    @FXML
    private VBox loginForm;
    @FXML
    private VBox registerForm;
    @FXML
    private TextField loginEmail;
    @FXML
    private PasswordField loginPassword;
    @FXML
    private TextField registerName;
    @FXML
    private TextField registerSurname;
    @FXML
    private TextField registerEmail;
    @FXML
    private PasswordField registerPassword;
    @FXML
    private PasswordField registerConfirmPassword;
    @FXML
    private TableColumn<MovieFromServer, String> titleCol;
    @FXML
    private TableColumn<MovieFromServer, String> genreCol;
    @FXML
    private TableColumn<MovieFromServer, String> dateCol;
    // Booking fields
    private int editingReservationId = -1;
    @FXML
    private VBox seatsListContainer;
    @FXML
    private VBox seatsListContainer1;
    @FXML
    private HBox timeButtonsContainer;
    @FXML
    private GridPane seatGrid;
    @FXML
    private ComboBox<String> MovieDate;
    @FXML
    private Button downloadPdfButton;
    @FXML
    private Label titleLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Label genreLabel;

    @FXML
    private Label yearLabel;

    @FXML
    private Label directorLabel;

    @FXML
    private TextArea actorsArea;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ImageView coverImage;
    @FXML
    private Label screenLabel;
    @FXML
    private Label serverStatusLabel;
    @FXML
    private Set<String> editingSeatKeys = new HashSet<>();
    private Label mtomStatusLabel;
    private Button currentSelectedTimeButton = null;
    private String selectedTime = "";
    private final Set<String> selectedSeatKeys = new HashSet<>();
    private final SeatController seatController = new SeatController();
    private final CinemaServerService serverService = CinemaServerService.getInstance();
    private List<MovieFromServer> movies;
    @FXML
    private TableView<MovieFromServer> moviesTable;
    private int currentFilmShowId = -1;
    private final Map<String, Integer> seatIdMap = new HashMap<>();
    private final Map<Integer, MovieFromServer> filmShowToMovieMap = new HashMap<>();
    private MovieFromServer activeMovie;
    String editStyle = "-fx-background-color: #0078D7;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 10;" +
            "-fx-background-radius: 4;";

    String deleteStyle = "-fx-background-color: #ff4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 10;" +
            "-fx-background-radius: 4;";
    String disabledStyle = "-fx-background-color: #555555;" +
            "-fx-text-fill: #cccccc;" +
            "-fx-font-size: 10;" +
            "-fx-background-radius: 4;";

    @FXML
    public void initialize() {

        new Thread(() -> {
            boolean reachable = serverService.isServerReachable();
            javafx.application.Platform.runLater(() -> {
                if (serverStatusLabel != null) {
                    serverStatusLabel.setText(reachable ? "Połączono z serwerem .NET" : "Brak połączenia z serwerem");
                    serverStatusLabel.setTextFill(reachable
                            ? javafx.scene.paint.Color.WHITE
                            : javafx.scene.paint.Color.web("#ff4444"));
                }
            });
        }).start();

        showAuthOverlay();
        loginForm.setVisible(true);
        loginForm.setManaged(true);

        registerForm.setVisible(false);
        registerForm.setManaged(false);
        if (seatGrid != null) {
            seatController.setBookingController(this);
            seatController.initSeatMap(seatGrid);

            try {
                movies = serverService.getMovies();

                if (movies != null && !movies.isEmpty()) {

                    Set<String> uniqueDates = new HashSet<>();
                    for (var movie : movies) {
                        String dateTime = movie.getShowDateTime();
                        if (dateTime != null && dateTime.length() >= 10) {
                            String date = dateTime.substring(0, 10);
                            uniqueDates.add(date);
                        }
                    }

                    MovieDate.getItems().addAll(uniqueDates.stream().sorted().toList());

                } else {
                    MovieDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
                }

            } catch (Exception e) {
                MovieDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
            }

            if (!MovieDate.getItems().isEmpty()) {
                MovieDate.getSelectionModel().selectFirst();
            }

            // 🔥 DOPIERO TERAZ:
            setupMovies();
            loadMovies();
            moviesTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldMovie, newMovie) -> {

                        if (newMovie != null) {
                            updatePdfButtonState();
                            showMovieDetails(newMovie);

                            String movieDate = extractDateString(newMovie.getShowDateTime());
                            boolean dateChanged = false;

                            if (MovieDate.getItems().contains(movieDate) && !movieDate.equals(MovieDate.getValue())) {
                                MovieDate.getSelectionModel().select(movieDate);
                                dateChanged = true;
                            }

                            if (!dateChanged) {
                                updateAvailableTimes(newMovie);
                            }
                        }
                    });

            MovieDate.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldDate, newDate) -> {
                        updatePdfButtonState();
                        MovieFromServer selectedMovie = moviesTable.getSelectionModel().getSelectedItem();
                        if (selectedMovie != null && newDate != null) {
                            updateAvailableTimes(selectedMovie);
                        }
                    });
        } else {
            System.out.println("ERROR: seatGrid is null!");
        }
        System.out.println("HIDE INSTANCE = " + System.identityHashCode(this));
    }

    private void showMovieDetails(MovieFromServer movie) {
        try {
            MovieDetails details = serverService.getMovieDetails(movie.getMovieId());

            javafx.application.Platform.runLater(() -> {
                titleLabel.setText(details.getTitle());
                directorLabel.setText(details.getDirector());
                descriptionArea.setText(details.getDescription());
                durationLabel.setText(details.getDuration() + " min");
                yearLabel.setText(String.valueOf(details.getPremiere()));
                actorsArea.setText(details.getActors());
            });

            new Thread(() -> {
                byte[] posterBytes = serverService.getMoviePoster(movie.getMovieId());
                boolean mtom = serverService.wasLastPosterMtom();
                javafx.application.Platform.runLater(() -> {
                    if (posterBytes != null && posterBytes.length > 0) {
                        coverImage.setImage(new Image(new ByteArrayInputStream(posterBytes)));
                    }
                    if (mtomStatusLabel != null) {
                        mtomStatusLabel.setText("Stan MTOM: " + (mtom ? "Aktywny" : "Nieaktywny"));
                        mtomStatusLabel.setTextFill(mtom
                                ? javafx.scene.paint.Color.web("#4fb3ff")
                                : javafx.scene.paint.Color.web("#aaa"));
                    }
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAvailableTimes(MovieFromServer movie) {

        currentSelectedTimeButton = null;
        timeButtonsContainer.getChildren().clear();

        String date = MovieDate.getValue();
        if (date == null)
            return;

        List<CinemaServerService.ShowtimeDto> showtimes = serverService.getShowtimes(movie.getMovieId(), date);

        if (showtimes == null || showtimes.isEmpty()) {
            System.out.println("NO SHOWTIMES FROM SERVER");
            return;
        }

        Button firstBtn = null;
        for (CinemaServerService.ShowtimeDto s : showtimes) {
            filmShowToMovieMap.put(s.getFilmShowId(), movie);
            Button btn = new Button(s.getShowDatetime().split("T")[1].substring(0, 5));
            btn.setPrefWidth(70);
            btn.setStyle("-fx-background-color: #121212; -fx-border-color: #313131; -fx-border-radius: 5;");
            btn.setTextFill(javafx.scene.paint.Color.WHITE);
            btn.setFont(javafx.scene.text.Font.font("Franklin Gothic Book", 14));
            btn.setUserData(s);
            btn.setOnAction(this::handleTimeSelection);

            timeButtonsContainer.getChildren().add(btn);
            if (firstBtn == null)
                firstBtn = btn;
        }

        if (firstBtn != null)
            firstBtn.fire();
    }

    private void setupMovies() {

        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        genreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenre()));

        dateCol.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getShowDateTime())));
    }

    private List<MovieFromServer> filterAndSort(List<MovieFromServer> list) {

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> seen = new HashSet<>();
        List<MovieFromServer> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);

        for (MovieFromServer m : list) {

            LocalDate date = extractDate(m);

            if (date.isBefore(today) || date.isAfter(end)) {
                continue;
            }

            String key = m.getTitle() + "_" + date;

            if (seen.add(key)) {
                result.add(m);
            }
        }

        result.sort(Comparator.comparing(this::extractDate));
        return result;
    }

    private LocalDate extractDate(MovieFromServer m) {
        return LocalDate.parse(extractDateString(m.getShowDateTime()));
    }

    private String extractDateString(String dateTime) {
        if (dateTime == null || dateTime.length() < 10) {
            return "";
        }

        return dateTime.substring(0, 10);
    }

    private String formatDate(String dateTime) {
        if (dateTime == null)
            return "";

        String date = extractDateString(dateTime); // 2025-04-28

        String[] parts = date.split("-");
        return parts[2] + "." + parts[1]; // 28.04
    }

    public void loadMovieDetails(int movieId) {
        try {
            CinemaServerService.MovieDetails movie = serverService.getMovieDetails(movieId);

            if (movie == null)
                return;

            titleLabel.setText(movie.getTitle());
            durationLabel.setText(movie.getDuration() + " min");
            directorLabel.setText("Reżyser: " + movie.getDirector());

            // PREMIERE (String → rok)
            if (movie.getPremiere() != null && movie.getPremiere().length() >= 4) {
                yearLabel.setText("Rok: " + movie.getPremiere().substring(0, 4));
            }

            actorsArea.setText(movie.getActors());
            descriptionArea.setText(movie.getDescription());

            // Poster via MTOM
            byte[] posterBytes = serverService.getMoviePoster(movieId);
            if (posterBytes != null && posterBytes.length > 0) {
                coverImage.setImage(new Image(new ByteArrayInputStream(posterBytes)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMovies() {

        List<MovieFromServer> filtered = filterAndSort(movies);

        moviesTable.setItems(
                FXCollections.observableArrayList(filtered));
    }

    private boolean isPast(String datetime) {
        try {
            return java.time.LocalDateTime.parse(datetime)
                    .isBefore(java.time.LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void loadReservations() {
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        List<CinemaServerService.UserReservationDto> reservations = serverService
                .getUserReservations(currentUser.getUserId());

        seatsListContainer1.getChildren().clear();

        for (var r : reservations) {

            VBox reservationBox = new VBox(5);
            reservationBox.setStyle(
                    "-fx-border-color: #313131; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #1a1a1a;");

            Label infoLabel = new Label(r.getTitle() + " | " + r.getShowDatetime());
            infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            String seatsText = r.getSeats();
            StringBuilder seatsSummary = new StringBuilder();

            if (seatsText != null && !seatsText.isEmpty()) {

                String[] parts = seatsText.split(",");

                for (int i = 0; i < parts.length - 1; i += 2) {
                    try {
                        String rowPart = parts[i].trim(); // "Rząd 6"
                        String seatPart = parts[i + 1].trim(); // "Miejsce 6"

                        int row = Integer.parseInt(rowPart.split(" ")[1]);
                        int col = Integer.parseInt(seatPart.split(" ")[1]);

                        char rowLetter = (char) ('A' + row - 1);

                        if (seatsSummary.length() > 0)
                            seatsSummary.append(", ");

                        seatsSummary.append(rowLetter).append(col);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Label seats = new Label("Miejsca: " + seatsSummary);
            seats.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");

            Button editBtn = new Button("Edytuj");
            Button deleteBtn = new Button("Usuń");

            editBtn.setStyle(editStyle);
            deleteBtn.setStyle(deleteStyle);

            HBox buttons = new HBox(10, editBtn, deleteBtn);
            buttons.setAlignment(Pos.CENTER);
            buttons.setPrefWidth(Double.MAX_VALUE);

            // 🔥 KLUCZOWE
            editBtn.setMaxWidth(Double.MAX_VALUE);
            deleteBtn.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(editBtn, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(deleteBtn, javafx.scene.layout.Priority.ALWAYS);

            reservationBox.getChildren().addAll(infoLabel, seats, buttons);

            int reservationId = r.getReservationId();

            // 🔥 DELETE działa od razu
            deleteBtn.setOnAction(e -> handleDeleteReservation(reservationId, reservationBox));

            // 🔴 EDIT — tylko jeśli masz filmShowId
            editBtn.setOnAction(e -> {

                this.editingReservationId = r.getReservationId();
                currentFilmShowId = r.getFilmShowId();

                refreshOccupancy();

                // 🔥 TU WKLEJ
                String[] seatsArr = r.getSeats().split(",");

                for (int i = 0; i < seatsArr.length - 1; i += 2) {
                    try {
                        String rowPart = seatsArr[i].trim();
                        String seatPart = seatsArr[i + 1].trim();

                        int row = Integer.parseInt(rowPart.split(" ")[1]);
                        int col = Integer.parseInt(seatPart.split(" ")[1]);

                        String key = row + "," + col;

                        seatController.preselectSeatColor(key);
                        addSeatToList(key, row, col);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            if (isPast(r.getShowDatetime())) {
                deleteBtn.setDisable(true);
                deleteBtn.setStyle(disabledStyle);
                editBtn.setDisable(true);
                editBtn.setStyle(disabledStyle);
            }
            seatsListContainer1.getChildren().add(reservationBox);
        }
    }

    private void showAuthOverlay() {
        authOverlay.setDisable(false);
        authOverlay.setVisible(true);
        authOverlay.setManaged(true);
        authOverlay.setMouseTransparent(false);
    }

    @FXML
    public void showRegisterForm() {
        showAuthOverlay();
        // Logika właściwa (wykona się tylko na mainInstance)
        if (loginForm != null) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);
        }
    }

    @FXML
    public void showLoginForm() {
        showAuthOverlay();
        System.out.println("Przełączam na logowanie...");
        if (loginForm != null && registerForm != null) {
            loginForm.setVisible(true);
            loginForm.setManaged(true);
            registerForm.setVisible(false);
            registerForm.setManaged(false);
        }
    }

    @FXML
    public void handleLogin() {
        String email = loginEmail.getText();
        String password = loginPassword.getText();

        String error = LoginController.validateLogin(email, password);
        if (error != null) {
            showAlert("Błąd", error);
            return;
        }

        CinemaServerService.UserLoginDto result = serverService.login(email, password);
        if (result != null && result.isSuccess()) {
            UserService.getInstance().setCurrentUser(
                    new User(result.getUserId(), result.getEmail(), result.getUserName()));
            hideAuthOverlay();
            loadReservations();
            updatePdfButtonState();

            if (moviesTable != null &&
                    moviesTable.getItems() != null &&
                    !moviesTable.getItems().isEmpty()) {

                MovieFromServer firstMovie = moviesTable.getItems().get(0);

                moviesTable.getSelectionModel().select(firstMovie);

                showMovieDetails(firstMovie);
                updateAvailableTimes(firstMovie);
            }
        } else {
            String errMsg = (result != null && result.getErrorMessage() != null && !result.getErrorMessage().isBlank())
                    ? result.getErrorMessage()
                    : "Nieprawidłowe dane logowania";
            showAlert("Błąd logowania", errMsg);
        }
    }

    @FXML
    public void handleRegister() {
        String name = registerName.getText();
        String surname = registerSurname != null ? registerSurname.getText() : "";
        String email = registerEmail.getText();
        String pass = registerPassword.getText();
        String confirm = registerConfirmPassword.getText();

        String error = RegisterController.validateRegister(name, surname, email, pass, confirm);
        if (error != null) {
            showAlert("Błąd", error);
            return;
        }

        CinemaServerService.RegisterResultDto result = serverService.register(name, surname, email, pass, confirm);
        if (result != null && result.isSuccess()) {
            UserService.getInstance().setCurrentUser(
                    new User(result.getUserId(), result.getEmail(),
                            (result.getName() != null ? result.getName() : name) + " " +
                                    (result.getSurname() != null ? result.getSurname() : surname)));
            hideAuthOverlay();
        } else {
            String errMsg = (result != null && result.getErrorMessage() != null && !result.getErrorMessage().isBlank())
                    ? result.getErrorMessage()
                    : "Błąd rejestracji. Sprawdź dane i spróbuj ponownie.";
            showAlert("Błąd rejestracji", errMsg);
        }
    }

    @FXML
    public void handleLogout() {
        UserService.getInstance().logout();
        loginEmail.clear();
        loginPassword.clear();
        showLoginForm();
    }

    private void hideAuthOverlay() {
        authOverlay.setVisible(false);
        authOverlay.setManaged(false);
        authOverlay.setMouseTransparent(true);
        authOverlay.setDisable(true);
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleTimeSelection(ActionEvent event) {

        Button btn = (Button) event.getSource();
        Object data = btn.getUserData();

        if (!(data instanceof CinemaServerService.ShowtimeDto showtime)) {
            System.out.println("ERROR: brak ShowtimeDto w buttonie");
            return;
        }

        if (currentSelectedTimeButton != null) {
            resetButtonToDefault(currentSelectedTimeButton);
        }
        btn.setStyle("-fx-background-color: #0078D7; -fx-border-color: #0078D7; -fx-border-radius: 5;");
        currentSelectedTimeButton = btn;

        selectedTime = btn.getText();
        currentFilmShowId = showtime.getFilmShowId();

        if (screenLabel != null) {
            int sid = showtime.getScreenId();
            screenLabel.setText("Plan Widowni: Sala " + (sid > 0 ? sid : "?"));
        }
        activeMovie = filmShowToMovieMap.get(showtime.getFilmShowId());
        updatePdfButtonState();
        refreshOccupancy();
    }

    private void refreshOccupancy() {

        if (currentFilmShowId == -1)
            return;

        seatController.resetAllSeatsToFree();
        seatsListContainer.getChildren().clear();
        selectedSeatKeys.clear();
        seatIdMap.clear();

        List<CinemaServerService.SeatDto> seats = serverService.getSeats(currentFilmShowId);

        if (seats == null)
            return;

        Set<String> occupied = new HashSet<>();

        for (CinemaServerService.SeatDto s : seats) {

            String key = s.getRowNum() + "," + s.getNumber();
            seatIdMap.put(key, s.getSeatId());

            if (s.isTaken() &&
                    (editingSeatKeys == null || !editingSeatKeys.contains(key))) {
                occupied.add(key);
            }
        }

        seatController.markOccupiedOnGrid(occupied);
    }

    @FXML
    private void handleConfirmReservation(ActionEvent event) {
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Błąd", "Musisz być zalogowany, aby dokonać rezerwacji.");
            return;
        }

        String currentDate = MovieDate.getValue();
        if (selectedSeatKeys.isEmpty() || selectedTime.isEmpty() || currentDate == null || currentFilmShowId == -1)
            return;

        List<Integer> seatIds = new ArrayList<>();
        for (String key : selectedSeatKeys) {
            Integer seatId = seatIdMap.get(key);
            if (seatId != null)
                seatIds.add(seatId);
        }

        if (seatIds.isEmpty()) {
            showAlert("Błąd", "Nie można ustalić identyfikatorów miejsc. Odśwież siedzenia.");
            return;
        }

        int reservationId;

        if (editingReservationId != -1) {
            // ✏️ EDYCJA
            boolean ok = serverService.updateReservation(
                    currentUser.getUserId(),
                    editingReservationId,
                    currentFilmShowId,
                    seatIds);

            if (!ok) {
                showAlert("Błąd", "Nie udało się zaktualizować rezerwacji.");
                return;
            }
            reservationId = editingReservationId;
            editingReservationId = -1;
            loadReservations();

        } else {
            // ➕ NOWA
            CinemaServerService.ReservationCreateResultDto result = serverService.createReservation(
                    currentUser.getUserId(),
                    currentFilmShowId,
                    seatIds);

            if (result == null) {
                showAlert("Błąd", "Nie udało się zapisać rezerwacji.");
                return;
            }

            reservationId = result.getReservationId(); // <-- TU OK
        }
        final int filmShowId = currentFilmShowId;
        final String capturedTime = selectedTime;
        final Set<String> capturedSeatKeys = new HashSet<>(selectedSeatKeys);
        final List<Integer> capturedSeatIds = new ArrayList<>(seatIds);

        MovieFromServer selectedMovie = activeMovie;
        String movieTitle = (selectedMovie != null) ? selectedMovie.getTitle()
                : (titleLabel != null ? titleLabel.getText() : "Film");

        StringBuilder seatsSummary = new StringBuilder();
        for (String key : selectedSeatKeys) {
            String[] parts = key.split(",");
            char rowLetter = (char) ('A' + Integer.parseInt(parts[0]) - 1);
            if (seatsSummary.length() > 0)
                seatsSummary.append(", ");
            seatsSummary.append(rowLetter).append(parts[1]);
        }

        VBox reservationBox = new VBox(5);
        reservationBox.setStyle(
                "-fx-border-color: #313131; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #1a1a1a;");

        Label infoLabel = new Label(movieTitle + " | " + currentDate + " | Godz: " + selectedTime);
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label seatsLabel = new Label("Miejsca: " + seatsSummary);
        seatsLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");

        Button editBtn = new Button("Edytuj");
        editBtn.setStyle(
                "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-size: 10; -fx-background-radius: 4;");

        Button deleteBtn = new Button("Usuń");
        deleteBtn.setStyle(
                "-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 10; -fx-background-radius: 4;");

        HBox buttons = new HBox(10, editBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPrefWidth(Double.MAX_VALUE);

        // 🔥 KLUCZOWE
        editBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(editBtn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, javafx.scene.layout.Priority.ALWAYS);
        reservationBox.getChildren().addAll(infoLabel, seatsLabel, buttons);
        deleteBtn.setOnAction(e -> handleDeleteReservation(reservationId, reservationBox));
        editBtn.setOnAction(e -> enterEditMode(
                reservationId, filmShowId, capturedSeatKeys, capturedSeatIds, reservationBox, capturedTime));

        seatsListContainer1.getChildren().add(reservationBox);

        seatController.markSelectedAsOccupied(selectedSeatKeys);
        seatsListContainer.getChildren().clear();
        selectedSeatKeys.clear();
        editingSeatKeys.clear();
        resetButtonToDefault(currentSelectedTimeButton);
        selectedTime = "";
        refreshOccupancy();
    }

    private void handleDeleteReservation(int reservationId, VBox box) {
        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        boolean ok = serverService.deleteReservation(currentUser.getUserId(), reservationId);
        if (ok) {
            seatsListContainer1.getChildren().remove(box);
            refreshOccupancy();
        } else {
            showAlert("Błąd", "Nie udało się usunąć rezerwacji. Seans mógł już minąć.");
        }
    }

    private void enterEditMode(int reservationId,
            int filmShowId,
            Set<String> seatKeys,
            List<Integer> seatIds,
            VBox box,
            String time) {

        this.editingReservationId = reservationId;
        this.currentFilmShowId = filmShowId;
        this.selectedTime = time;
        this.editingSeatKeys = new HashSet<>(seatKeys);

        seatsListContainer1.getChildren().remove(box);

        MovieFromServer movie = filmShowToMovieMap.get(filmShowId);

        if (movie != null) {
            moviesTable.getSelectionModel().select(movie);
            showMovieDetails(movie);
            updateAvailableTimes(movie);
        }

        selectedSeatKeys.clear();
        seatsListContainer.getChildren().clear();

        for (String key : seatKeys) {
            String[] parts = key.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            selectedSeatKeys.add(key);
            seatController.preselectSeatColor(key);
            addSeatToList(key, row, col);
        }

        refreshOccupancy();
    }
    // Pozostałe metody pomocnicze (addSeatToList, removeSeatFromList,
    // handleSeatClick, resetButtonToDefault) pozostają bez zmian...

    @FXML
    private void handleSeatClick(ActionEvent event) {
        seatController.processSeatClick(event);
    }

    public void addSeatToList(String seatKey, int row, int col) {
        if (selectedSeatKeys.contains(seatKey))
            return;
        String rowLetter = String.valueOf((char) ('A' + row - 1));
        Label seatLabel = new Label("Rząd: " + rowLetter + " miejsce " + col);
        seatLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
        Button removeButton = new Button("Usuń");
        removeButton.setStyle(
                "-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 10; -fx-background-radius: 4;");
        removeButton.setOnAction(e -> {
            seatController.resetSingleSeatColor(seatKey);
            removeSeatFromList(seatKey);
        });
        HBox rowBox = new HBox(8, seatLabel, removeButton);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setUserData(seatKey);
        seatsListContainer.getChildren().add(rowBox);
        selectedSeatKeys.add(seatKey);
        updatePdfButtonState();

    }

    public void removeSeatFromList(String seatKey) {
        selectedSeatKeys.remove(seatKey);
        seatsListContainer.getChildren().removeIf(node -> seatKey.equals(node.getUserData()));
        updatePdfButtonState();

    }

    private void resetButtonToDefault(Button btn) {
        if (btn != null)
            btn.setStyle(
                    "-fx-background-color: #121212; -fx-border-color: #313131; -fx-border-radius: 5; -fx-text-fill: white; -fx-font-weight: normal;");
    }

    @FXML
    private void handleDownloadPdf(ActionEvent event) {

        User currentUser = UserService.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Błąd", "Musisz być zalogowany.");
            return;
        }

        // jeśli masz zaznaczoną rezerwację / aktualną
        if (editingReservationId == -1) {
            showAlert("Błąd", "Wybierz rezerwację do pobrania PDF.");
            return;
        }

        new Thread(() -> {
            try {
                byte[] pdf = serverService.getReservationPdf(editingReservationId);

                System.out.println("PDF SIZE: " + pdf.length);
                if (pdf == null || pdf.length == 0) {
                    javafx.application.Platform.runLater(() -> showAlert("Błąd", "Nie udało się pobrać PDF"));
                    return;
                }

                java.nio.file.Path path = java.nio.file.Paths.get(
                        System.getProperty("user.home"),
                        "Downloads",
                        "bilet_" + editingReservationId + ".pdf");

                java.nio.file.Files.write(path, pdf);

                javafx.application.Platform.runLater(() -> showAlert("Sukces", "PDF zapisany: " + path));

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> showAlert("Błąd", "Błąd pobierania PDF"));
            }
        }).start();
    }

    private boolean canReserve() {
        return currentFilmShowId != -1
                && !selectedSeatKeys.isEmpty()
                && !selectedTime.isEmpty();
    }

    private void updatePdfButtonState() {
        downloadPdfButton.setDisable(!canDownloadPdf());
    }

    private boolean canDownloadPdf() {
        return canReserve();
    }
}
