package com.clockmods.sdk.style;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResponsiveTextPolicyTest {
    @Test public void extremeAndZoomedWindowsUseCompactComposition() {
        assertTrue(ResponsiveTextPolicy.needsCompactLayout(2400, 360, 1));
        assertTrue(ResponsiveTextPolicy.needsCompactLayout(360, 2400, 1));
        assertTrue(ResponsiveTextPolicy.needsCompactLayout(640, 480, 3));
        assertFalse(ResponsiveTextPolicy.needsCompactLayout(720, 1280, 2));
        assertFalse(ResponsiveTextPolicy.needsCompactLayout(1280, 720, 2));
    }

    @Test public void secondaryGrowthYieldsToPrimaryAndDisplayZoom() {
        float roomy = ResponsiveTextPolicy.secondaryLimit(1200, 800, 1, 1);
        assertTrue(ResponsiveTextPolicy.secondaryLimit(1200, 800, 1, 2) < roomy);
        assertTrue(ResponsiveTextPolicy.secondaryLimit(1200, 800, 3, 1) < roomy);
        assertEquals(ResponsiveTextPolicy.secondaryLimit(400, 300, 1, 1) * 2,
                ResponsiveTextPolicy.secondaryLimit(800, 600, 2, 1), .001f);
    }

    @Test public void allocatedLineNeverOverflowsEitherDimension() {
        for (float width : new float[] {0, 40, 320, 1600}) {
            for (float height : new float[] {0, 12, 80, 800}) {
                float size = ResponsiveTextPolicy.fitLine(300, 8, width, height, 1.3f);
                assertTrue(size >= 0 && size * 8 <= width + .001f);
                assertTrue(size * 1.3f <= height + .001f);
            }
        }
    }
}
