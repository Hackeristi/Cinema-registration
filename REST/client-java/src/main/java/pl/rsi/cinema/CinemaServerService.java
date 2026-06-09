package pl.rsi.cinema;

import pl.rsi.cinema.dto.MovieFromServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CinemaServerService {

    private static final String SERVER_URL = "https://nearest-crouch-liver.ngrok-free.dev/api";

    private static CinemaServerService instance;

    public static CinemaServerService getInstance() {
        if (instance == null) {
            instance = new CinemaServerService();
        }
        return instance;
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean lastPosterWasMtom = false;

    public MovieDetails getMovieDetailsFromReservation(int reservationId) {
        return null;
    }

    public boolean wasLastPosterMtom() {
        return lastPosterWasMtom;
    }

    public boolean isServerReachable() {
        try {
            String body = sendJsonGet("/movies");
            return body != null && !body.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    public List<MovieFromServer> getMovies() {
        try {
            String body = sendJsonGet("/movies");
            JsonArray array = JsonParser.parseString(body).getAsJsonArray();

            List<MovieFromServer> list = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject movie = element.getAsJsonObject();
                int movieId = getInt(movie, "movieId");
                int showId = getInt(movie, "showId");
                String title = getString(movie, "title");
                String genre = getString(movie, "genre");
                String showDateTime = getString(movie, "showDatetime");
                list.add(new MovieFromServer(showId, movieId, title, genre, showDateTime));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("getMovies failed: " + e.getMessage(), e);
        }
    }

    public MovieDetails getMovieDetails(int movieId) {
        try {
            String body = sendJsonGetWithRetry("/movies/" + movieId);
            JsonObject movie = JsonParser.parseString(body).getAsJsonObject();

            String actors = "";
            JsonElement actorsElement = movie.get("actors");
            if (actorsElement != null && actorsElement.isJsonArray()) {
                JsonArray actorsArray = actorsElement.getAsJsonArray();
                List<String> actorNames = new ArrayList<>();
                for (JsonElement element : actorsArray) {
                    String actorName = element.getAsString();
                    if (actorName != null && !actorName.isBlank()) {
                        actorNames.add(actorName);
                    }
                }
                actors = String.join("\n", actorNames);
            }

            return new MovieDetails(
                    getString(movie, "title"),
                    getString(movie, "description"),
                    getString(movie, "director"),
                    actors,
                    getInt(movie, "duration"),
                    String.valueOf(getInt(movie, "premiere")),
                    getString(movie, "poster"));
        } catch (Exception e) {
            throw new RuntimeException("getMovieDetails failed: " + e.getMessage(), e);
        }
    }

    public byte[] getMoviePoster(int movieId) {
        int delayMs = 1000;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(buildUrl("/movies/" + movieId + "/poster")))
                        .header("Accept", "image/jpeg")
                        .header("ngrok-skip-browser-warning", "true")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                    continue;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("getMoviePoster failed: HTTP error: " + response.statusCode());
                }
                lastPosterWasMtom = false;
                return response.body();
            } catch (RuntimeException e) {
                if (attempt == 3)
                    throw e;
            } catch (Exception e) {
                throw new RuntimeException("getMoviePoster failed: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("getMoviePoster failed: max retries exceeded");
    }

    public int getMovieId(int filmShowId) {
        try {
            for (MovieFromServer movie : getMovies()) {
                if (movie.getShowId() == filmShowId) {
                    return movie.getMovieId();
                }
            }
        } catch (Exception ignored) {
            // fallback to zero if the server is unavailable
        }
        return 0;
    }

    public static class MovieDetails {
        private final String title;
        private final String description;
        private final String director;
        private final String actors;
        private final String genre;
        private final int duration;
        private final String premiere;
        private final String posterBase64;

        public MovieDetails(String title, String description, String director,
                String actors, int duration,
                String premiere, String posterBase64) {
            this(title, description, director, actors, "", duration, premiere, posterBase64);
        }

        public MovieDetails(String title, String description, String director,
                String actors, String genre, int duration,
                String premiere, String posterBase64) {
            this.title = title;
            this.description = description;
            this.director = director;
            this.actors = actors;
            this.genre = genre;
            this.duration = duration;
            this.premiere = premiere;
            this.posterBase64 = posterBase64;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getDirector() {
            return director;
        }

        public String getActors() {
            return actors;
        }

        public String getGenre() {
            return genre;
        }

        public int getDuration() {
            return duration;
        }

        public String getPremiere() {
            return premiere;
        }

        public String getPoster() {
            return posterBase64;
        }
    }

    public List<ShowtimeDto> getShowtimes(int movieId, String date) {
        try {
            String formatted = normalizeDateForRest(date);
            String path = "/movies/" + movieId + "/showtimes?date="
                    + URLEncoder.encode(formatted, StandardCharsets.UTF_8);
            String body = sendJsonGetWithRetry(path);
            JsonArray array = JsonParser.parseString(body).getAsJsonArray();

            List<ShowtimeDto> list = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject showtime = element.getAsJsonObject();
                int id = getInt(showtime, "filmShowId");
                String dt = getString(showtime, "showDatetime");
                int screenId = getInt(showtime, "screenId");
                list.add(new ShowtimeDto(id, dt, screenId));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("getShowtimes failed: " + e.getMessage(), e);
        }
    }

    public List<SeatDto> getSeats(int filmShowId) {
        try {
            String body = sendJsonGetWithRetry("/showtimes/" + filmShowId + "/seats");
            JsonArray array = JsonParser.parseString(body).getAsJsonArray();

            List<SeatDto> list = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject seat = element.getAsJsonObject();
                int seatId = getInt(seat, "seatId");
                int number = getInt(seat, "number");
                int row = getInt(seat, "rowNum");
                boolean taken = getBoolean(seat, "isTaken");
                list.add(new SeatDto(seatId, number, row, taken));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("getSeats failed: " + e.getMessage(), e);
        }
    }

    public UserLoginDto login(String email, String password) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("userId", 0);
            payload.addProperty("userName", "");
            payload.addProperty("email", email);
            payload.addProperty("password", password);
            payload.addProperty("errorMessage", "");

            String body = sendJsonPost("/auth/login", payload.toString());
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            return new UserLoginDto(
                    getInt(object, "userId"),
                    getString(object, "email"),
                    getString(object, "userName"),
                    getString(object, "errorMessage"));
        } catch (Exception e) {
            throw new RuntimeException("login failed: " + e.getMessage(), e);
        }
    }

    public RegisterResultDto register(String name, String surname, String email, String password,
            String confirmPassword) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("name", name);
            payload.addProperty("surname", surname);
            payload.addProperty("email", email);
            payload.addProperty("password", password);
            payload.addProperty("confirmPassword", confirmPassword);
            payload.addProperty("errorMessage", "");

            String body = sendJsonPost("/auth/register", payload.toString());
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            return new RegisterResultDto(
                    getInt(object, "userId"),
                    getString(object, "email"),
                    getString(object, "name"),
                    getString(object, "surname"),
                    getString(object, "errorMessage"));
        } catch (Exception e) {
            throw new RuntimeException("register failed: " + e.getMessage(), e);
        }
    }

    public List<UserReservationDto> getUserReservations(int userId) {
        try {
            String body = sendJsonGetWithRetry("/users/" + userId + "/reservations");
            JsonArray array = JsonParser.parseString(body).getAsJsonArray();

            List<UserReservationDto> list = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject reservation = element.getAsJsonObject();
                int reservationId = getInt(reservation, "reservationId");
                String title = getString(reservation, "title");
                String showDatetime = getString(reservation, "showDatetime");
                int filmShowId = getInt(reservation, "filmShowId");

                StringBuilder seatsBuilder = new StringBuilder();
                JsonElement seatsElement = reservation.get("takenSeats");
                if (seatsElement != null && seatsElement.isJsonArray()) {
                    JsonArray seatsArray = seatsElement.getAsJsonArray();
                    for (int i = 0; i < seatsArray.size(); i++) {
                        if (i > 0) {
                            seatsBuilder.append(", ");
                        }
                        seatsBuilder.append(seatsArray.get(i).getAsString());
                    }
                }

                list.add(new UserReservationDto(reservationId, title, showDatetime, filmShowId,
                        seatsBuilder.toString()));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("getUserReservations failed: " + e.getMessage(), e);
        }
    }

    public ReservationCreateResultDto createReservation(int userId, int filmShowId, List<Integer> seatIds) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("filmShowId", filmShowId);
            JsonArray seatsArray = new JsonArray();
            for (Integer seatId : seatIds) {
                seatsArray.add(seatId);
            }
            payload.add("selectedSeats", seatsArray);

            String body = sendJsonPost("/users/" + userId + "/reservations", payload.toString());
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            int reservationId = getInt(object, "reservationId");
            List<String> seatKeys = new ArrayList<>();
            for (Integer seatId : seatIds) {
                seatKeys.add(String.valueOf(seatId));
            }
            return new ReservationCreateResultDto(reservationId, seatKeys);
        } catch (Exception e) {
            throw new RuntimeException("createReservation failed: " + e.getMessage(), e);
        }
    }

    public boolean deleteReservation(int userId, int reservationId) {
        try {
            sendJsonDelete("/users/" + userId + "/reservations/" + reservationId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("deleteReservation failed: " + e.getMessage(), e);
        }
    }

    public boolean updateReservation(int userId, int reservationId, int newFilmShowId, List<Integer> newSeatIds) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("newFilmShowId", newFilmShowId);
            JsonArray seatsArray = new JsonArray();
            for (Integer seatId : newSeatIds) {
                seatsArray.add(seatId);
            }
            payload.add("newSeats", seatsArray);

            sendJsonPut("/users/" + userId + "/reservations/" + reservationId, payload.toString());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("updateReservation failed: " + e.getMessage(), e);
        }
    }

    public byte[] getReservationPdf(int reservationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl("/reservations/" + reservationId + "/pdf")))
                    .header("ngrok-skip-browser-warning", "true")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("PDF error: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("getReservationPdf failed: " + e.getMessage(), e);
        }
    }

    public static class UserReservationDto {
        private final int reservationId;
        private final String title;
        private final String showDatetime;
        private final int filmShowId;
        private final String seats;

        public UserReservationDto(int reservationId, String title,
                String showDatetime, int filmShowId, String seats) {
            this.reservationId = reservationId;
            this.title = title;
            this.showDatetime = showDatetime;
            this.filmShowId = filmShowId;
            this.seats = seats;
        }

        public int getReservationId() {
            return reservationId;
        }

        public String getTitle() {
            return title;
        }

        public String getShowDatetime() {
            return showDatetime;
        }

        public int getFilmShowId() {
            return filmShowId;
        }

        public String getSeats() {
            return seats;
        }
    }

    public class ReservationCreateResultDto {
        private int reservationId;
        private List<String> seatKeys;

        public ReservationCreateResultDto(int reservationId, List<String> seatKeys) {
            this.reservationId = reservationId;
            this.seatKeys = seatKeys;
        }

        public int getReservationId() {
            return reservationId;
        }

        public List<String> getSeatKeys() {
            return seatKeys;
        }
    }

    private String sendJsonGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(path)))
                .header("Accept", "application/json")
                .header("ngrok-skip-browser-warning", "true")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("REST error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private String sendJsonPost(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(path)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("ngrok-skip-browser-warning", "true")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("REST error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private String sendJsonPut(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(path)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("ngrok-skip-browser-warning", "true")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("REST error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private String sendJsonDelete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(path)))
                .header("Accept", "application/json")
                .header("ngrok-skip-browser-warning", "true")
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("REST error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private String buildUrl(String path) {
        String base = System.getProperty("cinema.server.url", SERVER_URL);
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }

    private String normalizeDateForRest(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Date is empty");
        }

        String trimmed = date.trim();
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return trimmed;
        }

        String[] parts = trimmed.split("\\.");
        if (parts.length == 3) {
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }

        throw new IllegalArgumentException("Unsupported date format: " + date);
    }

    private int getInt(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return 0;
        }
        return element.getAsInt();
    }

    private String getString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    private boolean getBoolean(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return false;
        }
        return element.getAsBoolean();
    }

    public static class ShowtimeDto {
        private final int filmShowId;
        private final String showDatetime;
        private final int screenId;

        public ShowtimeDto(int filmShowId, String showDatetime, int screenId) {
            this.filmShowId = filmShowId;
            this.showDatetime = showDatetime;
            this.screenId = screenId;
        }

        public int getFilmShowId() {
            return filmShowId;
        }

        public String getShowDatetime() {
            return showDatetime;
        }

        public int getScreenId() {
            return screenId;
        }
    }

    public static class SeatDto {
        private final int seatId;
        private final int number;
        private final int rowNum;
        private final boolean isTaken;

        public SeatDto(int seatId, int number, int rowNum, boolean isTaken) {
            this.seatId = seatId;
            this.number = number;
            this.rowNum = rowNum;
            this.isTaken = isTaken;
        }

        public int getSeatId() {
            return seatId;
        }

        public int getNumber() {
            return number;
        }

        public int getRowNum() {
            return rowNum;
        }

        public boolean isTaken() {
            return isTaken;
        }
    }

    public static class UserLoginDto {
        private final int userId;
        private final String email;
        private final String userName;
        private final String errorMessage;

        public UserLoginDto(int userId, String email, String userName, String errorMessage) {
            this.userId = userId;
            this.email = email;
            this.userName = userName;
            this.errorMessage = errorMessage;
        }

        public int getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getUserName() {
            return userName;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null || errorMessage.isBlank();
        }
    }

    public static class RegisterResultDto {
        private final int userId;
        private final String email;
        private final String name;
        private final String surname;
        private final String errorMessage;

        public RegisterResultDto(int userId, String email, String name, String surname, String errorMessage) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.surname = surname;
            this.errorMessage = errorMessage;
        }

        public int getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null || errorMessage.isBlank();
        }
    }

    private String sendJsonGetWithRetry(String path) throws Exception {
        int delayMs = 3000;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                return sendJsonGet(path);
            } catch (RuntimeException e) {
                boolean is429 = e.getMessage() != null && e.getMessage().contains("429");
                if (is429 && attempt < 3) {
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Max retries exceeded for: " + path);
    }
}
