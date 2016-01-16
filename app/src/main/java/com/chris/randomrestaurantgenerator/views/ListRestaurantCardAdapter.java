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

public class ListRestaurantCardAdapter extends RecyclerView.Adapter<ListRestaurantCardAdapter.RestaurantViewHolder> {

    Context context;
    MaybeListHolder maybeListHolder;

    public ListRestaurantCardAdapter(Context con) {
        this.context = con;
        this.maybeListHolder = MaybeListHolder.getInstance();
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_list_restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    public void remove(int index) {
        maybeListHolder.getMaybeList().remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {

        Restaurant restaurant = maybeListHolder.getMaybeList().get(position);

        Log.d("Chris", "onBindViewHolder() from List with restaurant: " + restaurant.toString());

        Picasso.with(context).load(restaurant.getThumbnailURL()).into(holder.thumbnail);
        Picasso.with(context).load(restaurant.getRatingImageURL()).into(holder.ratingImage);
        holder.nameOfRestaurant.setText(restaurant.getName());
        holder.categories.setText(restaurant.getCategories().toString());
        holder.deals.setText(restaurant.getDeal());
    }

    @Override
    public int getItemCount() {
        return maybeListHolder.getMaybeList().size();
    }

    public class RestaurantViewHolder extends RecyclerView.ViewHolder {

        TextView nameOfRestaurant;
        ImageView ratingImage;
        ImageView thumbnail;
        TextView categories;
        TextView deals;

        public RestaurantViewHolder(View itemView) {
            super(itemView);

            Log.d("Chris", "RestaurantViewHolder() called with: " + "itemView = [" + itemView + "]");

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.listName);
            ratingImage = (ImageView) itemView.findViewById(R.id.listRating);
            thumbnail = (ImageView) itemView.findViewById(R.id.listThumbnail);
            categories = (TextView) itemView.findViewById(R.id.listCategories);
            deals = (TextView) itemView.findViewById(R.id.listDeals);
        }

    }
}
