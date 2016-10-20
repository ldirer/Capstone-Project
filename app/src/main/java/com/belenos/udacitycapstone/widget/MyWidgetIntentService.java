package com.belenos.udacitycapstone.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.belenos.udacitycapstone.LoginActivity;
import com.belenos.udacitycapstone.MainActivity;
import com.belenos.udacitycapstone.R;
import com.belenos.udacitycapstone.data.DbContract;

public class MyWidgetIntentService extends IntentService {
    private static final String LOG_TAG = MyWidgetIntentService.class.getSimpleName();

    private static final String[] COLUMNS = {
            "COUNT(" + DbContract.AttemptEntry.COLUMN_TIMESTAMP + ") as attempt_count",
            DbContract.LanguageEntry.COLUMN_ICON_NAME,
            DbContract.AttemptEntry.COLUMN_LANGUAGE_ID,
            DbContract.LanguageEntry.COLUMN_NAME
    };
    private static final int COLUMN_SUCCESS_COUNT = 0;
    private static final int COLUMN_ICON_NAME = 1;
    private static final int COLUMN_LANGUAGE_ID = 2;
    private static final int COLUMN_LANGUAGE_NAME = 3;
    private static boolean sInitialized = false;

    public MyWidgetIntentService() {
        super("MyWidgetIntentService");
    }

    /**
     * Implementation inspired from sunshine app.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                MyWidgetProvider.class));


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Long userId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);
        String userName = preferences.getString(LoginActivity.KEY_GOOGLE_GIVEN_NAME, null);

        Uri uri = DbContract.LanguageEntry.buildLanguagesAttemptCount(
                userId, System.currentTimeMillis() / 1000 - 60 * 60 * 24);

        Cursor data = getContentResolver().query(uri, COLUMNS, null, null, "attempt_count DESC");
        Log.d(LOG_TAG, String.format("INITIALIZED? %b", sInitialized));

        if ((data == null || !data.moveToFirst()) && !sInitialized) {
            // We try to provide a relevant logged out view when we don't have anything else to show.
            drawWidgets(null, userId, userName, appWidgetManager, appWidgetIds);
        }

        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }


        drawWidgets(data, userId, userName, appWidgetManager, appWidgetIds);
        data.close();
        sInitialized = true;
    }

    /**
     * @param data : if null we'll provide a 'dummy' view that looks bad, but better than an unformatted string.
     */

    private void drawWidgets(Cursor data, Long userId, String userName, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        // The actual drawing of the widget.
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            if (data != null) {
                launchIntent.putExtra(MainActivity.FRAGMENT_ARG_LANGUAGE_NAME, data.getString(COLUMN_LANGUAGE_NAME));
                launchIntent.putExtra(MainActivity.FRAGMENT_ARG_LANGUAGE_ID, data.getLong(COLUMN_LANGUAGE_ID));
            }

            //pending intent because the widget is sort of a remote application.
            // That flag thing... Extras were always null in my activity.
            // http://stackoverflow.com/questions/18049352/notification-created-by-intentservice-uses-allways-a-wrong-intent/18049676#18049676
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            if (data == null) {
                if (userId == null) {
                    views.setTextViewText(R.id.widget_textview, getString(R.string.login_welcome));
                }
                else {
                    // The user logged in but did not pick a language yet
                    views.setTextViewText(R.id.widget_textview, getString(R.string.widget_onboarding, userName));
                }
            } else {
                views.setTextViewText(R.id.widget_textview, getString(R.string.widget_card_count_last_day, data.getInt(COLUMN_SUCCESS_COUNT)));
                String iconName = data.getString(COLUMN_ICON_NAME);
                Log.d(LOG_TAG, String.format("Setting widget data - icon name: %s", iconName));
                views.setTextViewCompoundDrawables(R.id.widget_textview,
                        getResources().getIdentifier(iconName, "drawable", getPackageName()),
                        0, 0, 0);
            }
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

        }

    }
}
