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
        assertTrue(Integer.toHexString(text) + " on " + Integer.toHexString(surface),
                ClockPalette.contrast(text, surface) >= 4.5);
    }
}
