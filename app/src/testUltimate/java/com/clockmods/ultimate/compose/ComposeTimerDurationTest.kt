package com.clockmods.ultimate.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposeTimerDurationTest {
    @Test
    fun customDurationAcceptsMaximumValue() {
        assertEquals(359999000L, customDurationMillis(99, 59, 59))
    }

    @Test
    fun customDurationRejectsZeroAndOutOfRangeValues() {
        assertEquals(0L, customDurationMillis(0, 0, 0))
        assertEquals(0L, customDurationMillis(100, 0, 0))
        assertEquals(0L, customDurationMillis(1, 60, 0))
        assertEquals(0L, customDurationMillis(1, 0, 60))
    }
}
