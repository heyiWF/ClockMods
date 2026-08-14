package com.clockmods.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class CalendarCarouselTimelineTest {
    @Test public void allCellsAdvanceOnTheSameBoundary() {
        long lastHoldMillisecond = CalendarCarouselTimeline.CYCLE_MS - 1L;
        assertEquals(0, CalendarCarouselTimeline.indexAt(lastHoldMillisecond, 2));
        assertEquals(0, CalendarCarouselTimeline.indexAt(lastHoldMillisecond, 3));

        long nextCycle = CalendarCarouselTimeline.CYCLE_MS;
        assertEquals(1, CalendarCarouselTimeline.indexAt(nextCycle, 2));
        assertEquals(1, CalendarCarouselTimeline.indexAt(nextCycle, 3));
        assertEquals(0L, CalendarCarouselTimeline.elapsedAt(nextCycle));
    }

    @Test public void itemCountsWrapIndependentlyWithoutChangingPhase() {
        long thirdCycle = CalendarCarouselTimeline.CYCLE_MS * 2L;
        assertEquals(0, CalendarCarouselTimeline.indexAt(thirdCycle, 2));
        assertEquals(2, CalendarCarouselTimeline.indexAt(thirdCycle, 3));
        assertEquals(0L, CalendarCarouselTimeline.elapsedAt(thirdCycle));
    }

    @Test public void rejectsEmptyCarousel() {
        assertThrows(IllegalArgumentException.class,
                () -> CalendarCarouselTimeline.indexAt(0L, 0));
    }
}
