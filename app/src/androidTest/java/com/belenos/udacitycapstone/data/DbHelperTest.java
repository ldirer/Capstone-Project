package com.belenos.udacitycapstone.data;

import android.app.Instrumentation;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.test.InstrumentationTestCase;

import static org.junit.Assert.*;

public class DbHelperTest {

    private Context mContext;

    @org.junit.Before
    public void deleteDb() {
        mContext = InstrumentationRegistry.getTargetContext();
        mContext.deleteDatabase(DbHelper.DATABASE_NAME);
    }

    @org.junit.Test
    public void testOnCreate() throws Exception {
        SQLiteDatabase db  = new DbHelper(mContext).getWritableDatabase();

        assertTrue(db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        c.close();

    }
}