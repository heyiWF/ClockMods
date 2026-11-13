package com.clockmods.pro.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.clockmods.calendar.HolidayRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

public final class AlarmScheduler {
    public static final String EXTRA_ID = "alarm_id", EXTRA_TRIGGER = "alarm_trigger";
    private static AlarmPlan.Holidays calendar;
    private AlarmScheduler() {}
    public static synchronized AlarmPlan.Holidays holidays(Context context) {
        if (calendar == null) {
            HolidayRepository repository = new HolidayRepository(context.getApplicationContext());
            Set<Integer> years = new HashSet<>();
            try { for (String file : context.getAssets().list("holidays"))
                if (file.matches("\\d{4}\\.json")) years.add(Integer.parseInt(file.substring(0, 4))); }
            catch (Exception ignored) { }
            calendar = new AlarmPlan.Holidays() {
                public boolean isHoliday(LocalDate date) {
                    HolidayRepository.HolidayStatus status = repository.statusOn(date.toString());
                    return years.contains(date.getYear()) && status != null && status.offDay;
                }
                public boolean isWorkday(LocalDate date) {
                    if (!years.contains(date.getYear())) return false;
                    HolidayRepository.HolidayStatus status = repository.statusOn(date.toString());
                    return status == null ? date.getDayOfWeek().getValue() <= 5 : !status.offDay;
                }
            };
        }
        return calendar;
    }
    public static long nextTrigger(AlarmSpec alarm, Context context, long now) {
        return AlarmPlan.next(alarm, now, ZoneId.systemDefault(), holidays(context));
    }
    public static long nextTrigger(int hour, int minute, long now) {
        ZoneId zone = ZoneId.systemDefault();
        return AlarmPlan.nextDate(hour, minute, now, zone).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli();
    }
    public static boolean canScheduleExact(Context context) { return manager(context).canScheduleExactAlarms(); }
    public static void schedule(Context context, AlarmSpec alarm) {
        long trigger = nextTrigger(alarm, context, System.currentTimeMillis());
        alarm.scheduledAt = trigger; new AlarmStore(context).save(alarm);
        AlarmManager manager = manager(context);
        PendingIntent operation = operation(context, alarm.id, trigger);
        manager.cancel(operation);
        if (trigger == 0) return;
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, AlarmEditorActivity.class).setData(uri(alarm.id))
                        .putExtra(EXTRA_ID, alarm.id), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (manager.canScheduleExactAlarms()) {
                manager.setAlarmClock(new AlarmManager.AlarmClockInfo(trigger, show), operation); return;
            }
        } catch (SecurityException ignored) { }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation);
    }
    public static void rescheduleAll(Context context) {
        cancelLegacy(context);
        for (AlarmSpec alarm : new AlarmStore(context).all()) schedule(context, alarm);
    }
    public static void schedule(Context context, int hour, int minute) { rescheduleAll(context); }
    public static void cancel(Context context, long id) { manager(context).cancel(operation(context, id, 0)); }
    public static void cancel(Context context) {
        cancelLegacy(context); for (AlarmSpec alarm : new AlarmStore(context).all()) cancel(context, alarm.id);
    }
    private static void cancelLegacy(Context context) {
        manager(context).cancel(PendingIntent.getBroadcast(context, 100, new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager(context).cancel(PendingIntent.getActivity(context, 102, new Intent(context, AlarmRingingActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }
    private static PendingIntent operation(Context context, long id, long trigger) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class)
                .setData(uri(id)).putExtra(EXTRA_ID, id).putExtra(EXTRA_TRIGGER, trigger),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static Uri uri(long id) { return Uri.parse("clockmods://alarm/" + id); }
    private static AlarmManager manager(Context context) { return context.getSystemService(AlarmManager.class); }
}
