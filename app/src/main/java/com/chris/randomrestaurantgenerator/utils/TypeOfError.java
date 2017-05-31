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

/**
 * A utility class to help keep track of the types of error that may occur when querying Yelp.
 */
public class TypeOfError {
    public static final int NO_ERROR = 0;
    public static final int NO_RESTAURANTS = 1;
    public static final int MISSING_INFO = 2;
    public static final int TIMED_OUT = 3;
    public static final int NETWORK_CONNECTION_ERROR = 4;
    public static final int INVALID_LOCATION = 5;
}
