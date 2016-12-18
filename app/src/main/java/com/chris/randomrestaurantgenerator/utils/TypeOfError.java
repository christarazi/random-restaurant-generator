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
