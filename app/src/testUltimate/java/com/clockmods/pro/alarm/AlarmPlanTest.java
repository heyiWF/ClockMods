package com.clockmods.pro.alarm;

import org.junit.Test;
import org.json.JSONObject;
import java.time.*;
import static org.junit.Assert.*;

public class AlarmPlanTest {
    private final ZoneId zone = ZoneId.of("Asia/Shanghai");
    private final AlarmPlan.Holidays holidays = new AlarmPlan.Holidays() {
        public boolean isHoliday(LocalDate day) { return day.equals(LocalDate.of(2026, 10, 1)); }
        public boolean isWorkday(LocalDate day) { return day.equals(LocalDate.of(2026, 10, 10))
                || (day.getDayOfWeek().getValue() <= 5 && !isHoliday(day)); }
    };
    private AlarmSpec alarm() { AlarmSpec a = new AlarmSpec(); a.hour = 7; a.minute = 30; a.onceDate = LocalDate.of(2026, 10, 1); return a; }
    private long time(String iso) { return LocalDateTime.parse(iso).atZone(zone).toInstant().toEpochMilli(); }
    @Test public void onceUsesChosenDateAndDoesNotBecomeDaily() {
        AlarmSpec a = alarm(); assertEquals(time("2026-10-01T07:30"), AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays));
        assertEquals(0, AlarmPlan.next(a, time("2026-10-01T07:30"), zone, holidays));
    }
    @Test public void weekdaysAreMondayBasedAndSkipSameMinute() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.WEEKLY; a.weekdays = 1;
        assertEquals(time("2026-10-05T07:30"), AlarmPlan.next(a, time("2026-10-01T12:00"), zone, holidays));
        assertEquals(time("2026-10-12T07:30"), AlarmPlan.next(a, time("2026-10-05T07:30"), zone, holidays));
    }
    @Test public void exclusionsWinOverExtraAndRepeatingDates() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.WEEKLY; a.weekdays = 127;
        a.extraDates.add(a.onceDate); a.excludedDates.add(a.onceDate);
        assertEquals(time("2026-10-02T07:30"), AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays));
    }
    @Test public void additionalDatesWorkAcrossMonthsAndAfterOnceDate() {
        AlarmSpec a = alarm(); a.extraDates.add(LocalDate.of(2026, 12, 20)); a.extraDates.add(LocalDate.of(2040, 1, 2));
        assertEquals(time("2026-12-20T07:30"), AlarmPlan.next(a, time("2026-10-02T12:00"), zone, holidays));
        assertEquals(time("2040-01-02T07:30"), AlarmPlan.next(a, time("2026-12-21T12:00"), zone, holidays));
    }
    @Test public void statutoryWorkdayIncludesAdjustedSaturday() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.WORKDAYS;
        assertEquals(time("2026-10-10T07:30"), AlarmPlan.next(a, time("2026-10-09T12:00"), zone, holidays));
    }
    @Test public void statutoryHolidayDoesNotIncludeEveryWeekend() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.HOLIDAYS;
        assertEquals(time("2026-10-01T07:30"), AlarmPlan.next(a, time("2026-09-25T12:00"), zone, holidays));
    }
    @Test public void shiftStartsOnAnchorAndUsesEachDaysOwnTime() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleStart = LocalDate.of(2026, 11, 10); a.cycleLength = 2;
        a.cycleMinutes[0] = 9 * 60; a.cycleMinutes[1] = 17 * 60;
        assertEquals(time("2026-11-10T09:00"), AlarmPlan.next(a, time("2026-11-01T12:00"), zone, holidays));
        assertEquals(time("2026-11-11T17:00"), AlarmPlan.next(a, time("2026-11-10T10:00"), zone, holidays));
        assertEquals(time("2026-11-12T09:00"), AlarmPlan.next(a, time("2026-11-11T18:00"), zone, holidays));
    }
    @Test public void sixtyTwoDayCycleKeepsLastDayWithoutIntegerBitmaskOverflow() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleStart = LocalDate.of(2026, 11, 10); a.cycleLength = 62;
        java.util.Arrays.fill(a.cycleMinutes, -1); a.cycleMinutes[61] = 17 * 60 + 5;
        long expected = a.cycleStart.plusDays(61).atTime(17, 5).atZone(zone).toInstant().toEpochMilli();
        assertEquals(expected, AlarmPlan.next(a, time("2026-11-10T00:00"), zone, holidays));
        assertEquals(a.cycleStart.plusDays(123).atTime(17, 5).atZone(zone).toInstant().toEpochMilli(), AlarmPlan.next(a, expected, zone, holidays));
    }
    @Test public void extrasCanEnableShiftRestDaysAndExclusionsStillWin() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleStart = a.onceDate;
        java.util.Arrays.fill(a.cycleMinutes, -1); a.extraDates.add(a.onceDate);
        assertEquals(time("2026-10-01T07:30"), AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays));
        a.excludedDates.add(a.onceDate); assertEquals(0, AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays));
    }
    @Test public void snoozeSurvivesCompletionOfOneTimeAlarm() {
        AlarmSpec a = alarm(); a.enabled = false; a.snoozeAt = time("2026-10-01T07:40");
        assertEquals(a.snoozeAt, AlarmPlan.next(a, time("2026-10-01T07:31"), zone, holidays));
        assertEquals(0, AlarmPlan.next(a, a.snoozeAt, zone, holidays));
    }
    @Test public void disabledAlarmHasNoOccurrence() { AlarmSpec a = alarm(); a.enabled = false;
        assertEquals(0, AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays)); }
    @Test public void daylightSavingGapUsesLocalCalendarInsteadOfAddingTwentyFourHours() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.WEEKLY; a.weekdays = 127; a.hour = 2; a.minute = 30;
        ZoneId ny = ZoneId.of("America/New_York");
        long before = ZonedDateTime.of(2026, 3, 7, 3, 0, 0, 0, ny).toInstant().toEpochMilli();
        ZonedDateTime next = Instant.ofEpochMilli(AlarmPlan.next(a, before, ny, holidays)).atZone(ny);
        assertEquals(LocalDate.of(2026, 3, 8), next.toLocalDate()); assertEquals(LocalTime.of(3, 30), next.toLocalTime());
        ZonedDateTime following = Instant.ofEpochMilli(AlarmPlan.next(a, next.toInstant().toEpochMilli(), ny, holidays)).atZone(ny);
        assertEquals(LocalTime.of(2, 30), following.toLocalTime());
    }
    @Test public void daylightSavingOverlapDoesNotRingTwice() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.WEEKLY; a.weekdays = 127; a.hour = 1; a.minute = 30;
        ZoneId ny = ZoneId.of("America/New_York");
        long first = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, ny).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
        assertEquals(LocalDate.of(2026, 11, 2), Instant.ofEpochMilli(AlarmPlan.next(a, first, ny, holidays)).atZone(ny).toLocalDate());
    }
    @Test public void configurationRoundTripsWithoutLosingHighCycleDaysOrDateExceptions() throws Exception {
        AlarmSpec a = alarm(); a.id = 123; a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleLength = 62; a.cycleMinutes[61] = 23 * 60 + 59;
        a.ringtone = "content://media/external/audio/media/43"; a.extraDates.add(LocalDate.of(2035, 1, 1)); a.excludedDates.add(a.onceDate);
        AlarmSpec b = AlarmSpec.fromJson(new JSONObject(a.toJson().toString()));
        assertEquals(a.toJson().toString(), b.toJson().toString());
        b.extraDates.clear(); assertEquals(1, a.extraDates.size());
    }
    @Test public void extraDateRetainsAnActiveShiftDaysOwnTime() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleStart = a.onceDate;
        a.cycleMinutes[0] = 600; a.extraDates.add(a.onceDate);
        assertEquals(time("2026-10-01T10:00"), AlarmPlan.next(a, time("2026-09-30T12:00"), zone, holidays));
    }
    @Test public void snoozeCannotOverrideAnExcludedDate() {
        AlarmSpec a = alarm(); a.enabled = false; a.snoozeAt = time("2026-10-02T00:05");
        a.excludedDates.add(LocalDate.of(2026, 10, 2));
        assertEquals(0, AlarmPlan.next(a, time("2026-10-01T23:55"), zone, holidays));
    }
    @Test public void distantShiftAnchorCanBeScheduledWithoutGuessingToday() {
        AlarmSpec a = alarm(); a.repeat = AlarmSpec.Repeat.SHIFT; a.cycleStart = LocalDate.of(2040, 1, 1);
        assertEquals(time("2040-01-01T07:30"), AlarmPlan.next(a, time("2026-10-01T12:00"), zone, holidays));
    }
}
