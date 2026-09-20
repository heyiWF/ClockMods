package com.clockmods.pro.timer

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restores countdown and Pomodoro alarms after events that clear AlarmManager state. */
class TimerRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> TimerScheduler.restoreRunningTimers(context)
            else -> return
        }
    }
}
