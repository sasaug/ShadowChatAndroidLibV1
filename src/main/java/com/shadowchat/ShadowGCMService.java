package com.shadowchat;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;


public class ShadowGCMService extends IntentService {

    public ShadowGCMService() {
        super("PushIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);


        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                processPush(extras);
            }
        }
        ShadowGCMReceiver.completeWakefulIntent(intent);
    }

    protected void processPush(Bundle bundle){

    }
}