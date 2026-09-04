package com.clockmods.background;

import org.junit.Assert;
import org.junit.Test;

/**
 * The weight stops each family advertises. These drive the settings slider, so a family that
 * claims a stop it cannot render would show a slider position that does nothing.
 */
public class FontCatalogWeightsTest {
    @Test
    public void variableFamilySpansItsWholeAxis() {
        FontCatalog.FontOption roboto = FontCatalog.optionFor(ClockPreferences.FONT_ROBOTO);
        Assert.assertTrue(roboto.isVariable());
        Assert.assertArrayEquals(new int[] {100, 200, 300, 400, 500, 600, 700, 800, 900},
                roboto.availableWeights());
    }

    @Test
    public void loraStopsWhereItsAxisStops() {
        FontCatalog.FontOption lora = FontCatalog.optionFor(ClockPreferences.FONT_LORA);
        Assert.assertArrayEquals(new int[] {400, 500, 600, 700}, lora.availableWeights());
        // Asking below or above the axis snaps onto it rather than off the end.
        Assert.assertEquals(400, lora.nearestWeight(100));
        Assert.assertEquals(700, lora.nearestWeight(900));
    }

    @Test
    public void staticFamilyOffersOnlyTheWeightsItShips() {
        FontCatalog.FontOption googleSans =
                FontCatalog.optionFor(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY);
        Assert.assertFalse(googleSans.isVariable());
        Assert.assertArrayEquals(new int[] {400, 500, 700}, googleSans.availableWeights());
        Assert.assertEquals("fonts/GoogleSansDisplay-Medium.ttf", googleSans.staticAssetFor(500));
        // 600 has no file; the nearest one that exists answers instead.
        Assert.assertEquals("fonts/GoogleSansDisplay-Bold.ttf", googleSans.staticAssetFor(650));
    }

    @Test
    public void roundedShipsTheFiveMiddleWeights() {
        FontCatalog.FontOption rounded =
                FontCatalog.optionFor(ClockPreferences.FONT_SF_PRO_ROUNDED);
        Assert.assertArrayEquals(new int[] {300, 400, 500, 600, 700}, rounded.availableWeights());
    }

    @Test
    public void sfProPinsItsOpticalSizeAlongsideWeight() {
        FontCatalog.FontOption sfPro =
                FontCatalog.optionFor(ClockPreferences.FONT_SF_PRO_DISPLAY);
        Assert.assertTrue(sfPro.isVariable());
        Assert.assertEquals("'opsz' 28", sfPro.variableAxes);
    }

    @Test
    public void emphasizedWeightClearsTheBaseByAVisibleMargin() {
        FontCatalog.FontOption roboto = FontCatalog.optionFor(ClockPreferences.FONT_ROBOTO);
        Assert.assertEquals(600, roboto.emphasizedWeight(400));
        Assert.assertEquals(700, roboto.emphasizedWeight(500));
        // Already at the top: the emphasised tier cannot go higher, so it stays.
        Assert.assertEquals(900, roboto.emphasizedWeight(900));

        // A three-weight family still finds a heavier stop rather than synthesizing one.
        FontCatalog.FontOption googleSans =
                FontCatalog.optionFor(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY);
        Assert.assertEquals(700, googleSans.emphasizedWeight(400));
    }

    @Test
    public void systemFontMapsOntoThePlatformSansFamilies() {
        FontCatalog.FontOption system = FontCatalog.optionFor(ClockPreferences.FONT_SYSTEM);
        Assert.assertTrue(system.isSystem());
        Assert.assertArrayEquals(new int[] {100, 300, 400, 500, 900}, system.availableWeights());
    }

    @Test
    public void sliderIndexRoundTripsThroughWeight() {
        for (FontCatalog.FontOption option : FontCatalog.options()) {
            int[] weights = option.availableWeights();
            Assert.assertTrue(option.id, weights.length > 0);
            for (int index = 0; index < weights.length; index++) {
                Assert.assertEquals(option.id, weights[index], option.weightAt(index));
                Assert.assertEquals(option.id, index,
                        option.indexOfNearestWeight(weights[index]));
            }
            Assert.assertEquals(option.id, weights.length, option.weightCount());
        }
    }
}
