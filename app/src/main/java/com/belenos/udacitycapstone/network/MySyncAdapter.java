package com.belenos.udacitycapstone.network;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.belenos.udacitycapstone.LoginActivity;
import com.belenos.udacitycapstone.R;
import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.data.DbContract.AttemptEntry;
import com.belenos.udacitycapstone.data.DbContract.UserLanguageEntry;
import com.belenos.udacitycapstone.utils.Utils;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MySyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = MySyncAdapter.class.getSimpleName();
    // Sync interval supposedly in seconds.
    private static final int SYNC_INTERVAL = 7;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private String mServerUrl;

    private Context mContext;

    private static final String CLIENT_TO_SERVER_SYNC = "sync_to_server";
    private static final String SERVER_TO_CLIENT_SYNC = "sync_to_client";
    private static final String CLIENT_TO_SERVER_USER_SYNC = "sync_user_to_server";
    private static final String ALREADY_IN_SYNC = "already_in_sync";


    // BS!! Why the hell would we need to do this...
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String[] ATTEMPTS_COLUMNS = {
            DbContract.AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_WORD_ID,
            DbContract.AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_LANGUAGE_ID,
            DbContract.AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_TIMESTAMP,
            DbContract.AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_SUCCESS
    };

    private static final int COLUMN_ATTEMPT_WORD_ID = 0;
    private static final int COLUMN_ATTEMPT_LANGUAGE_ID = 1;
    private static final int COLUMN_ATTEMPT_TIMESTAMP = 2;
    private static final int COLUMN_ATTEMPT_SUCCESS = 3;

    private static final String[] USER_LANGUAGE_COLUMNS = {
            UserLanguageEntry.COLUMN_LANGUAGE_ID,
            UserLanguageEntry.COLUMN_CREATED_TIMESTAMP
    };

    private static final int COLUMN_USER_LANGUAGE_LANGUAGE_ID = 0;
    private static final int COLUMN_USER_LANGUAGE_CREATED_TIMESTAMP = 1;


    public MySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mServerUrl = context.getString(R.string.server_host);
    }

    /**
     * First we do a probing request to the server with the user google id and the last timestamp we have for data about this user.
     * The server tells us whether it has fresher data or needs sync, along with urls to get the data/post it.
     *
     */
    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(LOG_TAG, "in onPerformSync");

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String userGoogleId = preferences.getString(LoginActivity.KEY_GOOGLE_ID, "");
        String userName = preferences.getString(LoginActivity.KEY_GOOGLE_GIVEN_NAME, "");
        long userId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);

        // We want a 10-digit (second precision) unix timestamp.
        long lastUpdateUnix;

        // Testing: Yesterday... All my trouble...
        // lastUpdateUnix = System.currentTimeMillis() / 1000;
        // lastUpdateUnix -= 60 * 60 * 24;

        Cursor lastUpdateCursor = mContext.getContentResolver().query(DbContract.LAST_UPDATE_URI, null, null, null, null);
        if (lastUpdateCursor != null && lastUpdateCursor.moveToFirst()) {
            lastUpdateUnix = lastUpdateCursor.getLong(0);
            lastUpdateCursor.close();
        }
        else {
            Log.d(LOG_TAG, "User language and attempt table appear to be empty");
            if (lastUpdateCursor == null) {
                Log.d(LOG_TAG, "Sync failed due to some cursor being null.");
                return;
            }
            else {
                // Setting the last update to 0 is a big deal: if we do it by mistake, the server will send us all its data.
                // I tried to make the sync more robust by using database constraints so wrong updates will fail and won't corrupt the db.
                lastUpdateUnix = 0;
            }

        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(mServerUrl)
                .addPathSegment("polling")
                .addQueryParameter("user_google_id", userGoogleId)
                .addQueryParameter("last_update_unix", String.valueOf(lastUpdateUnix))
                .build();

        Log.d(LOG_TAG, String.format("url is: %s", url.toString()));

        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String bodyString = response.body().string();
            Log.d(LOG_TAG, bodyString);

            JSONObject responseJson = new JSONObject(bodyString);
            String action = responseJson.getString("action");

            switch (action) {
                case SERVER_TO_CLIENT_SYNC:
                    syncDataFromUrls(responseJson.getJSONObject("urls"), client, userId);
                    break;
                case CLIENT_TO_SERVER_USER_SYNC:
                    postUserToServer(client, userName, userGoogleId, userId);
                    break;
                case CLIENT_TO_SERVER_SYNC:
                    long serverLastUpdate = responseJson.getLong("last_update_unix");
                    String userUrlString = responseJson.getString("url");
                    postUserDataToServer(client, userUrlString, userId,
                            serverLastUpdate);
                    break;

                case ALREADY_IN_SYNC:
                    Log.d(LOG_TAG, "Client and server in sync.");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "FAIL SYNC IOEXCEPTION");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Got unvalid json from server. That's pretty bad.");
        }
    }

    private void postUserDataToServer(OkHttpClient client, String userUrlString, long userClientId, long serverLastUpdate) {
        JSONObject jsonBody = new JSONObject();

        // We get attempts for the right user that are more recent than the server.
        Cursor attemptsCursor = mContext.getContentResolver().query(
                AttemptEntry.CONTENT_URI, ATTEMPTS_COLUMNS,
                String.format("%s > ? and %s=?", AttemptEntry.COLUMN_TIMESTAMP, AttemptEntry.COLUMN_USER_ID),
                new String[]{String.valueOf(serverLastUpdate), String.valueOf(userClientId)},
                null);

        Cursor userLanguagesCursor = mContext.getContentResolver().query(
                UserLanguageEntry.CONTENT_URI, USER_LANGUAGE_COLUMNS,
                String.format("%s > ? and %s=?", UserLanguageEntry.COLUMN_CREATED_TIMESTAMP, UserLanguageEntry.COLUMN_USER_ID),
                new String[]{String.valueOf(serverLastUpdate), String.valueOf(userClientId)},
                null);

        JSONArray attemptsJson = getAttemptsJsonArray(attemptsCursor);
        JSONArray userLanguagesJson = getUserLanguagesJsonArray(userLanguagesCursor);

        JSONObject attemptPatch = new JSONObject();
        JSONObject languagePatch = new JSONObject();

        try {
            attemptPatch.put("add", attemptsJson);
            languagePatch.put("add", userLanguagesJson);
            jsonBody.put("attempts", attemptPatch);
            jsonBody.put("languages", languagePatch);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Could not put the attempt json object list in a json object somehow.");
        }

        RequestBody body = RequestBody.create(JSON, jsonBody.toString());
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(mServerUrl)
                .addPathSegment(userUrlString)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if(!response.isSuccessful()) {
                Log.e(LOG_TAG, "Failed in our PATCH request to send user data to server. Response body:" + response.body().string());
            }
            else {
                Log.i(LOG_TAG, "Great success! Synced freshest client user data with the server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private JSONArray getAttemptsJsonArray(Cursor attemptsCursor) {
        List<JSONObject> attemptsJsonList = new ArrayList<>();
        // Convert the objects into json.
        if (attemptsCursor != null) {
            while (attemptsCursor.moveToNext()) {
                long languageId = attemptsCursor.getLong(COLUMN_ATTEMPT_LANGUAGE_ID);
                int success = attemptsCursor.getInt(COLUMN_ATTEMPT_SUCCESS);
                long wordId = attemptsCursor.getLong(COLUMN_ATTEMPT_WORD_ID);
                long timestamp = attemptsCursor.getLong(COLUMN_ATTEMPT_TIMESTAMP);

                JSONObject attemptJson = new JSONObject();
                try {
                    attemptJson.put(AttemptEntry.COLUMN_LANGUAGE_ID, languageId);
                    attemptJson.put(AttemptEntry.COLUMN_SUCCESS, success);
                    attemptJson.put(AttemptEntry.COLUMN_WORD_ID, wordId);
                    attemptJson.put(AttemptEntry.COLUMN_TIMESTAMP, Utils.getIsoFromTimestamp(timestamp));

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Failed to jsonify attempt from cursor.");
                }
                attemptsJsonList.add(attemptJson);
            }
            attemptsCursor.close();
        }

        return new JSONArray(attemptsJsonList);
    }


    @NonNull
    private JSONArray getUserLanguagesJsonArray(Cursor userLanguagesCursor) {
        List<JSONObject> userLanguagesJsonList = new ArrayList<>();
        // Convert the objects into json.
        if (userLanguagesCursor != null) {
            while (userLanguagesCursor.moveToNext()) {
                long languageId = userLanguagesCursor.getLong(COLUMN_USER_LANGUAGE_LANGUAGE_ID);
                long timestamp = userLanguagesCursor.getLong(COLUMN_USER_LANGUAGE_CREATED_TIMESTAMP);

                JSONObject userLanguageJson = new JSONObject();
                try {
                    userLanguageJson.put(UserLanguageEntry.COLUMN_LANGUAGE_ID, languageId);
                    userLanguageJson.put(UserLanguageEntry.COLUMN_CREATED_TIMESTAMP, Utils.getIsoFromTimestamp(timestamp));

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Failed to jsonify attempt from cursor.");
                }
                userLanguagesJsonList.add(userLanguageJson);
            }
            userLanguagesCursor.close();
        }

        return new JSONArray(userLanguagesJsonList);
    }

    private void postUserToServer(OkHttpClient client, String userName, String userGoogleId, long userId) {

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", userName);
            jsonBody.put("google_id", userGoogleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, jsonBody.toString());
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(mServerUrl)
                .addPathSegment("api")
                .addPathSegment("user")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if(!response.isSuccessful()) {
                Log.e(LOG_TAG, "Failed to post user to server. Response body:" + response.body().string());
            }
            else {
                Log.i(LOG_TAG, "Successfully posted user to server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void syncDataFromUrls(JSONObject urls, OkHttpClient client, long userId) throws JSONException {
        Log.d(LOG_TAG, String.format("urls: %s", urls.toString()));
        Iterator<String> keys = urls.keys();
        while(keys.hasNext()) {
            String objectType = keys.next();
            String urlStr = urls.getString(objectType);
            Log.d(LOG_TAG, String.format("OBJECT TYPE / URL STRING: %s / %s", objectType, urlStr));
            syncFromUrl(urlStr, objectType, client, userId, 1);
        }
    }

    /**
     * @param urlStr: The url we want to GET
     * @param objectType: the type of object we'll get: allows choosing the right parser.
     */
    private void syncFromUrl(String urlStr, String objectType, OkHttpClient client, long userId, int page) {
        // The usual data-fetching boilerplate.

        Uri uri = Uri.parse("http://" + mServerUrl + urlStr)
                .buildUpon()
                .appendQueryParameter("page", String.valueOf(page))
                .build();
        URL url;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Could not parse url from server correctly.");
            e.printStackTrace();
            return;
        }
        Log.d(LOG_TAG, String.format("In syncFromUrl, full url is: %s", url.toString()));


        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;
        JSONObject responseJson = null;
        int totalPages = 1;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(LOG_TAG, String.format("Bad status when querying the server: %d", response.code()));
                return;
            }
            String bodyString = response.body().string();
            Log.d(LOG_TAG, bodyString);
            responseJson = new JSONObject(bodyString);
            // Handle pagination
            totalPages = responseJson.getInt("total_pages");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "FAIL SYNC IOEXCEPTION. Whatever that means.");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "BAAAAD JSON from the server. Bad server bad.");
        }

        parseObjects(objectType, userId, responseJson);
        if (page < totalPages) {
            syncFromUrl(urlStr, objectType, client, userId, page + 1);
        }
    }

    private void parseObjects(String objectType, long userId, JSONObject responseJson) {
        // Pick the right parsing based on the object type.
        switch (objectType) {
            case "attempt":
                try {
                    parseAttemptsForUser(responseJson, userId);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Failed to parse attempts!");
                }
                break;
            case "user_language":
                try {
                    parseUserLanguagesForUser(responseJson, userId);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Failed to parse user language data!");
                }
                break;
            default:
                Log.e(LOG_TAG, "WOOT, got unknown object type");
        }
    }


    /**
     * Takes a JSON response containing userLanguages for the relevant `userId`, parse and add
     * them to the database.
     */
    private void parseUserLanguagesForUser(JSONObject userLanguagesJson, long userId) throws JSONException {
        Log.d(LOG_TAG, "in parseUserLanguagesForUser");

        JSONArray userLanguages = userLanguagesJson.getJSONArray("objects");

        Vector<ContentValues> cVVector = new Vector<>(userLanguagesJson.length());

        for (int j = 0; j < userLanguages.length(); j++) {
            // Note that we made the json keys match our sqlite fields names.
            JSONObject userLanguageJson = userLanguages.getJSONObject(j);

            ContentValues userLanguageValues = new ContentValues();
            userLanguageValues.put(UserLanguageEntry.COLUMN_LANGUAGE_ID, userLanguageJson.getString(UserLanguageEntry.COLUMN_LANGUAGE_ID));
            // We can't take the user id from the json response since it may not match the one we have locally!
            userLanguageValues.put(UserLanguageEntry.COLUMN_USER_ID, userId);
            long unixTimestamp = Utils.getUnixTimestamp(userLanguageJson.getString(UserLanguageEntry.COLUMN_CREATED_TIMESTAMP));
            userLanguageValues.put(UserLanguageEntry.COLUMN_CREATED_TIMESTAMP, unixTimestamp);

            cVVector.add(userLanguageValues);

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                mContext.getContentResolver().bulkInsert(DbContract.UserLanguageEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");
            // Signal that we're done loading data. TODO: do i need to do smt here? Like notify content resolver stuff? Dont think so, should be done in the insert.
        }
    }

    /**
     * Takes a JSON response containing attempts for the relevant `userId`, parse and add
     * them to the database.
     */
    private void parseAttemptsForUser(JSONObject attemptsJson, long userId) throws JSONException {
        Log.d(LOG_TAG, "in parseAttemptsForUser");

        JSONArray attempts = attemptsJson.getJSONArray("objects");

        Vector<ContentValues> cVVector = new Vector<>(attemptsJson.length());

        for (int j = 0; j < attempts.length(); j++) {
            // Note that we made the json keys match our sqlite fields names.
            JSONObject attemptJson = attempts.getJSONObject(j);

            ContentValues attemptValues = new ContentValues();
            attemptValues.put(AttemptEntry.COLUMN_LANGUAGE_ID, attemptJson.getString(AttemptEntry.COLUMN_LANGUAGE_ID));
            attemptValues.put(AttemptEntry.COLUMN_WORD_ID, attemptJson.getString(AttemptEntry.COLUMN_WORD_ID));
            attemptValues.put(AttemptEntry.COLUMN_SUCCESS, attemptJson.getString(AttemptEntry.COLUMN_SUCCESS));

            long unixTimestamp = Utils.getUnixTimestamp(attemptJson.getString(AttemptEntry.COLUMN_TIMESTAMP));

            attemptValues.put(AttemptEntry.COLUMN_TIMESTAMP, unixTimestamp);

            // We can't take the user id from the json response since it may not match the one we have locally!
            attemptValues.put(AttemptEntry.COLUMN_USER_ID, userId);

            cVVector.add(attemptValues);

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                mContext.getContentResolver().bulkInsert(AttemptEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");
        }
    }


    /**
     * From sunshine app.
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Log.d(LOG_TAG, "in configurePeriodicSync");
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
        Log.d(LOG_TAG, "in syncImmediately");
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

    /**
     * From sunshine app.
     */
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
