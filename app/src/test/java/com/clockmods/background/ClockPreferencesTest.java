package com.clockmods.background;

import org.junit.Assert;
import org.junit.Test;

public class ClockPreferencesTest {
    @Test
    public void normalizesStatusIconScaleToSupportedRange() {
        Assert.assertEquals(ClockPreferences.MIN_STATUS_ICON_SCALE,
                ClockPreferences.normalizeStatusIconScale(0.1f), 0f);
        Assert.assertEquals(0.85f,
                ClockPreferences.normalizeStatusIconScale(0.85f), 0f);
        Assert.assertEquals(ClockPreferences.MAX_STATUS_ICON_SCALE,
                ClockPreferences.normalizeStatusIconScale(2f), 0f);
    }

    @Test
    public void invalidStatusIconScaleFallsBackToDefault() {
        Assert.assertEquals(ClockPreferences.DEFAULT_STATUS_ICON_SCALE,
                ClockPreferences.normalizeStatusIconScale(Float.NaN), 0f);
        Assert.assertEquals(ClockPreferences.MIN_STATUS_ICON_SCALE,
                ClockPreferences.normalizeStatusIconScale(Float.NEGATIVE_INFINITY), 0f);
        Assert.assertEquals(ClockPreferences.MAX_STATUS_ICON_SCALE,
                ClockPreferences.normalizeStatusIconScale(Float.POSITIVE_INFINITY), 0f);
    }
}
