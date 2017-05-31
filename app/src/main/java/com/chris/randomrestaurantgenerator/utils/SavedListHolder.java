/*
 * Copyright (C) 2017 Chris Tarazi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
