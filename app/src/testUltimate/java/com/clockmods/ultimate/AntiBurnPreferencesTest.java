package com.clockmods.ultimate;

import org.junit.Assert;
import org.junit.Test;

public class AntiBurnPreferencesTest {
    @Test
    public void defaultsStartDisabledWithDocumentedValues() {
        Assert.assertFalse(AntiBurnPreferences.DEFAULT_ENABLED);
        Assert.assertEquals(10, AntiBurnPreferences.DEFAULT_PERIOD_MINUTES);
        Assert.assertEquals(4f, AntiBurnPreferences.DEFAULT_AMPLITUDE_DP, 0f);
        Assert.assertTrue(AntiBurnPreferences.DEFAULT_AUTO_DIM);
    }

    @Test
    public void periodAndAmplitudeAreBoundedToSupportedControls() {
        Assert.assertEquals(1, AntiBurnPreferences.normalizePeriod(1));
        Assert.assertEquals(10, AntiBurnPreferences.normalizePeriod(12));
        Assert.assertEquals(30, AntiBurnPreferences.normalizePeriod(30));
        Assert.assertEquals(60, AntiBurnPreferences.normalizePeriod(60));
        Assert.assertEquals(0f, AntiBurnPreferences.normalizeAmplitude(-1f), 0f);
        Assert.assertEquals(12f, AntiBurnPreferences.normalizeAmplitude(13f), 0f);
        Assert.assertEquals(4f, AntiBurnPreferences.normalizeAmplitude(Float.NaN), 0f);
    }
}
