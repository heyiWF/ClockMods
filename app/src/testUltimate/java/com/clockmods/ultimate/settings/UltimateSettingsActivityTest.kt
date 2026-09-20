package com.clockmods.ultimate.settings

import com.clockmods.background.ClockPreferences
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.ultimate.clock.UltimateClockStyles
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UltimateSettingsActivityTest {
    @Test
    fun currentPageExtraTakesPriorityAndLegacyExtraStillWorks() {
        assertEquals("weather", UltimateSettingsActivity.resolvePageId("weather", "style"))
        assertEquals("style", UltimateSettingsActivity.resolvePageId(null, "style"))
        assertEquals("calendar", UltimateSettingsActivity.resolvePageId(" ", "calendar"))
        assertNull(UltimateSettingsActivity.resolvePageId(" ", ""))
    }

    @Test
    fun styleGalleryKeepsExistingPositionWhenPageIsRebuilt() {
        assertEquals(137, UltimateSettingsActivity.styleGalleryTargetScrollX(137, 480, 182, 360))
    }

    @Test
    fun styleGalleryCentersSelectedCardOnFirstDisplay() {
        assertEquals(391, UltimateSettingsActivity.styleGalleryTargetScrollX(-1, 480, 182, 360))
        assertEquals(0, UltimateSettingsActivity.styleGalleryTargetScrollX(-1, 20, 182, 360))
    }

    @Test
    fun palettePreviewViewportFollowsTheClockOrientation() {
        assertArrayEquals(intArrayOf(1600, 900), UltimateSettingsActivity.clockPreviewViewport(1600, 900, ClockPreferences.ORIENTATION_FOLLOW_SYSTEM))
        assertArrayEquals(intArrayOf(1600, 900), UltimateSettingsActivity.clockPreviewViewport(900, 1600, ClockPreferences.ORIENTATION_LANDSCAPE))
        assertArrayEquals(intArrayOf(900, 1600), UltimateSettingsActivity.clockPreviewViewport(1600, 900, ClockPreferences.ORIENTATION_PORTRAIT))
    }

    @Test
    fun palettePreviewFitsTheClockAspectInsideItsBounds() {
        assertArrayEquals(intArrayOf(347, 195), UltimateSettingsActivity.fitClockPreviewSize(875, 195, 1600, 900))
        assertArrayEquals(intArrayOf(110, 195), UltimateSettingsActivity.fitClockPreviewSize(875, 195, 900, 1600))
        assertArrayEquals(intArrayOf(300, 169), UltimateSettingsActivity.fitClockPreviewSize(300, 400, 1600, 900))
    }

    @Test
    fun secondMotionControlsAreLimitedToAnalogAndHybridThemes() {
        val analogOrHybridThemes = setOf(
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
            UltimateClockStyles.STYLE_PAPER_STATION,
            UltimateClockStyles.STYLE_ORBIT_NEON,
            UltimateClockStyles.STYLE_ORBIT,
            UltimateClockStyles.STYLE_BLEND,
        )
        for (style: ClockStyle in UltimateClockStyles.builtIns()) {
            assertEquals(
                style.getMetadata().getId(),
                analogOrHybridThemes.contains(style.getMetadata().getId()),
                UltimateSettingsActivity.shouldShowSecondMotionControls(style),
            )
        }
    }
}
