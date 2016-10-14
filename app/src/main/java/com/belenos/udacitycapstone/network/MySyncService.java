package com.belenos.udacitycapstone.network;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class MySyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static final String LOG_TAG = MySyncService.class.getSimpleName();
    private static MySyncAdapter sMySyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "in onCreate");
        synchronized (sSyncAdapterLock) {
            if (sMySyncAdapter == null) {
                sMySyncAdapter = new MySyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sMySyncAdapter.getSyncAdapterBinder();
    }
}
