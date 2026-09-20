package com.clockmods.pro.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class TimerScheduler private constructor() {
    companion object {
        private const val PREFERENCES_NAME = "pro_timers"
        private const val EXTRA_MODE = "mode"
        private const val MODE_POMODORO = "pomodoro"
        private const val POMODORO_REQUEST_CODE = 201
        private const val COUNTDOWN_REQUEST_CODE = 202

        @JvmStatic
        fun schedule(context: Context, mode: String?, deadlineMillis: Long) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val operation = intent(context, mode)
            val exactScheduled = manager.canScheduleExactAlarms() && runCatching {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    deadlineMillis,
                    operation,
                )
            }.isSuccess
            if (!exactScheduled) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineMillis, operation)
            }
        }

        @JvmStatic
        fun cancel(context: Context, mode: String?) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.cancel(intent(context, mode))
        }

        /** Rebuilds one-shot alarms after boot, package replacement or permission changes. */
        @JvmStatic
        fun restoreRunningTimers(context: Context, nowMillis: Long = System.currentTimeMillis()) {
            val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            restoreOne(context, preferences, "pomodoro_", MODE_POMODORO, nowMillis)
            restoreOne(context, preferences, "countdown_", null, nowMillis)
        }

        private fun restoreOne(
            context: Context,
            preferences: android.content.SharedPreferences,
            prefix: String,
            mode: String?,
            nowMillis: Long,
        ) {
            if (!preferences.getBoolean(prefix + "running", false)) return
            val deadline = preferences.getLong(prefix + "deadline", 0L)
            if (deadline > nowMillis) {
                schedule(context, mode, deadline)
            } else {
                TimerReceiver.complete(context, mode)
            }
        }

        private fun intent(context: Context, mode: String?): PendingIntent {
            val intent = Intent(context, TimerReceiver::class.java).putExtra(EXTRA_MODE, mode)
            val requestCode = if (mode == MODE_POMODORO) {
                POMODORO_REQUEST_CODE
            } else {
                COUNTDOWN_REQUEST_CODE
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
