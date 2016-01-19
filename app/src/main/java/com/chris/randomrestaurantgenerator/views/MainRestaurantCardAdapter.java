package com.chris.randomrestaurantgenerator.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.models.SavedListHolder;
import com.squareup.picasso.Picasso;

/**
 * A RecyclerView Adapter for the main fragment. Only displays one card at a time, rather than a list.
 */
public class MainRestaurantCardAdapter extends RecyclerView.Adapter<MainRestaurantCardAdapter.RestaurantViewHolder> {

    Context context;
    Restaurant restaurant;
    SavedListHolder savedListHolder;

    public MainRestaurantCardAdapter(Context con, Restaurant res) {
        this.context = con;
        this.restaurant = res;

        this.savedListHolder = SavedListHolder.getInstance();
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
        holder.categories.setText(restaurant.getCategories().toString());
        holder.deals.setText(restaurant.getDeal());
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

        ImageView addToSavedList;

        public RestaurantViewHolder(View itemView) {
            super(itemView);

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
            deals = (TextView) itemView.findViewById(R.id.deals);

            addToSavedList = (ImageView) itemView.findViewById(R.id.addToSavedList);

            // Adds current restaurant to the saved list on click.
            addToSavedList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // If the user tries to save the same restaurant more than once, alert them.
                    if (savedListHolder.getSavedList().contains(restaurant)) {
                        Toast.makeText(context, "You have already saved this restaurant.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addToList(restaurant);
                }
            });
        }

    }
}
