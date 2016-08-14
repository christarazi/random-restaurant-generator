package com.chris.randomrestaurantgenerator.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.chris.randomrestaurantgenerator.BuildConfig;
import com.chris.randomrestaurantgenerator.MainActivity;
import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.managers.UnscrollableLinearLayoutManager;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.utils.LocationProviderHelper;
import com.chris.randomrestaurantgenerator.utils.TwoStepOAuth;
import com.chris.randomrestaurantgenerator.utils.TypeOfError;
import com.chris.randomrestaurantgenerator.views.MainRestaurantCardAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import pub.devrel.easypermissions.EasyPermissions;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;
import uk.co.deanwild.materialshowcaseview.shape.CircleShape;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;

/**
 * A fragment containing the main activity.
 * Responsible for displaying to the user a random restaurant based off their location  / zip code.
 */
public class MainActivityFragment extends Fragment implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, EasyPermissions.PermissionCallbacks {

    FloatingSearchView searchLocationBox;
    EditText filterBox;
    Button generate;
    int generateBtnColor;

    RelativeLayout rootLayout;
    RecyclerView restaurantView;
    LinearLayout mapCardContainer;
    MapView mapView;
    GoogleMap map;
    GoogleApiClient mGoogleApiClient;

    MainRestaurantCardAdapter mainRestaurantCardAdapter;
    LocationProviderHelper locationHelper;
    Restaurant currentRestaurant;
    ArrayList<Restaurant> restaurants = new ArrayList<>();

    OAuthService service;
    Token accessToken;

    int errorInQuery;
    boolean taskRunning = false;
    boolean restartQuery = true;
    boolean runBackgroundQueryAfter = true;
    String searchQuery = "";
    String filterQuery = "";
    ArrayList<String> multiFilters = new ArrayList<>();

    CircularProgressBar progressBar;
    AsyncTask initialYelpQuery;
    AsyncTask backgroundYelpQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);
        restaurantView = (RecyclerView) rootLayout.findViewById(R.id.restaurantView);
        restaurantView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));

        mapCardContainer = (LinearLayout) rootLayout.findViewById(R.id.cardMapLayout);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mapView.onCreate(mapViewSavedInstanceState);

        filterBox = (EditText) rootLayout.findViewById(R.id.filterBox);
        generate = (Button) rootLayout.findViewById(R.id.generate);
        generateBtnColor = Color.parseColor("#F6511D");

        // Build OAuth service.
        service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(TwoStepOAuth.getConsumerKey())
                .apiSecret(TwoStepOAuth.getConsumerSecret()).build();
        accessToken = new Token(TwoStepOAuth.getToken(), TwoStepOAuth.getTokenSecret());

        progressBar = (CircularProgressBar) rootLayout.findViewById(R.id.circularProgressBarMainFragment);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                // If user has swiped left, perform a click on the Generate button.
                if (direction == 4)
                    generate.performClick();

                // If user has swiped right, open Yelp to current restaurant's page.
                if (direction == 8) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentRestaurant.getUrl())));

                    // We don't want to remove the restaurant here, so we add it back to the restaurantView.
                    mainRestaurantCardAdapter.remove();
                    mainRestaurantCardAdapter.add(currentRestaurant);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(restaurantView);

        return rootLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .enableAutoManage(getActivity(), this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Must be defined in onActivityCreated() because searchLocationBox is part of MainActivity.
        searchLocationBox = (FloatingSearchView) getActivity().findViewById(R.id.searchBox);

        // Create LocationProviderHelper instance.
        locationHelper = new LocationProviderHelper(getActivity(), searchLocationBox,
                mGoogleApiClient);

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        if (savedInstanceState != null) {
            currentRestaurant = savedInstanceState.getParcelable("currentRestaurant");
            searchLocationBox.setSearchText(savedInstanceState.getString("locationQuery"));
            filterBox.setText(savedInstanceState.getString("filterQuery"));
            restaurants = savedInstanceState.getParcelableArrayList("restaurants");
        }

        // Define actions on menu button clicks inside searchLocationBox.
        searchLocationBox.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.search_box_gps)
                    locationHelper.requestLocation();
                else if (id == R.id.search_box_filter) {
                    if (filterBox.getVisibility() == View.GONE) {
                        filterBox.setVisibility(View.VISIBLE);

                        // Show tutorial about entering multiple filters.
                        displayShowcaseViewFilterBox();
                    }
                    else if (filterBox.getVisibility() == View.VISIBLE)
                        filterBox.setVisibility(View.GONE);
                }
            }
        });

        searchLocationBox.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {}

            @Override
            public void onSearchAction(String currentQuery) {
                searchLocationBox.setSearchText(currentQuery);
                searchLocationBox.setSearchBarTitle(currentQuery);
            }
        });

        // Listener for when the user clicks done on keyboard after their input.
        filterBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideSoftKeyboard();
                    generate.performClick();
                    return true;
                }
                return false;
            }
        });

        // When the user clicks the Generate button.
        generate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                /**
                 * If the user doesn't wait on the task to complete, warn them it is still running
                 * so we can prevent a long stack of requests from piling up.
                 */
                if (taskRunning) {
                    Toast.makeText(getActivity(), R.string.string_task_running_msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                filterBox.setVisibility(View.GONE);

                /**
                 * Initialize searchQuery and filterQuery if they're empty.
                 * Else set restartQuery to true if the queries have changed.
                 */
                if (searchQuery.isEmpty() && filterQuery.isEmpty()) {
                    searchQuery = searchLocationBox.getQuery();
                    filterQuery = filterBox.getText().toString();
                }
                else if (searchQuery.compareTo(searchLocationBox.getQuery()) != 0 ||
                    filterQuery.compareTo(filterBox.getText().toString()) != 0) {

                    restartQuery = true;
                    runBackgroundQueryAfter = true;
                    restaurants.clear();

                    searchQuery = searchLocationBox.getQuery();
                    filterQuery = filterBox.getText().toString();

                    // We want to use GPS if searchQuery contains the string "Current Location".
                    LocationProviderHelper.useGPS = searchQuery.contains(getActivity()
                            .getString(R.string.string_current_location));
                }

                if (LocationProviderHelper.useGPS) {

                    /**
                     * Check to make sure the location is not null before starting.
                     * Else, begin the AsyncTask.
                     */
                    Location location = locationHelper.getLocation();
                    if (location == null) {
                        displayAlertDialog(R.string.string_location_not_found, "Error");
                    } else {

                        String filterBoxText = filterBox.getText().toString();
                        List<String> filterList = Arrays.asList(filterBox.getText().toString().split(","));

                        /**
                         * Split the filters by comma if the user wants multiple filters.
                         * Run separate query for each filter.
                         *
                         * If multiFilter contains all of filterList and restaurants is empty,
                         * that the user has exhausted through all the restaurants, and we
                         * must restart the query, so clear the multiFilters to start over.
                         */
                        if (multiFilters.containsAll(filterList) && restaurants.isEmpty())
                            multiFilters.clear();

                        if (filterBoxText.contains(",") && !multiFilters.containsAll(filterList)) {
                            multiFilters.clear();
                            multiFilters.addAll(filterList);

                            initialYelpQuery = new RunYelpQuery(
                                    false,
                                    String.valueOf(searchLocationBox.getQuery()),
                                    multiFilters.get(0).trim(),
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                            /**
                             * Skip the first filter because we run the initial query
                             * with the first filter above.
                             * Everything after will be a background query, hence the background
                             * query AsyncTask being run inside the body of this for loop.
                             */
                            for (String filter : multiFilters.subList(1, multiFilters.size())) {
                                backgroundYelpQuery = new RunYelpQueryBackground(0)
                                        .executeOnExecutor(
                                                AsyncTask.THREAD_POOL_EXECUTOR,
                                                String.valueOf(searchLocationBox.getQuery()),
                                                filter.trim(),
                                                String.valueOf(location.getLatitude()),
                                                String.valueOf(location.getLongitude()));
                            }
                        }
                        else {
                            initialYelpQuery = new RunYelpQuery(
                                    runBackgroundQueryAfter,
                                    String.valueOf(searchLocationBox.getQuery()),
                                    String.valueOf(filterBox.getText()),
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                } else {

                    String filterBoxText = filterBox.getText().toString();
                    List<String> filterList = Arrays.asList(filterBox.getText().toString().split(","));

                    /**
                     * Verify that the user has actually entered a location.
                     * Else, begin the AsyncTask.
                     */
                    if (searchLocationBox.getQuery().length() == 0 && restaurants.size() == 0) {
                        displayAlertDialog(R.string.string_enter_valid_location, "Error");
                    } else {

                        /**
                         * Split the filters by comma if the user wants multiple filters.
                         * Run separate query for each filter.
                         *
                         * If multiFilter contains all of filterList and restaurants is empty,
                         * that the user has exhausted through all the restaurants, and we
                         * must restart the query, so clear the multiFilters to start over.
                         */
                        if (multiFilters.containsAll(filterList) && restaurants.isEmpty())
                            multiFilters.clear();

                        if (filterBoxText.contains(",") && !multiFilters.containsAll(filterList)) {
                            multiFilters.clear();
                            multiFilters.addAll(filterList);

                            initialYelpQuery = new RunYelpQuery(
                                    false,
                                    String.valueOf(searchLocationBox.getQuery()),
                                    multiFilters.get(0).trim())
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                            /**
                             * Skip the first filter because we run the initial query
                             * with the first filter above.
                             * Everything after will be a background query, hence the background
                             * query AsyncTask being run inside the body of this for loop.
                             */
                            for (String filter : multiFilters.subList(1, multiFilters.size())) {
                                backgroundYelpQuery = new RunYelpQueryBackground(0)
                                        .executeOnExecutor(
                                                AsyncTask.THREAD_POOL_EXECUTOR,
                                                String.valueOf(searchLocationBox.getQuery()),
                                                filter.trim());
                            }
                        }
                        else {
                            initialYelpQuery = new RunYelpQuery(
                                    runBackgroundQueryAfter,
                                    String.valueOf(searchLocationBox.getQuery()),
                                    String.valueOf(filterBox.getText()))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                }
            }
        });

        // Reset all cache for showcase id.
        //MaterialShowcaseView.resetAll(getContext());

        // A tutorial that displays only once explaining the input to the app.
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "MAIN");
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        sequence.setConfig(config);

        sequence.addSequenceItem(buildShowcaseView(searchLocationBox, new RectangleShape(0, 0),
                "Enter any zip code, city, or address here.\n\n" +
                        "Click the GPS icon to use your current location.\n\n" +
                        "Filter your results by clicking the magnifying glass if you're in the mood for something specific."
        ));

        sequence.start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
        outState.putString("locationQuery", searchLocationBox.getQuery());
        outState.putString("filterQuery", String.valueOf(filterBox.getText()));
        outState.putParcelable("currentRestaurant", currentRestaurant);
        outState.putParcelableArrayList("restaurants", restaurants);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null && initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING) {
            initialYelpQuery.cancel(true);
            enableGenerateButton();

            if (mainRestaurantCardAdapter != null) {
                mainRestaurantCardAdapter.remove();
                mapCardContainer.setVisibility(View.GONE);
            }
        }

        if (backgroundYelpQuery != null && backgroundYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
            backgroundYelpQuery.cancel(true);
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // Refresh the RecyclerView on resume.
        if (mainRestaurantCardAdapter != null)
            mainRestaurantCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        Log.d("RRG", "Destroyed");

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null && initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
            initialYelpQuery.cancel(true);

        if (backgroundYelpQuery != null && backgroundYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
            backgroundYelpQuery.cancel(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // Google Maps API callback for MapFragment.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }

    // Callback for requesting permissions.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        locationHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        locationHelper.onPermissionsDenied(requestCode, perms);
    }

    // Callback for checking location settings.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LocationProviderHelper.MY_REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Toast.makeText(getActivity(), "Location enabled by user!", Toast.LENGTH_LONG).show();
                        locationHelper.requestLocation();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(getActivity(), "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        locationHelper.dismissLocationUpdater();
                        break;
                    }
                    default: {
                        Toast.makeText(getActivity(), "Never ask for location. " + requestCode, Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getContext(), "Error " + connectionResult.getErrorCode() +
                ": failed to connect to Google Play Services", Toast.LENGTH_LONG).show();
    }

    /**
     * Function to hide the keyboard.
     */
    private void hideSoftKeyboard() {
        Activity activity = getActivity();
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Helper function to build a custom ShowcaseView for a sequence.
     *
     * @param target:      the target view that will be highlighted.
     * @param shape:       the type of shape.
     * @param contentText: the text to be displayed.
     */
    private MaterialShowcaseView buildShowcaseView(View target, uk.co.deanwild.materialshowcaseview.shape.Shape shape, String contentText) {
        return new MaterialShowcaseView.Builder(getActivity())
                .setTarget(target)
                .setShape(shape)
                .setMaskColour(Color.rgb(0, 166, 237))
                .setContentText(contentText)
                .setDismissText("GOT IT")
                .build();
    }

    /**
     * Function to disable the generate button.
     */
    private void disableGenerateButton() {
        // Signal the task is running
        taskRunning = true;
        generate.setText(R.string.string_button_text_loading);
        generate.setBackgroundColor(Color.GRAY);
        generate.setEnabled(false);
    }

    /**
     * Function to enable the generate button.
     */
    private void enableGenerateButton() {
        // Signal the task is finished
        taskRunning = false;
        generate.setText(R.string.string_button_text_generate);
        generate.setBackgroundColor(generateBtnColor);
        generate.setEnabled(true);

        progressBar.setVisibility(View.GONE);
        progressBar.progressiveStop();
    }

    /**
     * Function to display the MaterialShowcaseView for filterBox.
     */
    private void displayShowcaseViewFilterBox() {
        MaterialShowcaseSequence filterShowcase = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "FILTER");
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        filterShowcase.setConfig(config);

        filterShowcase.addSequenceItem(buildShowcaseView(filterBox, new RectangleShape(0, 0),
                "In the mood for multiple things? List your filters separated by a comma to combine the results!"
        ));

        filterShowcase.start();
    }

    /**
     * Helper function to display an AlertDialog
     * @param stringToDisplay:  the string to display in the alert.
     * @param title:            the title of the dialog.
     */
    private void displayAlertDialog(int stringToDisplay, String title) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.setTitle(title);
        alert.setMessage(getActivity().getString(stringToDisplay));
        alert.show();
    }

    /**
     * Function to query Yelp for restaurants. Returns an ArrayList of Restaurants.
     *
     * @param lat:              the user's latitude, null if @param input is not empty.
     * @param lon:              the user's longitude, null if @param input is not empty.
     * @param input             the user's location string, e.g. zip, city, etc.
     * @param filter            the user's filterBox string, e.g. sushi, bbq, etc.
     * @param offset            the offset for the Yelp query.
     * @param whichAsyncTask    the AsyncTask to cancel if necessary.
     * @return true if successful querying Yelp; false otherwise.
     */
    private boolean queryYelp(String lat, String lon, String input,
                              String filter, int offset, int whichAsyncTask) {
        try {
            OAuthRequest request;

            if (LocationProviderHelper.useGPS) {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + filter +
                        "&ll=" + lat + "," + lon + "&offset=" + offset);

                request.setConnectTimeout(10, TimeUnit.SECONDS);
                request.setReadTimeout(10, TimeUnit.SECONDS);

                Log.d("RRG", "request made: " + "https://api.yelp.com/v2/search?" + filter +
                        "&ll=" + lat + "," + lon + "&offset=" + offset);
            } else {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + filter +
                        "&location=" + input + "&offset=" + offset);

                request.setConnectTimeout(10, TimeUnit.SECONDS);
                request.setReadTimeout(10, TimeUnit.SECONDS);

                Log.d("RRG", "request made: " + "https://api.yelp.com/v2/search?" + filter +
                        "&location=" + input + "&offset=" + offset);
            }

            service.signRequest(accessToken, request);
            Response response = request.send();

            // Get JSON array that holds the restaurants from Yelp.
            JSONArray jsonBusinessesArray = new JSONObject(response.getBody())
                    .getJSONArray("businesses");

            int length = jsonBusinessesArray.length();

            // This occurs if a network communication error occurs or if no restaurants were found.
            if (length <= 0) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (whichAsyncTask == 0 && initialYelpQuery.isCancelled())
                    break;
                else if (whichAsyncTask == 1 && backgroundYelpQuery.isCancelled())
                    break;


                Restaurant res = convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(i));
                if (res != null)
                    restaurants.add(res);
            }

            if (restaurants.isEmpty()) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                return false;
            }

        } catch (OAuthConnectionException e) {
            errorInQuery = TypeOfError.NETWORK_CONNECTION_ERROR;
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            if (e.getMessage().contains("No value for businesses"))
                errorInQuery = TypeOfError.NO_RESTAURANTS;
            else if (e.getMessage().contains("No value for"))
                errorInQuery = TypeOfError.MISSING_INFO;

            e.printStackTrace();
            return false;
        } catch (Exception e) {
            if (e.getMessage().contains("timed out")) errorInQuery = TypeOfError.TIMED_OUT;
            e.printStackTrace();
            return false;
        }

        errorInQuery = TypeOfError.NO_ERROR;
        return true;
    }

    /**
     * Convert JSON to a Restaurant object that encapsulates a restaurant from Yelp.
     *
     * @param obj: JSONObejct that holds all restaurant info.
     * @return Restaurant or null if an error occurs.
     */
    private Restaurant convertJSONToRestaurant(JSONObject obj) {
        try {
            // Getting the JSON array of categories
            JSONArray categoriesJSON = obj.getJSONArray("categories");
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < categoriesJSON.length(); i += 2)
                for (int j = 0; j < categoriesJSON.getJSONArray(i).length(); j += 2)
                    categories.add(categoriesJSON.getJSONArray(i).getString(j));

            // Getting the restaurant's location information
            JSONObject locationJSON = obj.getJSONObject("location");

            double lat = locationJSON.getJSONObject("coordinate").getDouble("latitude");
            double lon = locationJSON.getJSONObject("coordinate").getDouble("longitude");

            float distance;
            Location restaurantLoc = new Location("restaurantLoc");
            restaurantLoc.setLatitude(lat);
            restaurantLoc.setLongitude(lon);
            if (LocationProviderHelper.useGPS) {
                distance = locationHelper.getLocation().distanceTo(restaurantLoc);
            } else {
                Geocoder geocoder = new Geocoder(getContext());

                List<Address> addressList;
                try {
                    addressList = geocoder.getFromLocationName(searchLocationBox.getQuery(), 1);
                    double estimatedLat = addressList.get(0).getLatitude();
                    double estimatedLon = addressList.get(0).getLongitude();
                    Location estimatedLocation = new Location("estimatedLocation");
                    estimatedLocation.setLatitude(estimatedLat);
                    estimatedLocation.setLongitude(estimatedLon);
                    distance = estimatedLocation.distanceTo(restaurantLoc);
                } catch (IOException io) {
                    /*
                     * If we get an IOException, that means geocoder.getFromLocationName() timed out.
                     * Sometimes it times out, so set distance to 0 if it does.
                     * See below bug report:
                     * https://code.google.com/p/gmaps-api-issues/issues/detail?id=9153
                     */
                    distance = 0.0f;
                }
            }
            distance *= 0.000621371;    // Convert to miles

            // Getting restaurant's address
            JSONArray addressJSON = locationJSON.getJSONArray("display_address");
            ArrayList<String> address = new ArrayList<>();

            for (int i = 0; i < addressJSON.length(); i++)
                address.add(addressJSON.getString(i));

            // Get deals if JSON contains deals object.
            String deals;
            try {
                JSONArray dealsArray = obj.getJSONArray("deals");
                ArrayList<String> dealsList = new ArrayList<>();

                for (int i = 0; i < dealsArray.length(); i++) {
                    JSONObject jsonObject = dealsArray.getJSONObject(i);
                    dealsList.add(jsonObject.getString("title"));
                }
                deals = dealsList.toString().replace("[", "").replace("]", "").trim();
            } catch (Exception ignored) {
                deals = "";
            }

            // Construct a new Restaurant object with all the info we gathered above and return it
            return new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                    obj.getString("rating_img_url_large"), obj.getString("image_url"), obj.getInt("review_count"),
                    obj.getString("url"), categories, address, deals, distance, lat, lon);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update the map with a new marker based on restaurant's coordinates.
     *
     * @param restaurant the restaurant that will be on the map.
     */
    private void updateMapWithRestaurant(Restaurant restaurant) {
        // Clear all the markers on the map.
        map.clear();

        LatLng latLng = new LatLng(restaurant.getLat(), restaurant.getLon());

        map.addMarker(new MarkerOptions().position(latLng).title(String.format("%s: %s",
                restaurant.getName(), restaurant.getAddress())
                .replace("[", "").replace("]", "").trim()));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

        mapCardContainer.setVisibility(View.VISIBLE);
    }

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class RunYelpQuery extends AsyncTask<Void, Void, Restaurant> {

        String[] params;
        String userInputStr;
        String userFilterStr;
        boolean successfulQuery;

        public RunYelpQuery(boolean runBackgroundQuery, String... params) {
            runBackgroundQueryAfter = runBackgroundQuery;
            this.params = params;
            userInputStr = params[0];
            userFilterStr = params[1];
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            disableGenerateButton();

            if (restartQuery) {
                ((CircularProgressDrawable) progressBar.getIndeterminateDrawable()).start();
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d("RRG", "Cancelled Yelp AsyncTask");
            restartQuery = true;
            enableGenerateButton();
        }

        @Override
        protected Restaurant doInBackground(Void... aVoid) {

            if (isCancelled()) return null;

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";

            try {
                // If the user entered some input, make sure to encode all spaces and "+" for URL query.
                if (userInputStr.length() != 0) {
                    userInputStr = userInputStr.replaceAll(" ", "+");
                }

                // If the user entered a filterBox, make sure to encode all spaces and "+" for URL query.
                if (userFilterStr.length() != 0) {
                    userFilterStr = userFilterStr.replaceAll(" ", "+");

                    // Add Yelp API query verb.
                    userFilterStr = String.format("term=%s", userFilterStr);
                } else {
                    // Default search term.
                    userFilterStr = "term=food";
                }

                // If we have 4 parameters, then the user selected location and we must grab the lat / long.
                if (params.length == 4) {
                    lat = params[2];
                    lon = params[3];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (restartQuery) {
                restaurants.clear();
                restartQuery = false;
            }

            // Get restaurants only when the restaurants list is empty.
            Restaurant chosenRestaurant = null;
            if (restaurants == null || restaurants.isEmpty()) {
                successfulQuery = queryYelp(lat, lon, userInputStr, userFilterStr, 0, 0);

                if (successfulQuery && !restaurants.isEmpty()) {
                    // Make sure the restaurants list is not empty before accessing it.
                    chosenRestaurant = restaurants.get(new Random().nextInt(restaurants.size()));
                    restaurants.remove(chosenRestaurant);

                    /**
                     * Run background query only if the following BOTH hold:
                     *      1) initially set to true from constructor
                     *      2) and successfulQuery is true
                     */
                    if (runBackgroundQueryAfter)
                        runBackgroundQueryAfter = true;
                }
            } else if (restaurants != null && !restaurants.isEmpty()) {
                chosenRestaurant = restaurants.get(new Random().nextInt(restaurants.size()));
                restaurants.remove(chosenRestaurant);
            }

            // Return randomly chosen restaurant.
            return chosenRestaurant;
        }

        // Set UI appropriate UI elements to display mRestaurant info.
        @Override
        protected void onPostExecute(Restaurant restaurant) {

            if (restaurant == null) {
                if (errorInQuery == TypeOfError.NO_RESTAURANTS) {
                    Toast.makeText(getContext(),
                            R.string.string_no_restaurants_found,
                            Toast.LENGTH_LONG).show();
                    restaurants.clear();
                } else if (errorInQuery == TypeOfError.MISSING_INFO) {
                    // Try again if the current restaurant has missing info.
                    generate.performClick();
                    return;
                }
                else if (errorInQuery == TypeOfError.NETWORK_CONNECTION_ERROR) {
                    Toast.makeText(getContext(),
                            R.string.string_no_network,
                            Toast.LENGTH_LONG).show();
                }
                else if (errorInQuery == TypeOfError.TIMED_OUT) {
                    Toast.makeText(getContext(),
                            R.string.string_timed_out_msg,
                            Toast.LENGTH_LONG).show();
                }

                if (initialYelpQuery!= null) initialYelpQuery.cancel(true);
                if (backgroundYelpQuery!= null) backgroundYelpQuery.cancel(true);

                enableGenerateButton();
                return;
            }

            currentRestaurant = restaurant;

            // If the RecyclerView has not been set yet, set it with the currentRestaurant.
            if (mainRestaurantCardAdapter == null) {
                mainRestaurantCardAdapter = new MainRestaurantCardAdapter(getContext(), currentRestaurant);
                restaurantView.setAdapter(mainRestaurantCardAdapter);
            }

            // These calls notify the RecyclerView that the data set has changed and we need to refresh.
            mainRestaurantCardAdapter.remove();
            mainRestaurantCardAdapter.add(currentRestaurant);

            updateMapWithRestaurant(currentRestaurant);
            enableGenerateButton();

            if (runBackgroundQueryAfter) {
                backgroundYelpQuery = new RunYelpQueryBackground(20).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.params);
                runBackgroundQueryAfter = false;
            }

            // A tutorial that displays only once explaining the action that can be done on the restaurant card.
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "CARD");
            ShowcaseConfig config = new ShowcaseConfig();
            config.setDelay(100);
            sequence.setConfig(config);

            sequence.addSequenceItem(buildShowcaseView(restaurantView, new RectangleShape(0, 0),
                    "Swipe left to dismiss. Swipe right to open in Yelp. Tap bookmark button to save it for later."));

            sequence.addSequenceItem(buildShowcaseView(((MainActivity) getActivity()).getMenuItemView(R.id.action_saved_list),
                    new CircleShape(), "Tap here to view your saved list."));

            sequence.start();
        }
    }

    // Async task that runs in the background to query more restaurants from Yelp while the user
    // goes through the initial list of restaurants.
    public class RunYelpQueryBackground extends AsyncTask<String, Void, Void> {

        int offset;

        public RunYelpQueryBackground(int offset) {
            this.offset = offset;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d("RRG", "Cancelled background Yelp AsyncTask");
        }

        @Override
        protected Void doInBackground(String... params) {

            if (isCancelled()) return null;

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";
            String userInputStr = params[0];
            String userFilterStr = params[1];

            try {
                // If the user entered some input, make sure to encode all spaces and "+" for URL query.
                if (userInputStr.length() != 0) {
                    userInputStr = userInputStr.replaceAll(" ", "+");
                }

                // If the user entered a filterBox, make sure to encode all spaces and "+" for URL query.
                if (userFilterStr.length() != 0) {
                    userFilterStr = userFilterStr.replaceAll(" ", "+");

                    // Add Yelp API query verb.
                    userFilterStr = String.format("term=%s", userFilterStr);
                } else {
                    // Default search term.
                    userFilterStr = "term=food";
                }

                // If we have 4 parameters, then the user selected location and we must grab the lat / long.
                if (params.length == 4) {
                    lat = params[2];
                    lon = params[3];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            queryYelp(lat, lon, userInputStr, userFilterStr, offset, 1);

            return null;
        }
    }
}