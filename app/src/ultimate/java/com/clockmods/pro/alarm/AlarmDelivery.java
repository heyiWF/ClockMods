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
    public static final String NEXT = "com.clockmods.pro.alarm.action.NEXT_PHASE";
    private AlarmDelivery() {}
    static String duration(AlarmSpec alarm) {
        long seconds = alarm.timerDuration / 1000;
        if (alarm.countdown) return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d",
                seconds / 3600, seconds / 60 % 60, seconds % 60);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
    static int phaseLabel(AlarmSpec alarm) {
        return com.clockmods.pro.timer.TimerCompletion.phaseLabel(alarm.completedPhase);
    }
    static String title(Context localized, AlarmSpec alarm) {
        if (alarm != null && alarm.isTimer()) {
            if (alarm.countdown && !alarm.timerLabel.isEmpty())
                return localized.getString(R.string.countdown_named_finished, alarm.timerLabel, duration(alarm));
            if (alarm.timerDuration > 0) return alarm.countdown
                    ? localized.getString(R.string.countdown_finished_duration, duration(alarm))
                    : localized.getString(R.string.pomodoro_finished_duration, localized.getString(phaseLabel(alarm)), duration(alarm));
            return localized.getString(alarm.countdown ? R.string.countdown_complete : R.string.pomodoro_complete);
        }
        return alarm == null || alarm.label.isEmpty() ? localized.getString(R.string.alarm_ringing) : localized.getString(R.string.alarm_named_ringing, alarm.label);
    }
    static int stopLabel(AlarmSpec alarm) {
        if (alarm == null || !alarm.isTimer()) return R.string.alarm_dismiss;
        return alarm.countdown ? R.string.countdown_dismiss : R.string.pomodoro_dismiss;
    }
    public static Notification notification(Context context, AlarmSpec alarm, int queued) {
        Context localized = LocaleManager.wrap(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        boolean timer = alarm != null && alarm.isTimer();
        String channelId = timer ? (alarm.countdown ? "clockmods_countdown_complete" : "clockmods_pomodoro_complete") : CHANNEL;
        NotificationChannel channel = new NotificationChannel(channelId,
                localized.getString(timer ? (alarm.countdown ? R.string.pro_page_countdown : R.string.pro_page_pomodoro) : R.string.alarm_channel),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        channel.setSound(null, null); channel.enableVibration(false); manager.createNotificationChannel(channel);
        long id = alarm == null ? 0 : alarm.id;
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, AlarmRingingActivity.class).setData(AlarmScheduler.uri(id))
                        .putExtra(AlarmScheduler.EXTRA_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, activityOptions());
        String message = timer ? localized.getString(alarm.countdown ? R.string.countdown_ringing_hint : R.string.pomodoro_ringing_hint)
                : queued > 1 ? localized.getString(R.string.alarm_queue_count, queued) : localized.getString(R.string.alarm_open_to_dismiss);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle(title(localized, alarm))
                .setContentText(message)
                .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false).setContentIntent(show)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        if (timer) {
            builder.addAction(0, localized.getString(stopLabel(alarm)), action(context, id, STOP));
            if (!alarm.countdown && alarm.completedDeadline != 0) {
                builder.addAction(0, localized.getString(R.string.pomodoro_next), action(context, id, NEXT));
            }
        } else if (alarm != null) {
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
    public static void timer(Context context, boolean pomodoro, long duration, int phase, long deadline) {
        timer(context, pomodoro, duration, phase, deadline, "");
    }
    public static void timer(Context context, boolean pomodoro, long duration, int phase, long deadline, String timerLabel) {
        AlarmStore store = new AlarmStore(context); AlarmSpec alert = new AlarmSpec();
        alert.timerLabel = timerLabel;
        alert.id = -System.currentTimeMillis(); while (store.find(alert.id) != null) alert.id--;
        alert.enabled = false;
        alert.countdown = !pomodoro;
        alert.timerDuration = duration;
        alert.completedPhase = phase;
        alert.completedDeadline = deadline;
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
