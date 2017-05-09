package com.datausagenotifier;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.Toast;

import com.datausagenotifier.model.TrafficStatsUpdate;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DataUsageMonitorService extends IntentService {

    public volatile static boolean IS_STOPPED = true;
    private volatile static Thread SLEEPING_THREAD;

    public static final int POLLING_INTERVAL_MS = 8000;

    private static final String TAG = "DataUsageMonitorService";
    private static final int NOTIFICATION_ID = 1;

    private Handler uiThreadHandler;
    private Builder notificationBuilder;
    private NotificationManager notificationManager;

    public DataUsageMonitorService() {
        super("DataUsageMonitorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uiThreadHandler = new Handler();

        Intent intent_stopSelf = new Intent(this, StopService.class);
        intent_stopSelf.setAction(StopService.ACTION_STOP);
        PendingIntent pIntent_stopSelf = PendingIntent.getService(this, 0, intent_stopSelf, 0);
        PendingIntent pIntent_none = PendingIntent.getActivity(this, 0, new Intent(), 0);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.temp1)
                //.setContentTitle("content title")
                //.setContentText("content text")
                //.setSubText("subtext")
                //.setContentInfo("content info")
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.temp1, "Stop service", pIntent_stopSelf)
                .setContentIntent(pIntent_none)
                .setCategory(Notification.CATEGORY_STATUS)

                //.setPriority(Notification.PRIORITY_HIGH) // heads-up notification
                //.setDefaults(Notification.DEFAULT_SOUND)
        ;

        Notification notification = notificationBuilder.build();
        startForeground(NOTIFICATION_ID, notification);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    // thread blocked while in staticLockAndSleep()
    private static synchronized void waitForStaticLock() {
    }

    // static thread lock while sleeping to block setting IS_STOPPED=false
    private static synchronized void staticLockAndSleep() {
        SLEEPING_THREAD = Thread.currentThread();
        long threadId = SLEEPING_THREAD.getId();
        try {
            Thread.sleep(POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
            Log.v(TAG, "Interrupted sleeping thread (" + threadId + ")");
        }
        SLEEPING_THREAD = null;
    }

    /**
     * If the service is already performing a task this action will be queued.
     * @see IntentService
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        handleActionStart();
    }

    private void handleActionStart() {
        waitForStaticLock();
        IS_STOPPED = false;
        postToast("Service started");

        long threadId = Thread.currentThread().getId();
        int i=0;
        while(!IS_STOPPED && i++ < 100) {
            Log.v(TAG, "called handleActionStart (" + threadId + ") [i=" + i + "]");

            TrafficStatsUpdate stats;
            try {
                stats = TrafficStatsHelper.getTestStats(this); //getTrafficStatsUpdate(this); //
            } catch (UnsupportedDeviceException e) {
                postToast("Unsupported device:\nYour device does not support traffic stats monitoring.");
                stopSelf();
                return;
            }

            if (stats == null) {
                PendingIntent pIntent_main = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = notificationBuilder
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle("(No activity)")
                                .bigText(""))
                        .setContentIntent(pIntent_main)
                        .setContentTitle("(No activity)") // for sdk21
                        .setContentText("")
                        .setWhen(System.currentTimeMillis())
                        .build();
                notificationManager.notify(NOTIFICATION_ID, notification);
            } if (stats != null) {
                SpannableStringBuilder ssb = stats.formatSpannable();
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("com.datausagenotifier.extras.ssb", ssb);
                intent.setAction("update");
                PendingIntent pIntent_main = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                String contentTitle = stats.getContentTitle().toString();
                Notification notification = notificationBuilder
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle(contentTitle)
                                //.setSummaryText("Summary Text")
                                .bigText(ssb))
                        .setContentIntent(pIntent_main)
                        .setContentTitle(contentTitle) // for sdk21
                        .setContentText("")
                        .setWhen(System.currentTimeMillis())
                        .build();
                notificationManager.notify(NOTIFICATION_ID, notification);

                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //startActivity(intent); // refresh activity

                postToast(contentTitle);
                Log.v(TAG, ssb.toString());
            }

            staticLockAndSleep();
        }

        Log.v(TAG, "exiting handleActionStart (" + threadId + ")");
    }

    private void postToast(final CharSequence msg) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: position toast at top of screen; make font small
                Toast.makeText(DataUsageMonitorService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IS_STOPPED = true;
        if (SLEEPING_THREAD != null) SLEEPING_THREAD.interrupt();

        stopForeground(true);

        long threadId = Thread.currentThread().getId();
        Log.v(TAG, "called onDestroy (" + threadId + ")");
    }
}
