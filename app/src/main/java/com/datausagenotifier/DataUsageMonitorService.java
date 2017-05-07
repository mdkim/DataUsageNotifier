package com.datausagenotifier;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DataUsageMonitorService extends IntentService {

    public volatile static boolean IS_STOPPED = true;
    private volatile static Thread SLEEPING_THREAD;

    private static final String TAG = "DataUsageMonitorService";
    private static final int NOTIFICATION_ID = 1;
    private static final int POLLING_INTERVAL_MS = 8000;

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
                .setContentTitle("content title CONTENT TITLE CONTE")
                .setContentText("content text CONTENT TEXT CONTENT")
                .setSubText("subtext")
                .setContentInfo("content info CONTENT INFO CONTENT")
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.temp1, "Stop service", pIntent_stopSelf)
                .setContentIntent(pIntent_none)
                .setCategory(Notification.CATEGORY_STATUS)

                .setPriority(Notification.PRIORITY_HIGH) // heads-up notification
                .setDefaults(Notification.DEFAULT_VIBRATE);

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
        while(!IS_STOPPED && i++ < 10) {
            Log.v(TAG, "called handleActionStart (" + threadId + ") [i=" + i + "]");

            String msg;
            try {
                msg = TrafficStatsHelper.getTrafficStatsUpdate(this);
            } catch (UnsupportedDeviceException e) {
                alert("Traffic stats not supported on this device. Stopping service.");
                stopSelf();
                return;
            }

            if (msg != null) {
                Notification notification = notificationBuilder
                        .setContentText(msg)
                        .setWhen(System.currentTimeMillis())
                        .build();
                notificationManager.notify(NOTIFICATION_ID, notification);
            }

            staticLockAndSleep();
        }

        Log.v(TAG, "exiting handleActionStart (" + threadId + ")");

    }

    private void postToast(final String msg) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DataUsageMonitorService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void alert(final String msg) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Unsupported device");
        alert.setMessage("Your device does not support traffic stat monitoring.");
        alert.show();
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
