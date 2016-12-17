package com.chris.randomrestaurantgenerator.views;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.db.RestaurantDBHelper;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.utils.SavedListHolder;
import com.squareup.picasso.Picasso;

import java.util.Locale;

/**
 * A RecyclerView Adapter for the main fragment. Only displays one card at a time, rather than a list.
 */
public class MainRestaurantCardAdapter extends RecyclerView.Adapter<MainRestaurantCardAdapter.RestaurantViewHolder> {

    private Context context;
    private Restaurant restaurant;
    private SavedListHolder savedListHolder;
    private RestaurantDBHelper dbHelper;

    public MainRestaurantCardAdapter(Context con, Restaurant res) {
        this.context = con;
        this.restaurant = res;
        this.savedListHolder = SavedListHolder.getInstance();
        this.dbHelper = new RestaurantDBHelper(this.context, null);
    }

    public void add(Restaurant res) {
        this.restaurant = res;
        notifyItemInserted(0);
    }

    public void remove() {
        notifyItemRemoved(0);
    }

    /**
     * Helper function to add a restaurant to the savedList.
     *
     * @param res: the restaurant object we want to add.
     */
    private void addToList(Restaurant res) {
        savedListHolder.getSavedList().add(res);
    }

    /**
     * Helper function to load the corresponding rating image for Yelp stars.
     * Since Yelp v3 API (dubbed Fusion), they stopped providing a URL for the rating image, so this
     * is why this function is needed.
     *
     * @param rating: rating to load stars for.
     * @return corresponding drawable to the rating.
     */
    private int loadStars(Double rating) {
        if (rating == 0.0)
            return R.drawable.ic_zero_stars;
        else if (rating == 1.0)
            return R.drawable.ic_one_stars;
        else if (rating == 1.5)
            return R.drawable.ic_onehalf_stars;
        else if (rating == 2.0)
            return R.drawable.ic_two_stars;
        else if (rating == 2.5)
            return R.drawable.ic_twohalf_stars;
        else if (rating == 3.0)
            return R.drawable.ic_three_stars;
        else if (rating == 3.5)
            return R.drawable.ic_threehalf_stars;
        else if (rating == 4.0)
            return R.drawable.ic_four_stars;
        else if (rating == 4.5)
            return R.drawable.ic_fourhalf_stars;
        else if (rating == 5.0)
            return R.drawable.ic_five_stars;
        else
            return R.drawable.ic_zero_stars;
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_main_restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Picasso.with(context).load(restaurant.getThumbnailURL()).into(holder.thumbnail);
        holder.ratingImage.setImageResource(loadStars(restaurant.getRating()));
        holder.nameOfRestaurant.setText(restaurant.getName());
        holder.categories.setText(restaurant.getCategories().toString()
                .replace("[", "").replace("]", "").trim());

        if (restaurant.getDeal().length() != 0) {
            holder.deals.setVisibility(View.VISIBLE);
            holder.deals.setText(restaurant.getDeal());
        } else
            holder.deals.setVisibility(View.GONE);

        String priceText = String.format("%s", restaurant.getPrice());
        String reviewsText = String.format(Locale.ENGLISH, "%d reviews", restaurant.getReviewCount());
        String distanceText = String.format(Locale.ENGLISH, "%.2f mi away", restaurant.getDistance());

        // Color coding dollar signs for price and number of reviews.
        Spannable spannable = new SpannableString(String.format(Locale.ENGLISH, "%s | %s | %s",
                priceText, reviewsText, distanceText));
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#14AD5F")), 0, priceText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#1A6C9D")), priceText.length() + 3,
                (priceText.length() + 3 + String.valueOf(restaurant.getReviewCount()).length()),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.distancePriceReviewCount.setText(spannable);

        // Modify the save button depending on if the restaurant in the savedList or not.
        if (restaurant.isSaved()) {
            holder.saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bookmark_check));
        } else
            holder.saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bookmark_outline_plus));
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {

        TextView nameOfRestaurant;
        ImageView ratingImage;
        ImageView thumbnail;
        TextView categories;
        TextView deals;
        TextView distancePriceReviewCount;

        ImageButton saveButton;

        RestaurantViewHolder(View itemView) {
            super(itemView);

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
            deals = (TextView) itemView.findViewById(R.id.deals);
            distancePriceReviewCount = (TextView) itemView.findViewById(R.id.distancePriceReviewCount);

            saveButton = (ImageButton) itemView.findViewById(R.id.saveButton);

            // Adds current restaurant to the saved list on click.
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (Restaurant r : savedListHolder.getSavedList()) {
                        if (r.equals(restaurant)) {
                            restaurant.setSaved(true);
                            saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bookmark_check));
                            Toast.makeText(context, "You have already saved this restaurant.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    new InsertIntoDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, restaurant);

                    addToList(restaurant);
                    restaurant.setSaved(true);
                    saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bookmark_check));
                }
            });
        }
    }

    private class InsertIntoDB extends AsyncTask<Restaurant, Void, Void> {

        @Override
        protected Void doInBackground(Restaurant... params) {
            dbHelper.insert(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
