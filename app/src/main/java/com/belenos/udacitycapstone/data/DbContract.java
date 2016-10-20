package com.belenos.udacitycapstone.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Heavily inspired from sunshine app.
 */
public class DbContract {


    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.belenos.udacitycapstone";
    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths we'll append to the base content uri to form uris.
    public static final String PATH_USER = "user";
    public static final String PATH_LANGUAGE = "language";
    public static final String PATH_LANGUAGES = "languages";
    public static final String PATH_USER_LANGUAGE = "user_language";
    public static final String PATH_WORD = "word";
    public static final String PATH_ATTEMPT = "attempt";

    public static final String PATH_LAST_UPDATE = "last_update";

    public static final Uri LAST_UPDATE_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(PATH_LAST_UPDATE).build();


    public static final class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "user";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_GOOGLE_ID = "google_id";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_USER).build();

        /**
        Get the id in a uri: "user/3"
         */
        public static String getIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static Uri buildLanguagesForUserUri(long userId) {
            return buildUserUri(userId)
                    .buildUpon()
                    .appendPath("languages")
                    .build();
        }

        public static Uri buildUserUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildUserByGoogleIdUri(String googleId) {
            return CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(COLUMN_GOOGLE_ID, googleId)
                    .build();
        }
    }


    public static final class LanguageEntry implements BaseColumns {
        public static final String TABLE_NAME = "language";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ICON_NAME = "icon_name";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LANGUAGE).build();
        public static final String NOT_USER_PATH_SEGMENT = "notuser";
        public static final String ATTEMPT_COUNT_PATH_SEGMENT = "attempt_count";
        private static final String LOG_TAG = LanguageEntry.class.getSimpleName();

        public static Uri buildLanguagesUri() {
            // Well I realize after the fact that this should just be CONTENT_URI. Could use a refactor.
            return BASE_CONTENT_URI.buildUpon().appendPath(PATH_LANGUAGES).build();
        }


        public static Uri buildLanguagesNotLearnedByUserUri(long mUserId) {
            Log.d(LOG_TAG, String.format("in buildLanguagesNotLearnedByUserUri with userId: %d", mUserId));
            return buildLanguagesUri().buildUpon()
                    .appendPath(NOT_USER_PATH_SEGMENT)
                    .appendQueryParameter(UserLanguageEntry.COLUMN_USER_ID, String.valueOf(mUserId))
                    .build();
        }

        public static Uri buildLanguagesAttemptCount(Long userId, long timestamp) {
            return buildLanguagesUri().buildUpon()
                    .appendPath(ATTEMPT_COUNT_PATH_SEGMENT)
                    .appendQueryParameter(AttemptEntry.COLUMN_USER_ID, String.valueOf(userId))
                    .appendQueryParameter(AttemptEntry.TIMESTAMP_MINIMUM, String.valueOf(timestamp))
                    .build();
        }
    }


    public static final class UserLanguageEntry implements BaseColumns {
        public static final String TABLE_NAME = "user_language";

        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_LANGUAGE_ID = "language_id";
        public static final String COLUMN_CREATED_TIMESTAMP = "created_timestamp";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_USER_LANGUAGE).build();

        public static Uri buildUserLanguageUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }


    public static final class WordEntry implements BaseColumns {
        public static final String TABLE_NAME = "word";

        public static final String COLUMN_LANGUAGE_ID = "language_id";
        // The word in english
        public static final String COLUMN_WORD = "word";
        // The translated word
        public static final String COLUMN_TRANSLATION = "translation";


        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WORD).build();


        public static Uri buildWordUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static String getIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }

    public static final class AttemptEntry implements BaseColumns {
        public static final String TABLE_NAME = "attempt";

        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_WORD_ID = "word_id";
        // We repeat language id here: it's not necessary, we do it to get a faster query (no join required) on the Attempt table.
        public static final String COLUMN_LANGUAGE_ID = "language_id";
        public static final String COLUMN_SUCCESS = "success";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ATTEMPT).build();


        // Computed fields by group by on word_id
        public static final String COMPUTED_SUCCESS_RATE = "success_rate";
        public static final String COMPUTED_ATTEMPT_COUNT = "attempt_count";
        // A parameter for uris. When we want to query with TIMESTAMP > ?.
        public static final String TIMESTAMP_MINIMUM = "timestamp_minimum";

        public static Uri buildAttemptUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }
}
