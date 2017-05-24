package com.datausagenotifier.model;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.JsonReader;

import com.datausagenotifier.util.Span;
import com.datausagenotifier.util.SpanFactory;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrafficStatsArrayItem extends SpannableString {

    private List<String> packageNames;
    private boolean isFirstPass;

    public TrafficStatsArrayItem(CharSequence source, List<String> packageNames, boolean isFirstPass) {
        super(source);
        this.packageNames = packageNames;
        this.isFirstPass = isFirstPass;
    }

    public boolean isFirstPass() {
        return this.isFirstPass;
    }

    public JSONStringer toJSONStringer(JSONStringer stringer) throws JSONException {

        stringer.object(); // item object

        stringer.key("isFirstPass");
        stringer.value(this.isFirstPass);

        stringer.key("text");
        stringer.value(this.toString());

        stringerSpans(stringer, StyleSpan.class, "styleSpans");
        stringerSpans(stringer, RelativeSizeSpan.class, "relativeSpans");

        stringerPackageNames(stringer);

        stringer.endObject(); // end item object
        return stringer;
    }

    private void stringerSpans(JSONStringer stringer, Class<?> spanClass, String spanKey) throws JSONException {
        stringer.key(spanKey);
        stringer.array(); // spans array
        Object[] styleSpans = this.getSpans(0, this.length(), spanClass);
        for (Object span : styleSpans) {
            stringer.array(); // [start, end]
            stringer.value(this.getSpanStart(span));
            stringer.value(this.getSpanEnd(span));
            stringer.endArray();
        }
        stringer.endArray(); // end spans array
    }

    private void stringerPackageNames(JSONStringer stringer) throws JSONException {
        stringer.key("packageNames");
        stringer.array();
        for (String packageName : packageNames) {
            stringer.value(packageName);
        }
        stringer.endArray();
    }

    public static TrafficStatsArrayItem getItemFromJsonReader(JsonReader reader) throws IOException {

        boolean isFirstPass=false;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        reader.beginObject(); // item object
        String key;
        List<String> packageNames = null;
        while(reader.hasNext()) {
            key = reader.nextName();
            switch (key) {
                case "isFirstPass":
                    isFirstPass = reader.nextBoolean();
                    break;
                case "text":
                    ssb.append(reader.nextString());
                    break;
                case "styleSpans":
                    readNextSpan_setSpan(reader, ssb, new Span.BOLD_SPAN_FACTORY());
                    break;
                case "relativeSpans":
                    readNextSpan_setSpan(reader, ssb, new Span.TINY_SPAN_FACTORY());
                    break;
                case "packageNames":
                    packageNames = readPackageNames(reader);
                    break;
                default:
                    // throw exception
            }
        }
        reader.endObject(); // end item object

        TrafficStatsArrayItem item = new TrafficStatsArrayItem(ssb, packageNames, isFirstPass);
        return item;
    }

    private static List<String> readPackageNames(JsonReader reader) throws IOException {
        reader.beginArray();
        List<String> packageNames = new ArrayList<>();
        while (reader.hasNext()) {
            packageNames.add(reader.nextString());
        }
        reader.endArray();
        return packageNames;
    }

    private static void readNextSpan_setSpan(JsonReader reader, SpannableStringBuilder ssb,
                                             SpanFactory spanFactory) throws IOException {
        reader.beginArray();
        int spanStart, spanEnd;
        while (reader.hasNext()) {
            reader.beginArray();
            spanStart = reader.nextInt();
            spanEnd = reader.nextInt();
            reader.endArray();
            ssb.setSpan(spanFactory.getInstance(), spanStart, spanEnd, 0);
        }
        reader.endArray();
    }

    public List<String> getPackageNames() {
        return this.packageNames;
    }
}
