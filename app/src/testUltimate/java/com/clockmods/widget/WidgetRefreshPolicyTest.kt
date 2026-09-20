package com.clockmods.widget

import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.update.WidgetMidnightScheduler
import com.clockmods.widget.update.WidgetRefreshPolicy
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshPolicyTest {
    @Test
    fun onlyUsefulBoundedWork() {
        assertFalse(WidgetRefreshPolicy.shouldSchedule(0))
        assertTrue(WidgetRefreshPolicy.shouldSchedule(1))
        assertFalse(WidgetRefreshPolicy.shouldRetry(false, true, 0))
        assertFalse(WidgetRefreshPolicy.shouldRetry(true, false, 0))
        assertTrue(WidgetRefreshPolicy.shouldRetry(true, true, 1))
        assertFalse(WidgetRefreshPolicy.shouldRetry(true, true, 2))
    }

    @Test
    fun midnightAccountsForDstAndEveryInstance() {
        val now = Instant.parse("2026-03-08T05:00:00Z").toEpochMilli()
        val newYork = WidgetConfig.builder(1, WidgetKind.DIGITAL)
            .useSystemTimeZone(false)
            .timeZoneId("America/New_York")
            .build()
        assertEquals(23 * 3_600_000L, WidgetMidnightScheduler.nextMidnight(now, listOf(newYork)) - now)
        val shanghai = newYork.toBuilder().appWidgetId(2).timeZoneId("Asia/Shanghai").build()
        assertEquals(
            Instant.parse("2026-03-08T16:00:00Z").toEpochMilli(),
            WidgetMidnightScheduler.nextMidnight(now, listOf(newYork, shanghai)),
        )
        assertEquals(Long.MAX_VALUE, WidgetMidnightScheduler.nextMidnight(now, emptyList()))
    }
}
