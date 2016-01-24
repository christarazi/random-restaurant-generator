package com.chris.randomrestaurantgenerator.utils;

import com.chris.randomrestaurantgenerator.models.Restaurant;

import java.util.ArrayList;

/**
 * Singleton design pattern to hold one instance of our savedList throughout the application.
 */
public class SavedListHolder {

    private static SavedListHolder instance = null;
    private static ArrayList<Restaurant> savedList;

    public static SavedListHolder getInstance() {
        if (instance == null) {
            savedList = new ArrayList<>();
            instance = new SavedListHolder();
        }

        return instance;
    }

    public ArrayList<Restaurant> getSavedList() {
        return savedList;
    }

    public void setSavedList(ArrayList<Restaurant> list) {
        savedList = list;
    }
}
