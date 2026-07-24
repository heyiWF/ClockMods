package com.clockmods.pro.chime;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class HourlyChimeControllerTest {
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    @Test
    public void startsDuringFinalTwoSecondsBeforeHour() {
        Calendar now = timeAt(13, 59, 58, 250);

        long chimeAtMillis = HourlyChimeController.upcomingChimeAtMillis(now);

        Calendar chimeAt = Calendar.getInstance(TIME_ZONE);
        chimeAt.setTimeInMillis(chimeAtMillis);
        assertEquals(14, chimeAt.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, chimeAt.get(Calendar.MINUTE));
        assertEquals(0, chimeAt.get(Calendar.SECOND));
        assertEquals(0, chimeAt.get(Calendar.MILLISECOND));
    }

    @Test
    public void doesNotStartBeforeFinalTwoSecondsOrAfterHour() {
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(timeAt(13, 59, 57, 999)));
        assertEquals(Long.MIN_VALUE,
                HourlyChimeController.upcomingChimeAtMillis(timeAt(14, 0, 0, 0)));
    }

    private static Calendar timeAt(int hour, int minute, int second, int millis) {
        Calendar time = Calendar.getInstance(TIME_ZONE);
        time.set(2026, Calendar.JULY, 24, hour, minute, second);
        time.set(Calendar.MILLISECOND, millis);
        return time;
    }
}