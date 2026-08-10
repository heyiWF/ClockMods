package com.clockmods.pro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProTimerFragmentTest {
    @Test
    public void customDurationAcceptsMaximumValue() {
        assertEquals(359999000L, ProTimerFragment.customDurationMillis(99, 59, 59));
    }

    @Test
    public void customDurationRejectsZeroAndOutOfRangeValues() {
        assertEquals(0L, ProTimerFragment.customDurationMillis(0, 0, 0));
        assertEquals(0L, ProTimerFragment.customDurationMillis(100, 0, 0));
        assertEquals(0L, ProTimerFragment.customDurationMillis(1, 60, 0));
        assertEquals(0L, ProTimerFragment.customDurationMillis(1, 0, 60));
    }
}