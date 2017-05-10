package com.datausagenotifier.model;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class TrafficStatsArrayAdapter extends ArrayAdapter<TrafficStatsArrayItem> {

    public TrafficStatsArrayAdapter(@NonNull Context ctx, @LayoutRes int resource, @IdRes int textViewResourceId) {
        super(ctx, resource, textViewResourceId);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TrafficStatsArrayItem item = getItem(position);
        if (item.isFirstPass()) {
            view.setBackgroundColor(0xffffffcc);
        } else {
            view.setBackgroundColor(0xffffffff);
        }
        return view;
    }
}
