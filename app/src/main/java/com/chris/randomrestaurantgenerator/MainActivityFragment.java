package com.chris.randomrestaurantgenerator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String CONSUMER_KEY = BuildConfig.API_CONSUMER_KEY;
    private static final String CONSUMER_SECRET = BuildConfig.API_CONSUMER_SECRET;
    private static final String TOKEN = BuildConfig.API_TOKEN;
    private static final String TOKEN_SECRET = BuildConfig.API_TOKEN_SECRET;

    RadioGroup radioGroup;
    EditText zipcode;
    Button generate;

    RelativeLayout root;
    LinearLayout linearLayout;

    TextView nameOfRestaurant;
    ImageView ratingImage;
    ImageView thumbnail;
    TextView categories;

    String zipcodeStr;
    int startingResultOffset = 0;

    Restaurant currentBiz;

    LocationProviderHelper helper;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);

        radioGroup = (RadioGroup) root.findViewById(R.id.radioGroup);
        zipcode = (EditText) root.findViewById(R.id.zipcode);
        generate = (Button) root.findViewById(R.id.generate);

        thumbnail = (ImageView) root.findViewById(R.id.thumbnail);
        nameOfRestaurant = (TextView) root.findViewById(R.id.name);
        ratingImage = (ImageView) root.findViewById(R.id.rating);
        categories = (TextView) root.findViewById(R.id.categories);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.useZip:
                        zipcode.setVisibility(View.VISIBLE);
                        break;
                    case  R.id.useLocation:
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
                    default:
                        break;
                }
            }
        });

        linearLayout = (LinearLayout) root.findViewById(R.id.container);
        linearLayout.setOnTouchListener(new LinearLayoutTouchListener(getActivity()));

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.useZip:
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
                        }
                        else
                            new GetJsonData().execute(String.valueOf(zipcode.getText()));
                        break;
                    case  R.id.useLocation:
                        Location location = helper.getLocation();
                        new GetJsonData().execute(String.valueOf(zipcode.getText()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
                        break;

                }


                //new GetJsonData().execute(String.valueOf(zipcode.getText()));

                Log.d("Chris", "generate has been clicked.");
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // Handle the swipe gestures on the layout which holds the restaurant info.
    public class LinearLayoutTouchListener implements View.OnTouchListener {

        static final String logTag = "ActivitySwipeDetector";
        private Activity activity;
        static final int MIN_DISTANCE = 100;// TODO change this runtime based on screen resolution. for 1920x1080 is to small the 100 distance
        private float downX, downY, upX, upY;

        public LinearLayoutTouchListener(Activity act) {
            activity = act;
        }

        public void onRightToLeftSwipe() {
            Log.i(logTag, "RightToLeftSwipe!");
            Toast.makeText(activity, "RightToLeftSwipe", Toast.LENGTH_SHORT).show();
            Log.d("Chris", "number of children: " + linearLayout.getChildCount());
            linearLayout.getChildAt(0).setVisibility(View.INVISIBLE);
            generate.performClick();
            //linearLayout.removeView(findViewById(R.id.itemContainer));
            // activity.doSomething();
        }

        public void onLeftToRightSwipe() {
            Log.i(logTag, "LeftToRightSwipe!");
            Toast.makeText(activity, "LeftToRightSwipe", Toast.LENGTH_SHORT).show();
            Intent openYelp = new Intent(Intent.ACTION_VIEW, Uri.parse(currentBiz.getUrl()));
            startActivity(openYelp);
            // activity.doSomething();
        }

        public void onTopToBottomSwipe() {
            Log.i(logTag, "onTopToBottomSwipe!");
            Toast.makeText(activity, "onTopToBottomSwipe", Toast.LENGTH_SHORT).show();
            // activity.doSomething();
        }

        public void onBottomToTopSwipe() {
            Log.i(logTag, "onBottomToTopSwipe!");
            Toast.makeText(activity, "onBottomToTopSwipe", Toast.LENGTH_SHORT).show();
            // activity.doSomething();
        }

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    upX = event.getX();
                    upY = event.getY();

                    float deltaX = downX - upX;
                    float deltaY = downY - upY;

                    // swipe horizontal?
                    if (Math.abs(deltaX) > MIN_DISTANCE) {
                        // left or right
                        if (deltaX < 0) {
                            this.onLeftToRightSwipe();
                            return true;
                        }
                        if (deltaX > 0) {
                            this.onRightToLeftSwipe();
                            return true;
                        }
                    } else {
                        Log.i(logTag, "Swipe was only " + Math.abs(deltaX) + " long horizontally, need at least " + MIN_DISTANCE);
                        // return false; // We don't consume the event
                    }

                    // swipe vertical?
                    if (Math.abs(deltaY) > MIN_DISTANCE) {
                        // top or down
                        if (deltaY < 0) {
                            this.onTopToBottomSwipe();
                            return true;
                        }
                        if (deltaY > 0) {
                            this.onBottomToTopSwipe();
                            return true;
                        }
                    } else {
                        Log.i(logTag, "Swipe was only " + Math.abs(deltaX) + " long vertically, need at least " + MIN_DISTANCE);
                        // return false; // We don't consume the event
                    }

                    return false; // no swipe horizontally and no swipe vertically
                }// case MotionEvent.ACTION_UP:
            }
            return false;
        }

    }

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class GetJsonData extends AsyncTask<String, Void, Restaurant> {

        @Override
        protected Restaurant doInBackground(String... params) {
            OAuthService service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(CONSUMER_KEY)
                    .apiSecret(CONSUMER_SECRET).build();

            Token accessToken = new Token(TOKEN, TOKEN_SECRET);

            String lat = "";
            String lon = "";
            if (params.length == 3) {
                lat = params[1];
                lon = params[2];
            }
            zipcodeStr = params[0];

            String result;
            OAuthRequest request;

            startingResultOffset = new Random().nextInt(2) * 20;

            if (lat.equals("") || lon.equals("")) {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?term=food&location=" + zipcodeStr + "&offset=" + startingResultOffset);
                Log.d("Chris", "request: " + "https://api.yelp.com/v2/search?term=food&location=" + zipcodeStr + "&offset=" + startingResultOffset);
            }
            else {
                request = new OAuthRequest(Verb.GET, "https://api.yelp.com/v2/search?term=food" +
                        "&ll=" + lat + "," + lon + "&offset=" + startingResultOffset);
                Log.d("Chris", "request: " +  "https://api.yelp.com/v2/search?term=food" +
                        "&ll=" + lat + "," + lon + "&offset=" + startingResultOffset);
            }

            service.signRequest(accessToken, request);
            Response response = request.send();

            JSONObject jsonObject = null;
            JSONArray jsonArray = null;
            try {
                Log.d("Chris", "doInBackground() called with: " + "params = [" + params + "]");
                Log.d("Chris", "json: " + response.getBody());
                jsonObject = new JSONObject(response.getBody());
                jsonArray = jsonObject.getJSONArray("businesses");

                int length = jsonArray.length();

                // This occurs if an internet commuicnation error occurs or if no restaurants were found.
                if (length <= 0) {
                    return null;
                }

                int index = new Random().nextInt(length);
                Log.d("Chris", "length: " + jsonArray.length());
                Log.d("Chris", "index: " + index);

                Restaurant biz = convertJSONToRestaurant(jsonArray.getJSONObject(index));

                return biz;

            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Set UI appropriate UI elements to display restaurant info.
        @Override
        protected void onPostExecute(Restaurant b) {
            if (b == null) {
                Toast.makeText(getActivity(), "Error during transmission. Either no restaurants were " +
                        "found in your location or an internet communication error occurred.", Toast.LENGTH_SHORT).show();
                return;
            }
            super.onPostExecute(b);
            nameOfRestaurant.setText(b.getName());
            new ImageDownloader(ratingImage).execute(b.getRatingImageURL());
            new ImageDownloader(thumbnail).execute(b.getThumbnailURL());
            categories.setText(b.getCategories().toString());
            linearLayout.getChildAt(0).setVisibility(View.VISIBLE);
            currentBiz = b;
            Log.d("Chris", "main onPostExecute is done");
        }
    }

    // Async task to download images from the web and set it to the corresponding ImageView.
    public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

        ImageView image;

        public ImageDownloader(ImageView imageView) {
            this.image = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            Bitmap ratingImg = null;

            try {
                InputStream in = new URL(url).openStream();
                ratingImg = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return ratingImg;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            image.setImageBitmap(bitmap);
        }
    }

    // Given a JSONObject, convert it to a Restaurant object that encapsulates a restaurant from Yelp.
    private Restaurant convertJSONToRestaurant(JSONObject obj) {
        try {
            JSONArray categoriesJSON = obj.getJSONArray("categories");
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < categoriesJSON.length(); i+=2) {
                for (int j = 0; j < categoriesJSON.getJSONArray(i).length(); j+=2) {
                    categories.add(categoriesJSON.getJSONArray(i).getString(j));
                }
            }

            JSONArray addressJSON = obj.getJSONObject("location").getJSONArray("display_address");
            String[] address = new String[addressJSON.length()];

            for (int i = 0; i < addressJSON.length(); i++) {
                address[i] = addressJSON.getString(i);
            }

            Restaurant restaurant = new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                    obj.getString("rating_img_url_large"), obj.getString("image_url"), obj.getInt("review_count"), obj.getString("url"), categories, obj.getString("phone"),
                    obj.getBoolean("is_closed"), address);

            return restaurant;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                else {
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
}
