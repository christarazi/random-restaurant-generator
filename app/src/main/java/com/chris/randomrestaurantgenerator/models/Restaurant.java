package com.chris.randomrestaurantgenerator.models;


import java.util.ArrayList;

public class Restaurant {
    private String name;
    private float rating;
    private String ratingImageURL;
    private String thumbnailURL;
    private int reviewCount;
    private String url;
    private ArrayList<String> categories;
    private String phoneNumber;
    private boolean isClosed;
    private String[] address;
    private double lat;
    private double lon;

    public Restaurant(String name, float rating, String ratingImageURL, String thumbnailURL, int reviewCount,
                      String url, ArrayList<String> categories, String phoneNumber, boolean isClosed,
                      String[] address, double lat, double lon) {
        this.name = name;
        this.rating = rating;
        this.ratingImageURL = ratingImageURL;
        this.thumbnailURL = thumbnailURL;
        this.reviewCount = reviewCount;
        this.url = url;
        this.categories = categories;
        this.phoneNumber = phoneNumber;
        this.isClosed = isClosed;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public float getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getUrl() {
        return url;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public String[] getAddress() {
        return address;
    }

    public String getRatingImageURL() {
        return ratingImageURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
