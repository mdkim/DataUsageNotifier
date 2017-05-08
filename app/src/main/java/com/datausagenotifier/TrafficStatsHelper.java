package com.datausagenotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.text.format.Formatter;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.List;

public class TrafficStatsHelper {

    private static final String TAG = "TrafficStatsHelper";

    private static long TOTAL_RX_BYTES;
    private static long TOTAL_TX_BYTES;
    private static LongSparseArray<Long> UID_RX_MAP = new LongSparseArray<>();
    private static LongSparseArray<Long> UID_TX_MAP = new LongSparseArray<>();

    // returns null if no updated activity
    public static String getTrafficStatsUpdate(Context ctx) throws UnsupportedDeviceException {
        long starttime = System.currentTimeMillis();

        // quick check of total data usage
        long totalRxBytes = TrafficStats.getTotalRxBytes();
        long totalTxBytes = TrafficStats.getTotalTxBytes();
        if (totalRxBytes == TrafficStats.UNSUPPORTED) {
            throw new UnsupportedDeviceException();
        }
        if (totalRxBytes == TOTAL_RX_BYTES && totalTxBytes == TOTAL_TX_BYTES) {
            return null;
        }
        TOTAL_RX_BYTES = totalRxBytes;
        TOTAL_TX_BYTES = totalTxBytes;

        StringBuilder msg = new StringBuilder();
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        //List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        List<ActivityManager.RunningServiceInfo> rsiList = activityManager.getRunningServices(Integer.MAX_VALUE);

        int rxtxCount=0; //rxCount=0, txCount=0
        for (ActivityManager.RunningServiceInfo rsi : rsiList) {
            int uid = rsi.uid;
            //String processName = rsi.process;
            String serviceName = parseServiceName(rsi.service.getClassName());

            long uidRxBytes = TrafficStats.getUidRxBytes(uid);
            long uidTxBytes = TrafficStats.getUidTxBytes(uid);
            long prev_uidRxBytes = UID_RX_MAP.get(uid, 0L);
            long prev_uidTxBytes = UID_TX_MAP.get(uid, 0L);
            boolean isRx = (uidRxBytes > prev_uidRxBytes);
            boolean isTx = (uidTxBytes > prev_uidTxBytes);
            if (!(isRx || isTx)) continue;

            msg.append(serviceName);
            if (isRx) {
                String rxRate = formatBytesPerSec(ctx, (uidRxBytes - prev_uidRxBytes));
                msg.append(" Received ").append(rxRate).append("/s");
                //rxCount++;
            }
            if (isTx) {
                String txRate = formatBytesPerSec(ctx, (uidTxBytes - prev_uidTxBytes));
                msg.append(" Sent ").append(txRate).append("/s");
                //txCount++;
            }
            msg.append("\n");
            rxtxCount++;

            UID_RX_MAP.put(uid, uidRxBytes);
            UID_TX_MAP.put(uid, uidTxBytes);
        }

        if (rxtxCount == 0) {
            msg.append("(Unknown processes)\n");
        }

        // TODO: send msg as array for inboxstyle;
        // set the notification title to this:
        msg.append(rxtxCount).append("+ apps sent/received data");

        long dur = System.currentTimeMillis() - starttime;
        Log.v(TAG, "getTrafficStatsUpdate() dur=" + dur + " ms");

        String msg_s = msg.toString();
        Log.v(TAG, "---\n" + msg_s + "\n===\n");
        return msg_s;
    }

    private static String formatBytesPerSec(Context ctx, long bytes) {
        float bytesPerSec_float = (bytes * 1000) / DataUsageMonitorService.POLLING_INTERVAL_MS;
        long bytesPerSec = (long) bytesPerSec_float;
        String msg = Formatter.formatShortFileSize(ctx, bytesPerSec);
        return msg;
    }

    private static String parseServiceName(String className) {
        int pos = className.lastIndexOf('.');
        if (pos < 0 || pos == className.length() - 1) return className;
        //String serviceName = className.substring(pos+1, className.length());
        //return serviceName;
        return className;
    }

    // for reference: attributes of RunningServiceInfo and ApplicationInfo;
    // returns same result as rsi.uid
    @SuppressWarnings("unused")
    private static int getUid(Context ctx, ActivityManager.RunningServiceInfo rsi) {
        String processName = rsi.process; // "com.datausagenotifier"
        //rsi.service.getPackageName(); // "com.datausagenotifier"
        //rsi.service.getClassName(); // "com.datausagenotifier.DataUsageMonitorService"
        PackageManager pm = ctx.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(processName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App process name not found: '" + processName + "'");
            return -1;
        }
        int uid = appInfo.uid;
        return uid;
        /*
        if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1) {
            // skip updated system apps
            continue;
        } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
            // skip system image apps
            continue;
        }*/
    }
}