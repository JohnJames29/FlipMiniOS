package com.flipminios.launcher;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

/** Samsung Flex Window-compatible entry-point. No root or privileged permissions needed. */
public final class CoverWidgetProvider extends AppWidgetProvider {
    private PendingIntent entry(Context context, int request, String page) {
        Intent i = new Intent(context, LauncherActivity.class);
        i.setAction("com.flipminios.OPEN_" + page);
        i.putExtra("page", page);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(1); // Samsung Flex Window conventional display ID; needs device confirmation.
        return PendingIntent.getActivity(context, request, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, options.toBundle());
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        for (int id : widgetIds) {
            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.cover_widget);
            view.setOnClickPendingIntent(R.id.open_launcher, entry(context, 1, "apps"));
            view.setOnClickPendingIntent(R.id.open_panel, entry(context, 2, "controls"));
            manager.updateAppWidget(id, view);
        }
    }
}
