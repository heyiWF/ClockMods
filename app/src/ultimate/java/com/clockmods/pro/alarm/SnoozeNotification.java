package com.clockmods.pro.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import androidx.core.app.NotificationCompat;

import com.clockmods.LocaleManager;
import com.clockmods.R;

import java.time.Instant;
import java.time.ZoneId;

final class SnoozeNotification {
    static final String STOP = "com.clockmods.pro.alarm.action.STOP_SNOOZE";
    private static final String CHANNEL = "clockmods_alarm_snoozed";
    private static final int NOTICE_ID = 101;

    private SnoozeNotification() {}

    static void update(Context context, AlarmSpec alarm) {
        if (alarm.isTimer()) return;
        if (alarm.snoozeAt <= System.currentTimeMillis() || alarm.excludedDates.contains(
                Instant.ofEpochMilli(alarm.snoozeAt).atZone(ZoneId.systemDefault()).toLocalDate())) {
            cancel(context, alarm.id);
            return;
        }
        Context localized = LocaleManager.wrap(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, localized.getString(R.string.alarm_snooze),
                NotificationManager.IMPORTANCE_LOW));
        // Include the occurrence in the intent identity so an old button cannot cancel a later snooze.
        Intent stop = new Intent(context, AlarmReceiver.class).setAction(STOP)
                .setData(AlarmScheduler.uri(alarm.id).buildUpon().appendPath(Long.toString(alarm.snoozeAt)).build())
                .putExtra(AlarmScheduler.EXTRA_ID, alarm.id).putExtra(AlarmScheduler.EXTRA_TRIGGER, alarm.snoozeAt);
        PendingIntent action = PendingIntent.getBroadcast(context, 0, stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String when = DateUtils.formatDateTime(localized, alarm.snoozeAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        NotificationCompat.Builder notice = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle(alarm.label.isEmpty() ? localized.getString(R.string.pro_page_alarm) : alarm.label)
                .setContentText(localized.getString(R.string.alarm_next_trigger, when))
                .setOnlyAlertOnce(true).setSilent(true).setOngoing(true).setShowWhen(false)
                .addAction(0, localized.getString(R.string.alarm_dismiss), action);
        try {
            manager.notify(Long.toString(alarm.id), NOTICE_ID, notice.build());
        } catch (SecurityException denied) {
            // The snooze remains scheduled when notification access is denied.
        }
    }

    static void cancel(Context context, long id) {
        context.getSystemService(NotificationManager.class).cancel(Long.toString(id), NOTICE_ID);
    }

    static void stop(Context context, AlarmStore store, AlarmSpec alarm, long expected) {
        if (alarm.snoozeAt == expected) {
            alarm.snoozeAt = 0;
            AlarmScheduler.schedule(context, alarm);
        } else if (alarm.snoozeAt != 0 || alarm.lastFiredAt != expected) {
            return;
        }
        // The tap may arrive just after this snooze has started ringing.
        if (store.pendingRings().containsKey(alarm.id)) {
            context.startService(AlarmDelivery.command(context, alarm.id, AlarmDelivery.STOP));
        }
    }
}
