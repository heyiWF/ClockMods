package com.clockmods.pro.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class TimerScheduler {
    private TimerScheduler() { }

    public static void schedule(Context context, String mode, long deadlineMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = intent(context, mode);
        if (manager.canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineMillis, operation);
                return;
            } catch (SecurityException ignored) { /* Permission changed; use inexact fallback. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineMillis, operation);
    }

    public static void cancel(Context context, String mode) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(intent(context, mode));
        TimingLiveUpdates.cancelTimer(context, mode);
    }

    public static void restoreRunningTimers(Context context) {
        android.content.SharedPreferences p = context.getSharedPreferences("pro_timers", 0);
        for (String mode : new String[] {"pomodoro", "countdown"}) {
            if (!p.getBoolean(mode + "_running", false)) continue;
            long deadline = p.getLong(mode + "_deadline", 0);
            if (deadline > System.currentTimeMillis()) schedule(context, mode, deadline);
            else TimerReceiver.complete(context, mode);
        }
        TimingLiveUpdates.refreshAll(context);
    }

    private static PendingIntent intent(Context context, String mode) {
        Intent intent = new Intent(context, TimerReceiver.class).putExtra("mode", mode);
        int requestCode = "pomodoro".equals(mode) ? 201 : 202;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}