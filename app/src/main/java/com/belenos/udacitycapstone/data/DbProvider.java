package com.belenos.udacitycapstone.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.belenos.udacitycapstone.data.DbContract.AttemptEntry;
import com.belenos.udacitycapstone.data.DbContract.LanguageEntry;
import com.belenos.udacitycapstone.data.DbContract.UserEntry;
import com.belenos.udacitycapstone.data.DbContract.UserLanguageEntry;
import com.belenos.udacitycapstone.data.DbContract.WordEntry;

public class DbProvider extends ContentProvider {
    private static final String LOG_TAG = DbProvider.class.getSimpleName();
    private DbHelper mDbHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();


    private static final int LANGUAGES_FOR_USER = 100;
    private static final int LANGUAGES = 101;
    private static final int USER = 102;
    private static final int USER_LANGUAGE = 103;
    private static final int USER_ID = 105;
    private static final int WORD = 106;
    private static final int WORD_ID = 107;
    private static final int WORD_BY_LANGUAGE = 108;
    private static final int ATTEMPT = 109;
    private static final int ATTEMPTS_BY_USER_BY_LANGUAGE = 110;
    private static final int LANGUAGES_NOT_LEARNED_FOR_USER = 111;
    private static final int LAST_UPDATE = 112;


    private static final SQLiteQueryBuilder sLanguagesForUserQueryBuilder;
    private static final SQLiteQueryBuilder sLanguagesQueryBuilder;
    private static final SQLiteQueryBuilder sUsersQueryBuilder;
    private static final SQLiteQueryBuilder sWordsQueryBuilder;
    private static final SQLiteQueryBuilder sAttemptsQueryBuilder;
    private static final SQLiteQueryBuilder sWordsWithAttemptsQueryBuilder;
    private static final SQLiteQueryBuilder sLanguagesNotLearnedForUserQueryBuilder;
    private static final SQLiteQueryBuilder sUserLanguageQueryBuilder;

    private static final String strLanguageNotLearnedForUserTables = LanguageEntry.TABLE_NAME + " LEFT JOIN " +
            "(SELECT * FROM " + UserLanguageEntry.TABLE_NAME +
            " WHERE " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_USER_ID + " = 1" +
            ") " + UserLanguageEntry.TABLE_NAME +
            " ON " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_LANGUAGE_ID +
            " = " + LanguageEntry.TABLE_NAME + "." + LanguageEntry._ID;


    static {
        sLanguagesForUserQueryBuilder = new SQLiteQueryBuilder();
        sLanguagesQueryBuilder = new SQLiteQueryBuilder();
        sUsersQueryBuilder = new SQLiteQueryBuilder();
        sAttemptsQueryBuilder = new SQLiteQueryBuilder();
        sWordsWithAttemptsQueryBuilder = new SQLiteQueryBuilder();
        sLanguagesNotLearnedForUserQueryBuilder = new SQLiteQueryBuilder();
        sUserLanguageQueryBuilder = new SQLiteQueryBuilder();

        // A join of language and user_language tables.
        sLanguagesForUserQueryBuilder.setTables(
                UserLanguageEntry.TABLE_NAME + " LEFT JOIN " + LanguageEntry.TABLE_NAME +
                        " ON " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_LANGUAGE_ID +
                        " = " + LanguageEntry.TABLE_NAME + "." + LanguageEntry._ID);

        // here we want the complementary set of the user's language.
        sLanguagesNotLearnedForUserQueryBuilder.setTables(
                LanguageEntry.TABLE_NAME + " LEFT JOIN " +
                        "(SELECT * FROM " + UserLanguageEntry.TABLE_NAME +
                        " WHERE " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_USER_ID + " = ?" +
                        ") " + UserLanguageEntry.TABLE_NAME +
                        " ON " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_LANGUAGE_ID +
                        " = " + LanguageEntry.TABLE_NAME + "." + LanguageEntry._ID);


        sAttemptsQueryBuilder.setTables(DbContract.AttemptEntry.TABLE_NAME);
        sLanguagesQueryBuilder.setTables(LanguageEntry.TABLE_NAME);
        sUsersQueryBuilder.setTables(UserEntry.TABLE_NAME);

        sWordsQueryBuilder = new SQLiteQueryBuilder();
        sWordsQueryBuilder.setTables(WordEntry.TABLE_NAME);


        sUserLanguageQueryBuilder.setTables(UserLanguageEntry.TABLE_NAME);

        // What we want (more or less):
        // select *, attempt_count, success_rate from word left join (select word_id, count(word_id) as attempt_count, avg(success) as success_rate from attempt where attempt.user_id=1 GROUP BY word_id) attempt on word._id = attempt.word_id where word.language_id = 16;
        sWordsWithAttemptsQueryBuilder.setTables(WordEntry.TABLE_NAME + " LEFT JOIN " +
                "(SELECT " + AttemptEntry.COLUMN_WORD_ID + ", " +
                AttemptEntry.COLUMN_USER_ID + ", " +
                "count(" + AttemptEntry.COLUMN_WORD_ID +") AS " + AttemptEntry.COMPUTED_ATTEMPT_COUNT + ", " +
                "avg(" + AttemptEntry.COLUMN_SUCCESS + ") AS " + AttemptEntry.COMPUTED_SUCCESS_RATE +
                " FROM " + AttemptEntry.TABLE_NAME + " GROUP BY " + AttemptEntry.COLUMN_WORD_ID + ") attempt" +
        " ON " + WordEntry.TABLE_NAME + "." + WordEntry._ID + " = " + AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_WORD_ID);


    }

    //user._id = ?
    private static final String sUserSelection = UserEntry.TABLE_NAME + "." + UserEntry._ID + " = ? ";

    //user.google_id = ?
    private static final String sUserSelectionByGoogleId = UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_GOOGLE_ID + " = ? ";

    //word._id = ?
    private static final String sWordSelection = WordEntry.TABLE_NAME + "." + WordEntry._ID + " = ? ";

    //word.language_id = ?
    private static final String sWordByLanguageSelection = WordEntry.TABLE_NAME + "." + WordEntry.COLUMN_LANGUAGE_ID + " = ? ";

    private static final String sAttemptsByUserByLanguageSelection = "(" + AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_USER_ID + " = ? OR "
    + AttemptEntry.TABLE_NAME + "." + AttemptEntry.COLUMN_USER_ID + " IS NULL " + ") AND " +
            WordEntry.TABLE_NAME + "." + WordEntry.COLUMN_LANGUAGE_ID + " = ?";


    //user_language.user_id = ?
    private static final String sUserLanguageSelectionByUserId = UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_USER_ID + " = ? ";

    //user_id IS NULL -- Meant to be used on a query result.
    private static final String sSelectionByUserIdIsNull = UserLanguageEntry.COLUMN_USER_ID + " IS NULL ";

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DbContract.CONTENT_AUTHORITY;


        matcher.addURI(authority, DbContract.PATH_USER, USER);
        matcher.addURI(authority, DbContract.PATH_USER + "/#", USER_ID);
        matcher.addURI(authority, DbContract.PATH_USER + "/#/languages", LANGUAGES_FOR_USER);

        matcher.addURI(authority, DbContract.PATH_WORD, WORD);
        matcher.addURI(authority, DbContract.PATH_WORD + "/#", WORD_ID);

        matcher.addURI(authority, DbContract.PATH_USER_LANGUAGE, USER_LANGUAGE);
        matcher.addURI(authority, DbContract.PATH_LAST_UPDATE, LAST_UPDATE);

        matcher.addURI(authority, DbContract.PATH_LANGUAGES + "/" + LanguageEntry.NOT_USER_PATH_SEGMENT, LANGUAGES_NOT_LEARNED_FOR_USER);


        matcher.addURI(authority, DbContract.PATH_LANGUAGES, LANGUAGES);

        matcher.addURI(authority, DbContract.PATH_ATTEMPT, ATTEMPT);
        matcher.addURI(authority, DbContract.PATH_ATTEMPT + "/by_user_language", ATTEMPTS_BY_USER_BY_LANGUAGE);

        return matcher;

    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    /**
     * This is useful for third-party applications that would use our content provider.
     * Since I'm the only user it's fine not to implement getType.
     */
    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(LOG_TAG, "in query with uri: " + uri.toString());
        final int match = sUriMatcher.match(uri);
        Cursor retCursor = null;
        switch (match) {
            case LANGUAGES_FOR_USER: {
                retCursor = getLanguagesForUser(uri, projection, sortOrder);
                break;
            }
            case LANGUAGES: {
                retCursor = getLanguages(uri, projection, sortOrder);
                break;
            }
            case USER: {
                // That's our only use case for this endpoint atm.
                retCursor = getUserByGoogleId(uri, projection, sortOrder);
                break;
            }
            case USER_ID: {
                retCursor = getUser(uri, projection, sortOrder);
                break;
            }
            case WORD_ID: {
                retCursor = getWord(uri, projection, sortOrder);
                break;
            }
            case WORD: {
                retCursor = sWordsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case USER_LANGUAGE: {
                retCursor = sUserLanguageQueryBuilder.query(mDbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case ATTEMPT: {
                retCursor = sAttemptsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            }
            case ATTEMPTS_BY_USER_BY_LANGUAGE: {
                retCursor = getAttemptsForLanguageForUser(uri, projection, sortOrder);
                break;
            }
            case LANGUAGES_NOT_LEARNED_FOR_USER: {
                retCursor = getLanguagesNotLearnedForUser(uri, projection, sortOrder);
                break;
            }
            case LAST_UPDATE: {
                retCursor = mDbHelper.getReadableDatabase().rawQuery(
                        "SELECT MAX(ts) FROM (" +
                                "SELECT MAX(" + UserLanguageEntry.COLUMN_CREATED_TIMESTAMP + ") as ts FROM " + UserLanguageEntry.TABLE_NAME +
                                " UNION " +
                                "SELECT MAX(" + AttemptEntry.COLUMN_TIMESTAMP + ") as ts FROM " + AttemptEntry.TABLE_NAME +
                                ") x"
                        ,
                        null);
                break;
            }
        }
        return retCursor;
    }

    /**
     * Fetch languages that a user has **not** started learning.
     */
    private Cursor getLanguagesNotLearnedForUser(Uri uri, String[] projection, String sortOrder) {
        Log.d(LOG_TAG, "in getLanguagesNotLearnedForUser with uri: " + uri.toString());
        String userId = uri.getQueryParameter(UserLanguageEntry.COLUMN_USER_ID);

        // I know, I know, sql injection. Let's not share this content provider.
        // I did it because I did not find a way to have a selection parameter in a subquery.
        return mDbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM " +
                        strLanguageNotLearnedForUserTables.replace("?", userId) +
                        " WHERE " + sSelectionByUserIdIsNull  + " ORDER BY " + sortOrder,
                null);
    }

    /**
     * We do a groupby on word id here so projection needs to be some aggregated field.
     */
    private Cursor getAttempts(Uri uri, String[] projection, String sortOrder) {
        String[] selectionArgs = new String[] {
                uri.getQueryParameter(AttemptEntry.COLUMN_USER_ID),
                uri.getQueryParameter(AttemptEntry.COLUMN_LANGUAGE_ID)
        };

        return sAttemptsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sAttemptsByUserByLanguageSelection,
                selectionArgs,
                AttemptEntry.COLUMN_WORD_ID,
                null,
                sortOrder);
    }

    /**
     * Left join of words with attempts (grouped by word_id) filtered by user and language.
     */
    private Cursor getAttemptsForLanguageForUser(Uri uri, String[] projection, String sortOrder) {
        String[] selectionArgs = new String[] {
                uri.getQueryParameter(AttemptEntry.COLUMN_USER_ID),
                uri.getQueryParameter(WordEntry.COLUMN_LANGUAGE_ID)
        };

        return sWordsWithAttemptsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sAttemptsByUserByLanguageSelection,
                selectionArgs,
                AttemptEntry.COLUMN_WORD_ID,
                null,
                sortOrder);

    }


    /**
     * TODO: delete that?
     * @param uri
     * @param projection
     * @param sortOrder
     * @return
     */
    private Cursor getWordsByLanguage(Uri uri, String[] projection, String sortOrder) {
        Log.d(LOG_TAG, "in getWordsByLanguage, uri=" + uri.toString());
        String[] selectionArgs = new String[]{uri.getQueryParameter(WordEntry.COLUMN_LANGUAGE_ID)};
        return sWordsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sWordByLanguageSelection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getWord(Uri uri, String[] projection, String sortOrder) {
        String[] selectionArgs = new String[]{WordEntry.getIdFromUri(uri)};
        return sWordsQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sWordSelection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getUserByGoogleId(Uri uri, String[] projection, String sortOrder) {
        Log.d(LOG_TAG, "in getUserByGoogleId with uri=" + uri.toString());
        String[] selectionArgs = {uri.getQueryParameter(UserEntry.COLUMN_GOOGLE_ID)};
        return sUsersQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sUserSelectionByGoogleId,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getUser(Uri uri, String[] projection, String sortOrder) {
        String[] selectionArgs = new String[]{UserEntry.getIdFromUri(uri)};
        return sUsersQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sUserSelection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.d(LOG_TAG, "in insert with Uri: " + uri.toString());
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri returnUri;
        switch (match) {
            case USER: {
                long _id = db.insert(UserEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                    returnUri = UserEntry.buildUserUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case USER_LANGUAGE: {
                long _id = db.insert(UserLanguageEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                    returnUri = UserLanguageEntry.buildUserLanguageUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;

            }
            case ATTEMPT:
                long _id = db.insert(AttemptEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                    returnUri = AttemptEntry.buildAttemptUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);

        }
        // getContext() can return null if called before onCreate(). This wont happen here.
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    /**
     * Somewhat copy-pasted from the sunshine app.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount;
        switch (match) {
            case WORD:
                returnCount = simpleTransactionInsert(values, db, WordEntry.TABLE_NAME);
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case ATTEMPT:
                returnCount = simpleTransactionInsert(values, db, AttemptEntry.TABLE_NAME);
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case USER_LANGUAGE:
                returnCount = simpleTransactionInsert(values, db, UserLanguageEntry.TABLE_NAME);
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    /**
     * Insert all values in the relevant table in a single transaction.
     * @param values
     * @param db
     * @param tableName
     * @return
     */
    private int simpleTransactionInsert(ContentValues[] values, SQLiteDatabase db, String tableName) {
        db.beginTransaction();
        int returnCount = 0;
        try {
            for (ContentValues value : values) {
                long _id = db.insert(tableName, null, value);
                if (_id != -1) {
                    returnCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return returnCount;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }


    private Cursor getLanguages(Uri uri, String[] projection, String sortOrder) {
        return sLanguagesQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getLanguagesForUser(Uri uri, String[] projection, String sortOrder) {
        Log.d(LOG_TAG, "in getLanguagesForUser with uri: " + uri.toString());
        String userId = UserEntry.getIdFromUri(uri);

        String[] selectionArgs = new String[]{userId};
        String selection = sUserLanguageSelectionByUserId;

        return sLanguagesForUserQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
}
