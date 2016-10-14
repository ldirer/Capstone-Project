package com.belenos.udacitycapstone.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.network.MySyncAdapter;

import org.json.JSONException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    // iso 8601 format.
    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    static {
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static long getNextWordId(Context context, Long mWordToTranslateId, long mLanguageId, long mUserId) {
        Uri uri = DbContract.AttemptEntry.CONTENT_URI.buildUpon()
                .appendPath("by_user_language")
                .appendQueryParameter(DbContract.WordEntry.COLUMN_LANGUAGE_ID, String.valueOf(mLanguageId))
                .appendQueryParameter(DbContract.AttemptEntry.COLUMN_USER_ID, String.valueOf(mUserId))
                .build();

        String[] projection = {
                DbContract.WordEntry.TABLE_NAME + "." + DbContract.WordEntry._ID,
                DbContract.AttemptEntry.COMPUTED_ATTEMPT_COUNT,
                DbContract.AttemptEntry.COMPUTED_SUCCESS_RATE
        };
        int COL_WORD_ID = 0;
        int COL_COUNT_ATTEMPTS = 1;
        int COL_SUCCESS_RATE = 2;


        // A cursor with the words (their id) and the associated attempt count.
        Cursor wordsWithAttemptsCursor = context.getContentResolver().query(uri, projection, null, null, null);

        if(wordsWithAttemptsCursor == null) {
            Log.e(LOG_TAG, String.format("Failed to fetch attempts for languageId, userId: %d, %d", mLanguageId, mUserId));
            return 1;
        }

        // Note the size of this array is the number of words for the language. (Including non-attempted).
        List<Integer> attemptsWordIdArray = new ArrayList<>();
        List<Integer> attemptsCountArray = new ArrayList<>();
        List<Float> attemptsSuccessRateArray = new ArrayList<>();

        // We'll need these to compute probabilities later on.
        Integer nWordsSeenByUser = 0;
        Float totalSuccessRatesInverse = 0f;
        while(wordsWithAttemptsCursor.moveToNext()) {
            Integer count = wordsWithAttemptsCursor.getInt(COL_COUNT_ATTEMPTS);
            Float successRate = wordsWithAttemptsCursor.getFloat(COL_SUCCESS_RATE);
            attemptsWordIdArray.add(wordsWithAttemptsCursor.getInt(COL_WORD_ID));
            attemptsCountArray.add(count);
            attemptsSuccessRateArray.add(successRate);

            // count could be non-zero with a 0 success rate. We treat these as an unseen word.
            if (count != 0 && successRate != 0){
                nWordsSeenByUser += 1;
                totalSuccessRatesInverse += 1 / successRate;
            }
        }

        ArrayList<Float> distribution = (ArrayList<Float>) getProbabilityDistribution(attemptsWordIdArray, attemptsCountArray, attemptsSuccessRateArray, nWordsSeenByUser, totalSuccessRatesInverse);

        int i = randomDrawIndex(distribution);
        long wordId = attemptsWordIdArray.get(i);
        // We want a word id that's different from the one we just showed.
        // Note that mWordToTranslateId will be null if this is the first word of a session.
        while (mWordToTranslateId != null && wordId == mWordToTranslateId) {
            i = randomDrawIndex(distribution);
            wordId = attemptsWordIdArray.get(i);
        }

        wordsWithAttemptsCursor.close();
        return wordId;
    }

    private static int randomDrawIndex(ArrayList<Float> distribution) {
        // We use the 'repartition function' associated with the distribution.
        double p = Math.random();
        double pCumulated = 0.0;
        for (int i = 0; i < distribution.size(); i++)
            {
                Float probItem = distribution.get(i);
                pCumulated += probItem;
            if (p <= pCumulated) {
                return i;
            }
        }
        Log.e(LOG_TAG, "randomDraw failed with distribution: " + distribution.toString());
        return -1;
    }

    public static List<Float> getProbabilityDistribution(List<Integer> attemptsWordIdArray, List<Integer> attemptsCountArray,
                                                          List<Float> attemptsSuccessRateArray, Integer nWordsSeenByUser,
                                                          Float totalSuccessRatesInverse) {
        int nWords = attemptsWordIdArray.size();
        List<Float> wordIsNextProbability = new ArrayList<>(nWords);

        // Unseen words (or words with no success ever) have a 1/nWord probability of appearing.
        // Seen-before words share the rest, with priority given to words with low success rate.
        // This is probably a super-hardcore mode so we'd need to find smt else at some point ;).
        float pTotalSeenBefore = nWordsSeenByUser / (float) nWords;

        for (int i = 0; i < attemptsCountArray.size(); i++) {

            Integer count = attemptsCountArray.get(i);
            Float successRate = attemptsSuccessRateArray.get(i);

            Float probability;
            if(count == 0 || successRate == 0) {
                probability = 1f / nWords;
            }
            else{
                // totalSuccessRates *might* be null but we wont go through this else clause in that case.
                // We want all these weights to sum to 1.
                float wordWeight = (1 / successRate) / totalSuccessRatesInverse;
                probability = pTotalSeenBefore * wordWeight;
            }

            wordIsNextProbability.add(i, probability);
        }
        return wordIsNextProbability;
    }


    public static String sanitizeString(String userAnswer) {
        // Single quote vs apostrophe
        return userAnswer.replace((char) 39, (char) 8217).toLowerCase();
    }




    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }


    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
     * @return true if the network is available
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static long getUnixTimestamp(String dateStr) {
        long unixTimestamp;
        try {
            unixTimestamp = isoFormat.parse(dateStr).getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Failed to parse the freaking datetime string: " + dateStr);
            return 0;
        }
        return unixTimestamp;
    }

    /**
     * Return a date string (ISO8601) from a unix timestamp (second-precision).
     * That's cause I suck/was a bit lazy in manipulating dates backend-side ;).
     */
    public static String getIsoFromTimestamp(long timestamp) {
        // Us imperialism. We specify it to not get weird cyrillic numbers (I didnt rly get it).
        return isoFormat.format(new Date(timestamp * 1000));
    }
}
