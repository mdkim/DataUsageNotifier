package com.datausagenotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;
import android.util.LongSparseArray;

import java.io.File;
import java.util.List;

public class TrafficStatsHelper {

    private static final String TAG = "TrafficStatsHelper";

    private static long TOTAL_RX_BYTES;
    private static long TOTAL_TX_BYTES;
    private static LongSparseArray UID_RX_MAP = new LongSparseArray();
    private static LongSparseArray UID_TX_MAP = new LongSparseArray();

    // returns null if no updated activity
    public static String getTrafficStatsUpdate(Context ctx) throws UnsupportedDeviceException {

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
        PackageManager pm = ctx.getPackageManager();
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (int i = 0; i < appProcesses.size(); i++) {
            String processName = appProcesses.get(i).processName;
            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(processName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "App process name not found: '" + processName + "'");
                continue;
            }
            /*
            if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1) {
                // skip updated system apps
                continue;
            } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                // skip system image apps
                continue;
            }*/

            int uid = appInfo.uid;
            long uidRxBytes = TrafficStats.getUidRxBytes(uid);
            long uidTxBytes = TrafficStats.getUidTxBytes(uid);
            long prev_uidRxBytes = (Long) UID_RX_MAP.get(uid, 0L);
            long prev_uidTxBytes = (Long) UID_TX_MAP.get(uid, 0L);
            if (uidRxBytes > prev_uidRxBytes) {
                msg.append("UID(").append(uid).append(") ").append(processName).append(": Received ")
                        .append(uidRxBytes - prev_uidRxBytes).append(" bytes\n");
            }
            if (uidTxBytes > prev_uidTxBytes) {
                msg.append("UID(").append(uid).append(") ").append(processName).append(": Sent ")
                        .append(uidTxBytes - prev_uidTxBytes).append(" bytes\n");
            }
            UID_RX_MAP.put(uid, uidRxBytes);
            UID_TX_MAP.put(uid, uidTxBytes);
        }

        if (msg.length() == 0) {
            msg.append("(Unknown processes)");
        }
        return msg.toString();
    }

    private static void baz() {
        /*
            private fun getChildren(file: File): Int {
        if (file.listFiles() == null)
            return 0

        if (file.isDirectory) {
            return if (mShowHidden) {
                file.listFiles()?.size ?: 0
            } else {
                file.listFiles { file -> !file.isHidden }?.size ?: 0
            }
        }
        return 0
    }
         */
    }
}