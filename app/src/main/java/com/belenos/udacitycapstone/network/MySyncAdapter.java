package com.belenos.udacitycapstone.network;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.belenos.udacitycapstone.R;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MySyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = MySyncAdapter.class.getSimpleName();
    private static final int SYNC_INTERVAL = 360;
    private static final int SYNC_FLEXTIME = 360;
    private String mServerUrl;

    public MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mServerUrl = getContext().getString(R.string.server_url);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(LOG_TAG, "in onPerformSync");

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        // 1. Query to get user with id google_id. Include action=sync in the querystring and last timestamp of data in the body.
        // The backend will:
        //   * return everything it has after last timestamp if there's anything.
        //   * Ask for the data it misses if it's last timestamp is < ours.
        //   * Ask for ALL THE THINGS if it does not have the user.
        // Question: How does the server 'Asks'?
        // -> If it does not have the user, it'll return an empty list and we'll know.
        // -> If it *does* have the user... It'll return the user with a 'last_action_timestamp' field.
        // Which we might need to add to the db. -> Well it'll be hard to update on every insert...
        // We can then compare this timestamp with ours and POST data accordingly.

        // Copy pasted from fetchlangaugetask
        // We do a search query by name to get the language.
        String searchQueryStr = String.format("?q={\"filters\":[{\"name\":\"name\",\"op\":\"==\",\"val\":\"%s\"}]}", mLanguageName);
        HttpUrl url = HttpUrl.parse(mServerUrl + "/api/language" + searchQueryStr);
        Request request = new Request.Builder()
                .url(url)
                .build();


        Response response = client.newCall(request).execute();
//        return response.body().string();

    }



    /**
     * From sunshine app.
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * From sunshine app.
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), bundle);
    }


    /**
     * From sunshine app.
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        MySyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
