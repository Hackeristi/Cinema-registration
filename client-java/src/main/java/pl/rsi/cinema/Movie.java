package pl.rsi.cinema;

import javafx.scene.image.Image;

public class Movie {
    private String title;
    private String genre;
    private int year;
    private int duration;
    private String director;
    private String actors;
    private String description;
    private Image coverImage;

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public int getYear() {
        return year;
    }

    public int getDuration() {
        return duration;
    }

    public String getDirector() {
        return director;
    }

    public String getActors() {
        return actors;
    }

    public String getDescription() {
        return description;
    }

    public Image getCoverImage() {
        return coverImage;
    }
    // gettery
}
