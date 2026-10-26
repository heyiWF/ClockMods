package com.clockmods.ultimate.clock;

import com.clockmods.testing.MemorySharedPreferences;

import com.clockmods.sdk.clock.ClockState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class UltimateClockPreferencesTest {
    private MemorySharedPreferences stored;
    private UltimateClockPreferences preferences;

    @Before
    public void setUp() {
        stored = new MemorySharedPreferences();
        preferences = new UltimateClockPreferences(stored);
    }

    @Test
    public void defaultsMatchSettingsContract() {
        Assert.assertEquals("clockmods_ultimate_style",
                UltimateClockPreferences.PREFERENCES_NAME);
        Assert.assertEquals("style_id", UltimateClockPreferences.KEY_STYLE_ID);
        Assert.assertEquals("second_motion", UltimateClockPreferences.KEY_SECOND_MOTION);
        Assert.assertEquals("background_mode", UltimateClockPreferences.KEY_BACKGROUND_MODE);
        Assert.assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER,
                preferences.getStyleId());
        Assert.assertEquals(ClockState.SecondHandMotion.SWEEP,
                preferences.getSecondHandMotion());
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME,
                preferences.getBackgroundMode());
    }

    @Test
    public void settingsMotionStringsMapToSdkValues() {
        assertStoredMotion("smooth", ClockState.SecondHandMotion.SWEEP);
        assertStoredMotion("tick", ClockState.SecondHandMotion.TICK);
        assertStoredMotion("off", ClockState.SecondHandMotion.OFF);
        assertStoredMotion("unknown", UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION);
    }

    @Test
    public void sdkMotionValuesPersistAsSettingsStrings() {
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.SWEEP);
        Assert.assertEquals("smooth", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION + "__" + preferences.getStyleId(), null));
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.TICK);
        Assert.assertEquals("tick", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION + "__" + preferences.getStyleId(), null));
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF);
        Assert.assertEquals("off", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION + "__" + preferences.getStyleId(), null));
    }

    @Test
    public void dottedStyleIdsRoundTripAndBlankValuesUseDefault() {
        preferences.setStyleId(UltimateClockStyles.STYLE_ORBIT_NEON);
        Assert.assertEquals("orbit.neon", stored.getString(
                UltimateClockPreferences.KEY_STYLE_ID, null));
        Assert.assertEquals("orbit.neon", preferences.getStyleId());

        preferences.setStyleId("   ");
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                preferences.getStyleId());
    }

    @Test
    public void styleIdNormalizationRejectsEmptyAndOversizedValues() {
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId(null));
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId("  "));
        Assert.assertEquals("partner.meridian",
                UltimateClockPreferences.normalizeStyleId("  partner.meridian  "));
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 121; i++) oversized.append('a');
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId(oversized.toString()));
    }

    @Test
    public void restoreDefaultsWritesAllSettings() {
        preferences.setStyleId(UltimateClockStyles.STYLE_DIGITAL_GRID);
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF);
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE);

        preferences.restoreDefaults();

        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                preferences.getStyleId());
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION,
                preferences.getSecondHandMotion());
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_BACKGROUND_MODE,
                preferences.getBackgroundMode());
    }

    @Test
    public void backgroundModeAcceptsOnlyThreeStableValues() {
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_COLOR);
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_COLOR,
                preferences.getBackgroundMode());
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE);
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_IMAGE,
                preferences.getBackgroundMode());
        preferences.setBackgroundMode("unknown");
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME,
                preferences.getBackgroundMode());
    }

    @Test public void digitTransitionsAreIndependentAcrossThemesAndReset() {
        String bubbles = UltimateClockStyles.STYLE_BUBBLES;
        String orbit = UltimateClockStyles.STYLE_ORBIT;
        preferences.setDigitAnimationEnabled(bubbles, false);
        preferences.setDigitTransition(bubbles, "slide_up");
        preferences.setDigitTransition(orbit, "flip");

        UltimateClockPreferences reopened = new UltimateClockPreferences(stored);
        Assert.assertFalse(reopened.isDigitAnimationEnabled(bubbles));
        Assert.assertTrue(reopened.isDigitAnimationEnabled(orbit));
        Assert.assertEquals("slide_up", reopened.getDigitTransition(bubbles));
        Assert.assertEquals("flip", reopened.getDigitTransition(orbit));
        Assert.assertEquals("fade", reopened.getDigitTransition(
                UltimateClockStyles.STYLE_PRO_CLASSIC));

        reopened.restoreDefaults();
        Assert.assertTrue(reopened.isDigitAnimationEnabled(bubbles));
        Assert.assertEquals("fade", reopened.getDigitTransition(bubbles));
        Assert.assertEquals("fade", reopened.getDigitTransition(orbit));
    }

    @Test public void palettesPersistIndependentlyAndResetWithDefaults() {
        String orbit = UltimateClockStyles.STYLE_ORBIT;
        String dual = UltimateClockStyles.STYLE_DUAL_BLOCKS;
        preferences.setPalette(orbit, new ClockPalette(0xFFFFFFFF, 0xFF123456, 0xFF000000)
                .withGaussianBlur(true).withBlurStrength(75).withBlurBrightness(68));
        preferences.setPalette(dual, new ClockPalette(0xFF345678, 0xFFEEEEEE, 0xFFFF0000));
        UltimateClockPreferences reopened = new UltimateClockPreferences(stored);
        Assert.assertTrue(reopened.getPalette(orbit).gaussianBlur);
        Assert.assertEquals(75, reopened.getPalette(orbit).blurStrength);
        Assert.assertEquals(68, reopened.getPalette(orbit).blurBrightness);
        Assert.assertEquals(ClockPalette.DEFAULT.blurStrength, reopened.getPalette(dual).blurStrength);
        Assert.assertEquals(ClockPalette.DEFAULT.blurBrightness, reopened.getPalette(dual).blurBrightness);
        Assert.assertFalse(reopened.getPalette(dual).gaussianBlur);
        reopened.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_COLOR);
        Assert.assertTrue(reopened.getPalette(orbit).gaussianBlur);
        Assert.assertEquals(0xFFFFFFFF, reopened.getPalette(orbit).background);
        Assert.assertEquals(0xFF000000, reopened.getPalette(orbit).accent);
        Assert.assertEquals(0xFFEEEEEE, reopened.getPalette(dual).panel);
        Assert.assertEquals(ClockPalette.DEFAULT.background,
                reopened.getPalette(UltimateClockStyles.STYLE_BLEND).background);
        reopened.restoreDefaults();
        Assert.assertFalse(reopened.getPalette(orbit).gaussianBlur);
        Assert.assertEquals(ClockPalette.DEFAULT.blurStrength, reopened.getPalette(orbit).blurStrength);
        Assert.assertEquals(ClockPalette.DEFAULT.blurBrightness, reopened.getPalette(orbit).blurBrightness);
        Assert.assertEquals(ClockPalette.DEFAULT.background, reopened.getPalette(orbit).background);
        Assert.assertEquals(ClockPalette.DEFAULT.panel, reopened.getPalette(dual).panel);
    }

    private void assertStoredMotion(String storedValue, ClockState.SecondHandMotion expected) {
        stored.edit().putString(UltimateClockPreferences.KEY_SECOND_MOTION, storedValue).commit();
        Assert.assertEquals(expected, preferences.getSecondHandMotion());
    }

}
