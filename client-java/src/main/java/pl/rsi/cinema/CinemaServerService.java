package pl.rsi.cinema;

import pl.rsi.cinema.dto.MovieFromServer;

import java.net.http.*;
import java.net.URI;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
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

            System.out.println(xml);

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
                System.err.println("DEBUG: Serwer nie zwrócił danych w GetMovieDetailsResult.");
                return null;
            }

            // Zwracamy nowy obiekt ze wszystkimi danymi
            return new MovieDetails(
                    getValue(res, "Title"),
                    getValue(res, "Description"),
                    getValue(res, "Director"),
                    finalActors,
                    Integer.parseInt(getValue(res, "Duration")),
                    getValue(res, "Premiere"),
                    getValue(res, "Poster"));

        } catch (Exception e) {
            System.err.println("Błąd podczas przetwarzania szczegółów filmu (ID: " + movieId + "):");
            e.printStackTrace();
            return null;
        }
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
        private final String premiere;
        private final String posterBase64;

        public MovieDetails(String title, String description, String director,
                String actors, int duration,
                String premiere, String posterBase64) {
            this.title = title;
            this.description = description;
            this.director = director;
            this.actors = actors;
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

            System.out.println("=== SOAP DEBUG GetShowtimes ===");
            System.out.println("movieId = " + movieId);
            System.out.println("raw date = [" + date + "]");
            System.out.println("formatted date = [" + formatted + "]");
            System.out.println("is yyyy-MM-dd = " + formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
            System.out.println(soap);

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

                list.add(new ShowtimeDto(id, dt));
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

        public ShowtimeDto(int filmShowId, String showDatetime) {
            this.filmShowId = filmShowId;
            this.showDatetime = showDatetime;
        }

        public int getFilmShowId() {
            return filmShowId;
        }

        public String getShowDatetime() {
            return showDatetime;
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
}
