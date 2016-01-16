package com.chris.randomrestaurantgenerator.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.models.MaybeListHolder;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.squareup.picasso.Picasso;

public class MainRestaurantCardAdapter extends RecyclerView.Adapter<MainRestaurantCardAdapter.RestaurantViewHolder> {

    Context context;
    Restaurant restaurant;
    MaybeListHolder maybeListHolder;

    public MainRestaurantCardAdapter(Context con, Restaurant res) {
        this.context = con;
        this.restaurant = res;

        this.maybeListHolder = MaybeListHolder.getInstance();
        Log.d("Chris", "MainRestaurantCardAdapter() called with: " + "con = [" + con + "], res = [" + res + "]");
    }

    public void add(Restaurant res) {
        this.restaurant = res;
        notifyItemInserted(0);
    }

    public void remove() {
        notifyItemRemoved(0);
    }

    // Function to use the interface to add restaurant to maybeList.
    private void addToList(Restaurant res) {
        maybeListHolder.getMaybeList().add(res);
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("Chris", "onCreateViewHolder() called with: " + "parent = [" + parent + "], viewType = [" + viewType + "]");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_main_restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Log.d("Chris", "onBindViewHolder() called with: " + "holder = [" + holder + "], position = [" + position + "]");

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

        ImageView addToMaybeList;

        public RestaurantViewHolder(View itemView) {
            super(itemView);

            Log.d("Chris", "RestaurantViewHolder() called with: " + "itemView = [" + itemView + "]");

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
            deals = (TextView) itemView.findViewById(R.id.deals);

            addToMaybeList = (ImageView) itemView.findViewById(R.id.addToMaybeList);

            // Adds current restaurant to the maybe list on click.
            addToMaybeList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToList(restaurant);
                    Log.d("Chris", "added to maybe list");
                }
            });
        }

    }
}
