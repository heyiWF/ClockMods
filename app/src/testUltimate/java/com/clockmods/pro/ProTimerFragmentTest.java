package com.clockmods.pro;

import com.clockmods.pro.timer.CountdownTimers;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ProTimerFragmentTest {
    @Test public void keypadBuildsHoursMinutesAndSecondsFromTheRight() {
        assertEquals(0L, CountdownTimers.keypadMillis(""));
        assertEquals(90_000L, CountdownTimers.keypadMillis("90"));
        assertEquals(90_000L, CountdownTimers.keypadMillis("130"));
        assertEquals(359_999_000L, CountdownTimers.keypadMillis("995959"));
        assertEquals("01:02:04", CountdownTimers.time(3_723_001L));
    }
}
