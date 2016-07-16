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
import android.view.View;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.chris.randomrestaurantgenerator.R;

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

    private final String CURRENT_LOCATION;

    private Activity activity;
    private View view;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;
    private FloatingSearchView searchLocationBox;
    private ProgressDialog progressDialog;
    private AlertDialog.Builder noLocationDialog;
    private String PROVIDER;

    private SharedPrefsHelper sharedPrefsHelper;

    public LocationProviderHelper(final Activity act, final View view, FloatingSearchView infoBox) {

        this.activity = act;
        this.view = view;
        this.searchLocationBox = infoBox;

        CURRENT_LOCATION = this.activity.getString(R.string.string_current_location);

        this.progressDialog = new ProgressDialog(this.activity);
        this.noLocationDialog = new AlertDialog.Builder(this.activity);
        this.sharedPrefsHelper = new SharedPrefsHelper(this.activity);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) {
                location = loc;
                Toast.makeText(activity, "Location acquired: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                dismissLocationUpdater();

                if (PROVIDER.equals(LocationManager.GPS_PROVIDER)) {
                    searchLocationBox.clearQuery();
                    searchLocationBox.setSearchText(String.format("%s (GPS)", CURRENT_LOCATION));
                }
                else if (PROVIDER.equals(LocationManager.NETWORK_PROVIDER)) {
                    searchLocationBox.clearQuery();
                    searchLocationBox.setSearchText(String.format("%s (NETWORK)", CURRENT_LOCATION));
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        progressDialog.setMessage(this.activity.getString(R.string.string_getting_location));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dismissLocationUpdater();
                useGPS = false;
                searchLocationBox.clearQuery();
            }
        });

        this.noLocationDialog.setMessage(R.string.string_location_is_off);
        this.noLocationDialog.setTitle("Error");
        this.noLocationDialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean shouldShowRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_COARSE_LOCATION);

                // Alert the user that permission is required if this is the first time.
                if (sharedPrefsHelper.checkFirstTimeRequestingLocation()) {
                    sharedPrefsHelper.modifyFirstTimeRequestingLocation(false);

                    final Snackbar snackbar = Snackbar.make(view, "Location permissions are needed to use GPS. Please allow them.", Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                            activity.requestPermissions(new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, MY_LOCATION_REQUEST_CODE);
                        }
                    }).show();
                } else {
                    // If it is not the first time, then the user has already pressed "Deny" at least once.
                    if (shouldShowRationale) {

                        final Snackbar snackbar = Snackbar.make(view, "Location permissions are needed to use GPS. Please allow them.", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackbar.dismiss();
                                activity.requestPermissions(new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, MY_LOCATION_REQUEST_CODE);
                            }
                        }).show();
                    } else {
                        // User has elected to deny all permissions; must enter location manually.
                        // Most likely "Do not ask again" has been checked.

                        final Snackbar snackbar = Snackbar.make(view, "Location permissions denied. Please enter location manually.", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackbar.dismiss();
                            }
                        }).show();
                    }
                }
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
                this.noLocationDialog.show();
                return;
            }
        }

        if (PROVIDER == null) return;

        this.locationManager.requestLocationUpdates(this.PROVIDER, 1000, 1, this.getLocationListener());

        // For some reason, requesting Location updates gets stuck randomly, so this kicks it in the butt and hurries it along.
        // See: http://stackoverflow.com/q/14700755/2193236
        this.locationManager.getLastKnownLocation(this.PROVIDER);

        useGPS = true;

        progressDialog.show();
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
