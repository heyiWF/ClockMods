package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.ClockThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockPaletteTest {
    @Test
    fun everySurfaceHasReadableTextAcrossColorCube() {
        for (r in 0..255 step 17) {
            for (g in 0..255 step 17) {
                for (b in 0..255 step 17) {
                    val color = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                    val palette = ClockPalette(
                        color,
                        color xor 0x00FFFFFF,
                        ClockPalette.mix(color, 0xFF808080.toInt(), .5f),
                    )
                    readable(palette.onBackground, palette.background)
                    readable(palette.mutedBackground, palette.background)
                    readable(palette.onPanel, palette.panel)
                    readable(palette.mutedPanel, palette.panel)
                    readable(palette.onAccent, palette.accent)
                    readable(palette.mutedAccent, palette.accent)
                    readable(palette.onPanelAlt, palette.panelAlt)
                    readable(palette.onBadge, palette.badge)
                    assertTrue(ClockPalette.contrast(palette.hand(), palette.panelAlt) >= 3)
                }
            }
        }
    }

    @Test
    fun changesAreImmutableAndRoundTripThroughTokens() {
        val changed = ClockPalette.DEFAULT.withColor(0, 0xFFFFFFFF.toInt())
        assertEquals(0xFF154974.toInt(), ClockPalette.DEFAULT.background)
        val restored = ClockPalette.fromTokens(
            changed.applyTo(
                UltimateClockStyles.sharedRegistry()
                    .resolveForApi(UltimateClockStyles.STYLE_ORBIT, 35)
                    .getThemeTokens(),
            ),
        )
        assertEquals(changed.background, restored.background)
        assertEquals(changed.panel, restored.panel)
        assertEquals(changed.accent, restored.accent)
    }

    @Test
    fun cardShadowDefaultsOnAndSurvivesTokenCopies() {
        assertTrue("the migrated cards cast their shadow out of the box", ClockPalette.DEFAULT.cardShadow)
        val off = ClockPalette.DEFAULT.withCardShadow(false)
        assertFalse(off.cardShadow)
        assertEquals(ClockPalette.DEFAULT.background, off.background)
        val restored = ClockPalette.fromTokens(
            off.applyTo(ClockThemeTokens.builder().build()).toBuilder().build(),
        )
        assertFalse(restored.cardShadow)
        assertTrue(restored.withCardShadow(true).cardShadow)
    }

    private fun readable(text: Int, surface: Int) {
        assertNotEquals(0xFF000000.toInt(), text)
        assertNotEquals(0xFFFFFFFF.toInt(), text)
        assertTrue(
            "${text.toUInt().toString(16)} on ${surface.toUInt().toString(16)}",
            ClockPalette.contrast(text, surface) >= 4.5,
        )
    }

    @Test
    fun glassTextRemainsReadableOverEveryPossibleImageBrightness() {
        val palette = ClockPalette.GLASS
        val surfaces = intArrayOf(
            palette.background, palette.panel, palette.accent, palette.panelAlt, palette.badge,
        )
        val text = intArrayOf(
            palette.onBackground, palette.onPanel, palette.onAccent, palette.onPanelAlt, palette.onBadge,
        )
        for (i in surfaces.indices) {
            for (brightness in 0..255) {
                val tinted = ClockPalette.mix(0xFF000000.toInt(), surfaces[i], brightness / 255f)
                readable(text[i], tinted)
                if (i == 0) readable(palette.mutedBackground, tinted)
                if (i == 1) readable(palette.mutedPanel, tinted)
                if (i == 2) readable(palette.mutedAccent, tinted)
            }
        }
        assertTrue(ClockPalette.contrast(palette.hand(), palette.panelAlt) >= 3)
    }

    @Test
    fun blurSurvivesPaletteAndTypographyTokenCopies() {
        val palette = ClockPalette.DEFAULT.withGaussianBlur(true)
            .withColor(1, 0xFFFFFFFF.toInt()).withBlurStrength(80).withBlurBrightness(70)
        val restored = ClockPalette.fromTokens(
            palette.applyTo(ClockThemeTokens.builder().build()).toBuilder().build(),
        )
        assertTrue(restored.gaussianBlur)
        assertEquals(0xFFFFFFFF.toInt(), restored.panel)
        assertEquals(restored.panel, restored.withGaussianBlur(false).panel)
        assertEquals(80, restored.blurStrength)
        assertEquals(70, restored.withGaussianBlur(false).blurBrightness)
        assertEquals(0, palette.withBlurStrength(-1).blurStrength)
        assertEquals(100, palette.withBlurBrightness(101).blurBrightness)
    }

    @Test
    fun tintedGlassTextFollowsTheBrightnessAdjustedSample() {
        for (r in 0..255 step 51) {
            for (g in 0..255 step 51) {
                for (b in 0..255 step 51) {
                    val tint = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                    for (brightness in 0..100) {
                        val palette = ClockPalette.glass(brightness, tint)
                        assertNotEquals(0xFF000000.toInt(), palette.onPanel)
                        assertNotEquals(0xFFFFFFFF.toInt(), palette.onPanel)
                        val surface = GaussianGlass.applyBrightnessOverlay(tint, brightness)
                        readable(palette.onPanel, surface)
                        readable(palette.mutedPanel, surface)
                    }
                }
            }
        }
        assertNotEquals(
            ClockPalette.glass(25, 0xFF2050E0.toInt()).onPanel,
            ClockPalette.glass(25, 0xFFE08020.toInt()).onPanel,
        )
        assertNotEquals(
            ClockPalette.glass(75, 0xFF2050E0.toInt()).onPanel,
            ClockPalette.glass(75, 0xFFE08020.toInt()).onPanel,
        )
    }
}
