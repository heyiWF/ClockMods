package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.ClockBackground
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockThemeTokens
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

class GaussianGlassTest {
    @Test
    fun missingOrSolidBackgroundKeepsTheSavedSolidPalette() {
        val theme = ClockPalette.DEFAULT.withGaussianBlur(true)
            .applyTo(ClockThemeTokens.builder().build())
        val backgrounds = arrayOf<ClockBackground?>(
            null,
            ClockBackground.theme(false),
            ClockBackground.color(0xFFFFFFFF.toInt(), false),
            ClockBackground.image(null, 0xFF000000.toInt(), false),
        )
        for (background in backgrounds) {
            val context = ClockRenderContext(0f, 0f, 640f, 360f, 1f, 1f, 0L, background)
            assertNull(GaussianGlass.create(context, theme))
        }
        assertEquals(ClockPalette.DEFAULT.panel, ClockPalette.fromTokens(theme).panel)
    }

    @Test
    fun uniformColorsAndTinyImagesHaveNoDarkEdges() {
        for (color in intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF2389AB.toInt())) {
            val pixels = IntArray(35)
            Arrays.fill(pixels, color)
            assertArrayEquals(pixels, GaussianGlass.blurPixels(pixels, 5, 7))
            assertArrayEquals(intArrayOf(color), GaussianGlass.blurPixels(intArrayOf(color), 1, 1))
        }
    }

    @Test
    fun sharpEdgeBecomesSmoothSymmetricGaussianTransition() {
        val pixels = IntArray(31)
        Arrays.fill(pixels, 0xFF000000.toInt())
        Arrays.fill(pixels, 15, 31, 0xFFFFFFFF.toInt())
        val result = GaussianGlass.blurPixels(pixels, 31, 1)
        assertEquals(0xFF000000.toInt(), result[0])
        assertEquals(0xFFFFFFFF.toInt(), result[30])
        assertTrue((result[14] and 255) > 0)
        assertTrue((result[15] and 255) < 255)
        assertEquals(255, (result[14] and 255) + (result[15] and 255))
        for (i in 1 until result.size) assertTrue((result[i] and 255) >= (result[i - 1] and 255))
        assertEquals(0xFF000000.toInt(), pixels[14])
        assertArrayEquals(result, GaussianGlass.blurPixels(pixels, 1, 31))
    }

    @Test
    fun transparentWhiteDoesNotProduceBrightHalos() {
        assertArrayEquals(
            intArrayOf(0xFF000000.toInt()),
            GaussianGlass.blurPixels(intArrayOf(0x00FFFFFF), 1, 1),
        )
    }

    @Test
    fun strengthZeroPreservesDetailAndHigherStrengthSpreadsTheBlur() {
        val pixels = IntArray(81)
        Arrays.fill(pixels, 0xFF000000.toInt())
        pixels[40] = 0xFFFFFFFF.toInt()
        assertArrayEquals(pixels, GaussianGlass.blurPixels(pixels, 81, 1, 0))
        val soft = GaussianGlass.blurPixels(pixels, 81, 1, 25)
        val strong = GaussianGlass.blurPixels(pixels, 81, 1, 100)
        assertTrue((soft[40] and 255) > (strong[40] and 255))
        assertEquals(0, soft[47] and 255)
        assertTrue((strong[47] and 255) > 0)
    }

    @Test
    fun weakBlurUsesMoreSamplesThanStrongBlur() {
        val sourceEdge = 2400
        assertEquals(sourceEdge, GaussianGlass.workingLongEdge(sourceEdge, 0))
        assertEquals(1024, GaussianGlass.workingLongEdge(sourceEdge, 1))
        assertEquals(1024, GaussianGlass.workingLongEdge(sourceEdge, 10))
        assertEquals(512, GaussianGlass.workingLongEdge(sourceEdge, 25))
        assertEquals(256, GaussianGlass.workingLongEdge(sourceEdge, 50))
        assertEquals(192, GaussianGlass.workingLongEdge(sourceEdge, 100))
        var previous = sourceEdge
        for (strength in 1..100) {
            val edge = GaussianGlass.workingLongEdge(sourceEdge, strength)
            assertTrue(edge <= previous)
            assertTrue(edge >= 192)
            previous = edge
        }
    }

    @Test
    fun adaptiveResolutionPreservesTheBlurRadiusInSourceCoordinates() {
        val sourceEdge = 2400
        for (strength in intArrayOf(1, 5, 10, 25, 50, 75, 100)) {
            val workingEdge = GaussianGlass.workingLongEdge(sourceEdge, strength)
            val sourceSigma = GaussianGlass.blurSigma(strength, workingEdge, sourceEdge) * sourceEdge / workingEdge
            val legacySourceSigma = strength * .06f * sourceEdge / 192f
            assertEquals(legacySourceSigma, sourceSigma, .0001f)
        }
    }

    @Test
    fun smallSourcesAreNeverUpscaledForBlur() {
        for (strength in intArrayOf(-1, 0, 1, 25, 50, 100, 101)) {
            assertEquals(96, GaussianGlass.workingLongEdge(96, strength))
        }
    }

    @Test
    fun midpointBrightnessIsAnIdentityTransform() {
        assertArrayEquals(floatArrayOf(1f, 0f), GaussianGlass.brightnessTransform(50), 0f)
        for (color in intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF123456.toInt(), 0xFFCA842B.toInt())) {
            assertEquals(color, GaussianGlass.applyBrightnessOverlay(color, 50))
        }
    }

    @Test
    fun brightnessEndpointsDarkenAndLightenWithoutFlatteningTheImage() {
        val color = 0xFF406080.toInt()
        val darker = GaussianGlass.applyBrightnessOverlay(color, 0)
        val lighter = GaussianGlass.applyBrightnessOverlay(color, 100)
        for (shift in intArrayOf(0, 8, 16)) {
            val originalChannel = color ushr shift and 255
            assertTrue((darker ushr shift and 255) < originalChannel)
            assertTrue((lighter ushr shift and 255) > originalChannel)
        }
        assertTrue(darker != 0xFF000000.toInt())
        assertTrue(lighter != 0xFFFFFFFF.toInt())
        for (brightness in intArrayOf(0, 25, 50, 75, 100)) {
            val black = GaussianGlass.applyBrightnessOverlay(0xFF000000.toInt(), brightness) and 255
            val white = GaussianGlass.applyBrightnessOverlay(0xFFFFFFFF.toInt(), brightness) and 255
            assertTrue("blur detail was flattened at $brightness", white > black)
        }
    }
}
