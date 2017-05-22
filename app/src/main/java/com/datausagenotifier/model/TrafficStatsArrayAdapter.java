package com.datausagenotifier.model;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrafficStatsArrayAdapter extends ArrayAdapter<TrafficStatsArrayItem> {

    private static final String TAG = "TrafficStatsAdapter";

    private int textViewResourceId;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());

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

    public int exportLogs() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String datetime = sdf.format(new Date());
        File file = new File(dir, "DUNotif." + datetime + ".html");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<head><style>body { margin: 0; background-color: white } ");
            writer.write("div { font-family: sans-serif, monospace; padding: 3pt 12pt; } ");
            writer.write("p { margin: 0 }</style></head><body>\n");
            for (int i = 0; i < getCount(); i++) {
                TrafficStatsArrayItem item = getItem(i);
                String itemHtml = Html.toHtml(item);
                StringBuilder divHtml = new StringBuilder();
                if (item.isFirstPass()) {
                    divHtml.append("<div style=\"background-color: #cccccc\">");
                } else {
                    divHtml.append("<div>");
                }
                divHtml.append(itemHtml).append("<br></div>");

                writer.write(divHtml.toString());
            }
            writer.write("</body>\n");
        } catch (IOException e) {
            Log.e(TAG, "Error exporting logs", e);
            return -1;
        }
        return 1;
    }
}
