package com.datausagenotifier.model;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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

    public void serialize(FileWriter fw) throws IOException, JSONException {
        JSONStringer stringer = new JSONStringer();
        stringer.array();
        for (int i=0; i < getCount(); i++) {
            TrafficStatsArrayItem item = getItem(i);
            item.toJSONStringer(stringer);
        }
        stringer.endArray();

        fw.write(stringer.toString());
    }

    public void deserialize(FileReader fileReader) throws IOException {
        TrafficStatsArrayItem item;
        JsonReader reader = new JsonReader(fileReader);
        reader.beginArray();
        while(reader.hasNext()) {
            item = TrafficStatsArrayItem.getItemFromJsonReader(reader);
            add(item);
        }
        reader.endArray();
    }
}
