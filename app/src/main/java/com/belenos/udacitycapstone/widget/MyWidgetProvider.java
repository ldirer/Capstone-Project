package com.belenos.udacitycapstone.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import com.belenos.udacitycapstone.GameFragment;
import com.belenos.udacitycapstone.MainActivity;
import com.belenos.udacitycapstone.R;
import com.belenos.udacitycapstone.data.DbProvider;

public class MyWidgetProvider extends AppWidgetProvider{

    private static final String LOG_TAG = MyWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, MyWidgetIntentService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, MyWidgetIntentService.class));
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        Log.d(LOG_TAG, "in onReceive");
        if (GameFragment.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, MyWidgetIntentService.class));
        }
    }

}
