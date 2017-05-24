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

    private static boolean IS_FIRST_PASS = true;
    private static long TOTAL_RX_BYTES;
    private static long TOTAL_TX_BYTES;
    private static LongSparseArray<Long> UID_RX_MAP = new LongSparseArray<>();
    private static LongSparseArray<Long> UID_TX_MAP = new LongSparseArray<>();
    private static long LAST_UPDATED=0;

    // returns null if no updated activity
    public static TrafficStatsUpdate getTrafficStatsUpdate(Context ctx) throws UnsupportedDeviceException {
        if (MainActivity.IS_TEST_DATA) {
            return getTestStats(ctx);
        }
        long starttime = System.currentTimeMillis();
        long updatedAgo = refreshLastUpdated_GetUpdatedAgo();

        // quick check of total data usage
        long totalRxBytes = TrafficStats.getTotalRxBytes();
        long totalTxBytes = TrafficStats.getTotalTxBytes();
        if (totalRxBytes == TrafficStats.UNSUPPORTED) {
            throw new UnsupportedDeviceException();
        }
        TrafficStatsUpdate stats = new TrafficStatsUpdate();
        if (totalRxBytes == TOTAL_RX_BYTES && totalTxBytes == TOTAL_TX_BYTES) {
            // no activity (skip uid traffic stats)
            stats.setNoActivity(true);
            stats.setUpdatedAgo(updatedAgo);
            long dur = System.currentTimeMillis() - starttime;
            stats.setDurationMs(dur);
            return stats;
        }
        long deltaTotalRxBytes = totalRxBytes - TOTAL_RX_BYTES;
        long deltaTotalTxBytes = totalTxBytes - TOTAL_TX_BYTES;
        IS_FIRST_PASS = (TOTAL_RX_BYTES == 0 && TOTAL_TX_BYTES == 0 && UID_RX_MAP.size() == 0 && UID_TX_MAP.size() == 0);
        TOTAL_RX_BYTES = totalRxBytes;
        TOTAL_TX_BYTES = totalTxBytes;

        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        //List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        List<ActivityManager.RunningServiceInfo> rsiList = activityManager.getRunningServices(Integer.MAX_VALUE);

        int rxtxCount=0; //rxCount=0, txCount=0
        long deltaTotalUidRxBytes=0, deltaTotalUidTxBytes=0;
        for (ActivityManager.RunningServiceInfo rsi : rsiList) {
            //if (!rsi.started && !IS_FIRST_PASS) continue; // skip stopped services

            int uid = rsi.uid;
            //String processName = rsi.process;
            String serviceClass = rsi.service.getClassName();
            String packageName = rsi.service.getPackageName();

            long uidRxBytes = TrafficStats.getUidRxBytes(uid);
            long uidTxBytes = TrafficStats.getUidTxBytes(uid);
            long prev_uidRxBytes = UID_RX_MAP.get(uid, 0L);
            long prev_uidTxBytes = UID_TX_MAP.get(uid, 0L);
            boolean isRx = (uidRxBytes > prev_uidRxBytes);
            boolean isTx = (uidTxBytes > prev_uidTxBytes);
            if (!(isRx || isTx)) continue;
            deltaTotalUidRxBytes += (uidRxBytes - prev_uidRxBytes);
            deltaTotalUidTxBytes += (uidTxBytes - prev_uidTxBytes);

            if (!rsi.started && !IS_FIRST_PASS) {
                Log.e(TAG, "Unexpected activity on stopped service: " + serviceClass);
            }

            TrafficStatsUid statsUid = new TrafficStatsUid(ctx, serviceClass, packageName, uid);
            if (isRx) {
                long rxBytes = (uidRxBytes - prev_uidRxBytes);
                statsUid.setRxBytes(rxBytes);
                deltaTotalUidRxBytes += rxBytes;
                //rxCount++;
            }
            if (isTx) {
                long txBytes = (uidTxBytes - prev_uidTxBytes);
                statsUid.setTxBytes(txBytes);
                deltaTotalUidTxBytes += txBytes;
                //txCount++;
            }
            stats.addStatsUid(statsUid);
            rxtxCount++;

            UID_RX_MAP.put(uid, uidRxBytes);
            UID_TX_MAP.put(uid, uidTxBytes);
        }

        stats.setRxTxCount(rxtxCount);
        stats.setUnknownRxBytes(deltaTotalRxBytes - deltaTotalUidRxBytes);
        stats.setUnknownTxBytes(deltaTotalTxBytes - deltaTotalUidTxBytes);
        stats.setIsFirstPass(IS_FIRST_PASS);
        stats.setUpdatedAgo(updatedAgo);

        long dur = System.currentTimeMillis() - starttime;
        stats.setDurationMs(dur);
        Log.v(TAG, "getTrafficStatsUpdate() dur=" + dur + " ms");

        return stats;
    }

    private static long refreshLastUpdated_GetUpdatedAgo() {
        long lastUpdated = LAST_UPDATED;
        LAST_UPDATED = System.currentTimeMillis();
        long updatedAgo = ((LAST_UPDATED - lastUpdated) / 1000);
        return updatedAgo;
    }

    private static int tempcounter=0;
    public static TrafficStatsUpdate getTestStats(Context ctx) throws UnsupportedDeviceException {
        long updatedAgo = refreshLastUpdated_GetUpdatedAgo();

        TrafficStatsUpdate stats = new TrafficStatsUpdate();
        if (tempcounter++ % 2 == 1) {
            // no activity
            stats.setNoActivity(true);
            stats.setUpdatedAgo(updatedAgo);
            stats.setDurationMs(1);
            return stats;
        }

        String[] serviceNames = { "DataUsageMonitorService", "KeyguardService", "PlaceDetectionService", "LatinIME", "GeofenceHardwareService", "MmsService", "DefaultContainerService", "TelephonyDebugService" };
        String[] packageNames = { "com.datausagenotifier", "com.android.systemui", "com.google.android.gms", "com.android.inputmethod.latin", "android", "com.android.mms.service", "com.android.defcontainer", "com.android.phone" };
        for (int i = 0; i < serviceNames.length; i++) {
            TrafficStatsUid statsUid = new TrafficStatsUid(ctx,
                    serviceNames[i],
                    packageNames[i], i);
            statsUid.setRxBytes(300_000 * i);
            statsUid.setTxBytes(300_000 * i);
            stats.addStatsUid(statsUid);
        }
        stats.setRxTxCount(1);
        stats.setUnknownRxBytes(111);
        stats.setUnknownTxBytes(222);
        stats.setIsFirstPass(IS_FIRST_PASS);
        stats.setUpdatedAgo(updatedAgo);
        stats.setDurationMs(2);
        IS_FIRST_PASS = false;
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