package com.chris.randomrestaurantgenerator.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.managers.UnscrollableLinearLayoutManager;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.utils.LocationProviderHelper;
import com.chris.randomrestaurantgenerator.utils.TwoStepOAuth;
import com.chris.randomrestaurantgenerator.views.RestaurantCardAdapter;
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

    RadioGroup radioGroup;
    EditText zipcode;
    Button generate;

    RelativeLayout rootLayout;
    RecyclerView recyclerView;

    RestaurantCardAdapter restaurantCardAdapter;

    Restaurant currentRestaurant;
    LocationProviderHelper helper;

    LinearLayout mapCard;
    MapView mapView;
    GoogleMap map;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);
        recyclerView = (RecyclerView) rootLayout.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));

        mapCard = (LinearLayout) rootLayout.findViewById(R.id.cardMapView);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        radioGroup = (RadioGroup) rootLayout.findViewById(R.id.radioGroup);
        zipcode = (EditText) rootLayout.findViewById(R.id.zipcode);
        generate = (Button) rootLayout.findViewById(R.id.generate);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Log.d("Chris", "onSwiped() called with: " + "viewHolder = [" + viewHolder + "], direction = [" + direction + "]");
                mapCard.setVisibility(View.INVISIBLE);
                generate.performClick();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return rootLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        // Ask the user if they prefer using zip code or their location.
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.useZip:
                        zipcode.setVisibility(View.VISIBLE);
                        break;
                    case R.id.useLocation: {
                        zipcode.setVisibility(View.GONE);

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

                        break;
                    }
                    default:
                        break;
                }
            }
        });

        generate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Clear all the markers on the map.
                map.clear();

                switch (radioGroup.getCheckedRadioButtonId()) {

                    // If the user is using zip code, check to make sure they have entered one.
                    // Else, begin the AsyncTask.
                    case R.id.useZip: {
                        Log.d("Chris", "zip: " + zipcode.getText());
                        if (zipcode.getText().length() == 0) {
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
                            new GetJsonData().execute(String.valueOf(zipcode.getText()));
                        }
                        break;
                    }

                    // If the user is using location, check to make sure the location is not null before beginning.
                    // Else, begin the AsyncTask.
                    case R.id.useLocation: {
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
                            new GetJsonData().execute(String.valueOf(zipcode.getText()),
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()));
                        }
                        break;
                    }
                }

                Log.d("Chris", "generate has been clicked.");
            }
        });
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    // Google Maps API callback for MapFragment.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("Chris", "onMapReady() called with: " + "googleMap = [" + googleMap + "]");
        map = googleMap;
        map.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Marker"));
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

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class GetJsonData extends AsyncTask<String, Void, Restaurant> {

        @Override
        protected Restaurant doInBackground(String... params) {
            // Build OAuth request
            OAuthService service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(TwoStepOAuth.getConsumerKey())
                    .apiSecret(TwoStepOAuth.getConsumerSecret()).build();

            Token accessToken = new Token(TwoStepOAuth.getToken(), TwoStepOAuth.getTokenSecret());

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";
            String zipcodeStr = params[0];

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
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?term=food&location=" + zipcodeStr + "&offset=" + startingOffset);
                Log.d("Chris", "request: " + "https://api.yelp.com/v2/search?term=food&location=" + zipcodeStr + "&offset=" + startingOffset);
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

                // This occurs if an internet communication error occurs or if no restaurants were found.
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

            // If the RecyclerView has not been set yet, set it with the currentRestaurant
            if (restaurantCardAdapter == null) {
                restaurantCardAdapter = new RestaurantCardAdapter(getContext(), currentRestaurant);
                recyclerView.setAdapter(restaurantCardAdapter);
            }

            // These calls notify the RecyclerView that the data set has changed and we need to refresh.
            restaurantCardAdapter.remove();
            restaurantCardAdapter.add(currentRestaurant);

            // Update the map with a new marker based on restaurant's coordinates.
            LatLng latLng = new LatLng(restaurant.getLat(), restaurant.getLon());
            map.addMarker(new MarkerOptions().position(latLng));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

            mapCard.setVisibility(View.VISIBLE);

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
                String[] address = new String[addressJSON.length()];

                for (int i = 0; i < addressJSON.length(); i++) {
                    address[i] = addressJSON.getString(i);
                }

                // Construct a new Restaurant object with all the info we gathered above and return it
                Restaurant restaurant = new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                        obj.getString("rating_img_url_large"), obj.getString("image_url"), obj.getInt("review_count"), obj.getString("url"), categories, obj.getString("phone"),
                        obj.getBoolean("is_closed"), address, lat, lon);

                return restaurant;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

    }
}
