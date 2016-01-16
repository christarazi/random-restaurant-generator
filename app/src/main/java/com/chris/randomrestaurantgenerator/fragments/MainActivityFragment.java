package com.chris.randomrestaurantgenerator.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import java.util.Random;

/**
 * A fragment containing the main activity.
 * Responsible for displaying to the user a random restaurant based off their location  / zip code.
 */
public class MainActivityFragment extends Fragment implements OnMapReadyCallback {

    EditText userLocationInfo;
    Button generate;

    RelativeLayout rootLayout;
    RecyclerView recyclerView;

    MainRestaurantCardAdapter mainRestaurantCardAdapter;

    Restaurant currentRestaurant;
    LocationProviderHelper helper;

    LinearLayout mapCardContainer;
    MapView mapView;
    GoogleMap map;

    boolean useGPS;
    boolean taskRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Chris", "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        Log.d("Chris", "onCreateView()");

        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);
        recyclerView = (RecyclerView) rootLayout.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));

        mapCardContainer = (LinearLayout) rootLayout.findViewById(R.id.cardMapView);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        userLocationInfo = (EditText) rootLayout.findViewById(R.id.userLocationInfo);
        generate = (Button) rootLayout.findViewById(R.id.generate);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                Log.d("Chris", "onSwiped() called with: " + "viewHolder = [" + viewHolder + "], direction = [" + direction + "]");

                // If user has swiped left, perform a click on the Generate button.
                if (direction == 4) {
                    //mapCardContainer.setVisibility(View.INVISIBLE);
                    generate.performClick();
                }

                // If user has swiped right, open Yelp to current restaurant's page.
                if (direction == 8) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentRestaurant.getUrl())));

                    // We don't want to remove therestaurantt here, so we add it back to the recyclerView.
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

        Log.d("Chris", "onActivityCreated()");

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        // OnTouchListener for the Location icon in the EditText box.
        userLocationInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    // If the user clicks on the Location icon, enable Location feature.
                    if (event.getRawX() >= (userLocationInfo.getRight() - userLocationInfo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        ProgressDialog dialog = new ProgressDialog(getActivity());
                        dialog.setMessage("Getting location...");
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                helper.dismissLocationUpdater();
                            }
                        });
                        dialog.show();

                        helper = new LocationProviderHelper(getActivity(), dialog);
                        helper.requestLocation();

                        useGPS = true;

                        return true;
                    } else {

                        // If the user touches anywhere else, then we need to make the map
                        // invisible to prevent the keyboard from pushing up.
                        mapCardContainer.setVisibility(View.INVISIBLE);
                        //Toast.makeText(getContext(), "Touched", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });

        // Listener for when the user clicks done on keyboard after their input.
        userLocationInfo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

                Log.d("Chris", "generate has been clicked.");

                // Send the maybeList to the interface so that it can be sent to the maybe list fragment.
                //maybeListCommunicator.sendList(maybeList);

                // If the user doesn't wait on the task to complete, warn them it is still running
                // so we can prevent a long stack of requests from piling up.
                if (taskRunning) {
                    Toast.makeText(getActivity(), "Task still running, please wait.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Clear all the markers on the map.
                map.clear();

                if (useGPS) {

                    // If the user is using location, check to make sure the location is not null before beginning.
                    // Else, begin the AsyncTask.
                    Location location = helper.getLocation();
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
                        userLocationInfo.setText(String.format("%s, %s", location.getLatitude(), location.getLongitude()));
                        new GetJsonData().execute(String.valueOf(userLocationInfo.getText()),
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude()));
                    }
                } else {

                    // If the user is using zip code, check to make sure they have entered one.
                    // Else, begin the AsyncTask.
                    if (userLocationInfo.getText().length() == 0) {
                        Log.d("Chris", "zip: " + userLocationInfo.getText());

                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.setTitle("Error");
                        alert.setMessage("Please enter your zip code.");
                        alert.show();
                    } else {
                        new GetJsonData().execute(String.valueOf(userLocationInfo.getText()));
                    }
                }
            }
        });


    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
        Log.d("Chris", "onPause()");
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
        Log.d("Chris", "onResume()");
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        Log.d("Chris", "onDestroy()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("Chris", "onDestroyView()");
    }

    // Google Maps API callback for MapFragment.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("Chris", "onMapReady() called with: " + "googleMap = [" + googleMap + "]");
        map = googleMap;
    }

    // Mandatory Android M permissions callback.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setMessage("Please enter your zip code because location services could not be used.");
                    dialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.setTitle("Location permissions denied");
                    dialog.create();
                }
            }
        }
    }

    private void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class GetJsonData extends AsyncTask<String, Void, Restaurant> {

        @Override
        protected Restaurant doInBackground(String... params) {

            // Signal the task is running
            taskRunning = true;

            // Build OAuth request
            OAuthService service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(TwoStepOAuth.getConsumerKey())
                    .apiSecret(TwoStepOAuth.getConsumerSecret()).build();

            Token accessToken = new Token(TwoStepOAuth.getToken(), TwoStepOAuth.getTokenSecret());

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";
            String userInputStr = params[0];

            // If the user entered some input, make sure to encode all spaces and "+" for URL query.
            if (userInputStr.length() != 0) {
                userInputStr = userInputStr.replaceAll(" ", "+");
            }

            // If we have 3 parameters, then the user selected location and we must grab the lat / long.
            if (params.length == 3) {
                lat = params[1];
                lon = params[2];
            }

            OAuthRequest request;
            int startingOffset;

            // Get a random offset for Yelp results
            startingOffset = new Random().nextInt(2) * 20;

            if (lat.equals("") || lon.equals("")) {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?term=food&location=" + userInputStr + "&offset=" + startingOffset);
                Log.d("Chris", "request: " + "https://api.yelp.com/v2/search?term=food&location=" + userInputStr + "&offset=" + startingOffset);
            } else {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?term=food" +
                        "&ll=" + lat + "," + lon + "&offset=" + startingOffset);
                Log.d("Chris", "request: " + "https://api.yelp.com/v2/search?term=food" +
                        "&ll=" + lat + "," + lon + "&offset=" + startingOffset);
            }

            service.signRequest(accessToken, request);
            Response response = request.send();

            // Parsing JSON response
            JSONObject mainJsonObject = null;
            JSONArray jsonBusinessesArray = null;
            try {
                Log.d("Chris", "doInBackground() called with: " + "params = [" + params + "]");
                Log.d("Chris", "json: " + response.getBody());
                mainJsonObject = new JSONObject(response.getBody());
                jsonBusinessesArray = mainJsonObject.getJSONArray("businesses");

                int length = jsonBusinessesArray.length();

                // This occurs if an network communication error occurs or if no restaurants were found.
                if (length <= 0) {
                    return null;
                }

                int index = new Random().nextInt(length);
                Log.d("Chris", "length: " + jsonBusinessesArray.length());
                Log.d("Chris", "index: " + index);

                // Convert a random restaurant from json to Restaurant object
                return convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(index));

            } catch (JSONException e) {
                e.printStackTrace();

                // Signal the task has finished (failed task is still a finished task)
                taskRunning = false;

                return null;
            }
        }

        // Set UI appropriate UI elements to display mRestaurant info.
        @Override
        protected void onPostExecute(Restaurant restaurant) {
            if (restaurant == null) {
                Toast.makeText(getActivity(), "Error during transmission. Either no restaurants were " +
                        "found in your area or an internet communication error occurred. Try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            super.onPostExecute(restaurant);

            Log.d("Chris", "onPostExecute() called with: " + "restaurant = [" + restaurant + "]");

            currentRestaurant = restaurant;

            Log.d("Chris", "GetJonTask returned: " + currentRestaurant);

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
                    restaurant.getName(), restaurant.getAddress())));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

            mapCardContainer.setVisibility(View.VISIBLE);

            // Signal the task as completed
            taskRunning = false;

            Log.d("Chris", "onPostExecute() returned: void");
        }

        // Given a JSONObject, convert it to a Restaurant object that encapsulates a mRestaurant from Yelp.
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
                    deals = dealsList.toString();
                } catch (Exception ignored) {
                    deals = "";
                }

                Log.d("Chris", "deals: " + deals);

                // Construct a new Restaurant object with all the info we gathered above and return it
                Restaurant restaurant = new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                        obj.getString("rating_img_url_large"), obj.getString("image_url"), obj.getInt("review_count"), obj.getString("url"), categories, obj.getString("phone"),
                        obj.getBoolean("is_closed"), address, deals, lat, lon);

                return restaurant;
            } catch (JSONException e) {
                e.printStackTrace();

                // Signal the task has finished (failed task is still a finished task)
                taskRunning = false;

                return null;
            }
        }
    }
}
