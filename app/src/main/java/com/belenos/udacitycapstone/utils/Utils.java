package com.belenos.udacitycapstone.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.belenos.udacitycapstone.data.DbContract;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();
    private static long mFakeItIndex = 0;
    public static long getNextWordId(Context context, Long mWordToTranslateId, long mLanguageId, long mUserId) {
        // TODO: smart stuff to get the id of the next word we want to show.
        Uri uri = DbContract.AttemptEntry.CONTENT_URI.buildUpon()
                .appendPath("by_user_language")
                .appendQueryParameter(DbContract.WordEntry.COLUMN_LANGUAGE_ID, String.valueOf(mLanguageId))
                .appendQueryParameter(DbContract.AttemptEntry.COLUMN_USER_ID, String.valueOf(mUserId))
                .build();
//
//
//
//
//        Cursor wordsCursor = context.getContentResolver().query(uri, null, null, null, null);
//
//        if (wordsCursor == null || !wordsCursor.moveToFirst()) {
//            Log.e(LOG_TAG, String.format("Failed to fetch any words for language id %d", mLanguageId));
//            return 1;
//        }


        String[] projection = {
                DbContract.WordEntry.TABLE_NAME + "." + DbContract.WordEntry._ID,
                DbContract.AttemptEntry.COMPUTED_ATTEMPT_COUNT,
                DbContract.AttemptEntry.COMPUTED_SUCCESS_RATE
        };
        int COL_WORD_ID = 0;
        int COL_COUNT_ATTEMPTS = 1;
        int COL_SUCCESS_RATE = 2;


        Cursor wordsWithAttemptsCursor = context.getContentResolver().query(uri, projection, null, null, null);

//        Cursor attemptsCursor = context.getContentResolver().query(DbContract.AttemptEntry.CONTENT_URI, projection, null, null, null);

        if(wordsWithAttemptsCursor == null) {
            Log.e(LOG_TAG, String.format("Failed to fetch attempts for languageId, userId: %d, %d", mLanguageId, mUserId));
            return 1;
        }

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


//        long wordId = wordsCursor.getLong(0);
//        while (mWordToTranslateId != null && wordId == mWordToTranslateId && wordsCursor.moveToNext()) {
//            wordId = wordsCursor.getLong(0);
//        }

        int i = randomDrawIndex(distribution);
        long wordId = attemptsWordIdArray.get(i);
        // We want a word id that's different from the one we just showed.
        // Note that mWordToTranslateId will be null if this is the first word of a session.
        while (mWordToTranslateId != null && wordId == mWordToTranslateId) {
            i = randomDrawIndex(distribution);
            wordId = attemptsWordIdArray.get(i);
        }


//        attemptsCursor.close();
//        wordsCursor.close();
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

}
