package com.shadowchat;


import android.content.Context;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;

public class ShadowChatBase {
    public static int ERROR_NONE = 0;
    public static int ERROR_FAIL = 1;

    public static int ERROR_EXCEPTION = 100;
    public static int ERROR_SENDFAILURE = 101;
    public static int ERROR_TIMEOUT = 200;

    ShadowChatConfig cfg;
    Context context;

    public ShadowChatBase(Context context, ShadowChatConfig cfg){
        this.context = context;
        this.cfg = cfg;
    }

    protected String getDevice(){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
