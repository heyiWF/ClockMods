package com.clockmods.pro.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.clockmods.LocaleManager;
import com.clockmods.R;

/** The service owns sound; a silent notification channel cannot play a second, wrong ringtone. */
public final class AlarmDelivery {
    public static final String CHANNEL = "clockmods_alarm_v2";
    public static final String STOP = "com.clockmods.pro.alarm.action.STOP_RINGING";
    public static final String SNOOZE = "com.clockmods.pro.alarm.action.SNOOZE";
    private AlarmDelivery() {}
    public static Notification notification(Context context, AlarmSpec alarm, int queued) {
        Context localized = LocaleManager.wrap(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, localized.getString(R.string.alarm_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        channel.setSound(null, null); channel.enableVibration(false); manager.createNotificationChannel(channel);
        long id = alarm == null ? 0 : alarm.id;
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, AlarmRingingActivity.class).setData(AlarmScheduler.uri(id))
                        .putExtra(AlarmScheduler.EXTRA_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, activityOptions());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle(alarm == null || alarm.label.isEmpty() ? localized.getString(R.string.alarm_ringing) : alarm.label)
                .setContentText(queued > 1 ? localized.getString(R.string.alarm_queue_count, queued) : localized.getString(R.string.alarm_open_to_dismiss))
                .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false).setContentIntent(show)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        if (alarm != null) {
            boolean swapped = AlarmPreferences.swapped(context, id);
            builder.addAction(0, localized.getString(swapped ? R.string.alarm_dismiss : R.string.alarm_snooze), action(context, id, swapped ? STOP : SNOOZE))
                    .addAction(0, localized.getString(swapped ? R.string.alarm_snooze : R.string.alarm_dismiss), action(context, id, swapped ? SNOOZE : STOP));
        }
        if (alarm != null && (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent())) builder.setFullScreenIntent(show, true);
        return builder.build();
    }
    private static android.os.Bundle activityOptions() {
        if (Build.VERSION.SDK_INT < 35) return null;
        android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
        options.setPendingIntentCreatorBackgroundActivityStartMode(Build.VERSION.SDK_INT >= 36
                ? android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                : android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
        return options.toBundle();
    }
    /** Exact alarms may start audio; restricted/inexact/boot delivery falls back to a user-visible FSI. */
    public static void ring(Context context, AlarmSpec alarm) {
        AlarmStore store = new AlarmStore(context); store.rememberRing(alarm.id);
        try {
            androidx.core.content.ContextCompat.startForegroundService(context,
                    new Intent(context, AlarmRingingService.class).putExtra(AlarmScheduler.EXTRA_ID, alarm.id));
        } catch (android.app.ForegroundServiceStartNotAllowedException | SecurityException restricted) {
            store.setRingingId(alarm.id);
            try { context.getSystemService(NotificationManager.class).notify(100, notification(context, alarm, store.pendingRings().size())); }
            catch (SecurityException denied) { /* Notification access remains user-controlled. */ }
        }
    }
    public static void timer(Context context, boolean pomodoro) {
        AlarmStore store = new AlarmStore(context); AlarmSpec alert = new AlarmSpec();
        alert.id = -System.currentTimeMillis(); while (store.find(alert.id) != null) alert.id--;
        alert.enabled = false; alert.snoozeMinutes = 5;
        alert.label = LocaleManager.wrap(context).getString(pomodoro ? R.string.pomodoro_complete : R.string.countdown_complete);
        store.save(alert); ring(context, alert);
    }
    public static Intent command(Context context, long id, String action) {
        return new Intent(context, AlarmRingingService.class).setAction(action)
                .setData(AlarmScheduler.uri(id)).putExtra(AlarmScheduler.EXTRA_ID, id);
    }
    private static PendingIntent action(Context context, long id, String action) {
        return PendingIntent.getService(context, 0, command(context, id, action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
