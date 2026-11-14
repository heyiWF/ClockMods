package com.clockmods.pro.timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.clockmods.LocaleManager;
import com.clockmods.R;

public final class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "clockmods_timer";

    @Override public void onReceive(Context context, Intent intent) {
        complete(context, intent.getStringExtra("mode"));
    }

    public static synchronized void complete(Context context, String mode) {
        boolean pomodoro = "pomodoro".equals(mode);
        String prefix = pomodoro ? "pomodoro_" : "countdown_";
        android.content.SharedPreferences p = context.getSharedPreferences("pro_timers", 0);
        if (!p.getBoolean(prefix + "running", false)
                || p.getLong(prefix + "deadline", 0) > System.currentTimeMillis()) return;
        android.content.SharedPreferences.Editor editor = p.edit()
                .putBoolean(prefix + "running", false).putLong(prefix + "deadline", 0);
        if (pomodoro) {
            int phase = (p.getInt(prefix + "phase", 0) + 1) % 3;
            long duration = phase == 0 ? 25 * 60_000L : phase == 1 ? 5 * 60_000L : 15 * 60_000L;
            editor.putInt(prefix + "phase", phase).putLong(prefix + "duration", duration)
                    .putLong(prefix + "remaining", duration);
        } else editor.putLong(prefix + "remaining", 0);
        editor.apply();
        TimerScheduler.cancel(context, mode);
        Context localized = LocaleManager.wrap(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                localized.getString(R.string.timer_channel), NotificationManager.IMPORTANCE_HIGH));
        PendingIntent open = TimingLiveUpdates.open(context, pomodoro ? "pomodoro" : "countdown",
                pomodoro ? 201 : 202);
        if (!manager.areNotificationsEnabled()) return;
        try {
            manager.notify(pomodoro ? 201 : 202, new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_clock)
                    .setContentTitle(localized.getString(pomodoro
                            ? R.string.pomodoro_complete : R.string.countdown_complete))
                    .setContentText(localized.getString(R.string.timer_complete_open))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .build());
        } catch (SecurityException ignored) { /* Permission revoked while completing. */ }
    }
}
