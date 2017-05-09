package com.datausagenotifier.model;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

public class TrafficStatsArrayAdapter extends ArrayAdapter<CharSequence> {

    private Context ctx;

    public TrafficStatsArrayAdapter(@NonNull Context ctx, @LayoutRes int resource, @IdRes int textViewResourceId) {
        super(ctx, resource, textViewResourceId);
        this.ctx = ctx;
    }
}
