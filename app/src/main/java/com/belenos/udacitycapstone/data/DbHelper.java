package com.belenos.udacitycapstone.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.belenos.udacitycapstone.data.DbContract.AttemptEntry;
import com.belenos.udacitycapstone.data.DbContract.LanguageEntry;
import com.belenos.udacitycapstone.data.DbContract.UserLanguageEntry;
import com.belenos.udacitycapstone.data.DbContract.WordEntry;

import static com.belenos.udacitycapstone.data.DbContract.UserEntry;

public class DbHelper extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 8;

    static final String DATABASE_NAME = "capstone.db";


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //Create all tables.
        // We set a unique constraint on Google id so we don't create several times the same user.
        // Ok here's a small (nice) trick for the super-thorough reviewer/future me.
        // Copy concatenation text to the clipboard: alt+enter with cursor on a concatenation (`+`) operator.
        // -> If everything is static Intellij will create the string and we can check that there's no typo.
        final String SQL_CREATE_USER_TABLE = "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
                UserEntry._ID + " INTEGER PRIMARY KEY," +
                UserEntry.COLUMN_NAME + " TEXT NOT NULL," +
                UserEntry.COLUMN_GOOGLE_ID + " TEXT NOT NULL," +
                " UNIQUE (" + UserEntry.COLUMN_GOOGLE_ID + ") ON CONFLICT IGNORE" +
                ");";
        final String SQL_CREATE_LANGUAGE_TABLE = "CREATE TABLE " + LanguageEntry.TABLE_NAME + " (" +
                LanguageEntry._ID + " INTEGER PRIMARY KEY," +
                LanguageEntry.COLUMN_NAME + " TEXT NOT NULL," +
                LanguageEntry.COLUMN_ICON_NAME + " TEXT NOT NULL" +
                ");";

        // TODO: nice to have: a UNIQUE constraint on user_id, language_id. This will be enforced by the UI though (the user wont be able to pick twice the same language).
        // TODO: enforce it in the UI ;).
        final String SQL_CREATE_USER_LANGUAGE_TABLE = "CREATE TABLE " + UserLanguageEntry.TABLE_NAME + " (" +
                UserLanguageEntry._ID + " INTEGER PRIMARY KEY," +
                UserLanguageEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                UserLanguageEntry.COLUMN_LANGUAGE_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + UserLanguageEntry.COLUMN_USER_ID + ") REFERENCES " + UserEntry.TABLE_NAME + " (" + UserEntry._ID + "), " +
                "FOREIGN KEY (" + UserLanguageEntry.COLUMN_LANGUAGE_ID + ") REFERENCES " + LanguageEntry.TABLE_NAME + " (" + LanguageEntry._ID + ")" +
                ");";


        final String SQL_CREATE_WORD_TABLE = "CREATE TABLE " + WordEntry.TABLE_NAME + " (" +
                WordEntry._ID + " INTEGER PRIMARY KEY," +
        WordEntry.COLUMN_LANGUAGE_ID + " INTEGER NOT NULL," +
                WordEntry.COLUMN_WORD + " TEXT NOT NULL," +
        WordEntry.COLUMN_TRANSLATION + " TEXT NOT NULL" +
                ");";


        final String SQL_CREATE_ATTEMPT_TABLE = "CREATE TABLE " + AttemptEntry.TABLE_NAME + " (" +
                AttemptEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AttemptEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                AttemptEntry.COLUMN_WORD_ID + " INTEGER NOT NULL, " +
                // No boolean column in sqlite so we 0-1 encode it.
                AttemptEntry.COLUMN_SUCCESS + " INTEGER NOT NULL, " +
                //TODO: check timestamp column dtype.
                AttemptEntry.COLUMN_TIMESTAMP + " TIMESTAMP NOT NULL" +
                ");";

        final String SQL_FIXTURE_LANGUAGES = "INSERT INTO " + LanguageEntry.TABLE_NAME +
                " (" + LanguageEntry.COLUMN_NAME + ", " + LanguageEntry.COLUMN_ICON_NAME + ") " +
                "VALUES ('French', 'fr'), ('German', 'de'), ('Romanian', 'ro'), ('Spanish', 'es');";

        final String SQL_FIXTURE_WORDS = "INSERT INTO " + WordEntry.TABLE_NAME +
                " (" + WordEntry.COLUMN_LANGUAGE_ID + ", " + WordEntry.COLUMN_WORD + ", " + WordEntry.COLUMN_TRANSLATION + ") " +
                "VALUES ('1', 'I eat', 'Je mange'), ('1', 'You like', 'Tu aimes');";

        sqLiteDatabase.execSQL(SQL_CREATE_USER_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_LANGUAGE_TABLE);

        sqLiteDatabase.execSQL(SQL_CREATE_USER_LANGUAGE_TABLE);

        sqLiteDatabase.execSQL(SQL_CREATE_WORD_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ATTEMPT_TABLE);

        sqLiteDatabase.execSQL(SQL_FIXTURE_LANGUAGES);
        sqLiteDatabase.execSQL(SQL_FIXTURE_WORDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LanguageEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UserLanguageEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
