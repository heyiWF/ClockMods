package com.clockmods.pro.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class TimerScheduler {
    private TimerScheduler() { }

    public static void schedule(Context context, String mode, long deadlineMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = intent(context, mode);
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineMillis, operation);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineMillis, operation);
        }
    }

    public static void cancel(Context context, String mode) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(intent(context, mode));
    }

    private static PendingIntent intent(Context context, String mode) {
        Intent intent = new Intent(context, TimerReceiver.class).putExtra("mode", mode);
        int requestCode = "pomodoro".equals(mode) ? 201 : 202;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}