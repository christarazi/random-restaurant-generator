package com.chris.randomrestaurantgenerator.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
    private String PROVIDER;
    private String infoText = "Current Location";

    public LocationProviderHelper(final Activity act, final View view) {

        this.activity = act;
        this.dialog = new ProgressDialog(this.activity);
        this.userLocationInfoBox = (TextView) view.findViewById(R.id.userLocationInfo);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                location = loc;
                Toast.makeText(activity, "Got location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                dismissLocationUpdater();

                if (PROVIDER.equals(LocationManager.GPS_PROVIDER))
                    userLocationInfoBox.setText(String.format("%s (GPS)", infoText));
                else if (PROVIDER.equals(LocationManager.NETWORK_PROVIDER))
                    userLocationInfoBox.setText(String.format("%s (NETWORK)", infoText));
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

    public void requestLocation() {

        // If location permissions are denied, then try to request them.
        // Else, proceed with getting Location.
        if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission_group.LOCATION}, MY_LOCATION_REQUEST_CODE);
            }
        } else {

            // Check which Location provider is available to us.
            if (this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                this.PROVIDER = LocationManager.GPS_PROVIDER;
            } else if (this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(this.activity, "GPS is not turned on. Please enable high accuracy mode for better accuracy. Using network for location...", Toast.LENGTH_LONG)
                        .show();
                this.PROVIDER = LocationManager.NETWORK_PROVIDER;
            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this.activity);
                dialog.setMessage("Location is off. Please enable Location in your settings.");
                dialog.setTitle("Error");
                dialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();

                return;
            }

            this.locationManager.requestLocationUpdates(this.PROVIDER, 1000, 1, this.getLocationListener());

            // For some reason, requesting Location updates gets stuck randomly, so this kicks it in the butt and hurries it along.
            // See: http://stackoverflow.com/q/14700755/2193236
            this.locationManager.getLastKnownLocation(this.PROVIDER);

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
        } else
            this.locationManager.removeUpdates(locationListener);
    }
}
