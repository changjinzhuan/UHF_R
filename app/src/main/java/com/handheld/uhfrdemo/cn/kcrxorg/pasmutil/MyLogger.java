package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil;

import android.util.Log;

public class MyLogger {

    public static boolean debug = true ;

    public static void e(String TAG, String msg) {
        if(debug){
            Log.e(TAG, msg) ;
        }

    }
}
