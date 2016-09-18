package com.belenos.udacitycapstone.utils;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.belenos.udacitycapstone.MyApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Inspired by http://stackoverflow.com/questions/14883613/using-google-analytics-to-track-fragments/19284014#19284014
 */
public abstract class TrackedFragment extends Fragment {

    private Tracker mTracker;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Google Analytics. We do this here and not in onCreate because we want to be sure we have an activity.
        MyApplication mApplication = (MyApplication) getActivity().getApplication();
        mTracker = mApplication.getDefaultTracker();
    }


    @Override
    public void onResume() {
        super.onResume();
        String LOG_TAG = getClass().getSimpleName();
        Log.d(LOG_TAG, "Analytics: Setting screen name");
        if(mTracker != null){
            mTracker.setScreenName(LOG_TAG);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }
}

