package com.clockmods.ultimate.clock;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClockPaletteTest {
    @Test public void everySurfaceHasReadableTextAcrossColorCube() {
        for (int r = 0; r <= 255; r += 17) {
            for (int g = 0; g <= 255; g += 17) {
                for (int b = 0; b <= 255; b += 17) {
                    int color = 0xFF000000 | r << 16 | g << 8 | b;
                    ClockPalette p = new ClockPalette(color, color ^ 0x00FFFFFF,
                            ClockPalette.mix(color, 0xFF808080, .5f));
                    readable(p.onBackground, p.background);
                    readable(p.mutedBackground, p.background);
                    readable(p.onPanel, p.panel);
                    readable(p.mutedPanel, p.panel);
                    readable(p.onAccent, p.accent);
                    readable(p.mutedAccent, p.accent);
                    readable(p.onPanelAlt, p.panelAlt);
                    readable(p.onBadge, p.badge);
                    assertTrue(ClockPalette.contrast(p.hand(), p.panelAlt) >= 3);
                }
            }
        }
    }

    @Test public void changesAreImmutableAndRoundTripThroughTokens() {
        ClockPalette changed = ClockPalette.DEFAULT.withColor(0, 0xFFFFFFFF);
        assertEquals(0xFF154974, ClockPalette.DEFAULT.background);
        ClockPalette restored = ClockPalette.fromTokens(changed.applyTo(
                UltimateClockStyles.sharedRegistry().resolveForApi(
                        UltimateClockStyles.STYLE_ORBIT, 35).getThemeTokens()));
        assertEquals(changed.background, restored.background);
        assertEquals(changed.panel, restored.panel);
        assertEquals(changed.accent, restored.accent);
    }

    private static void readable(int text, int surface) {
        assertNotEquals(0xFF000000, text);
        assertNotEquals(0xFFFFFFFF, text);
        assertTrue(Integer.toHexString(text) + " on " + Integer.toHexString(surface),
                ClockPalette.contrast(text, surface) >= 4.5);
    }

    @Test public void glassTextRemainsReadableOverEveryPossibleImageBrightness() {
        ClockPalette p = ClockPalette.GLASS;
        int[] surfaces = {p.background, p.panel, p.accent, p.panelAlt, p.badge};
        int[] text = {p.onBackground, p.onPanel, p.onAccent, p.onPanelAlt, p.onBadge};
        for (int i = 0; i < surfaces.length; i++) {
            for (int brightness = 0; brightness <= 255; brightness++) {
                int tinted = ClockPalette.mix(0xFF000000, surfaces[i], brightness / 255f);
                readable(text[i], tinted);
                if (i == 0) readable(p.mutedBackground, tinted);
                if (i == 1) readable(p.mutedPanel, tinted);
                if (i == 2) readable(p.mutedAccent, tinted);
            }
        }
        assertTrue(ClockPalette.contrast(p.hand(), p.panelAlt) >= 3);
    }

    @Test public void blurSurvivesPaletteAndTypographyTokenCopies() {
        ClockPalette p = ClockPalette.DEFAULT.withGaussianBlur(true).withColor(1, 0xFFFFFFFF)
                .withBlurStrength(80).withBlurBrightness(70);
        ClockPalette restored = ClockPalette.fromTokens(p.applyTo(
                com.clockmods.sdk.clock.ClockThemeTokens.builder().build()).toBuilder().build());
        assertTrue(restored.gaussianBlur);
        assertEquals(0xFFFFFFFF, restored.panel);
        assertEquals(restored.panel, restored.withGaussianBlur(false).panel);
        assertEquals(80, restored.blurStrength);
        assertEquals(70, restored.withGaussianBlur(false).blurBrightness);
        assertEquals(0, p.withBlurStrength(-1).blurStrength);
        assertEquals(100, p.withBlurBrightness(101).blurBrightness);
    }

    @Test public void tintedGlassTextHasContrastAtEverySliderBrightness() {
        for (int r = 0; r <= 255; r += 51) {
            for (int g = 0; g <= 255; g += 51) {
                for (int b = 0; b <= 255; b += 51) {
                    int tint = 0xFF000000 | r << 16 | g << 8 | b;
                    int previousCenter = -1;
                    for (int brightness = 0; brightness <= 100; brightness++) {
                        ClockPalette p = ClockPalette.glass(brightness, tint);
                        assertNotEquals(0xFF000000, p.onPanel);
                        assertNotEquals(0xFFFFFFFF, p.onPanel);
                        int[] range = GaussianGlass.brightnessRange(brightness, p.onPanel);
                        assertTrue(range[0] >= 0 && range[1] <= 255 && range[0] <= range[1]);
                        for (int channel : range) {
                            int surface = 0xFF000000 | channel << 16 | channel << 8 | channel;
                            readable(p.onPanel, surface);
                            readable(p.mutedPanel, surface);
                        }
                        int center = (range[0] + range[1]) / 2;
                        assertTrue(center >= previousCenter);
                        previousCenter = center;
                    }
                }
            }
        }
        assertNotEquals(ClockPalette.glass(25, 0xFF2050E0).onPanel,
                ClockPalette.glass(25, 0xFFE08020).onPanel);
        assertNotEquals(ClockPalette.glass(75, 0xFF2050E0).onPanel,
                ClockPalette.glass(75, 0xFFE08020).onPanel);
    }
}
