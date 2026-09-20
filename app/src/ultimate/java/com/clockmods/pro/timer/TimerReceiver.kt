package com.clockmods.pro.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.ultimate.UltimateMainActivity

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = requireNotNull(context)
        val mode = intent!!.getStringExtra(EXTRA_MODE)
        complete(appContext, mode)
    }

    companion object {
        private const val CHANNEL_ID = "clockmods_timer"
        private const val EXTRA_MODE = "mode"
        private const val MODE_POMODORO = "pomodoro"
        private const val POMODORO_REQUEST_CODE = 201
        private const val COUNTDOWN_REQUEST_CODE = 202
        private const val OPEN_REQUEST_CODE = 203

        @JvmStatic
        fun complete(context: Context, mode: String?) {
            val appContext = context.applicationContext
            val prefix = if (mode == MODE_POMODORO) "pomodoro_" else "countdown_"
            val preferences = appContext.getSharedPreferences("pro_timers", Context.MODE_PRIVATE)
            val editor = preferences.edit()
                .putBoolean(prefix + "running", false)
                .putLong(prefix + "deadline", 0L)
            val pomodoro = mode == MODE_POMODORO
            if (pomodoro) {
                val phase = (preferences.getInt(prefix + "phase", 0) + 1) % 3
                val duration = when (phase) {
                    0 -> 25 * 60_000L
                    1 -> 5 * 60_000L
                    else -> 15 * 60_000L
                }
                editor.putInt(prefix + "phase", phase)
                    .putLong(prefix + "duration", duration)
                    .putLong(prefix + "remaining", duration)
            } else {
                editor.putLong(prefix + "remaining", 0L)
            }
            editor.apply()

            val localized = requireNotNull(LocaleManager.wrap(appContext))
            val manager = requireNotNull(
                appContext.getSystemService(NotificationManager::class.java),
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    localized.getString(R.string.timer_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
            val open = PendingIntent.getActivity(
                appContext,
                OPEN_REQUEST_CODE,
                Intent(appContext, UltimateMainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.notify(
                if (pomodoro) POMODORO_REQUEST_CODE else COUNTDOWN_REQUEST_CODE,
                NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(
                        localized.getString(
                            if (pomodoro) R.string.pomodoro_complete
                            else R.string.countdown_complete,
                        ),
                    )
                    .setContentText(localized.getString(R.string.timer_complete_open))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .build(),
            )
        }
    }
}
