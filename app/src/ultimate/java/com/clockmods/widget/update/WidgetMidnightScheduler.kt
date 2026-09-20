package com.clockmods.widget.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.clockmods.widget.model.WidgetConfig
import java.time.Instant
import java.time.ZonedDateTime

object WidgetMidnightScheduler {
    @JvmStatic fun nextMidnight(now: Long, configs: List<WidgetConfig>): Long {
        var next = Long.MAX_VALUE
        for (config in configs) {
            val local = Instant.ofEpochMilli(now).atZone(config.zone().toZoneId())
            next = minOf(
                next,
                local.toLocalDate().plusDays(1).atStartOfDay(local.zone).toInstant().toEpochMilli(),
            )
        }
        return next
    }

    @JvmStatic fun schedule(context: Context, configs: List<WidgetConfig>) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val alarm = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetDataChangedReceiver::class.java).setAction(WidgetDataChangedReceiver.MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.cancel(alarm)
        if (configs.isEmpty()) return
        // Inexact RTC alarm: no exact-alarm permission or waking a sleeping device for a date label.
        manager.set(AlarmManager.RTC, nextMidnight(System.currentTimeMillis(), configs) + 1000, alarm)
    }
}
