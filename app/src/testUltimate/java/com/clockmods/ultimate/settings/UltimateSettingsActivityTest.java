package com.clockmods.ultimate.settings;

import com.clockmods.background.ClockPreferences;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.ultimate.clock.UltimateClockStyles;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UltimateSettingsActivityTest {
    @Test
    public void styleGalleryKeepsExistingPositionWhenPageIsRebuilt() {
        Assert.assertEquals(137, UltimateSettingsActivity.styleGalleryTargetScrollX(
                137, 480, 182, 360));
    }

    @Test
    public void styleGalleryCentersSelectedCardOnFirstDisplay() {
        Assert.assertEquals(391, UltimateSettingsActivity.styleGalleryTargetScrollX(
                -1, 480, 182, 360));
        Assert.assertEquals(0, UltimateSettingsActivity.styleGalleryTargetScrollX(
                -1, 20, 182, 360));
    }

    @Test
    public void palettePreviewViewportFollowsTheClockOrientation() {
        Assert.assertArrayEquals(new int[] {1600, 900},
                UltimateSettingsActivity.clockPreviewViewport(1600, 900,
                        ClockPreferences.ORIENTATION_FOLLOW_SYSTEM));
        Assert.assertArrayEquals(new int[] {1600, 900},
                UltimateSettingsActivity.clockPreviewViewport(900, 1600,
                        ClockPreferences.ORIENTATION_LANDSCAPE));
        Assert.assertArrayEquals(new int[] {900, 1600},
                UltimateSettingsActivity.clockPreviewViewport(1600, 900,
                        ClockPreferences.ORIENTATION_PORTRAIT));
    }

    @Test
    public void palettePreviewFitsTheClockAspectInsideItsBounds() {
        Assert.assertArrayEquals(new int[] {347, 195},
                UltimateSettingsActivity.fitClockPreviewSize(875, 195, 1600, 900));
        Assert.assertArrayEquals(new int[] {110, 195},
                UltimateSettingsActivity.fitClockPreviewSize(875, 195, 900, 1600));
        Assert.assertArrayEquals(new int[] {300, 169},
                UltimateSettingsActivity.fitClockPreviewSize(300, 400, 1600, 900));
    }

    @Test
    public void secondMotionControlsAreLimitedToAnalogAndHybridThemes() {
        Set<String> analogOrHybridThemes = new HashSet<String>(Arrays.asList(
                UltimateClockStyles.STYLE_GLASS_ATELIER,
                UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
                UltimateClockStyles.STYLE_PAPER_STATION,
                UltimateClockStyles.STYLE_ORBIT_NEON,
                UltimateClockStyles.STYLE_ORBIT,
                UltimateClockStyles.STYLE_BLEND));

        for (ClockStyle style : UltimateClockStyles.builtIns()) {
            Assert.assertEquals(style.getMetadata().getId(),
                    analogOrHybridThemes.contains(style.getMetadata().getId()),
                    UltimateSettingsActivity.shouldShowSecondMotionControls(style));
        }
    }
}
