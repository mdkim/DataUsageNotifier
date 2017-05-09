package com.datausagenotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;
import android.util.LongSparseArray;

import com.datausagenotifier.model.TrafficStatsUid;
import com.datausagenotifier.model.TrafficStatsUpdate;

import java.util.List;

public class TrafficStatsHelper {

    private static final String TAG = "TrafficStatsHelper";

    private static long TOTAL_RX_BYTES;
    private static long TOTAL_TX_BYTES;
    private static LongSparseArray<Long> UID_RX_MAP = new LongSparseArray<>();
    private static LongSparseArray<Long> UID_TX_MAP = new LongSparseArray<>();

    // returns null if no updated activity
    public static TrafficStatsUpdate getTrafficStatsUpdate(Context ctx) throws UnsupportedDeviceException {
        if (MainActivity.IS_TEST_DATA) {
            return getTestStats(ctx);
        }
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

        TrafficStatsUpdate stats = new TrafficStatsUpdate();
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        //List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        List<ActivityManager.RunningServiceInfo> rsiList = activityManager.getRunningServices(Integer.MAX_VALUE);

        int rxtxCount=0; //rxCount=0, txCount=0
        for (ActivityManager.RunningServiceInfo rsi : rsiList) {
            int uid = rsi.uid;
            //String processName = rsi.process;
            String serviceClass = rsi.service.getClassName();

            long uidRxBytes = TrafficStats.getUidRxBytes(uid);
            long uidTxBytes = TrafficStats.getUidTxBytes(uid);
            long prev_uidRxBytes = UID_RX_MAP.get(uid, 0L);
            long prev_uidTxBytes = UID_TX_MAP.get(uid, 0L);
            boolean isRx = (uidRxBytes > prev_uidRxBytes);
            boolean isTx = (uidTxBytes > prev_uidTxBytes);
            if (!(isRx || isTx)) continue;

            TrafficStatsUid statsUid = new TrafficStatsUid(ctx, serviceClass, uid);
            if (isRx) {
                statsUid.setRxBytes((uidRxBytes - prev_uidRxBytes));
                //rxCount++;
            }
            if (isTx) {
                statsUid.setTxBytes((uidTxBytes - prev_uidTxBytes));
                //txCount++;
            }
            stats.addStatsUid(statsUid);
            rxtxCount++;

            UID_RX_MAP.put(uid, uidRxBytes);
            UID_TX_MAP.put(uid, uidTxBytes);
        }

        stats.setRxTxCount(rxtxCount);

        long dur = System.currentTimeMillis() - starttime;
        stats.setDurationMs(dur);
        Log.v(TAG, "getTrafficStatsUpdate() dur=" + dur + " ms");

        return stats;
    }
    private static int tempcounter=0;
    public static TrafficStatsUpdate getTestStats(Context ctx) throws UnsupportedDeviceException {
        TrafficStatsUpdate stats = new TrafficStatsUpdate();
        for (int i=1; i < 10; i++) {
            TrafficStatsUid statsUid = new TrafficStatsUid(ctx, "com.google.android.location.places.service.PlaceDet" + tempcounter, i);
            statsUid.setRxBytes(200_000 * i);
            statsUid.setTxBytes(200_000 * i);
            stats.addStatsUid(statsUid);
        }
        tempcounter++;
        stats.setRxTxCount(1);
        return stats;
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