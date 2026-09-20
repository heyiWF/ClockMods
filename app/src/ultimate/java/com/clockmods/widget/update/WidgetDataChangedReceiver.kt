package com.clockmods.widget.update

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockmods.widget.model.WidgetKind

class WidgetDataChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val system = action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_LOCALE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_CONFIGURATION_CHANGED ||
            action == Intent.ACTION_WALLPAPER_CHANGED
        if (!system && action != REFRESH && action != MIDNIGHT && action != WEATHER_CHANGED) return

        val pending = goAsync()
        val appContext = context.applicationContext
        WidgetUpdateCoordinator.execute {
            try {
                when {
                    action == REFRESH -> {
                        if (WidgetUpdateCoordinator.kindFor(
                                appContext,
                                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1),
                            ) == WidgetKind.WEATHER
                        ) WidgetRefreshWorker.refresh(appContext)
                    }
                    action == WEATHER_CHANGED -> WidgetUpdateCoordinator.updateWeatherWidgets(appContext)
                    else -> WidgetUpdateCoordinator.updateAll(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val REFRESH = "com.clockmods.widget.REFRESH"
        const val MIDNIGHT = "com.clockmods.widget.MIDNIGHT"
        const val WEATHER_CHANGED = "com.clockmods.widget.WEATHER_CHANGED"
    }
}
