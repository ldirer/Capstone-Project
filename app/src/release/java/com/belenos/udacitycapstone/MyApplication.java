package com.belenos.udacitycapstone;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import okhttp3.OkHttpClient;

/** Google Analytics part from: https://developers.google.com/analytics/devguides/collection/android/v4/
 *
 */
public class MyApplication extends Application {

    private Tracker mTracker;

    public void onCreate() {
        super.onCreate();
    }


    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;

    }


    public static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }
}