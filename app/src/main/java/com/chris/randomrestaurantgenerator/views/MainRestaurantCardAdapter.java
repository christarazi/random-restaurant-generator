package com.chris.randomrestaurantgenerator.views;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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

    Context context;
    Restaurant restaurant;
    SavedListHolder savedListHolder;
    RestaurantDBHelper dbHelper;

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

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_main_restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Picasso.with(context).load(restaurant.getThumbnailURL()).into(holder.thumbnail);
        Picasso.with(context).load(restaurant.getRatingImageURL()).into(holder.ratingImage);
        holder.nameOfRestaurant.setText(restaurant.getName());
        holder.categories.setText(restaurant.getCategories().toString()
                .replace("[", "").replace("]", "").trim());

        if (restaurant.getDeal().length() != 0) {
            holder.deals.setVisibility(View.VISIBLE);
            holder.deals.setText(restaurant.getDeal());
        } else
            holder.deals.setVisibility(View.GONE);

        holder.distanceAndReviewCount.setText(String.format(Locale.ENGLISH, "%d reviews | %.2f mi away",
                restaurant.getReviewCount(), restaurant.getDistance()));

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

    public class RestaurantViewHolder extends RecyclerView.ViewHolder {

        TextView nameOfRestaurant;
        ImageView ratingImage;
        ImageView thumbnail;
        TextView categories;
        TextView deals;
        TextView distanceAndReviewCount;

        ImageButton saveButton;

        public RestaurantViewHolder(View itemView) {
            super(itemView);

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
            deals = (TextView) itemView.findViewById(R.id.deals);
            distanceAndReviewCount = (TextView) itemView.findViewById(R.id.distanceAndReviewCount);

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
