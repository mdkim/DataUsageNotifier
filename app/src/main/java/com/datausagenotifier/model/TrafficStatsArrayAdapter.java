package com.datausagenotifier.model;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TrafficStatsArrayAdapter extends ArrayAdapter<TrafficStatsArrayItem> {

    private int textViewResourceId;

    public TrafficStatsArrayAdapter(@NonNull Context ctx, @LayoutRes int resource, @IdRes int textViewResourceId) {
        super(ctx, resource, textViewResourceId);
        this.textViewResourceId = textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TrafficStatsArrayItem item = getItem(position);
        if (item.isFirstPass()) {
            view.setBackgroundColor(0xff757575); //333300 //secondary_text
        } else {
            view.setBackgroundColor(0xff212121); //primary_text
        }
        TextView textView = (TextView) view.findViewById(textViewResourceId);
        textView.setTextColor(0xffffffff);
        return view;
    }
}
