package com.clockmods.ui;

import com.clockmods.background.ClockPreferences;

import org.junit.Assert;
import org.junit.Test;

public class StatusBarViewTest {
    @Test
    public void sizesBothLinesFromDensityAndScale() {
        Assert.assertEquals(14.5f, StatusBarView.calculateStatusTextSize(1f, 1f), 0.0001f);
        Assert.assertEquals(8.7f, StatusBarView.calculateStatusTextSize(1f, 0.60f), 0.0001f);
        Assert.assertEquals(32.625f, StatusBarView.calculateStatusTextSize(2.25f, 1f), 0.0001f);
    }

    @Test
    public void supportsScalingUpToTwoHundredPercent() {
        Assert.assertEquals(2f, ClockPreferences.MAX_STATUS_ICON_SCALE, 0f);
        Assert.assertEquals(29f,
                StatusBarView.calculateStatusTextSize(1f, ClockPreferences.MAX_STATUS_ICON_SCALE),
                0.0001f);
    }
}
