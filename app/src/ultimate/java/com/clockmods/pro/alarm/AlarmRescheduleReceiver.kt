package com.clockmods.pro.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class AlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> Unit
            else -> return
        }
        val store = AlarmStore(context)
        // A process kill (or reboot) can interrupt an alarm that was already ringing.  Restore the
        // ringing surface so the user always keeps a visible way to stop it.  BOOT_COMPLETED grants
        // a temporary allowlist, so starting the activity from here is permitted.
        if (store.isRinging() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingingService::class.java),
                )
            }
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
        if (store.enabled()) {
            AlarmScheduler.schedule(context, store.hour(), store.minute())
        }
    }
}
