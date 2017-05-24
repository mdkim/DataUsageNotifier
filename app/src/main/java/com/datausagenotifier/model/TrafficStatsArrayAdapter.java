package com.datausagenotifier.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrafficStatsArrayAdapter extends ArrayAdapter<TrafficStatsArrayItem> {

    private static final String TAG = "TrafficStatsAdapter";
    private static final int ICON_SIZE_PX = 48;

    //private int textViewResourceId;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());

    private static final int WRAP_CONTENT = RelativeLayout.LayoutParams.WRAP_CONTENT;
    private static Drawable genericAndroidIcon;

    public TrafficStatsArrayAdapter(@NonNull Context ctx, @LayoutRes int resource, @IdRes int textViewResourceId) {
        super(ctx, resource, textViewResourceId);
        //this.textViewResourceId = textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        RelativeLayout view = (RelativeLayout) super.getView(position, null, parent); //convertView
        // `view` has a TextView `textViewResourceId` with stats item (as SpannableString)

        TrafficStatsArrayItem item = getItem(position);
        if (item.isFirstPass()) {
            view.setBackgroundColor(0xff757575); //333300 //secondary_text
        } else {
            view.setBackgroundColor(0xff212121); //primary_text
        }
        //TextView textView = (TextView) view.findViewById(textViewResourceId);

        Context ctx = view.getContext();
        ApplicationInfo appInfo;
        Resources res;
        List<String> packageNames = item.getPackageNames();
        int prevImageViewId = -1;
        for (String packageName : packageNames) {
            try {
                appInfo = ctx.getPackageManager().getApplicationInfo(packageName, 0);
                res = ctx.getPackageManager().getResourcesForApplication(appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "packageName '" + packageName + "' not found", e);
                throw new RuntimeException(e);
            }

            Drawable d;
            if (appInfo.icon == 0) {
                d = getGenericAndroidIcon(ctx, packageName);
            } else {
                try {
                    d = res.getDrawableForDensity(appInfo.icon, DisplayMetrics.DENSITY_MEDIUM, null);
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "unexpected: appInfo.icon=" + appInfo.icon, e);
                    d = getGenericAndroidIcon(ctx, packageName);
                }
            }
            if (d == null) continue;

            int h = d.getIntrinsicHeight();
            int w = d.getIntrinsicWidth();
            if (h != ICON_SIZE_PX || w != ICON_SIZE_PX) {
                Log.i(TAG, "packageName=" + packageName + ", h=" + h + ", w=" + w);
                // d can be VectorDrawable
                if (!(d instanceof BitmapDrawable)) break;

                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap bitmap = Bitmap.createScaledBitmap(bd.getBitmap(), ICON_SIZE_PX, ICON_SIZE_PX, false);
                d = new BitmapDrawable(res, bitmap);
            }

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.setMargins(12, 0, 0, 16);
            if (prevImageViewId > 0) params.addRule(RelativeLayout.BELOW, prevImageViewId);

            ImageView imageView = new ImageView(ctx);
            prevImageViewId = View.generateViewId();
            imageView.setId(prevImageViewId);
            imageView.setImageDrawable(d);
            view.addView(imageView, params);
        }

        return view;
    }

    private Drawable getGenericAndroidIcon(Context ctx, String packageName) {
        if (genericAndroidIcon != null) return genericAndroidIcon;
        Drawable d;
        try {
            d = ctx.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            d = null; // unexpected
        }
        genericAndroidIcon = d;
        return d;
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
                String itemHtml = Html.toHtml(item); //Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
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
