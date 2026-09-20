package com.clockmods.ultimate.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchTimebaseTest {
    @Test
    fun stoppedReturnsAccumulatedValue() {
        assertEquals(1_500L, StopwatchTimebase.elapsed(1_500L, false, 10L, 20L, 90L, 100L))
    }

    @Test
    fun runningUsesMonotonicClock() {
        assertEquals(
            5_000L,
            StopwatchTimebase.elapsed(2_000L, true, 10_000L, 50_000L, 13_000L, 90_000L),
        )
    }

    @Test
    fun rebootFallsBackToWallClock() {
        assertEquals(
            8_000L,
            StopwatchTimebase.elapsed(2_000L, true, 50_000L, 100_000L, 2_000L, 106_000L),
        )
    }

    @Test
    fun backwardsWallClockNeverSubtractsTime() {
        assertEquals(
            2_000L,
            StopwatchTimebase.elapsed(2_000L, true, 50_000L, 100_000L, 2_000L, 90_000L),
        )
    }
}
