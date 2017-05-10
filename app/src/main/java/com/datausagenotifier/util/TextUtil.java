package com.datausagenotifier.util;

import android.content.Context;
import android.text.format.Formatter;

public class TextUtil {

    public static String formatBytesPerSec(Context ctx, long bytes) {
        // for now, return total bytes, not bytes per sec
        float bytesPerSec_float = bytes;
        //(bytes * 1000) / DataUsageMonitorService.POLLING_INTERVAL_MS;

        long bytesPerSec = (long) bytesPerSec_float;
        String msg = Formatter.formatShortFileSize(ctx, bytesPerSec);
        msg = msg.toLowerCase();
        msg = msg.replace("mb", "MB");
        return msg;
    }
}
