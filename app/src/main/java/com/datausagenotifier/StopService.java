package com.datausagenotifier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class StopService extends Service {

    public static final String ACTION_STOP = "ACTION_STOP";

    public StopService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null && action.equals(ACTION_STOP)) {
            Intent intentStop = new Intent(this, DataUsageMonitorService.class);
            stopService(intentStop);
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
