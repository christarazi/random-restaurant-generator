package com.chris.randomrestaurantgenerator.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.io.IOException;
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

    EditText userLocationInfo;
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

    boolean taskRunning;

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

        userLocationInfo = (EditText) rootLayout.findViewById(R.id.userLocationInfo);
        filterBox = (EditText) rootLayout.findViewById(R.id.filter);
        generate = (Button) rootLayout.findViewById(R.id.generate);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                // If user has swiped left, perform a click on the Generate button.
                if (direction == 4) {
                    //mapCardContainer.setVisibility(View.INVISIBLE);
                    generate.performClick();
                }

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

        locationHelper = new LocationProviderHelper(getActivity(), rootLayout);

        return rootLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Reset all cache for showcase id.
        //MaterialShowcaseView.resetAll(getContext());

        // A tutorial that displays only once explaining the input to the app.
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "1");
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        sequence.setConfig(config);

        sequence.addSequenceItem(buildShowcaseView(userLocationInfo, new RectangleShape(0, 0),
                "Enter any zip code or city or address here, or click the GPS icon to use your current location."
        ));

        sequence.addSequenceItem(buildShowcaseView(filterBox, new RectangleShape(0, 0),
                "Filter your results if you're in the mood for something specific."
        ));

        sequence.start();

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        // OnTouchListener for the GPS icon in the EditText box.
        userLocationInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    // Increase the clickable boundary for the GPS icon to make it easier to tap on.
                    int clickBoundary = userLocationInfo.getRight()
                            - userLocationInfo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
                            - 45;

                    // If the user clicks on the Location icon, enable Location feature.
                    if (event.getRawX() >= clickBoundary) {

                        // If Location permissions have been granted, proceed.
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED
                                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                            locationHelper.requestLocation();

                            return true;
                        }

                        // Else show request for Location permissions and/or request them.
                        else {

                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                                    || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                Snackbar.make(rootLayout, "Location permissions are required in order to use your GPS.",
                                        Snackbar.LENGTH_INDEFINITE)
                                        .setAction("GRANT", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ActivityCompat.requestPermissions(getActivity(),
                                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                                        LocationProviderHelper.MY_LOCATION_REQUEST_CODE);
                                            }
                                        })
                                        .show();
                            }

                            return true;
                        }
                    } else {

                        // If the user touches anywhere else, then we need to make the map
                        // invisible to prevent the keyboard from pushing up.
                        mapCardContainer.setVisibility(View.INVISIBLE);
                    }
                }
                return false;
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

                // Clear all the markers on the map.
                map.clear();

                if (LocationProviderHelper.useGPS) {

                    // If the user is using location, check to make sure the location is not null before beginning.
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
                        new GetJsonData().execute(String.valueOf(userLocationInfo.getText()),
                                String.valueOf(filterBox.getText()),
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude()));
                    }
                } else {

                    // If the user is using entering their location, check to make sure they have entered one.
                    // Else, begin the AsyncTask.
                    if (userLocationInfo.getText().length() == 0) {

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
                        new GetJsonData().execute(String.valueOf(userLocationInfo.getText()), String.valueOf(filterBox.getText()));
                    }
                }
            }
        });


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

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
     * Callback from MainActivity to respond to when permissions have changed.
     * @param isGranted: if the permissions have been granted or denied
     */
    public void reactToPermissionsCallback(boolean isGranted) {

        if (isGranted) {
            Snackbar.make(rootLayout, "Permission granted, now you can access the GPS.",
                    Snackbar.LENGTH_LONG).show();

            locationHelper.requestLocation();
            LocationProviderHelper.useGPS = true;
        } else {
            Snackbar.make(rootLayout, "Permission denied, you cannot access the GPS. " +
                    "Please enter your location manually.", Snackbar.LENGTH_LONG).show();

            LocationProviderHelper.useGPS = false;
        }
    }

    /**
     * Function to hide the keyboard.
     *
     * @param activity: current Activity.
     */
    private void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
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

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class GetJsonData extends AsyncTask<String, Void, Restaurant> {

        // Holds a copy of the parameters in case we need to make another call (recursive) to this AsyncTask.
        String[] params;

        // We don't want to display a restaurant that is closed to the user.
        boolean isRestaurantClosed = false;

        @Override
        protected Restaurant doInBackground(String... params) {

            this.params = params;

            // Signal the task is running
            taskRunning = true;

            try {
                // Build OAuth request
                OAuthService service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(TwoStepOAuth.getConsumerKey())
                        .apiSecret(TwoStepOAuth.getConsumerSecret()).build();

                Token accessToken = new Token(TwoStepOAuth.getToken(), TwoStepOAuth.getTokenSecret());

                // Check for parameters so we can send the appropriate request based on user input.
                String lat = "";
                String lon = "";
                String userInputStr = params[0];
                String userFilterStr = params[1];

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

                OAuthRequest request;
                int startingOffset;

                // Get a random offset for Yelp results
                startingOffset = new Random().nextInt(2) * 20;

                if (LocationProviderHelper.useGPS) {
                    request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + userFilterStr +
                            "&ll=" + lat + "," + lon + "&offset=" + startingOffset);

                    request.setConnectTimeout(15, TimeUnit.SECONDS);
                    request.setReadTimeout(15, TimeUnit.SECONDS);

                    Log.d("Chris", "request made: " + "https://api.yelp.com/v2/search?" + userFilterStr +
                            "&ll=" + lat + "," + lon + "&offset=" + startingOffset);
                } else {
                    request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?" + userFilterStr +
                            "&location=" + userInputStr + "&offset=" + startingOffset);

                    request.setConnectTimeout(15, TimeUnit.SECONDS);
                    request.setReadTimeout(15, TimeUnit.SECONDS);

                    Log.d("Chris", "request made: " + "https://api.yelp.com/v2/search?" + userFilterStr +
                            "&location=" + userInputStr + "&offset=" + startingOffset);
                }

                service.signRequest(accessToken, request);
                Response response = request.send();

                // Parsing JSON response
                JSONObject mainJsonObject = null;
                JSONArray jsonBusinessesArray = null;

                mainJsonObject = new JSONObject(response.getBody());
                jsonBusinessesArray = mainJsonObject.getJSONArray("businesses");

                int length = jsonBusinessesArray.length();

                // This occurs if a network communication error occurs or if no restaurants were found.
                if (length <= 0) {
                    return null;
                }

                int index = new Random().nextInt(length);

                isRestaurantClosed = jsonBusinessesArray.getJSONObject(index).getBoolean("is_closed");
                if (isRestaurantClosed) {
                    return null;
                }

                // Convert a random restaurant from json to Restaurant object
                return convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(index));

            } catch (Exception e) {
                e.printStackTrace();

                // Signal the task has finished (failed task is still a finished task)
                taskRunning = false;

                return null;
            }
        }

        // Set UI appropriate UI elements to display mRestaurant info.
        @Override
        protected void onPostExecute(Restaurant restaurant) {

            super.onPostExecute(restaurant);

            // If restaurant is closed, run this AsyncTask again.
            if (isRestaurantClosed) {
                new GetJsonData().execute(params);
                return;
            }

            if (restaurant == null) {
                Toast.makeText(getActivity(), "Error during transmission. Either no restaurants were " +
                        "found in your area or an internet communication error occurred. Try again.", Toast.LENGTH_SHORT).show();

                // Signal the task has finished (failed task is still a finished task)
                taskRunning = false;

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

            // Signal the task as completed
            taskRunning = false;

            // A tutorial that displays only once explaining the action that can be done on the restaurant card.
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "2");
            ShowcaseConfig config = new ShowcaseConfig();
            config.setDelay(100);
            sequence.setConfig(config);

            sequence.addSequenceItem(buildShowcaseView(recyclerView, new RectangleShape(0, 0),
                    "Swipe left to dismiss. Swipe right to open in Yelp. Tap bookmark button to save it for later."));

            sequence.addSequenceItem(buildShowcaseView(((MainActivity) getActivity()).getMenuItemView(),
                    new CircleShape(), "Tap here to view your saved list."));

            sequence.start();
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

                for (int i = 0; i < categoriesJSON.length(); i += 2) {
                    for (int j = 0; j < categoriesJSON.getJSONArray(i).length(); j += 2) {
                        categories.add(categoriesJSON.getJSONArray(i).getString(j));
                    }
                }

                // Getting the restaurant's location information
                JSONObject locationJSON = obj.getJSONObject("location");

                double lat = locationJSON.getJSONObject("coordinate").getDouble("latitude");
                double lon = locationJSON.getJSONObject("coordinate").getDouble("longitude");

                float distance = 0;
                Location restaurantLoc = new Location("restaurantLoc");
                restaurantLoc.setLatitude(lat);
                restaurantLoc.setLongitude(lon);
                if (LocationProviderHelper.useGPS) {
                    distance = locationHelper.getLocation().distanceTo(restaurantLoc);
                } else {
                    Geocoder geocoder = new Geocoder(getContext());
                    List<Address> addressList = geocoder.getFromLocationName(userLocationInfo.getText().toString(), 1);
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

                for (int i = 0; i < addressJSON.length(); i++) {
                    address.add(addressJSON.getString(i));
                }

                // Get deals if JSON contains deals object.
                String deals = "";
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
                Restaurant restaurant = new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                        obj.getString("rating_img_url_large"), obj.getString("image_url"), obj.getInt("review_count"),
                        obj.getString("url"), categories, address, deals, distance, lat, lon);

                return restaurant;
            } catch (JSONException | IOException e) {
                e.printStackTrace();

                // Signal the task has finished (failed task is still a finished task)
                taskRunning = false;

                return null;
            }
        }
    }
}