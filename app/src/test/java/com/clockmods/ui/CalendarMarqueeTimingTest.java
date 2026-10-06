package com.clockmods.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalendarMarqueeTimingTest {
    @Test public void eachBeltLapPausesWithoutJumpingAtTheSeam() {
        float distance = CalendarMarqueeTiming.loopDistance(300f, 20f, 2f);
        assertEquals(348f, distance, .01f);
        long scroll = CalendarMarqueeTiming.scrollMillis(distance, 2f);
        long cycle = 1000L + scroll;
        assertEquals(0f, CalendarMarqueeTiming.loopOffset(999L, distance, 2f), 0f);
        assertTrue(CalendarMarqueeTiming.loopOffset(2000L, distance, 2f) > 0f);
        assertTrue(CalendarMarqueeTiming.loopOffset(cycle - 1, distance, 2f) > distance - .1f);
        assertEquals(0f, CalendarMarqueeTiming.loopOffset(cycle, distance, 2f), 0f);
        assertEquals(0f, CalendarMarqueeTiming.loopOffset(cycle + 999, distance, 2f), 0f);
        assertEquals(0f, CalendarMarqueeTiming.loopOffset(-10L, distance, 2f), 0f);
    }

    @Test public void footerRestsAtTheTailBeforeAdvancing() {
        long hold = CalendarMarqueeTiming.holdMillis(300f, 2f);
        assertEquals(5750L, hold);
        assertEquals(0f, CalendarMarqueeTiming.scrollOffset(999L, 300f, 2f), 0f);
        assertEquals(300f, CalendarMarqueeTiming.scrollOffset(hold - 1000L, 300f, 2f), 0f);
        assertEquals(300f, CalendarMarqueeTiming.scrollOffset(hold - 1L, 300f, 2f), 0f);
        assertEquals(3000L, CalendarMarqueeTiming.holdMillis(0f, 2f));
        assertEquals(1500L, CalendarMarqueeTiming.scrollMillis(60f, 1f));
        assertEquals(1500L, CalendarMarqueeTiming.scrollMillis(120f, 2f));
    }
}
