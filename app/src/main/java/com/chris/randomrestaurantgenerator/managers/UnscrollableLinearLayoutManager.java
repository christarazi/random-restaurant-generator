package com.chris.randomrestaurantgenerator.managers;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class UnscrollableLinearLayoutManager extends LinearLayoutManager {
    public UnscrollableLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }
}

