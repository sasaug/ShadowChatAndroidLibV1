package com.shadowchat.utils;

import android.util.Log;

/**
 * Created by Sasaug on 12/20/2014.
 */
public class LogUtil {

    public static void debug(String tag, String msg){
        Log.d(tag, msg);
    }

    public static void error(String tag, String msg){
        Log.e(tag, msg);
    }
}
