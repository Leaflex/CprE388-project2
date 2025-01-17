package com.example.project2.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.List;

/**
 * Route POJO.
 */
@IgnoreExtraProperties
public class Route {

    /**
     * String variables for the Route object consisting of different text fields
     */
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_DIFFICULTY = "difficulty";
    public static final String FIELD_SLOPE = "slope";
    public static final String FIELD_NUM_RATINGS = "numRatings";
    public static final String FIELD_AVG_RATING = "avgRating";
    public static final String FIELD_DIFFICULTY_ORDER = "difficultyOrder";
    public static final String FIELD_SLOPE_ORDER = "slopeOrder";
    public static final String FIELD_DESCRIPTION = "description"; // New constant

    /**
     * Private variables for the Route object consisting of different fields to describe a route
     */
    private String title;
    private String city;
    private String difficulty;
    private String photo;
    private String slope;
    private String description;
    private double avgRating;
    private int difficultyOrder; // Used for sorting routes by their difficulty
    private int slopeOrder;      // Used for sorting routes by their slope

    /**
     * Default constructor for Route
     */
    public Route() {}

    /**
     * Constructor for Route that takes a title, city, difficulty, photo, slope, description,
     * @param title Title for the route
     * @param city The city where the route is located
     * @param difficulty The difficulty of the route
     * @param photo The URL for the route's photo
     * @param slope The slope of the route
     * @param description The description of the route
     * @param numRatings The number of ratings for the route
     * @param avgRating The average rating of the route
     */
    public Route(String title, String city, String difficulty, String photo,
                 String slope, String description, int numRatings, double avgRating) {
        this.title = title;
        this.city = city;
        this.difficulty = difficulty;
        this.photo = photo;
        this.slope = slope;
        this.description = description;
        this.avgRating = avgRating;
        this.difficultyOrder = calculateDifficultyOrder(difficulty);
        this.slopeOrder = calculateSlopeOrder(slope);
    }

    /**
     * Get the title for the route
     * @return A string that consists of a route's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title for the route
     * @param title A string that consists of a route's title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the city for the route
     * @return A string that consists of a route's city
     */
    public String getCity() {
        return city;
    }

    /**
     * Set the city for the route
     * @param city A string that consists of a route's city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Get the difficulty for the route
     * @return A string that consists of a route's difficulty
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * Set the difficulty for the route
     * @param difficulty A string that consists of a route's difficulty
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        this.difficultyOrder = calculateDifficultyOrder(difficulty); // Used to sort routes by their difficulty in ascending order
    }

    /**
     * Set a route's photo
     * @param photo A string that consists of a URL to a route's photo
     */
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    /**
     * Get a route's slope
     * @return A string that consists of a route's slope
     */
    public String getSlope() {
        return slope;
    }

    /**
     * Set a route's slope
     * @param slope A string that consists of a route's slope
     */
    public void setSlope(String slope) {
        this.slope = slope;
        this.slopeOrder = calculateSlopeOrder(slope); // Used to sort routes by their slope in ascending order
    }

    /**
     * Get a route's description
     * @return A string that consists of a route's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set a route's description
     * @param description A string that consists of a route's description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the average rating for a route
     * @return A double that consists of a route's average rating
     */
    public double getAvgRating() {
        return avgRating;
    }

    /**
     * Set the average rating for a route
     * @param avgRating A double that consists of a route's average rating
     */
    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    /**
     * Helper method to calculate the difficulty order for sorting by difficulty
     * @param difficulty The difficulty of the route
     * @return An integer used to sort routes by their difficulty
     */
    public static int calculateDifficultyOrder(String difficulty) {
        if (difficulty == null) return Integer.MAX_VALUE; // Unknown difficulty
        switch (difficulty.toLowerCase()) {
            case "easy":
                return 1;
            case "moderate":
                return 2;
            case "hard":
                return 3;
            case "expert":
                return 4;
            default:
                return Integer.MAX_VALUE; // Unknown difficulty
        }
    }

    /**
     * Helper method to calculate the slope order for sorting by slope
     * @param slope The slope of the route
     * @return An integer used to sort routes by their slope
     */
    public static int calculateSlopeOrder(String slope) {
        if (slope == null) return Integer.MAX_VALUE; // Unknown slope
        switch (slope.toLowerCase()) {
            case "gentle":
                return 1;
            case "inclined":
                return 2;
            case "steep":
                return 3;
            case "very steep":
                return 4;
            default:
                return Integer.MAX_VALUE; // Unknown slope
        }
    }
}
