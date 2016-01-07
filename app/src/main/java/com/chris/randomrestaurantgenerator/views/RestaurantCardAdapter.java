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
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.squareup.picasso.Picasso;

public class RestaurantCardAdapter extends RecyclerView.Adapter<RestaurantCardAdapter.RestaurantViewHolder> {

    Context context;
    Restaurant restaurant;

    public RestaurantCardAdapter(Context con, Restaurant res) {
        this.context = con;
        this.restaurant = res;
        Log.d("Chris", "RestaurantCardAdapter() called with: " + "con = [" + con + "], res = [" + res + "]");
    }

    public void add(Restaurant res) {
        this.restaurant = res;
        notifyItemInserted(0);
    }

    public void remove() {
        notifyItemRemoved(0);
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("Chris", "onCreateViewHolder() called with: " + "parent = [" + parent + "], viewType = [" + viewType + "]");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Log.d("Chris", "onBindViewHolder() called with: " + "holder = [" + holder + "], position = [" + position + "]");

        Picasso.with(context).load(restaurant.getThumbnailURL()).into(holder.thumbnail);
        Picasso.with(context).load(restaurant.getRatingImageURL()).into(holder.ratingImage);
        holder.nameOfRestaurant.setText(restaurant.getName());
        holder.categories.setText(restaurant.getCategories().toString());
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

        public RestaurantViewHolder(View itemView) {
            super(itemView);

            Log.d("Chris", "RestaurantViewHolder() called with: " + "itemView = [" + itemView + "]");

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
        }

    }
}
