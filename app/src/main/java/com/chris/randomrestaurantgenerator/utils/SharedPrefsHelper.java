package com.chris.randomrestaurantgenerator.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.chris.randomrestaurantgenerator.BuildConfig;

/**
 * Helper class to handle operations with SharedPreferences.
 */
public class SharedPrefsHelper {

    private Activity activity;
    private final static String SHARED_PREFS_VERSION = "SHARED_PREFS_VERSION";
    private final static String FIRST_TIME_REQUESTING_LOC = "FIRST_TIME_REQUESTING_LOC";

    public SharedPrefsHelper(Activity activity) {
        this.activity = activity;
        checkVersion();
    }

    private void checkVersion() {
        SharedPreferences sharedPrefs = this.activity.getPreferences(Context.MODE_PRIVATE);
        final String versionName = sharedPrefs.getString(SHARED_PREFS_VERSION, BuildConfig.VERSION_NAME);

        if (versionName.compareTo(BuildConfig.VERSION_NAME) != 0) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.clear();
            editor.putString(SHARED_PREFS_VERSION, BuildConfig.VERSION_NAME);
            editor.apply();
        }
    }

    public boolean checkFirstTimeRequestingLocation() {
        SharedPreferences sharedPrefs = this.activity.getPreferences(Context.MODE_PRIVATE);

        return sharedPrefs.getBoolean(FIRST_TIME_REQUESTING_LOC, true);
    }

    public void modifyFirstTimeRequestingLocation(boolean value) {
        SharedPreferences sharedPrefs = this.activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(FIRST_TIME_REQUESTING_LOC, value);
        editor.apply();
    }
}
