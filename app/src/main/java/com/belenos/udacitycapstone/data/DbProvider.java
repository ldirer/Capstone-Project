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

import com.belenos.udacitycapstone.data.DbContract.LanguageEntry;
import com.belenos.udacitycapstone.data.DbContract.UserEntry;
import com.belenos.udacitycapstone.data.DbContract.UserLanguageEntry;

public class DbProvider extends ContentProvider {
    private static final String LOG_TAG = DbProvider.class.getSimpleName();
    private DbHelper mDbHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();


    private static final int LANGUAGES_FOR_USER = 100;
    private static final int LANGUAGES = 101;
    private static final int USER = 102;
    private static final int USER_LANGUAGE = 103;
    private static final int USER_BY_GOOGLE_ID = 104;
    private static final int USER_ID = 105;


    private static final SQLiteQueryBuilder sLanguagesForUserQueryBuilder;
    private static final SQLiteQueryBuilder sLanguagesQueryBuilder;
    private static final SQLiteQueryBuilder sUsersQueryBuilder;

    static {
        sLanguagesForUserQueryBuilder = new SQLiteQueryBuilder();
        sLanguagesQueryBuilder = new SQLiteQueryBuilder();
        sUsersQueryBuilder = new SQLiteQueryBuilder();

        // A join of user, language and user_language tables.
        //TODO: OOOPS. This is one too many join if it's just about getting the languages for a user.
        sLanguagesForUserQueryBuilder.setTables(
                UserEntry.TABLE_NAME + " INNER JOIN " + UserLanguageEntry.TABLE_NAME +
                        " ON " + UserEntry.TABLE_NAME + "." + UserEntry._ID +
                        " = " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_USER_ID +
                        " LEFT JOIN " + LanguageEntry.TABLE_NAME +
                        " ON " + UserLanguageEntry.TABLE_NAME + "." + UserLanguageEntry.COLUMN_LANGUAGE_ID +
                        " = " + LanguageEntry.TABLE_NAME + "." + LanguageEntry._ID);

        sLanguagesQueryBuilder.setTables(LanguageEntry.TABLE_NAME);
        sUsersQueryBuilder.setTables(UserEntry.TABLE_NAME);
    }


    //user._id = ?
    private static final String sUserSelection = UserEntry.TABLE_NAME + "." + UserEntry._ID + " = ? ";
    //user.google_id = ?
    private static final String sUserSelectionByGoogleId = UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_GOOGLE_ID + " = ? ";


    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DbContract.CONTENT_AUTHORITY;


        matcher.addURI(authority, DbContract.PATH_USER, USER);
        matcher.addURI(authority, DbContract.PATH_USER + "/#", USER_ID);
        matcher.addURI(authority, DbContract.PATH_USER_LANGUAGE, USER_LANGUAGE);

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, DbContract.PATH_USER + "/#/languages", LANGUAGES_FOR_USER);

        matcher.addURI(authority, DbContract.PATH_LANGUAGES, LANGUAGES);

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
            case USER_ID: {
                retCursor = getUser(uri, projection, sortOrder);
                break;
            }
            case USER: {
                // Querying by google id is our only use case for now.
                retCursor = getUserByGoogleId(uri, projection, sortOrder);
            }
        }
        return retCursor;
    }

    private Cursor getUserByGoogleId(Uri uri, String[] projection, String sortOrder) {
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
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);

        }
        // getContext() can return null if called before onCreate(). This wont happen here.
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
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
        String userId = UserEntry.getIdFromUri(uri);

        String[] selectionArgs = new String[]{userId};
        String selection = sUserSelection;

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
