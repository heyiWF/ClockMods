package com.clockmods.pro.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public final class AlarmScheduler {
    private AlarmScheduler() { }

    public static long nextTrigger(int hour, int minute, long nowMillis) {
        Calendar trigger = Calendar.getInstance();
        trigger.setTimeInMillis(nowMillis);
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        if (trigger.getTimeInMillis() <= nowMillis) trigger.add(Calendar.DAY_OF_YEAR, 1);
        return trigger.getTimeInMillis();
    }

    public static void schedule(Context context, int hour, int minute) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAt = nextTrigger(hour, minute, System.currentTimeMillis());
        PendingIntent operation = alarmIntent(context);
        PendingIntent showIntent = PendingIntent.getActivity(context, 101,
                new Intent(context, AlarmRingingActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        }
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(alarmIntent(context));
    }

    private static PendingIntent alarmIntent(Context context) {
        return PendingIntent.getBroadcast(context, 100,
                new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}