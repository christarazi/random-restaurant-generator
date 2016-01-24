package com.chris.randomrestaurantgenerator.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chris.randomrestaurantgenerator.R;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * A helper class to aid with keeping one single instance of the Location object and all the other
 * periphery classes that go along with it.
 */
public class LocationProviderHelper {

    public static final int MY_LOCATION_REQUEST_CODE = 1;
    public static boolean useGPS = false;

    private Activity activity;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;
    private TextView userLocationInfoBox;
    private ProgressDialog dialog;
    private boolean locationChanged = false;

    public LocationProviderHelper(final Activity act, final View view) {

        this.activity = act;
        this.dialog = new ProgressDialog(this.activity);
        this.userLocationInfoBox = (TextView) view.findViewById(R.id.userLocationInfo);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                locationChanged = true;
                location = loc;
                Toast.makeText(activity, "Got location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                userLocationInfoBox.setText("Current Location");
                dismissLocationUpdater();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
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

        // If location permissions are denied, then try to request them.
        // Else, proceed with getting Location.
        if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission_group.LOCATION}, MY_LOCATION_REQUEST_CODE);
            }
        }
        else {
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this.getLocationListener());

            useGPS = true;
            dialog.setMessage("Getting location...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    dismissLocationUpdater();
                    useGPS = false;
                    userLocationInfoBox.setText("");
                }
            });
            dialog.show();
        }
    }

    public void dismissLocationUpdater() {
        if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission_group.LOCATION}, MY_LOCATION_REQUEST_CODE);
            }
        }
        else
            this.locationManager.removeUpdates(locationListener);
    }
}
