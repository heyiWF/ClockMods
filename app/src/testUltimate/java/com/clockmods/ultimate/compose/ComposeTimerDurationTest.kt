package com.clockmods.ultimate.compose

import com.clockmods.pro.timer.CountdownTimers
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposeTimerDurationTest {
    @Test fun keypadBuildsHoursMinutesAndSecondsFromTheRight() {
        assertEquals(0L, CountdownTimers.keypadMillis(""))
        assertEquals(90_000L, CountdownTimers.keypadMillis("90"))
        assertEquals(90_000L, CountdownTimers.keypadMillis("130"))
        assertEquals(359_999_000L, CountdownTimers.keypadMillis("995959"))
        assertEquals("01:02:04", CountdownTimers.time(3_723_001L))
    }
}
