package com.clockmods.pro;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class StopwatchTimebaseTest {
    @Test public void recreationRetainsTimeSpentAwayFromPage() {
        assertEquals(6500, StopwatchTimebase.elapsed(1500, true, 1000, 10000, 6000, 15000));
    }
    @Test public void pausedTimeDoesNotAdvance() {
        assertEquals(1500, StopwatchTimebase.elapsed(1500, false, 1000, 10000, 6000, 15000));
    }
    @Test public void rebootUsesWallClockFallback() {
        assertEquals(6500, StopwatchTimebase.elapsed(1500, true, 10000, 10000, 1000, 15000));
    }
    @Test public void wallClockAdjustmentDoesNotChangeLiveStopwatch() {
        assertEquals(2500, StopwatchTimebase.elapsed(1500, true, 1000, 10000, 2000, 5000));
    }
}
