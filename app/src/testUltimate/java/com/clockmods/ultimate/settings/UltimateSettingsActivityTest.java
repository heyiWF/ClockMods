package com.clockmods.ultimate.settings;

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
