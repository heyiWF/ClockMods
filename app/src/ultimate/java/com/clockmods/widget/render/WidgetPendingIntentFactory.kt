package com.clockmods.widget.render

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clockmods.ultimate.ComposeMainActivity
import com.clockmods.ultimate.UltimateMainActivity
import com.clockmods.ultimate.settings.ComposeSettingsActivity
import com.clockmods.widget.config.WidgetConfigActivity
import com.clockmods.widget.update.WidgetDataChangedReceiver

object WidgetPendingIntentFactory {
    enum class Action { CLOCK, CALENDAR, WEATHER, CONFIG, REFRESH }

    @JvmStatic fun requestCode(id: Int, action: Action): Int = 31 * id + action.ordinal

    @JvmStatic fun identity(id: Int, action: Action): String =
        "clockmods-widget://instance/$id/${action.name}"

    @JvmStatic fun create(context: Context, id: Int, action: Action): PendingIntent {
        val intent = when (action) {
            Action.CONFIG -> Intent(context, WidgetConfigActivity::class.java)
            Action.WEATHER -> ComposeSettingsActivity.createSubpageIntent(context, "weather")
            Action.REFRESH -> Intent(context, WidgetDataChangedReceiver::class.java)
                .setAction(WidgetDataChangedReceiver.REFRESH)
            Action.CLOCK, Action.CALENDAR -> Intent(context, UltimateMainActivity::class.java)
                .putExtra(ComposeMainActivity.EXTRA_DESTINATION, destination(action))
        }.apply {
            data = Uri.parse(identity(id, action))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (action == Action.REFRESH) {
            PendingIntent.getBroadcast(context, requestCode(id, action), intent, flags)
        } else {
            PendingIntent.getActivity(context, requestCode(id, action), intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)
        }
    }

    @JvmStatic fun tap(value: String?): Action = when (value) {
        "open_config" -> Action.CONFIG
        "open_weather" -> Action.WEATHER
        "open_calendar" -> Action.CALENDAR
        else -> Action.CLOCK
    }

    @JvmStatic fun destination(action: Action): String? = when (action) {
        Action.CLOCK -> ComposeMainActivity.DESTINATION_CLOCK
        Action.CALENDAR -> ComposeMainActivity.DESTINATION_CALENDAR
        else -> null
    }
}
