package com.chris.randomrestaurantgenerator.utils;

/**
 * A utility class to help keep track of the types of error that may occur when querying Yelp.
 */
public class TypeOfError {
    public static int NO_ERROR = 0;
    public static int NO_RESTAURANTS = 1;
    public static int MISSING_INFO = 2;
    public static int TIMED_OUT = 3;
    public static int NETWORK_CONNECTION_ERROR = 4;
}
