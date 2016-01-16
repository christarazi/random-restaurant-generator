package com.chris.randomrestaurantgenerator.models;

import java.util.ArrayList;

// Singleton design pattern to hold one instance of our maybeList throughout the application.
public class MaybeListHolder {

    private static MaybeListHolder instance = null;
    private static ArrayList<Restaurant> maybeList;

    public static MaybeListHolder getInstance() {
        if (instance == null) {
            maybeList = new ArrayList<>();
            instance = new MaybeListHolder();
        }

        return instance;
    }

    public ArrayList<Restaurant> getMaybeList() {
        return maybeList;
    }
}
