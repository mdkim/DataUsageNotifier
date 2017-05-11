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

public class TrafficStatsArrayItem extends SpannableString {

    private boolean isFirstPass;

    public TrafficStatsArrayItem(CharSequence source, boolean isFirstPass) {
        super(source);
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

    public static TrafficStatsArrayItem getItemFromJsonReader(JsonReader reader) throws IOException {

        boolean isFirstPass=false;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        reader.beginObject(); // item object
        String key;
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
                default:
                    // throw exception
            }
        }
        reader.endObject(); // end item object

        TrafficStatsArrayItem item = new TrafficStatsArrayItem(ssb, isFirstPass);
        return item;
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
}
