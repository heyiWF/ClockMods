package com.clockmods.background;

import org.junit.Assert;
import org.junit.Test;

public class BackgroundDimScheduleTest {
    @Test
    public void supportsSameDayAndOvernightRanges() {
        Assert.assertTrue(BackgroundDimSchedule.isActive(13 * 60, 12 * 60, 18 * 60));
        Assert.assertFalse(BackgroundDimSchedule.isActive(19 * 60, 12 * 60, 18 * 60));
        Assert.assertTrue(BackgroundDimSchedule.isActive(23 * 60, 22 * 60, 6 * 60));
        Assert.assertTrue(BackgroundDimSchedule.isActive(5 * 60, 22 * 60, 6 * 60));
        Assert.assertFalse(BackgroundDimSchedule.isActive(12 * 60, 22 * 60, 6 * 60));
    }

    @Test
    public void treatsEqualTimesAsDisabledRange() {
        Assert.assertFalse(BackgroundDimSchedule.isActive(12 * 60, 6 * 60, 6 * 60));
    }
}