package com.clockmods.background

import org.junit.Assert
import org.junit.Test

class FontCatalogWeightsTest {
    @Test
    fun variableFamilySpansItsWholeAxis() {
        val roboto = FontCatalog.optionFor(ClockPreferences.FONT_ROBOTO)
        Assert.assertTrue(roboto.isVariable())
        Assert.assertArrayEquals(intArrayOf(100, 200, 300, 400, 500, 600, 700, 800, 900), roboto.availableWeights())
    }

    @Test
    fun loraStopsWhereItsAxisStops() {
        val lora = FontCatalog.optionFor(ClockPreferences.FONT_LORA)
        Assert.assertArrayEquals(intArrayOf(400, 500, 600, 700), lora.availableWeights())
        Assert.assertEquals(400, lora.nearestWeight(100))
        Assert.assertEquals(700, lora.nearestWeight(900))
    }

    @Test
    fun staticFamilyOffersOnlyTheWeightsItShips() {
        val googleSans = FontCatalog.optionFor(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY)
        Assert.assertFalse(googleSans.isVariable())
        Assert.assertArrayEquals(intArrayOf(400, 500, 700), googleSans.availableWeights())
        Assert.assertEquals("fonts/GoogleSansDisplay-Medium.ttf", googleSans.staticAssetFor(500))
        Assert.assertEquals("fonts/GoogleSansDisplay-Bold.ttf", googleSans.staticAssetFor(650))
    }

    @Test
    fun roundedShipsTheFiveMiddleWeights() {
        val rounded = FontCatalog.optionFor(ClockPreferences.FONT_SF_PRO_ROUNDED)
        Assert.assertArrayEquals(intArrayOf(300, 400, 500, 600, 700), rounded.availableWeights())
    }

    @Test
    fun sfProPinsItsOpticalSizeAlongsideWeight() {
        val sfPro = FontCatalog.optionFor(ClockPreferences.FONT_SF_PRO_DISPLAY)
        Assert.assertTrue(sfPro.isVariable())
        Assert.assertEquals("'opsz' 28", sfPro.variableAxes)
    }

    @Test
    fun emphasizedWeightClearsTheBaseByAVisibleMargin() {
        val roboto = FontCatalog.optionFor(ClockPreferences.FONT_ROBOTO)
        Assert.assertEquals(600, roboto.emphasizedWeight(400))
        Assert.assertEquals(700, roboto.emphasizedWeight(500))
        Assert.assertEquals(900, roboto.emphasizedWeight(900))
        val googleSans = FontCatalog.optionFor(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY)
        Assert.assertEquals(700, googleSans.emphasizedWeight(400))
    }

    @Test
    fun systemFontMapsOntoThePlatformSansFamilies() {
        val system = FontCatalog.optionFor(ClockPreferences.FONT_SYSTEM)
        Assert.assertTrue(system.isSystem())
        Assert.assertArrayEquals(intArrayOf(100, 300, 400, 500, 900), system.availableWeights())
    }

    @Test
    fun sliderIndexRoundTripsThroughWeight() {
        for (option in FontCatalog.options()) {
            val weights = option.availableWeights()
            Assert.assertTrue(option.id, weights.isNotEmpty())
            for (index in weights.indices) {
                Assert.assertEquals(option.id, weights[index], option.weightAt(index))
                Assert.assertEquals(option.id, index, option.indexOfNearestWeight(weights[index]))
            }
            Assert.assertEquals(option.id, weights.size, option.weightCount())
        }
    }
}
