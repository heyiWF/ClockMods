package com.clockmods.pro.chime;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HourlyChimeControllerTest {
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    @Test
    public void startsDuringFinalTwoSecondsBeforeHour() {
        Calendar now = timeAt(13, 59, 58, 250);

        long chimeAtMillis = HourlyChimeController.upcomingChimeAtMillis(now, true, false);

        Calendar chimeAt = Calendar.getInstance(TIME_ZONE);
        chimeAt.setTimeInMillis(chimeAtMillis);
        assertEquals(14, chimeAt.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, chimeAt.get(Calendar.MINUTE));
        assertEquals(0, chimeAt.get(Calendar.SECOND));
        assertEquals(0, chimeAt.get(Calendar.MILLISECOND));
    }

    @Test
    public void startsDuringFinalTwoSecondsBeforeHalfHourWhenEnabled() {
        Calendar now = timeAt(13, 29, 58, 250);

        long chimeAtMillis = HourlyChimeController.upcomingChimeAtMillis(now, true, true);

        Calendar chimeAt = Calendar.getInstance(TIME_ZONE);
        chimeAt.setTimeInMillis(chimeAtMillis);
        assertEquals(13, chimeAt.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, chimeAt.get(Calendar.MINUTE));
        assertEquals(0, chimeAt.get(Calendar.SECOND));
        assertEquals(0, chimeAt.get(Calendar.MILLISECOND));
    }

    @Test
    public void halfHourDoesNotStartWhenItsOptionIsDisabled() {
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(
                        timeAt(13, 29, 59, 0), true, false));
    }

    @Test
    public void halfHourCanRunIndependentlyWhenHourlyIsDisabled() {
        Calendar now = timeAt(13, 29, 58, 250);
        assertEquals(13 * 60 + 30,
                minuteOf(HourlyChimeController.upcomingChimeAtMillis(now, false, true)));
    }

    @Test
    public void noChimeIsScheduledWhenBothOptionsAreDisabled() {
        Calendar now = timeAt(13, 59, 59, 0);
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(now, false, false));
    }

    @Test
    public void doesNotStartBeforeFinalTwoSecondsOrAfterHour() {
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(
                        timeAt(13, 59, 57, 999), true, true));
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(
                        timeAt(14, 0, 0, 0), true, true));
    }

    @Test
    public void quietHoursHandleNormalAndOvernightBoundaries() {
        assertTrue(HourlyChimeController.isQuietAtMinute(22 * 60, 22 * 60, 7 * 60));
        assertTrue(HourlyChimeController.isQuietAtMinute(6 * 60 + 59, 22 * 60, 7 * 60));
        assertFalse(HourlyChimeController.isQuietAtMinute(7 * 60, 22 * 60, 7 * 60));
        assertTrue(HourlyChimeController.isQuietAtMinute(12 * 60, 8 * 60, 18 * 60));
        assertFalse(HourlyChimeController.isQuietAtMinute(18 * 60, 8 * 60, 18 * 60));
        assertTrue(HourlyChimeController.isQuietAtMinute(12 * 60, 0, 0));
    }

    @Test
    public void configuredTimeZoneIsUsedWhenPresent() {
        assertEquals("Asia/Shanghai",
                HourlyChimeController.resolveTimeZone("Asia/Shanghai").getID());
    }

    private static int minuteOf(long millis) {
        Calendar value = Calendar.getInstance(TIME_ZONE);
        value.setTimeInMillis(millis);
        return value.get(Calendar.HOUR_OF_DAY) * 60 + value.get(Calendar.MINUTE);
    }

    private static Calendar timeAt(int hour, int minute, int second, int millis) {
        Calendar time = Calendar.getInstance(TIME_ZONE);
        time.set(2026, Calendar.JULY, 24, hour, minute, second);
        time.set(Calendar.MILLISECOND, millis);
        return time;
    }
}
