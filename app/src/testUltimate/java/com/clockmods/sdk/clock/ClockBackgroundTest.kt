package com.clockmods.sdk.clock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockBackgroundTest {
    @Test
    fun exposesThreeStableModes() {
        assertArrayEquals(
            arrayOf(ClockBackground.Mode.THEME, ClockBackground.Mode.COLOR, ClockBackground.Mode.IMAGE),
            ClockBackground.Mode.values(),
        )
    }

    @Test
    fun themeBackgroundUsesNeutralDefaults() {
        val background = ClockBackground.theme(false)
        assertEquals(ClockBackground.Mode.THEME, background.getMode())
        assertTrue(background.usesThemeSurface())
        assertFalse(background.hasImage())
        assertNull(background.getBitmap())
        assertEquals(0, background.getColor())
        assertFalse(background.isDimmed())
    }

    @Test
    fun colorBackgroundPreservesColorAndDimPolicy() {
        val background = ClockBackground.color(0xFF123456.toInt(), true)
        assertEquals(ClockBackground.Mode.COLOR, background.getMode())
        assertFalse(background.usesThemeSurface())
        assertFalse(background.hasImage())
        assertNull(background.getBitmap())
        assertEquals(0xFF123456.toInt(), background.getColor())
        assertTrue(background.isDimmed())
    }

    @Test
    fun missingImageFallsBackToColorMode() {
        val background = ClockBackground.image(null, 0xFF654321.toInt(), true)
        assertEquals(ClockBackground.Mode.COLOR, background.getMode())
        assertFalse(background.hasImage())
        assertEquals(0xFF654321.toInt(), background.getColor())
        assertTrue(background.isDimmed())
    }

    @Test
    fun legacyRenderContextHasNoHostBackgroundByDefault() {
        val context = ClockRenderContext(0f, 0f, 100f, 80f, 1f, 1f, 42L)
        assertNull(context.getBackground())
    }
}
