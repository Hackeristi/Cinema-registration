
package pl.rsi.cinema;

import pl.rsi.cinema.dto.MovieFromServer;

import java.net.http.*;
import java.net.URI;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CinemaServerService {

    private static final String SERVER_URL = "https://nearest-crouch-liver.ngrok-free.dev/CinemaService.asmx";

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

        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovieDetailsFromReservation xmlns=\"http://tempuri.org/\">" +
                    "<reservationId>" + reservationId + "</reservationId>" +
                    "</GetMovieDetailsFromReservation>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetMovieDetailsFromReservation", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            Element res = (Element) doc.getElementsByTagNameNS("*", "GetMovieDetailsFromReservationResult").item(0);

            if (res == null)
                return null;

            // ACTORS parsing (jeśli są jak w GetMovieDetails)
            StringBuilder actorsBuilder = new StringBuilder();
            NodeList actors = doc.getElementsByTagNameNS("*", "string");

            for (int i = 0; i < actors.getLength(); i++) {
                if (i > 0)
                    actorsBuilder.append("\n");
                actorsBuilder.append(actors.item(i).getTextContent());
            }

            return new MovieDetails(
                    getValue(res, "Title"),
                    getValue(res, "Description"),
                    getValue(res, "Director"),
                    actorsBuilder.toString(),
                    Integer.parseInt(getValue(res, "Duration")),
                    getValue(res, "Genre"),
                    getValue(res, "Premiere"),
                    getValue(res, "Poster"));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean wasLastPosterMtom() {
        return lastPosterWasMtom;
    }

    // =========================================================
    // 🆕 GetMovieId by showId
    // =========================================================
    public int getMovieId(int filmshowId) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovieId xmlns=\"http://tempuri.org/\">" +
                    "<filmshowId>" + filmshowId + "</filmshowId>" +
                    "</GetMovieId>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetMovieId", soap);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            Element result = (Element) doc.getElementsByTagNameNS("*", "GetMovieIdResult").item(0);
            if (result == null)
                return -1;
            return Integer.parseInt(result.getTextContent().trim());
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // ---------------------------
    // SOAP REQUEST HELPER
    // ---------------------------
    private String sendSoap(String action, String body) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://tempuri.org/" + action)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }

        throw new RuntimeException("SOAP error: " + response.statusCode() + "\n" + response.body());
    }

    // ---------------------------
    // PARSER HELPER
    // ---------------------------

    private String getValue(Element e, String tag) {
        NodeList list = e.getElementsByTagNameNS("*", tag);

        if (list.getLength() == 0 || list.item(0) == null)
            return null;

        return list.item(0).getTextContent();
    }

    // =========================================================
    // 🔌 SERVER STATUS
    // =========================================================
    public boolean isServerReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "http://tempuri.org/GetMovies")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                    "<soap:Body><GetMovies xmlns=\"http://tempuri.org/\" /></soap:Body>" +
                                    "</soap:Envelope>"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // 🎬 MOVIES LIST
    // =========================================================
    public List<MovieFromServer> getMovies() {

        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovies xmlns=\"http://tempuri.org/\" />" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetMovies", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList moviesNodes = doc.getElementsByTagName("d4p1:MovieDto");
            List<MovieFromServer> list = new ArrayList<>();

            for (int i = 0; i < moviesNodes.getLength(); i++) {

                Element e = (Element) moviesNodes.item(i);

                String showIdStr = getValue(e, "ShowId");

                if (showIdStr == null)
                    continue;

                int movieId = Integer.parseInt(getValue(e, "MovieId"));
                int showId = Integer.parseInt(getValue(e, "ShowId"));

                String title = getValue(e, "Title");
                String genre = getValue(e, "Genre");
                String date = getValue(e, "ShowDatetime");

                // MovieId NIE MA w tym endpointcie
                list.add(new MovieFromServer(showId, movieId, title, genre, date));
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("getMovies failed: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🎬 MOVIE DETAILS (NAJWAŻNIEJSZE)
    // =========================================================
    public MovieDetails getMovieDetails(int movieId) {
        try {
            // Przygotowanie żądania SOAP
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMovieDetails xmlns=\"http://tempuri.org/\">" +
                    "<movieId>" + movieId + "</movieId>" +
                    "</GetMovieDetails>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            // Wysłanie zapytania i odebranie XML
            String xml = sendSoap("GetMovieDetails", soap);

            // Parsowanie dokumentu XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            // --- SEKCJA: AKTORZY (FORMATOWANIE PIONOWE) ---
            StringBuilder actorsBuilder = new StringBuilder();
            NodeList actorsContainer = doc.getElementsByTagNameNS("*", "Actors");

            if (actorsContainer.getLength() > 0) {
                Element container = (Element) actorsContainer.item(0);
                // Wyciągamy wszystkie napisy z tagów <string>
                NodeList stringNodes = container.getElementsByTagNameNS("*", "string");

                for (int i = 0; i < stringNodes.getLength(); i++) {
                    String actorName = stringNodes.item(i).getTextContent();
                    if (actorName != null && !actorName.trim().isEmpty()) {
                        // Jeśli to nie pierwszy element, dodaj enter przed kolejnym
                        if (actorsBuilder.length() > 0) {
                            actorsBuilder.append("\n");
                        }
                        actorsBuilder.append(actorName.trim());
                    }
                }
            }

            String finalActors = actorsBuilder.toString();

            // --- SEKCJA: GŁÓWNE DANE O FILMIE ---
            // Szukamy kontenera z wynikiem (GetMovieDetailsResult)
            Element res = (Element) doc.getElementsByTagNameNS("*", "GetMovieDetailsResult").item(0);

            if (res == null) {
                return null;
            }

            // Zwracamy nowy obiekt ze wszystkimi danymi
            return new MovieDetails(
                    getValue(res, "Title"),
                    getValue(res, "Description"),
                    getValue(res, "Director"),
                    finalActors,
                    Integer.parseInt(getValue(res, "Duration")),
                    getValue(res, "Genre"),
                    getValue(res, "Premiere"),
                    getValue(res, "Poster"));

        } catch (Exception e) {
            System.err.println("Błąd podczas przetwarzania szczegółów filmu (ID: " + movieId + "):");
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // 🖼 MOVIE POSTER (MTOM)
    // =========================================================
    public byte[] getMoviePoster(int movieId) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetMoviePoster xmlns=\"http://tempuri.org/\">" +
                    "<movieId>" + movieId + "</movieId>" +
                    "</GetMoviePoster>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "http://tempuri.org/GetMoviePoster")
                    .POST(HttpRequest.BodyPublishers.ofString(soap))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP error: " + response.statusCode());
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (contentType.contains("multipart/related")) {
                lastPosterWasMtom = true;
                return extractMtomBinaryPart(response.body(), contentType);
            } else {
                lastPosterWasMtom = false;
                // Fallback: Base64 in regular XML response
                String xml = new String(response.body(), "UTF-8");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
                Element result = (Element) doc.getElementsByTagNameNS("*", "GetMoviePosterResult").item(0);
                if (result == null)
                    return null;
                return Base64.getDecoder().decode(result.getTextContent().trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] extractMtomBinaryPart(byte[] body, String contentType) throws Exception {
        String boundary = null;
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.startsWith("boundary=")) {
                boundary = param.substring(9).replace("\"", "").trim();
            }
        }
        if (boundary == null)
            throw new RuntimeException("No MIME boundary in Content-Type");

        byte[] delimiter = ("\r\n--" + boundary).getBytes("UTF-8");
        byte[] firstDelimiter = ("--" + boundary).getBytes("UTF-8");

        List<byte[]> parts = new ArrayList<>();
        int start = indexOfBytes(body, firstDelimiter, 0);
        if (start == -1)
            throw new RuntimeException("MIME boundary not found in response body");
        start += firstDelimiter.length;

        while (start < body.length) {
            if (start + 1 < body.length && body[start] == '\r' && body[start + 1] == '\n')
                start += 2;
            else if (start < body.length && body[start] == '\n')
                start += 1;

            int end = indexOfBytes(body, delimiter, start);
            if (end == -1)
                break;
            parts.add(Arrays.copyOfRange(body, start, end));
            start = end + delimiter.length;
            if (start + 1 < body.length && body[start] == '-' && body[start + 1] == '-')
                break;
        }

        for (byte[] part : parts) {
            int headerEnd = indexOfCrLfCrLf(part);
            if (headerEnd == -1)
                continue;
            String headers = new String(part, 0, headerEnd, "UTF-8");
            if (!headers.contains("application/xop+xml") && !headers.contains("text/xml")) {
                int dataStart = headerEnd + 4;
                return Arrays.copyOfRange(part, dataStart, part.length);
            }
        }
        throw new RuntimeException("No binary attachment found in MTOM response");
    }

    private int indexOfBytes(byte[] source, byte[] target, int fromIndex) {
        outer: for (int i = fromIndex; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j])
                    continue outer;
            }
            return i;
        }
        return -1;
    }

    private int indexOfCrLfCrLf(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    // =========================================================
    // DTOs
    // =========================================================

    public static class MovieDetails {
        private final String title;
        private final String description;
        private final String director;
        private final String actors;
        private final int duration;
        private final String genre;
        private final String premiere;
        private final String posterBase64;

        public MovieDetails(String title, String description, String director,
                String actors, int duration, String genre,
                String premiere, String posterBase64) {
            this.title = title;
            this.description = description;
            this.director = director;
            this.actors = actors;
            this.duration = duration;
            this.genre = genre;
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

        public int getDuration() {
            return duration;
        }

        public String getPremiere() {
            return premiere;
        }

        public String getPoster() {
            return posterBase64;
        }

        public String getGenre() {
            return genre;
        }
    }

    public List<ShowtimeDto> getShowtimes(int movieId, String date) {
        try {

            String formatted = normalizeDateForSoap(date);

            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetShowtimes xmlns=\"http://tempuri.org/\">" +
                    "<movieId>" + movieId + "</movieId>" +
                    "<date>" + formatted + "</date>" +
                    "</GetShowtimes>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetShowtimes", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList nodes = doc.getElementsByTagNameNS("*", "ShowtimeDto");

            List<ShowtimeDto> list = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {

                Element e = (Element) nodes.item(i);

                int id = Integer.parseInt(getValue(e, "FilmShowId"));
                String dt = getValue(e, "ShowDatetime");
                String screenIdStr = getValue(e, "ScreenId");
                int screenId = (screenIdStr != null) ? Integer.parseInt(screenIdStr) : 0;

                list.add(new ShowtimeDto(id, dt, screenId));
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private String normalizeDateForSoap(String date) {
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

    // =========================================================
    // 🔐 AUTH: LOGIN
    // =========================================================
    public UserLoginDto login(String email, String password) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<Login xmlns=\"http://tempuri.org/\">" +
                    "<email>" + escapeXml(email) + "</email>" +
                    "<password>" + escapeXml(password) + "</password>" +
                    "</Login>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("Login", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            Element result = (Element) doc.getElementsByTagNameNS("*", "LoginResult").item(0);
            if (result == null)
                return null;

            String userIdStr = getValue(result, "UserId");
            int userId = (userIdStr != null && !userIdStr.isBlank()) ? Integer.parseInt(userIdStr) : 0;

            return new UserLoginDto(
                    userId,
                    getValue(result, "Email"),
                    getValue(result, "UserName"),
                    getValue(result, "ErrorMessage"));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // 🔐 AUTH: REGISTER
    // =========================================================
    public RegisterResultDto register(String name, String surname, String email, String password,
            String confirmPassword) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<Register xmlns=\"http://tempuri.org/\">" +
                    "<name>" + escapeXml(name) + "</name>" +
                    "<surname>" + escapeXml(surname) + "</surname>" +
                    "<email>" + escapeXml(email) + "</email>" +
                    "<password>" + escapeXml(password) + "</password>" +
                    "<confirmPassword>" + escapeXml(confirmPassword) + "</confirmPassword>" +
                    "</Register>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("Register", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            Element result = (Element) doc.getElementsByTagNameNS("*", "RegisterResult").item(0);
            if (result == null)
                return null;

            String userIdStr = getValue(result, "UserId");
            int userId = (userIdStr != null && !userIdStr.isBlank()) ? Integer.parseInt(userIdStr) : 0;

            return new RegisterResultDto(
                    userId,
                    getValue(result, "Email"),
                    getValue(result, "Name"),
                    getValue(result, "Surname"),
                    getValue(result, "ErrorMessage"));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // 📋 RESERVATIONS
    // =========================================================

    public List<UserReservationDto> getUserReservations(int userId) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetUserReservations xmlns=\"http://tempuri.org/\">" +
                    "<userId>" + userId + "</userId>" +
                    "</GetUserReservations>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetUserReservations", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList nodes = doc.getElementsByTagNameNS("*", "ReservationDto");

            List<UserReservationDto> list = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);

                int reservationId = Integer.parseInt(getValue(e, "ReservationId"));
                String title = getValue(e, "Title");
                String showDatetime = getValue(e, "ShowDatetime");

                // 🔥 SEATS (lista <string>)
                NodeList seatsNodes = e.getElementsByTagNameNS("*", "string");
                int filmShowId = Integer.parseInt(getValue(e, "FilmShowId"));
                StringBuilder seatsBuilder = new StringBuilder();
                for (int j = 0; j < seatsNodes.getLength(); j++) {
                    if (j > 0)
                        seatsBuilder.append(", ");
                    seatsBuilder.append(seatsNodes.item(j).getTextContent());
                }

                list.add(new UserReservationDto(
                        reservationId,
                        title,
                        showDatetime,
                        filmShowId,
                        seatsBuilder.toString()));
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static class UserReservationDto {
        private final int reservationId;
        private final String title;
        private final String showDatetime;
        private final int filmShowId; // 🔥 NOWE
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

    public ReservationCreateResultDto createReservation(int userId, int filmShowId, List<Integer> seatIds) {
        try {
            StringBuilder seatsXml = new StringBuilder(
                    "<seats xmlns:d4p1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">");
            for (int id : seatIds) {
                seatsXml.append("<d4p1:int>").append(id).append("</d4p1:int>");
            }
            seatsXml.append("</seats>");

            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<CreateReservation xmlns=\"http://tempuri.org/\">" +
                    "<userId>" + userId + "</userId>" +
                    "<filmshowId>" + filmShowId + "</filmshowId>" +
                    seatsXml +
                    "</CreateReservation>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("CreateReservation", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            Element result = (Element) doc.getElementsByTagNameNS("*", "CreateReservationResult").item(0);
            if (result == null)
                return null;

            String idStr = getValue(result, "ReservationId");
            int reservationId = (idStr != null && !idStr.isBlank()) ? Integer.parseInt(idStr) : 0;
            List<String> seatKeys = new ArrayList<>();
            for (int id : seatIds) {
                seatKeys.add(String.valueOf(id));
            }
            return new ReservationCreateResultDto(reservationId, seatKeys);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteReservation(int userId, int reservationId) {
        try {
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<ReservationDelete xmlns=\"http://tempuri.org/\">" +
                    "<userId>" + userId + "</userId>" +
                    "<reservationId>" + reservationId + "</reservationId>" +
                    "</ReservationDelete>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("ReservationDelete", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            Element result = (Element) doc.getElementsByTagNameNS("*", "ReservationDeleteResult").item(0);
            if (result == null)
                return false;

            return Boolean.parseBoolean(result.getTextContent().trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateReservation(int userId, int reservationId, int newFilmShowId, List<Integer> newSeatIds) {
        try {
            StringBuilder seatsXml = new StringBuilder(
                    "<newseats xmlns:d4p1=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">");
            for (int id : newSeatIds) {
                seatsXml.append("<d4p1:int>").append(id).append("</d4p1:int>");
            }
            seatsXml.append("</newseats>");

            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<UpdateReservation xmlns=\"http://tempuri.org/\">" +
                    "<userId>" + userId + "</userId>" +
                    "<reservationId>" + reservationId + "</reservationId>" +
                    "<newshowId>" + newFilmShowId + "</newshowId>" +
                    seatsXml +
                    "</UpdateReservation>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("UpdateReservation", soap);
            return xml != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    private String escapeXml(String value) {
        if (value == null)
            return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    // =========================================================
    // DTOs: AUTH
    // =========================================================
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

    public List<SeatDto> getSeats(int filmShowId) {

        try {

            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetSeats xmlns=\"http://tempuri.org/\">" +
                    "<filmshowId>" + filmShowId + "</filmshowId>" +
                    "</GetSeats>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

            String xml = sendSoap("GetSeats", soap);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            NodeList nodes = doc.getElementsByTagNameNS("*", "SeatDto");

            List<SeatDto> list = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {

                Element e = (Element) nodes.item(i);

                int seatId = Integer.parseInt(getValue(e, "SeatId"));
                int number = Integer.parseInt(getValue(e, "Number"));
                int row = Integer.parseInt(getValue(e, "RowNum"));
                boolean taken = Boolean.parseBoolean(getValue(e, "IsTaken"));

                list.add(new SeatDto(seatId, number, row, taken));
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
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

    public byte[] getReservationPdf(int reservationId) {
        try {
            String url = SERVER_URL + "/ReservationToPdf?reservationId=" + reservationId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {

                String xml = response.body();

                // 🔥 wyciągamy Base64 z XML
                String base64 = xml
                        .replaceAll("(?s).*<ReservationToPdfResult>", "")
                        .replaceAll("</ReservationToPdfResult>.*", "");

                return java.util.Base64.getDecoder().decode(base64);
            }

            throw new RuntimeException("PDF error: " + response.statusCode());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
