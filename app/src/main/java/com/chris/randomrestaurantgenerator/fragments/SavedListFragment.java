package com.chris.randomrestaurantgenerator.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chris.randomrestaurantgenerator.R;
import com.chris.randomrestaurantgenerator.models.Restaurant;
import com.chris.randomrestaurantgenerator.models.SavedListHolder;
import com.chris.randomrestaurantgenerator.views.ListRestaurantCardAdapter;

/**
 * A fragment containing the SavedListActivity.
 * Responsible for displaying the restaurants the user has saved.
 */
public class SavedListFragment extends Fragment {

    LinearLayout rootLayout;
    RecyclerView listRecyclerView;
    TextView emptyListView;

    ListRestaurantCardAdapter listRestaurantCardAdapter;

    SavedListHolder savedListHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        // http://developer.android.com/training/basics/activity-lifecycle/recreating.html
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            savedListHolder.setSavedList(savedInstanceState.<Restaurant>getParcelableArrayList("savedList"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        savedListHolder = SavedListHolder.getInstance();

        rootLayout = (LinearLayout) inflater.inflate(R.layout.fragment_saved_list, container, false);
        listRecyclerView = (RecyclerView) rootLayout.findViewById(R.id.listRecyclerView);
        listRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyListView = (TextView) rootLayout.findViewById(R.id.emptyText);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                // If user has swiped left, remove the item from the list.
                if (direction == 4) {
                    listRestaurantCardAdapter.remove(viewHolder.getAdapterPosition());
                }

                // If user has swiped right, open Yelp to current restaurant's page.
                if (direction == 8) {

                    // Open restaurant in Yelp.
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(savedListHolder.getSavedList().get(viewHolder.getAdapterPosition()).getUrl())));

                    // We don't want to remove the list item if user wants to see it in Yelp.
                    // Tell the adapter to refresh so the item is can be visible again.
                    listRestaurantCardAdapter.notifyDataSetChanged();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(listRecyclerView);

        return rootLayout;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the list is null or empty, we want to avoid any exceptions thrown from the
        // RecyclerView Adapter. So, set TextView to inform the user no items have been added to the savedList.
        if (savedListHolder.getSavedList() == null || savedListHolder.getSavedList().isEmpty()) {
            listRecyclerView.setVisibility(View.GONE);
            emptyListView.setVisibility(View.VISIBLE);

            return;
        }

        // Else, make the list visible.
        listRecyclerView.setVisibility(View.VISIBLE);
        emptyListView.setVisibility(View.GONE);
        listRestaurantCardAdapter = new ListRestaurantCardAdapter(getContext());
        listRecyclerView.setAdapter(listRestaurantCardAdapter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        // http://developer.android.com/training/basics/activity-lifecycle/recreating.html
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("savedList", savedListHolder.getSavedList());
    }
}
