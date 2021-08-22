package com.hello.test;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PullAliveService extends Service {
    private static final String TAG = "PullAliveService";

    public PullAliveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, String.format("onBind %s", intent.getAction()));
        return null;
    }
}