package com.shadowchat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ShadowChatService extends Service {

    public class LocalBinder extends Binder {
        public ShadowChatService getService() {
            return ShadowChatService.this;
        }
    }

    private final LocalBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Service", "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.w("Service", "onDestroy");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "onStartCommand");
        return android.app.Service.START_STICKY;
    }
}
