package com.belenos.udacitycapstone.network;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.belenos.udacitycapstone.R;
import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.utils.Utils;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Vector;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A task to fetch all data we have about a language.
 * Basically the language words with translation.
 *
 */
public class FetchLanguageTask extends AsyncTask<Void, Integer, Void> {

    private static final String LOG_TAG = FetchLanguageTask.class.getSimpleName();

    private int mErrorStatus = 0;
    private static final int ERROR_SERVER_DOWN = 1;
    private static final int ERROR_INVALID_RESPONSE = 2;

    private Context mContext;
    private ProgressBar mProgressBar;
    private OnPostExecuteCallback mOnPostExecuteCallback;
    private String mLanguageName;

    // I tried port forwarding to use localhost using chrome://inspect but it did not work.
    private String mServerUrl;

    public FetchLanguageTask(Context context, String languageName,
                             @Nullable ProgressBar progressBar,
                             OnPostExecuteCallback onPostExecuteCallback) {
        super();
        mContext = context;
        mLanguageName = languageName;
        mProgressBar = progressBar;
        mOnPostExecuteCallback = onPostExecuteCallback;
        mServerUrl = mContext.getString(R.string.server_host);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mProgressBar != null) {
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            String languagesJsonStr = fetchLanguage();
            Log.d(LOG_TAG, "in doInBackground, fetched: " + languagesJsonStr);
            try {
                parseLanguages(languagesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "in doInBackground, failed parsing json language.");
                e.printStackTrace();
                mErrorStatus = ERROR_INVALID_RESPONSE;
            }


            publishProgress(50);
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Fail in doInBackground, IOException");
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, Utils.getStackTrace(e));
            mErrorStatus = ERROR_SERVER_DOWN;
        }

        //TODO: parse fetched stuff. Add words to database. Report progress.
        return null;
    }

    private void parseLanguage(JSONObject languageJson) throws JSONException {
        Log.d(LOG_TAG, "in parseLanguage - parse ONE language");

        String LANG_NAME = "name";
        String LANG_ICON_NAME = "icon_name";
        String LANG_WORDS = "words";
        String LANG_ID = "_id";

        String languageShortName = languageJson.getString(LANG_ICON_NAME);
        JSONArray dictionary = languageJson.getJSONArray(LANG_WORDS);
        Integer languageId = languageJson.getInt(LANG_ID);

        Vector<ContentValues> cVVector = new Vector<>(languageJson.length());

        for (int j = 0; j < dictionary.length(); j++) {
            JSONObject wordJson = dictionary.getJSONObject(j);


            String WORD_ID = "_id";
            String WORD_WORD = "word";
            String WORD_TRANSLATION = "translation";


            ContentValues wordValues = new ContentValues();
            // It's important (paramount ;)) that the ids on the device and on the server are consistent for words.
            // Same goes for languages: the ids are both used in the attempt table.
            wordValues.put(DbContract.WordEntry._ID, wordJson.getInt(WORD_ID));
            wordValues.put(DbContract.WordEntry.COLUMN_LANGUAGE_ID, languageId);
            wordValues.put(DbContract.WordEntry.COLUMN_WORD, wordJson.getString(WORD_WORD));
            wordValues.put(DbContract.WordEntry.COLUMN_TRANSLATION, wordJson.getString(WORD_TRANSLATION));

            cVVector.add(wordValues);

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                // Note we have a ON CONFLICT IGNORE clause on the _id so we dont have duplicates.
                mContext.getContentResolver().bulkInsert(DbContract.WordEntry.CONTENT_URI, cvArray);
            }
            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");
            // Signal that we're done loading language data.
        }

    }

    public static interface OnPostExecuteCallback {
        void onPostExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        switch (mErrorStatus) {
            case 0:
                mOnPostExecuteCallback.onPostExecute();
                break;
            case ERROR_SERVER_DOWN:
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
                Toast.makeText(mContext, R.string.error_server_down, Toast.LENGTH_LONG).show();
                break;
            case ERROR_INVALID_RESPONSE:
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
                Toast.makeText(mContext, R.string.error_invalid_response, Toast.LENGTH_LONG).show();
                break;
        }
    }


    /**
     * Note that this method can (and will) be called to parse an array of languages with *only one language*.
     */
    private void parseLanguages(String languagesJsonStr) throws JSONException {
        JSONObject languagesJson = new JSONObject(languagesJsonStr);

        JSONArray languagesArray = languagesJson.getJSONArray("objects");

        for (int i = 0; i < languagesArray.length(); i++) {
            JSONObject languageJson = languagesArray.getJSONObject(i);
            parseLanguage(languageJson);
        }

    }

    private String fetchLanguage() throws IOException {

        OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor()).build();

//        HttpUrl url = HttpUrl.parse(mServerUrl + "/api/language" + searchQueryStr);

        // We do a search query by name to get the language.
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(mServerUrl)
                .addPathSegment("api")
                .addPathSegment("language")
                .addQueryParameter("q", String.format("{\"filters\":[{\"name\":\"name\",\"op\":\"==\",\"val\":\"%s\"}]}", mLanguageName))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
