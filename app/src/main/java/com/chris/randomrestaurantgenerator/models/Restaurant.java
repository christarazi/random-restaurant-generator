package com.chris.randomrestaurantgenerator.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * This class is responsible for encapsulating a restaurant from Yelp.
 */
public class Restaurant implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Restaurant> CREATOR = new Parcelable.Creator<Restaurant>() {
        @Override
        public Restaurant createFromParcel(Parcel in) {
            return new Restaurant(in);
        }

        @Override
        public Restaurant[] newArray(int size) {
            return new Restaurant[size];
        }
    };
    private String name;
    private float rating;
    private String ratingImageURL;
    private String thumbnailURL;
    private int reviewCount;
    private String url;
    private ArrayList<String> categories;
    private ArrayList<String> address;
    private String deal;
    private double distance;
    private double lat;
    private double lon;
    private boolean isSaved;

    public Restaurant(String name, float rating, String ratingImageURL, String thumbnailURL, int reviewCount,
                      String url, ArrayList<String> categories, ArrayList<String> address,
                      String deal, double distance, double lat, double lon) {
        this.name = name;
        this.rating = rating;
        this.ratingImageURL = ratingImageURL;
        this.thumbnailURL = thumbnailURL;
        this.reviewCount = reviewCount;
        this.url = url;
        this.categories = categories;
        this.address = address;
        this.deal = deal;
        this.distance = distance;
        this.lat = lat;
        this.lon = lon;
    }

    protected Restaurant(Parcel in) {
        name = in.readString();
        rating = in.readFloat();
        ratingImageURL = in.readString();
        thumbnailURL = in.readString();
        reviewCount = in.readInt();
        url = in.readString();
        if (in.readByte() == 0x01) {
            categories = new ArrayList<>();
            in.readList(categories, String.class.getClassLoader());
        } else {
            categories = null;
        }
        if (in.readByte() == 0x01) {
            address = new ArrayList<>();
            in.readList(address, String.class.getClassLoader());
        } else {
            address = null;
        }
        deal = in.readString();
        distance = in.readDouble();
        lat = in.readDouble();
        lon = in.readDouble();
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

    public ArrayList<String> getAddress() {
        return address;
    }

    public String getRatingImageURL() {
        return ratingImageURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public String getDeal() {
        return deal;
    }

    public double getDistance() {
        return distance;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean b) {
        this.isSaved = b;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeFloat(rating);
        dest.writeString(ratingImageURL);
        dest.writeString(thumbnailURL);
        dest.writeInt(reviewCount);
        dest.writeString(url);
        if (categories == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(categories);
        }
        if (address == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(address);
        }
        dest.writeString(deal);
        dest.writeDouble(distance);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Restaurant)
            return this.url.equals(((Restaurant) o).url);
        else return super.equals(o);
    }
}
