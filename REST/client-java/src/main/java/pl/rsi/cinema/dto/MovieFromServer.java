package pl.rsi.cinema.dto;

public class MovieFromServer {

    private final int showId; // SEANS (ważne do sali / miejsc)
    private final int movieId; // FILM
    private final String title;
    private final String genre;
    private final String showDateTime;

    public MovieFromServer(int showId, int movieId, String title, String genre, String showDateTime) {
        this.showId = showId;
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.showDateTime = showDateTime;
    }

    public int getShowId() {
        return showId;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getShowDateTime() {
        return showDateTime;
    }
}
