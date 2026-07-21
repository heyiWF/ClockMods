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
}