package com.clockmods.pro;

import org.junit.Assert;
import org.junit.Test;

public final class MonthGestureRegionTest {
    @Test
    public void regionDecisionStaysLockedForTouchStream() {
        MonthGestureRegion region = new MonthGestureRegion();

        region.start(false);
        Assert.assertFalse(region.acceptsGesture());
        // A MOVE may cross into the calendar, but only another DOWN/start can change ownership.
        Assert.assertFalse(region.acceptsGesture());

        region.finish();
        region.start(true);
        Assert.assertTrue(region.acceptsGesture());
        region.finish();
        Assert.assertFalse(region.acceptsGesture());
    }

    @Test
    public void containsUsesPanelCoordinateBounds() {
        Assert.assertTrue(MonthGestureRegion.contains(120, 40, 520, 180, 120f, 40f));
        Assert.assertTrue(MonthGestureRegion.contains(120, 40, 520, 180, 519.9f, 179.9f));
        Assert.assertFalse(MonthGestureRegion.contains(120, 40, 520, 180, 119.9f, 100f));
        Assert.assertFalse(MonthGestureRegion.contains(120, 40, 520, 180, 520f, 100f));
        Assert.assertFalse(MonthGestureRegion.contains(120, 40, 520, 180, 300f, 180f));
    }
}
