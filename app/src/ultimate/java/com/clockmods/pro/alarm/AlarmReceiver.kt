package com.clockmods.pro.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = AlarmStore(context)
        if (!store.enabled()) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmRingingService::class.java),
        )
        // An alarm broadcast runs in the background, and Android 11+ blocks background activity
        // launches from manifest receivers.  Opening the ringing UI from here would silently fail,
        // so it is only attempted when the full-screen intent route is actually permitted (the FSI
        // path carries an explicit background-activity-launch exemption on Android 14+).
        if (AlarmNotifications.shouldOpenRingingActivityFromReceiver(context)) {
            runCatching {
                context.startActivity(
                    AlarmNotifications.ringingActivityIntent(context).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
                )
            }
        }
        AlarmScheduler.schedule(context, store.hour(), store.minute())
    }
}
