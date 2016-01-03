package com.chris.randomrestaurantgenerator;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * Created by chris on 1/2/16.
 */
public class LocationProviderHelper {

    private Activity activity;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;
    private boolean locationChanged = false;

    public LocationProviderHelper(final Activity act, final ProgressDialog dialog) {

        this.activity = act;

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                locationChanged = true;
                location = loc;
                Log.d("Chris", "current location: " + location.getLatitude() + ", " + location.getLongitude());
                Toast.makeText(activity, "Got location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                dismissLocationUpdater();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Chris", ":status changed");
            }

            public void onProviderEnabled(String provider) {
                Log.d("Chris", "provider enabled");
            }

            public void onProviderDisabled(String provider) {
                Log.d("Chris", "provider disabled");
            }
        };
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public LocationListener getLocationListener() {
        return locationListener;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isLocationChanged() {
        return locationChanged;
    }

    public void requestLocation() {
        if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        }
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this.getLocationListener());
    }

    public void dismissLocationUpdater() {
        if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        }
        this.locationManager.removeUpdates(locationListener);
    }
}
