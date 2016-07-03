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
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.chris.randomrestaurantgenerator.views.MainRestaurantCardAdapter;
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
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;
import uk.co.deanwild.materialshowcaseview.shape.CircleShape;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;

/**
 * A fragment containing the main activity.
 * Responsible for displaying to the user a random restaurant based off their location  / zip code.
 */
public class MainActivityFragment extends Fragment implements OnMapReadyCallback {

    FloatingSearchView searchLocationBox;
    EditText filterBox;
    Button generate;

    RelativeLayout rootLayout;
    RecyclerView recyclerView;

    MainRestaurantCardAdapter mainRestaurantCardAdapter;

    Restaurant currentRestaurant;
    LocationProviderHelper locationHelper;

    LinearLayout mapCardContainer;
    MapView mapView;
    GoogleMap map;

    OAuthService service;
    Token accessToken;

    int generateBtnColor;
    boolean taskRunning = false;
    boolean runSecondQuery = true;
    boolean restartQuery = false;
    ArrayList<Restaurant> restaurants = new ArrayList<>();

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
        recyclerView = (RecyclerView) rootLayout.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));

        mapCardContainer = (LinearLayout) rootLayout.findViewById(R.id.cardMapLayout);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mapView.onCreate(mapViewSavedInstanceState);

        filterBox = (EditText) rootLayout.findViewById(R.id.filter);
        generate = (Button) rootLayout.findViewById(R.id.generate);
        generateBtnColor = Color.parseColor("#F6511D");

        // Build OAuth request
        service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(TwoStepOAuth.getConsumerKey())
                .apiSecret(TwoStepOAuth.getConsumerSecret()).build();
        accessToken = new Token(TwoStepOAuth.getToken(), TwoStepOAuth.getTokenSecret());

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

                    // We don't want to remove the restaurant here, so we add it back to the recyclerView.
                    mainRestaurantCardAdapter.remove();
                    mainRestaurantCardAdapter.add(currentRestaurant);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return rootLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Must be defined in onActivityCreated() because searchLocationBox is part of MainActivity.
        searchLocationBox = (FloatingSearchView) getActivity().findViewById(R.id.searchBox);

        // Create LocationProviderHelper instance.
        locationHelper = new LocationProviderHelper(getActivity(), rootLayout, searchLocationBox);

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        if (savedInstanceState != null) {
            currentRestaurant = savedInstanceState.getParcelable("currentRestaurant");
            searchLocationBox.setSearchText(savedInstanceState.getString("locationQuery"));
            filterBox.setText(savedInstanceState.getString("filterQuery"));
            restaurants = savedInstanceState.getParcelableArrayList("restaurants");
            Log.d("CHRIS", "restored state");
        }

        // Define actions on menu button clicks inside searchLocationBox.
        searchLocationBox.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.search_box_gps)
                    locationHelper.requestLocation();
                else if (id == R.id.search_box_filter) {
                    if (filterBox.getVisibility() == View.GONE)
                        filterBox.setVisibility(View.VISIBLE);
                    else if (filterBox.getVisibility() == View.VISIBLE)
                        filterBox.setVisibility(View.GONE);
                }
            }
        });

        searchLocationBox.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {

            }

            @Override
            public void onSearchAction(String currentQuery) {
                searchLocationBox.setSearchBarTitle(currentQuery);
            }
        });

        searchLocationBox.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {
                if (oldQuery.compareTo(newQuery) != 0)
                    restartQuery = true;
            }
        });

        // Listener for when the user clicks done on keyboard after their input.
        filterBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideSoftKeyboard(getActivity());
                    generate.performClick();
                    return true;
                }
                return false;
            }
        });

        filterBox.addTextChangedListener(new TextWatcher() {

            String oldQuery;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldQuery = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().compareTo(oldQuery) != 0)
                    restartQuery = true;
            }
        });

        // When the user clicks the Generate button.
        generate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // If the user doesn't wait on the task to complete, warn them it is still running
                // so we can prevent a long stack of requests from piling up.
                if (taskRunning) {
                    Toast.makeText(getActivity(), "Task still running, please wait.", Toast.LENGTH_SHORT).show();
                    return;
                }

                filterBox.setVisibility(View.GONE);

                // Clear all the markers on the map.
                map.clear();

                if (LocationProviderHelper.useGPS) {

                    // If the user is using location, check to make sure the location is not null before starting.
                    // Else, begin the AsyncTask.
                    Location location = locationHelper.getLocation();
                    if (location == null) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.setTitle("Error");
                        alert.setMessage("Location has not been found yet. Please try again. " +
                                "Acquiring GPS signal may take up to a minute on some devices.");
                        alert.show();
                    } else {
                        initialYelpQuery = new RunYelpQuery(String.valueOf(searchLocationBox.getQuery()),
                                String.valueOf(filterBox.getText()),
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude())).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {

                    // If the user is entering their location, check to make sure they have entered one.
                    // Else, begin the AsyncTask.
                    if (searchLocationBox.getQuery().length() == 0 && restaurants.size() == 0) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.setTitle("Error");
                        alert.setMessage("Please enter a valid address, city, zip code, or use GPS by clicking the icon.");
                        alert.show();
                    } else {
                        initialYelpQuery = new RunYelpQuery(String.valueOf(searchLocationBox.getQuery()),
                                String.valueOf(filterBox.getText())).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        });

        // Reset all cache for showcase id.
        //MaterialShowcaseView.resetAll(getContext());

        // A tutorial that displays only once explaining the input to the app.
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME);
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        sequence.setConfig(config);

        sequence.addSequenceItem(buildShowcaseView(searchLocationBox, new RectangleShape(0, 0),
                "Enter any zip code, city, or address here.\n\n" +
                "Click the GPS icon to use your current location.\n\n" +
                "Filter your results if you're in the mood for something specific."
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
        Log.d("CHRIS", "saved state");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null) {
            if (initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING) {
                initialYelpQuery.cancel(true);
                enableGenerateButton();

                if (mainRestaurantCardAdapter != null) {
                    mainRestaurantCardAdapter.remove();
                    mapCardContainer.setVisibility(View.GONE);
                }
            }
        }

        if (backgroundYelpQuery != null)
            if (backgroundYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
                backgroundYelpQuery.cancel(true);
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
        if (initialYelpQuery != null) {
            if (initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING) {
                initialYelpQuery.cancel(true);
                enableGenerateButton();

                if (mainRestaurantCardAdapter != null) {
                    mainRestaurantCardAdapter.remove();
                    mapCardContainer.setVisibility(View.GONE);
                }
            }
        }

        if (backgroundYelpQuery != null)
            if (backgroundYelpQuery.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundYelpQuery.cancel(true);
                enableGenerateButton();
            }
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

    /**
     * Function to hide the keyboard.
     *
     * @param activity: current Activity.
     */
    private void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Helper function to build a custom ShowcaseView for a sequence.
     *
     * @param target:      the target view that will be highlighted
     * @param shape:       the type of shape
     * @param contentText: the text to be displayed
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

    private void disableGenerateButton() {
        // Signal the task is running
        taskRunning = true;
        generate.setText(R.string.string_button_text_loading);
        generate.setBackgroundColor(Color.GRAY);
        generate.setEnabled(false);
    }

    private void enableGenerateButton() {
        // Signal the task is finished
        taskRunning = false;
        generate.setText(R.string.string_button_text_generate);
        generate.setBackgroundColor(generateBtnColor);
        generate.setEnabled(true);
    }

    /**
     * Function to query Yelp for restaurants. Returns an ArrayList of Restaurants.
     * @param lat:      the user's latitude, null if @param input is not empty.
     * @param lon:      the user's longitude, null if @param input is not empty..
     * @param input     the user's location string, e.g. zip, city, etc.
     * @param filter    the user's filter string, e.g. sushi, bbq, etc.
     * @param offset    the offset for the Yelp query.
     * @return          true if successful querying Yelp; false otherwise.
     */
    private boolean queryYelp(String lat, String lon, String input,
                           String filter, int offset) {

        OAuthRequest request;

        if (LocationProviderHelper.useGPS) {
            request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + filter +
                    "&ll=" + lat + "," + lon + "&offset=" + offset);

            request.setConnectTimeout(10, TimeUnit.SECONDS);
            request.setReadTimeout(10, TimeUnit.SECONDS);

            Log.d("Chris", "request made: " + "https://api.yelp.com/v2/search?" + filter +
                    "&ll=" + lat + "," + lon + "&offset=" + offset);
        } else {
            request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + filter +
                    "&location=" + input + "&offset=" + offset);

            request.setConnectTimeout(10, TimeUnit.SECONDS);
            request.setReadTimeout(10, TimeUnit.SECONDS);

            Log.d("Chris", "request made: " + "https://api.yelp.com/v2/search?" + filter +
                    "&location=" + input + "&offset=" + offset);
        }

        service.signRequest(accessToken, request);
        Response response = request.send();

        JSONArray jsonBusinessesArray;

        try {
            // Get JSON array that holds the restaurants from Yelp.
            jsonBusinessesArray = new JSONObject(response.getBody())
                    .getJSONArray("businesses");

            int length = jsonBusinessesArray.length();

            // This occurs if a network communication error occurs or if no restaurants were found.
            if (length <= 0)
                return false;

            for (int i = 0; i < length; i++)
                restaurants.add(convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(i)));

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

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
                List<Address> addressList = geocoder.getFromLocationName(searchLocationBox.getQuery(), 1);
                double estimatedLat = addressList.get(0).getLatitude();
                double estimatedLon = addressList.get(0).getLongitude();
                Location estimatedLocation = new Location("estimatedLocation");
                estimatedLocation.setLatitude(estimatedLat);
                estimatedLocation.setLongitude(estimatedLon);
                distance = estimatedLocation.distanceTo(restaurantLoc);
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

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class RunYelpQuery extends AsyncTask<Void, Void, Restaurant> {

        String[] params;
        String userInputStr;
        String userFilterStr;
        boolean successfulQuery;

        public RunYelpQuery(String...params) {
            this.params = params;
            userInputStr = params[0];
            userFilterStr = params[1];
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            disableGenerateButton();
        }

        @Override
        protected Restaurant doInBackground(Void... aVoid) {

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";

            try {
                // If the user entered some input, make sure to encode all spaces and "+" for URL query.
                if (userInputStr.length() != 0) {
                    userInputStr = userInputStr.replaceAll(" ", "+");
                }

                // If the user entered a filter, make sure to encode all spaces and "+" for URL query.
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
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (restartQuery) {
                restaurants.clear();
                restartQuery = false;
            }

            // Get restaurants only when the current list is empty.
            Restaurant chosenRestaurant = null;
            if (restaurants == null || restaurants.size() == 0) {
                successfulQuery = queryYelp(lat, lon, userInputStr, userFilterStr, 0);

                if (successfulQuery) {
                    chosenRestaurant = restaurants.get(new Random().nextInt(restaurants.size()));
                    restaurants.remove(chosenRestaurant);
                }

                runSecondQuery = true;
            }
            else if (restaurants.size() != 0) {
                chosenRestaurant = restaurants.get(new Random().nextInt(restaurants.size()));
                restaurants.remove(chosenRestaurant);
            }

            // Return randomly chosen restaurant.
            Log.d("CHRIS", "Query was " + successfulQuery);
            Log.d("CHRIS", "Restaurants left: " + restaurants.size());
            return chosenRestaurant;
        }

        // Set UI appropriate UI elements to display mRestaurant info.
        @Override
        protected void onPostExecute(Restaurant restaurant) {

            if (restaurant == null) {
                Log.d("CHRIS", "Error during transmission");
                restaurants.clear();
                enableGenerateButton();
                return;
            }

            currentRestaurant = restaurant;

            // If the RecyclerView has not been set yet, set it with the currentRestaurant.
            if (mainRestaurantCardAdapter == null) {
                mainRestaurantCardAdapter = new MainRestaurantCardAdapter(getContext(), currentRestaurant);
                recyclerView.setAdapter(mainRestaurantCardAdapter);
            }

            // These calls notify the RecyclerView that the data set has changed and we need to refresh.
            mainRestaurantCardAdapter.remove();
            mainRestaurantCardAdapter.add(currentRestaurant);

            // Update the map with a new marker based on restaurant's coordinates.
            LatLng latLng = new LatLng(restaurant.getLat(), restaurant.getLon());
            map.addMarker(new MarkerOptions().position(latLng).title(String.format("%s: %s",
                    restaurant.getName(), restaurant.getAddress())
                    .replace("[", "").replace("]", "").trim()));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

            mapCardContainer.setVisibility(View.VISIBLE);

            enableGenerateButton();

            if (runSecondQuery) {
                backgroundYelpQuery = new RunYelpQueryBackground().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.params);
                runSecondQuery = false;
            }

            // A tutorial that displays only once explaining the action that can be done on the restaurant card.
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "2");
            ShowcaseConfig config = new ShowcaseConfig();
            config.setDelay(100);
            sequence.setConfig(config);

            sequence.addSequenceItem(buildShowcaseView(recyclerView, new RectangleShape(0, 0),
                    "Swipe left to dismiss. Swipe right to open in Yelp. Tap bookmark button to save it for later."));

            sequence.addSequenceItem(buildShowcaseView(((MainActivity) getActivity()).getMenuItemView(R.id.action_saved_list),
                    new CircleShape(), "Tap here to view your saved list."));

            sequence.start();
        }
    }

    // Async task that runs in the background to query more restaurants from Yelp while the user
    // goes through the initial list of restaurants.
    public class RunYelpQueryBackground extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

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

                // If the user entered a filter, make sure to encode all spaces and "+" for URL query.
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
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("CHRIS", "About to query for new restaurants");
            queryYelp(lat, lon, userInputStr, userFilterStr, 20);
            Log.d("CHRIS", "Added all, now with " + restaurants.size());

            return null;
        }
    }
}