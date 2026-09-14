package com.clockmods.ultimate.clock;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GaussianGlassTest {
    @Test public void missingOrSolidBackgroundKeepsTheSavedSolidPalette() {
        com.clockmods.sdk.clock.ClockThemeTokens theme = ClockPalette.DEFAULT
                .withGaussianBlur(true).applyTo(com.clockmods.sdk.clock.ClockThemeTokens.builder().build());
        for (com.clockmods.sdk.clock.ClockBackground background : new com.clockmods.sdk.clock.ClockBackground[] {
                null, com.clockmods.sdk.clock.ClockBackground.theme(false),
                com.clockmods.sdk.clock.ClockBackground.color(0xFFFFFFFF, false),
                com.clockmods.sdk.clock.ClockBackground.image(null, 0xFF000000, false)}) {
            com.clockmods.sdk.clock.ClockRenderContext context = new com.clockmods.sdk.clock.ClockRenderContext(
                    0, 0, 640, 360, 1, 1, 0, background);
            assertNull(GaussianGlass.create(context, theme));
        }
        assertEquals(ClockPalette.DEFAULT.panel, ClockPalette.fromTokens(theme).panel);
    }

    @Test public void uniformColorsAndTinyImagesHaveNoDarkEdges() {
        for (int color : new int[] {0xFF000000, 0xFFFFFFFF, 0xFF2389AB}) {
            int[] pixels = new int[35];
            Arrays.fill(pixels, color);
            assertArrayEquals(pixels, GaussianGlass.blurPixels(pixels, 5, 7));
            assertArrayEquals(new int[] {color}, GaussianGlass.blurPixels(new int[] {color}, 1, 1));
        }
    }

    @Test public void sharpEdgeBecomesSmoothSymmetricGaussianTransition() {
        int[] pixels = new int[31];
        Arrays.fill(pixels, 0xFF000000);
        Arrays.fill(pixels, 15, 31, 0xFFFFFFFF);
        int[] result = GaussianGlass.blurPixels(pixels, 31, 1);
        assertEquals(0xFF000000, result[0]);
        assertEquals(0xFFFFFFFF, result[30]);
        assertTrue((result[14] & 255) > 0);
        assertTrue((result[15] & 255) < 255);
        assertEquals(255, (result[14] & 255) + (result[15] & 255));
        for (int i = 1; i < result.length; i++) {
            assertTrue((result[i] & 255) >= (result[i - 1] & 255));
        }
        // The source remains untouched, and the same convolution applies vertically.
        assertEquals(0xFF000000, pixels[14]);
        assertArrayEquals(result, GaussianGlass.blurPixels(pixels, 1, 31));
    }

    @Test public void transparentWhiteDoesNotProduceBrightHalos() {
        assertArrayEquals(new int[] {0xFF000000},
                GaussianGlass.blurPixels(new int[] {0x00FFFFFF}, 1, 1));
    }

    @Test public void strengthZeroPreservesDetailAndHigherStrengthSpreadsTheBlur() {
        int[] pixels = new int[81];
        Arrays.fill(pixels, 0xFF000000);
        pixels[40] = 0xFFFFFFFF;
        assertArrayEquals(pixels, GaussianGlass.blurPixels(pixels, 81, 1, 0));
        int[] soft = GaussianGlass.blurPixels(pixels, 81, 1, 25);
        int[] strong = GaussianGlass.blurPixels(pixels, 81, 1, 100);
        assertTrue((soft[40] & 255) > (strong[40] & 255));
        assertEquals(0, soft[47] & 255);
        assertTrue((strong[47] & 255) > 0);
    }
}
