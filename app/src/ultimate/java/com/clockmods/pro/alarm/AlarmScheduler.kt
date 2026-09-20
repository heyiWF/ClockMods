package com.clockmods.pro.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class AlarmScheduler private constructor() {
    companion object {
        private const val ALARM_REQUEST_CODE = 100
        private const val SHOW_REQUEST_CODE = 101
        private const val DIRECT_RINGING_REQUEST_CODE = 102

        @JvmStatic
        fun nextTrigger(hour: Int, minute: Int, nowMillis: Long): Long {
            val trigger = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (trigger.timeInMillis <= nowMillis) {
                trigger.add(Calendar.DAY_OF_YEAR, 1)
            }
            return trigger.timeInMillis
        }

        @JvmStatic
        fun canScheduleExact(context: Context): Boolean =
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .canScheduleExactAlarms()

        @JvmStatic
        fun schedule(context: Context, hour: Int, minute: Int) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = nextTrigger(hour, minute, System.currentTimeMillis())
            val operation = alarmIntent(context)
            // Android 11+ blocks background activity launches from a manifest receiver, so the
            // "launch the ringing activity directly" path can never work from an alarm broadcast.
            // The receiver always uses the notification + foreground service route instead, which
            // also exposes a "stop" action that does not depend on the activity being visible.
            manager.cancel(directRingingIntent(context))
            val showIntent = PendingIntent.getActivity(
                context,
                SHOW_REQUEST_CODE,
                Intent(context, AlarmRingingActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val exactScheduled = canScheduleExact(context) && runCatching {
                manager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                    operation,
                )
            }.isSuccess
            if (!exactScheduled) {
                // Without the exact-alarm permission an exact request is rejected outright, and an
                // inexact alarm survives a later permission change instead of being cancelled.
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }

        @JvmStatic
        fun cancel(context: Context) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.cancel(alarmIntent(context))
            manager.cancel(directRingingIntent(context))
        }

        private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun directRingingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            DIRECT_RINGING_REQUEST_CODE,
            AlarmNotifications.ringingActivityIntent(context)
                .putExtra(AlarmRingingActivity.EXTRA_START_RINGING, true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
