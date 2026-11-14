package com.clockmods.pro.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.ultimate.UltimateMainActivity;

/** System-owned chronometers keep ticking even when the app process is gone. */
public final class TimingLiveUpdates {
    public static final String EXTRA_DESTINATION = "com.clockmods.ultimate.extra.DESTINATION";
    public static final String CHANNEL = "clockmods_active_timers";
    public static final int POMODORO_ID = 211, COUNTDOWN_ID = 212, STOPWATCH_ID = 213;
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;

    private TimingLiveUpdates() { }

    public static void initialize(Context context) {
        Context app = context.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refresh = () -> refreshAll(app);
        // Keep a strong reference: SharedPreferences stores listeners weakly. Coalesce one edit.
        listener = (prefs, key) -> {
            if ("laps".equals(key)) return;
            handler.removeCallbacks(refresh);
            handler.post(refresh);
        };
        app.getSharedPreferences("pro_timers", 0).registerOnSharedPreferenceChangeListener(listener);
        app.getSharedPreferences("pro_stopwatch", 0).registerOnSharedPreferenceChangeListener(listener);
        handler.post(refresh);
    }

    public static void refreshAll(Context context) {
        refreshTimer(context, "pomodoro");
        refreshTimer(context, "countdown");
        SharedPreferences p = context.getSharedPreferences("pro_stopwatch", 0);
        if (!p.getBoolean("running", false)) {
            manager(context).cancel(STOPWATCH_ID);
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = com.clockmods.ultimate.compose.StopwatchTimebase.INSTANCE.elapsed(p.getLong("accumulated", 0), true, p.getLong("started_at_realtime", 0),
                p.getLong("started_at_wall", 0), SystemClock.elapsedRealtime(), now);
        post(context, STOPWATCH_ID, "stopwatch", R.string.pro_page_stopwatch,
                R.string.timing_running, now - elapsed, false, 0);
    }

    public static void refreshTimer(Context context, String mode) {
        boolean pomodoro = "pomodoro".equals(mode);
        int id = pomodoro ? POMODORO_ID : COUNTDOWN_ID;
        String prefix = pomodoro ? "pomodoro_" : "countdown_";
        SharedPreferences p = context.getSharedPreferences("pro_timers", 0);
        long deadline = p.getLong(prefix + "deadline", 0);
        long remaining = deadline - System.currentTimeMillis();
        if (!p.getBoolean(prefix + "running", false) || remaining <= 0) {
            manager(context).cancel(id);
            return;
        }
        int phase = p.getInt(prefix + "phase", 0);
        int text = !pomodoro ? R.string.timing_running : phase == 0 ? R.string.pomodoro_focus
                : phase == 1 ? R.string.pomodoro_short_break : R.string.pomodoro_long_break;
        post(context, id, pomodoro ? "pomodoro" : "countdown",
                pomodoro ? R.string.pro_page_pomodoro : R.string.pro_page_countdown,
                text, deadline, true, remaining);
    }

    public static void cancelTimer(Context context, String mode) {
        manager(context).cancel("pomodoro".equals(mode) ? POMODORO_ID : COUNTDOWN_ID);
    }

    public static PendingIntent open(Context context, String destination, int requestCode) {
        Intent intent = new Intent(context, UltimateMainActivity.class)
                .putExtra(EXTRA_DESTINATION, destination)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static NotificationManager manager(Context context) {
        return context.getSystemService(NotificationManager.class);
    }

    private static void post(Context context, int id, String destination, int title, int text,
            long when, boolean countdown, long timeout) {
        NotificationManager manager = manager(context);
        if (!manager.areNotificationsEnabled()) return;
        Context localized = LocaleManager.wrap(context);
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                localized.getString(R.string.timing_channel), NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
        Bundle extras = new Bundle();
        // Official EXTRA_REQUEST_PROMOTED_ONGOING wire key. Older Android versions ignore it;
        // this also supports Android 16 QPR builds without calling the newer 36.1 method.
        extras.putBoolean("android.requestPromotedOngoing", true);
        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle(localized.getString(title))
                .setContentText(localized.getString(text))
                .setContentIntent(open(context, destination, id))
                .setCategory(countdown ? Notification.CATEGORY_PROGRESS : Notification.CATEGORY_STOPWATCH)
                .setOngoing(true).setOnlyAlertOnce(true)
                .setWhen(when).setShowWhen(true).setUsesChronometer(true)
                .setChronometerCountDown(countdown).addExtras(extras);
        // Remove an expired countdown even when an inexact alarm is delayed by Doze.
        if (timeout > 0) builder.setTimeoutAfter(timeout);
        try {
            manager.notify(id, builder.build());
        } catch (SecurityException ignored) {
            // Notification permission can be revoked between the check and notify.
        }
    }
}
