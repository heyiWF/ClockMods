package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class StatusBarViewTest {
    @Test
    public void normalizesDeviceWifiLevelsToFiveIcons() {
        Assert.assertEquals(0, StatusBarView.normalizeSignalLevel(0, 4));
        Assert.assertEquals(1, StatusBarView.normalizeSignalLevel(1, 4));
        Assert.assertEquals(2, StatusBarView.normalizeSignalLevel(2, 4));
        Assert.assertEquals(3, StatusBarView.normalizeSignalLevel(3, 4));
        Assert.assertEquals(4, StatusBarView.normalizeSignalLevel(4, 4));
    }

    @Test
    public void normalizesDifferentDeviceMaximumsAndClampsValues() {
        Assert.assertEquals(4, StatusBarView.normalizeSignalLevel(5, 5));
        Assert.assertEquals(2, StatusBarView.normalizeSignalLevel(3, 6));
        Assert.assertEquals(4, StatusBarView.normalizeSignalLevel(8, 6));
        Assert.assertEquals(0, StatusBarView.normalizeSignalLevel(-1, 6));
        Assert.assertEquals(0, StatusBarView.normalizeSignalLevel(1, 0));
    }

    @Test
    public void scalesStatusGroupFromTheExistingIconHeight() {
        Assert.assertEquals(14f,
                StatusBarView.calculateStatusIconHeight(28f, 1f, 1f), 0.0001f);
        Assert.assertEquals(8.4f,
                StatusBarView.calculateStatusIconHeight(28f, 1f, 0.60f), 0.0001f);
        Assert.assertEquals(17.5f,
                StatusBarView.calculateStatusIconHeight(28f, 1f, 1.25f), 0.0001f);
    }

    @Test
    public void maximumScaleKeepsBatteryBoxInsideFixedHeightViews() {
        float iconHeight = StatusBarView.calculateStatusIconHeight(28f, 1f, 1.25f);

        Assert.assertTrue(iconHeight * 1.55f <= 28f);
    }
}
