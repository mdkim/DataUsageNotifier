package com.datausagenotifier.model;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.Formatter;

import com.datausagenotifier.util.Span;

public class TrafficStatsUid {

    private Context ctx; // for Formatter

    String serviceClass;
    int uid;
    long rxBytes;
    long txBytes;

    public TrafficStatsUid(Context ctx, String serviceClass, int uid) {
        this.ctx = ctx;
        this.serviceClass = serviceClass;
        this.uid = uid;
    }

    public void setRxBytes(long rxBytes) {
        this.rxBytes = rxBytes;
    }
    public void setTxBytes(long txBytes) {
        this.txBytes = txBytes;
    }

    public void appendStatsUidTo(SpannableStringBuilder ssb) {
        appendBytes(ssb, "Received", this.rxBytes);
        appendBytes(ssb, "Sent", this.txBytes);
        String[] service_class= parseServiceName(this.serviceClass);
        String service_class_f = " (" + service_class[1] + ")";
        ssb.append(service_class[0], Span.BOLD_SPAN(), 0)
                .append(service_class_f, Span.TINY_SPAN(), 0)
                .append("\n");
    }

    private void appendBytes(SpannableStringBuilder ssb, String text, long bytes) {
        if (bytes == 0) return;
        ssb.append(text).append(" ");
        if (bytes > 900_000) {
            ssb.append(formatBytesPerSec(bytes), Span.BOLD_SPAN(), 0);
        } else {
            ssb.append(formatBytesPerSec(bytes));
        }
        //ssb.append("/s");
        ssb.append(" ");
    }

    // { serviceName, packageName }
    private static String[] parseServiceName(String className) {
        int pos = className.lastIndexOf('.');
        if (pos < 0 || pos == className.length() - 1) {
            String[] result = { className, className };
            return result;
        }
        String serviceName = className.substring(pos+1, className.length());
        String packageName = className.substring(0, pos);
        String[] result = { serviceName, packageName };
        return result;
    }

    private String formatBytesPerSec(long bytes) {
        // for now, return total bytes, not bytes per sec
        float bytesPerSec_float = bytes;
        //(bytes * 1000) / DataUsageMonitorService.POLLING_INTERVAL_MS;

        long bytesPerSec = (long) bytesPerSec_float;
        String msg = Formatter.formatShortFileSize(ctx, bytesPerSec);
        return msg;
    }
}
