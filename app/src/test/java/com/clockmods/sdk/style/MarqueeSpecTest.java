package com.clockmods.sdk.style;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarqueeSpecTest {
    @Test public void sleepsThroughPausesAndStopsAfterSingleScroll() {
        MarqueeSpec spec = MarqueeSpec.DEFAULT;
        assertEquals(1000L, spec.loopFrameDelay(0, 80, 1));
        assertEquals(200L, spec.loopFrameDelay(800, 80, 1));
        assertEquals(16L, spec.loopFrameDelay(1000, 80, 1));
        assertEquals(1000L, spec.loopFrameDelay(3000, 80, 1));
        assertEquals(MarqueeSpec.IDLE, spec.scrollFrameDelay(3000, 80, 1));
        assertEquals(MarqueeSpec.IDLE, spec.scrollFrameDelay(0, 0, 1));
        // A rotating footer must schedule its transition even when its text does not overflow.
        assertEquals(3000L, Math.min(spec.holdMillis(0, 1), spec.scrollFrameDelay(0, 0, 1)));
    }

    @Test public void settingsChangeVelocityPauseAndSeamWithoutChangingTextSize() {
        MarqueeSpec fast = new MarqueeSpec(80, 2000, 48);
        assertEquals(1000L, fast.scrollMillis(160, 2));
        assertEquals(396f, fast.loopDistance(300, 20, 2), 0f);
        assertEquals(0f, fast.loopOffset(1999, 160, 2), 0f);
        assertEquals(80f, fast.loopOffset(2500, 160, 2), .01f);
        assertEquals(5000L, fast.holdMillis(160, 2));
        assertEquals(0f, fast.loopOffset(3000, 160, 2), 0f);
        assertEquals(2000L, fast.loopFrameDelay(3000, 160, 2));
        assertEquals(16L, new MarqueeSpec(80, 0, 12).loopFrameDelay(0, 160, 2));
    }

    @Test public void untrustedSettingsAreBoundedAndDefaultsRemainCompatible() {
        assertEquals(MarqueeSpec.DEFAULT, new MarqueeSpec(Float.NaN, 1000, Float.POSITIVE_INFINITY));
        MarqueeSpec bounded = new MarqueeSpec(0, -1, 999);
        assertEquals(10f, bounded.speedDpPerSecond, 0f);
        assertEquals(0L, bounded.pauseMillis);
        assertEquals(96f, bounded.gapDp, 0f);
        assertEquals(5000L, new MarqueeSpec(40, Long.MAX_VALUE, 24).pauseMillis);
        assertEquals(0f, bounded.scrollOffset(-1, 80, 1), 0f);
    }
}
