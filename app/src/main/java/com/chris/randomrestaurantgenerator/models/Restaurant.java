package com.chris.randomrestaurantgenerator.models;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Restaurant implements Parcelable {
    public static final Creator<Restaurant> CREATOR = new Creator<Restaurant>() {
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
    private String phoneNumber;
    private boolean isClosed;
    private ArrayList<String> address;
    private String deal;
    private double lat;
    private double lon;

    public Restaurant(String name, float rating, String ratingImageURL, String thumbnailURL, int reviewCount,
                      String url, ArrayList<String> categories, String phoneNumber, boolean isClosed,
                      ArrayList<String> address, String deal, double lat, double lon) {
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
        this.deal = deal;
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
            categories = new ArrayList<String>();
            in.readList(categories, String.class.getClassLoader());
        } else {
            categories = null;
        }
        phoneNumber = in.readString();
        isClosed = in.readByte() != 0x00;
        if (in.readByte() == 0x01) {
            address = new ArrayList<String>();
            in.readList(address, String.class.getClassLoader());
        } else {
            address = null;
        }
        deal = in.readString();
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isClosed() {
        return isClosed;
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

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
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
        dest.writeString(phoneNumber);
        dest.writeByte((byte) (isClosed ? 0x01 : 0x00));
        if (address == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(address);
        }
        dest.writeString(deal);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
    }
}
