package com.chris.randomrestaurantgenerator.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.chris.randomrestaurantgenerator.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * A helper class to aid with keeping one single instance of the Location object and all the other
 * periphery classes that go along with it.
 */
public class LocationProviderHelper implements LocationListener,
        ResultCallback<LocationSettingsResult> {

    public static final int REQUEST_CHECK_SETTINGS = 1;
    public static final int RC_LOCATION_PERM = 120;
    public static final String[] PERMISSIONS = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
    public static boolean useGPS = false;

    private Activity activity;
    private GoogleApiClient mGoogleClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mCurrentLocation;
    private Location mLastLocation;
    private FloatingSearchView searchLocationBox;
    private ProgressDialog progressDialog;

    public LocationProviderHelper(final Activity act, FloatingSearchView infoBox,
                                  GoogleApiClient googleApiClient) {
        this.activity = act;
        this.searchLocationBox = infoBox;
        this.mGoogleClient = googleApiClient;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();

        progressDialog = new ProgressDialog(this.activity);

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
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.hasAccuracy() && location.getAccuracy() < 200.0) {
            mCurrentLocation = location;
            progressDialog.dismiss();
            Log.d("RRG", "onLocationChanged: " + location.getAccuracy() + "m");
            searchLocationBox.setSearchText(activity.getString(R.string.string_current_location));
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, this);
        }
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can
                // initialize location requests here.
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(
                            activity,
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore the error.
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                // Location settings are not satisfied. However, we have no way
                // to fix the settings so we won't show the dialog.
                break;
        }
    }

    // Called by EasyPermissions from MainActivityFragment
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d("RRG", "onPermissionsGranted:" + requestCode + ":" + perms.size());
        requestLocation();
    }

    // Called by EasyPermissions from MainActivityFragment
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d("RRG", "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // Handle negative button on click listener
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(activity, R.string.string_location_perm_manual, Toast.LENGTH_SHORT).show();
            }
        };

        // (Optional) Check whether the user denied permissions and checked NEVER ASK AGAIN.
        // This will display a dialog directing them to enable the permission in app settings.
        EasyPermissions.checkDeniedPermissionsNeverAskAgain(activity,
                activity.getString(R.string.string_location_perm_rationale),
                R.string.settings, R.string.cancel, onClickListener, Arrays.asList(PERMISSIONS));
    }

    public void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    public void startLocationUpdates() {
        if (EasyPermissions.hasPermissions(activity, PERMISSIONS)) {

            try {
                Log.d("RRG", "Starting location updates...");
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleClient, mLocationRequest, this);

                if (mLastLocation == null) {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
                    Log.d("RRG", "Reusing last location");
                }

                // Make sure mLastLocation is not null because getLastLocation() may return null.
                if (mLastLocation != null) {
                    onLocationChanged(mLastLocation);
                } else {
                    progressDialog.show();
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleClient, mLocationRequest, this);
                }
            } catch (SecurityException ignored) {
                // Ignoring exception because this is handled already by EasyPermissions.
            }

        } else {
            EasyPermissions.requestPermissions(activity, activity.getString(R.string.string_location_permission_required),
                    RC_LOCATION_PERM, PERMISSIONS);
        }
    }

    public void requestLocation() {
        if (EasyPermissions.hasPermissions(activity, PERMISSIONS)) {
            progressDialog.show();
            checkLocationSettings();
            useGPS = true;
        } else {
            EasyPermissions.requestPermissions(activity, activity.getString(R.string.string_location_permission_required),
                    RC_LOCATION_PERM, PERMISSIONS);
        }
    }

    public void pauseAndSaveLocationUpdates() {
        progressDialog.dismiss();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, this);

        if (mCurrentLocation != null) {
            mLastLocation = mCurrentLocation;
            Log.d("RRG", "Saved location on pause");
        }
    }

    public void dismissLocationUpdater() {
        progressDialog.dismiss();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, this);
        searchLocationBox.clearQuery();
    }

    public Location getLocation() {
        return mCurrentLocation;
    }
}
