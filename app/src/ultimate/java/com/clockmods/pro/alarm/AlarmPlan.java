package com.clockmods.pro.alarm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Calendar recurrence in the device time zone; never advances dates using 24-hour milliseconds. */
public final class AlarmPlan {
    public interface Holidays {
        boolean isHoliday(LocalDate date);
        boolean isWorkday(LocalDate date);
    }
    private AlarmPlan() {}
    public static int minutesOn(AlarmSpec alarm, LocalDate date, Holidays holidays) {
        if (alarm.excludedDates.contains(date)) return -1;
        boolean extra = alarm.extraDates.contains(date);
        boolean occurs;
        switch (alarm.repeat) {
            case WEEKLY: occurs = (alarm.weekdays & (1 << (date.getDayOfWeek().getValue() - 1))) != 0; break;
            case HOLIDAYS: occurs = holidays.isHoliday(date); break;
            case WORKDAYS: occurs = holidays.isWorkday(date); break;
            case SHIFT:
                long days = ChronoUnit.DAYS.between(alarm.cycleStart, date);
                int shift = days < 0 ? -1 : alarm.cycleMinutes[(int) (days % alarm.cycleLength)];
                return shift >= 0 ? shift : extra ? alarm.hour * 60 + alarm.minute : -1;
            default: occurs = date.equals(alarm.onceDate);
        }
        return occurs || extra ? alarm.hour * 60 + alarm.minute : -1;
    }
    public static long next(AlarmSpec alarm, long now, ZoneId zone, Holidays holidays) {
        if (!alarm.enabled && alarm.snoozeAt <= now) return 0;
        long result = alarm.snoozeAt > now && !alarm.excludedDates.contains(Instant.ofEpochMilli(alarm.snoozeAt).atZone(zone).toLocalDate()) ? alarm.snoozeAt : 0;
        if (!alarm.enabled) return result;
        long after = Math.max(now, alarm.lastFiredAt);
        LocalDate today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        LocalDate searchStart = alarm.repeat == AlarmSpec.Repeat.SHIFT && alarm.cycleStart.isAfter(today) ? alarm.cycleStart : today;
        // Bound recurring searches, but separately evaluate arbitrarily distant explicit dates.
        for (int offset = 0; alarm.repeat != AlarmSpec.Repeat.ONCE && offset <= 366 * 5; offset++) {
            LocalDate date = searchStart.plusDays(offset);
            int minutes = minutesOn(alarm, date, holidays);
            if (minutes < 0) continue;
            long time = date.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone).toInstant().toEpochMilli();
            if (time > after) { result = earlier(result, time); break; }
        }
        for (LocalDate date : alarm.extraDates) {
            if (date.isBefore(today) || alarm.excludedDates.contains(date)) continue;
            int minutes = minutesOn(alarm, date, holidays);
            long time = date.atTime(minutes / 60, minutes % 60).atZone(zone).toInstant().toEpochMilli();
            if (time > after) result = earlier(result, time);
        }
        if (alarm.repeat == AlarmSpec.Repeat.ONCE && !alarm.onceDate.isBefore(today)
                && !alarm.excludedDates.contains(alarm.onceDate)) {
            long time = alarm.onceDate.atTime(alarm.hour, alarm.minute).atZone(zone).toInstant().toEpochMilli();
            if (time > after) result = earlier(result, time);
        }
        return result;
    }
    public static LocalDate nextDate(int hour, int minute, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        return today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli() > now ? today : today.plusDays(1);
    }
    private static long earlier(long first, long second) { return first == 0 ? second : Math.min(first, second); }
}
