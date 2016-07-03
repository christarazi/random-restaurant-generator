package com.chris.randomrestaurantgenerator.utils;

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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * A helper class to aid with keeping one single instance of the Location object and all the other
 * periphery classes that go along with it.
 */
public class LocationProviderHelper {

    public static final int MY_LOCATION_REQUEST_CODE = 1;
    public static boolean useGPS = false;

    private final String INFO_TEXT = "Current Location";

    private Activity activity;
    private View view;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;
    private FloatingSearchView searchLocationBox;
    private ProgressDialog dialog;
    private String PROVIDER;

    private SharedPrefsHelper sharedPrefsHelper;

    public LocationProviderHelper(final Activity act, final View view, FloatingSearchView infoBox) {

        this.activity = act;
        this.view = view;
        this.dialog = new ProgressDialog(this.activity);
        this.searchLocationBox = infoBox;

        this.sharedPrefsHelper = new SharedPrefsHelper(this.activity);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                location = loc;
                Toast.makeText(activity, "Location acquired: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                dismissLocationUpdater();

                if (PROVIDER.equals(LocationManager.GPS_PROVIDER))
                    searchLocationBox.setSearchText(String.format("%s (GPS)", INFO_TEXT));
                else if (PROVIDER.equals(LocationManager.NETWORK_PROVIDER))
                    searchLocationBox.setSearchText(String.format("%s (NETWORK)", INFO_TEXT));
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
        boolean permissionsDenied =
                checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                checkSelfPermission(activity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED;

        if (permissionsDenied) {
            Log.d("CHRIS", "permissions denied");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean shouldShowRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_COARSE_LOCATION);

                // Alert the user that permission is required if this is the first time.
                if (sharedPrefsHelper.checkFirstTimeRequestingLocation()) {
                    sharedPrefsHelper.modifyFirstTimeRequestingLocation(false);
                    Log.d("CHRIS", "First time and shouldShow == " + shouldShowRationale);

                    final Snackbar snackbar = Snackbar.make(view, "Location permissions are needed to use GPS. Please allow them.", Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                        }
                    }).show();
                } else {
                    // If it is not the first time, then the user has already pressed "Deny" at least once.
                    if (shouldShowRationale) {
                        Log.d("CHRIS", "Not first time and should show");

                        final Snackbar snackbar = Snackbar.make(view, "Location permissions are needed to use GPS. Please allow them.", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackbar.dismiss();
                            }
                        }).show();
                    } else {
                        // User has elected to deny all permissions; must enter location manually.
                        // Most likely "Do not ask again" has been checked.
                        Log.d("CHRIS", "Not first time and should not show");

                        final Snackbar snackbar = Snackbar.make(view, "Location permissions denied. Please enter location manually.", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackbar.dismiss();
                            }
                        }).show();
                    }
                }

                /**
                 * Request permission regardless of the outcome above.
                 * Because if the permissions are denied, the method below will just do nothing
                 * and will have no side effects.
                 */
                activity.requestPermissions(new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, MY_LOCATION_REQUEST_CODE);
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
                    searchLocationBox.setSearchText("");
                }
            });
            dialog.show();
        }
    }

    public void dismissLocationUpdater() {
        boolean permissionsDenied =
                checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                checkSelfPermission(activity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED;

        if (permissionsDenied) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                activity.requestPermissions(new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, MY_LOCATION_REQUEST_CODE);
        } else
            this.locationManager.removeUpdates(locationListener);
    }
}
