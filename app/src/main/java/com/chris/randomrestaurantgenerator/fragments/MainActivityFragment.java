package com.chris.randomrestaurantgenerator.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.chris.randomrestaurantgenerator.BuildConfig;
import com.chris.randomrestaurantgenerator.MainActivity;
import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.managers.UnscrollableLinearLayoutManager;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.utils.LocationProviderHelper;
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

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
        GoogleApiClient.OnConnectionFailedListener, EasyPermissions.PermissionCallbacks,
        TimePickerFragment.TimePickerCallbacks {

    ArrayList<Restaurant> restaurants = new ArrayList<>();
    AsyncTask initialYelpQuery;
    boolean restartQuery = true;
    boolean taskRunning = false;
    Button generate;
    CheckBox priceFour;
    CheckBox priceOne;
    CheckBox priceThree;
    CheckBox priceTwo;
    CircularProgressBar progressBar;
    EditText filterBox;
    FloatingSearchView searchLocationBox;
    GoogleApiClient mGoogleApiClient;
    GoogleMap map;
    int errorInQuery;
    int generateBtnColor;
    LinearLayout filtersLayout;
    LinearLayout mapCardContainer;
    LinearLayout priceFilterLayout;
    LocationProviderHelper locationHelper;
    long openAtTimeFilter = 0;
    MainRestaurantCardAdapter mainRestaurantCardAdapter;
    MapView mapView;
    RecyclerView restaurantView;
    RelativeLayout rootLayout;
    Restaurant currentRestaurant;
    String accessToken;
    String filterQuery = "";
    String searchQuery = "";
    ToggleButton pickTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);
        filtersLayout = (LinearLayout) rootLayout.findViewById(R.id.filtersLayout);
        priceFilterLayout = (LinearLayout) filtersLayout.findViewById(R.id.priceFilterLayout);
        restaurantView = (RecyclerView) rootLayout.findViewById(R.id.restaurantView);
        restaurantView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));

        mapCardContainer = (LinearLayout) rootLayout.findViewById(R.id.cardMapLayout);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mapView.onCreate(mapViewSavedInstanceState);

        filterBox = (EditText) rootLayout.findViewById(R.id.filterBox);
        priceOne = (CheckBox) rootLayout.findViewById(R.id.priceOne);
        priceTwo = (CheckBox) rootLayout.findViewById(R.id.priceTwo);
        priceThree = (CheckBox) rootLayout.findViewById(R.id.priceThree);
        priceFour = (CheckBox) rootLayout.findViewById(R.id.priceFour);
        pickTime = (ToggleButton) rootLayout.findViewById(R.id.pickTime);
        generate = (Button) rootLayout.findViewById(R.id.generate);
        generateBtnColor = Color.parseColor("#F6511D");

        // Yelp API access token
        accessToken = BuildConfig.API_ACCESS_TOKEN;

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
            Log.d("RRG", "restored saved instance state");
        }

        // Define actions on menu button clicks inside searchLocationBox.
        searchLocationBox.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                int id = item.getItemId();
                restartQuery = true;

                if (id == R.id.search_box_gps)
                    locationHelper.requestLocation();
                else if (id == R.id.search_box_filter) {
                    if (filtersLayout.getVisibility() == View.GONE) {
                        showFilterElements();

                        // Show tutorial about entering multiple filters.
                        displayShowcaseViewFilterBox();
                    } else if (filtersLayout.getVisibility() == View.VISIBLE) {
                        showNormalLayout();
                    }
                }
            }
        });

        searchLocationBox.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {}

            @Override
            public void onSearchAction(String currentQuery) {
                restartQuery = true;
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
                    return true;
                }
                return false;
            }
        });

        pickTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    showTimePickerDialog();
                else
                    openAtTimeFilter = 0;
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

                showFilterElements();
                showNormalLayout();

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

                    searchQuery = searchLocationBox.getQuery();
                    filterQuery = filterBox.getText().toString();

                    // We want to use GPS if searchQuery contains the string "Current Location".
                    LocationProviderHelper.useGPS = searchQuery.contains(getActivity()
                            .getString(R.string.string_current_location));
                }

                // Replace all spaces from filters for Yelp query.
                String filterBoxText = filterBox.getText().toString().replaceAll(" ", "");

                if (LocationProviderHelper.useGPS) {
                    /**
                     * Check to make sure the location is not null before starting.
                     * Else, begin the AsyncTask.
                     */
                    Location location = locationHelper.getLocation();
                    if (location == null) {
                        displayAlertDialog(R.string.string_location_not_found, "Error");
                    } else {
                            initialYelpQuery = new RunYelpQuery(
                                    filterBoxText,
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {
                    /**
                     * Verify that the user has actually entered a location.
                     * Else, begin the AsyncTask.
                     */
                    if (searchLocationBox.getQuery().length() == 0 && restaurants.size() == 0) {
                        displayAlertDialog(R.string.string_enter_valid_location, "Error");
                    } else {
                            initialYelpQuery = new RunYelpQuery(
                                    String.valueOf(searchLocationBox.getQuery()),
                                    filterBoxText)
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            searchLocationBox.setSearchText(savedInstanceState.getString("locationQuery"));
            filterBox.setText(savedInstanceState.getString("filterQuery"));
            currentRestaurant = savedInstanceState.getParcelable("currentRestaurant");
            restaurants = savedInstanceState.getParcelableArrayList("restaurants");
        }

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

        locationHelper.pauseAndSaveLocationUpdates();
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

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null && initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
            initialYelpQuery.cancel(true);

        Log.d("RRG", "onDestroy");
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
            case LocationProviderHelper.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Log.d("RRG", "Location enabled by user!");
                        locationHelper.requestLocation();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Log.d("RRG", "Location not enabled, user cancelled.");
                        locationHelper.dismissLocationUpdater();
                        break;
                    }
                    default: {
                        Log.d("RRG", "User opted to never ask for location. ");
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("RRG", "Connected to Google Play Services.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("RRG", "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getContext(), "Error " + connectionResult.getErrorCode() +
                        ": failed to connect to Google Play Services. This may affect acquiring your location.",
                Toast.LENGTH_LONG).show();
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
                .setMaskColour(Color.parseColor("#1A6C9D"))
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

    private void showFilterElements() {
        filtersLayout.setVisibility(View.VISIBLE);
        mapCardContainer.setVisibility(View.GONE);
        restaurantView.setVisibility(View.GONE);
    }

    private void showNormalLayout() {
        filtersLayout.setVisibility(View.GONE);
        mapCardContainer.setVisibility(View.VISIBLE);
        restaurantView.setVisibility(View.VISIBLE);
    }

    private void hideNormalLayout() {
        mapCardContainer.setVisibility(View.GONE);
        restaurantView.setVisibility(View.GONE);
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

        // Build Yelp request.
        try {
            URL url;
            HttpURLConnection urlConnection;
            String requestUrl = "https://api.yelp.com/v3/businesses/search";
            StringBuilder builder = new StringBuilder(requestUrl);

            builder.append("?term=").append("food");
            builder.append("&limit=" + 50);
            builder.append("&offset=").append(offset);

            // Check if user wants to filter by categories.
            if (filter.length() != 0)
                builder.append("&categories=").append(filter);

            // Check if the user wants to filter by price range.
            StringBuilder priceBuilder = new StringBuilder("");
            ArrayList<String> priceRange = new ArrayList<>();

            if (priceOne.isChecked() || priceTwo.isChecked() ||
                    priceThree.isChecked() || priceFour.isChecked())
                builder.append("&price=");

            if (priceOne.isChecked()) priceRange.add("1");
            if (priceTwo.isChecked()) priceRange.add("2");
            if (priceThree.isChecked()) priceRange.add("3");
            if (priceFour.isChecked()) priceRange.add("4");

            // Making sure to prevent trailing commas in price range list.
            for (String elem : priceRange) {
                priceBuilder.append(elem);
                if (priceRange.indexOf(elem) != (priceRange.size() - 1))
                    priceBuilder.append(",");
            }
            builder.append(priceBuilder.toString());

            if (openAtTimeFilter != 0)
                builder.append("&open_at=").append(openAtTimeFilter);

            if (LocationProviderHelper.useGPS) {
                builder.append("&latitude=").append(lat);
                builder.append("&longitude=").append(lon);
                requestUrl = builder.toString();
                Log.d("RRG", "request made: " + requestUrl);
            } else {
                builder.append("&location=").append(input);
                requestUrl = builder.toString();
                Log.d("RRG", "request made: " + requestUrl);
            }

            url = new URL(requestUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Authorization", String.format("Bearer %s", accessToken));
            urlConnection.setConnectTimeout(10 * 1000);
            urlConnection.setReadTimeout(10 * 1000);

            // Make connection and read the response.
            urlConnection.connect();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String buf, jsonString;
            while ((buf = br.readLine()) != null)
                sb.append(buf);
            br.close();
            jsonString = sb.toString();

            JSONObject response = new JSONObject(jsonString);

            // Get JSON array that holds the listings from Yelp.
            JSONArray jsonBusinessesArray = response.getJSONArray("businesses");
            int length = jsonBusinessesArray.length();
            Log.d("RRG", "length: " + length);

            // This occurs if a network communication error occurs or if no restaurants were found.
            if (length <= 0) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (whichAsyncTask == 0 && initialYelpQuery.isCancelled())
                    break;

                Restaurant res = convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(i));
                if (res != null)
                    restaurants.add(res);
            }

            if (restaurants.isEmpty()) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                return false;
            }

        } catch (JSONException e) {
            if (e.getMessage().contains("No value for businesses"))
                errorInQuery = TypeOfError.NO_RESTAURANTS;
            else if (e.getMessage().contains("No value for"))
                errorInQuery = TypeOfError.MISSING_INFO;
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            errorInQuery = TypeOfError.INVALID_LOCATION;
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
     * @param obj: JSONObject that holds all restaurant info.
     * @return Restaurant or null if an error occurs.
     */
    private Restaurant convertJSONToRestaurant(JSONObject obj) {
        try {
            Log.d("RRG", "convertJSONToRestaurant: " + obj);
            // Getting the JSON array of categories
            JSONArray categoriesJSON = obj.getJSONArray("categories");
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < categoriesJSON.length(); i++)
                categories.add(categoriesJSON.getJSONObject(i).getString("title"));

            // Getting the restaurant's coordinates and price
            double lat = obj.getJSONObject("coordinates").getDouble("latitude");
            double lon = obj.getJSONObject("coordinates").getDouble("longitude");
            double distance = obj.getDouble("distance") * 0.000621371; // Convert to miles

            // Getting restaurant's address
            JSONObject locationJSON = obj.getJSONObject("location");
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

            // If restaurant doesn't have a price, put a question mark.
            String price;
            try {
                price = obj.getString("price");
            } catch (Exception ignored) {
                price = "?";
            }

            // If listing does not have an image, make sure it routes to localhost because
            // Picasso will complain when a URL string is empty.
            String imageUrl = obj.getString("image_url");
            if (imageUrl.length() == 0)
                imageUrl = "localhost";

            // Construct a new Restaurant object with all the info we gathered above and return it
            return new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                    imageUrl, obj.getInt("review_count"), obj.getString("url"),
                    categories, address, deals, price, distance, lat, lon);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("RRG", "convertJSONToRestaurant: " + e.getMessage());
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

    /**
     * This function launches the TimePicker dialog from the @Fragment TimePickerFragment.
     * Set the callback listener to this class so we can receive the result in
     *
     * @func timePickerDataCallback()
     */
    public void showTimePickerDialog() {
        TimePickerFragment tpf = new TimePickerFragment();
        tpf.setListener(this);

        //DialogFragment newFragment = new TimePickerFragment();
        tpf.show(getActivity().getSupportFragmentManager(), "timePicker");
    }

    @Override
    public void timePickerDataCallback(long data) {
        Log.d("RRG", "RECEIVED: " + data);

        // If we received a 0, then the user cancel out of the dialog.
        if (data == 0) {
            pickTime.setChecked(false);
            return;
        }

        openAtTimeFilter = data;

        Date date = new Date(data * 1000L);
        /*
         * Suppressing warning because we handle the timezone issue below
         * by setting the default timezone manually.
         */
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        sdf.setTimeZone(TimeZone.getDefault());
        String formattedDate = sdf.format(date);
        Log.d("RRG", "formattedDate: " + formattedDate);

        // https://stackoverflow.com/a/3792554/2193236
        pickTime.setTextOn(formattedDate);
        pickTime.setChecked(pickTime.isChecked());
    }

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class RunYelpQuery extends AsyncTask<Void, Void, Restaurant> {

        String[] params;
        String userInputStr;
        String userFilterStr;
        boolean successfulQuery;

        RunYelpQuery(String... params) {
            this.params = params;

            if (params.length == 2) {
                userInputStr = params[0];
                userFilterStr = params[1];
            } else if (params.length == 3) {
                userInputStr = "";
                userFilterStr = params[0];
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            disableGenerateButton();

            if (restartQuery) {
                ((CircularProgressDrawable) progressBar.getIndeterminateDrawable()).start();
                hideNormalLayout();
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

                // If we have 3 parameters, then the user selected location and we must grab the lat / long.
                if (params.length == 3) {
                    lat = params[1];
                    lon = params[2];
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
                switch (errorInQuery) {
                    case TypeOfError.NO_RESTAURANTS: {
                        Toast.makeText(getContext(),
                                R.string.string_no_restaurants_found,
                                Toast.LENGTH_LONG).show();
                        restaurants.clear();
                        break;
                    }

                    case TypeOfError.MISSING_INFO: {
                        // Try again if the current restaurant has missing info.
                        generate.performClick();
                        return;
                    }

                    case TypeOfError.NETWORK_CONNECTION_ERROR: {
                        Toast.makeText(getContext(),
                                R.string.string_no_network,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    case TypeOfError.TIMED_OUT: {
                        Toast.makeText(getContext(),
                                R.string.string_timed_out_msg,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    case TypeOfError.INVALID_LOCATION: {
                        Toast.makeText(getContext(),
                                R.string.string_no_restaurants_found,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    default:
                        break;
                }

                if (initialYelpQuery!= null) initialYelpQuery.cancel(true);

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

            showNormalLayout();
            updateMapWithRestaurant(currentRestaurant);
            enableGenerateButton();

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
}