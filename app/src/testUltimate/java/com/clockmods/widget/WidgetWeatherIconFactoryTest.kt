package com.clockmods.widget

import com.clockmods.widget.render.WidgetWeatherIconFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetWeatherIconFactoryTest {
    @Test
    fun boundsAndUnsafeAssetNames() {
        assertEquals(16, WidgetWeatherIconFactory.boundedSize(-1))
        assertEquals(144, WidgetWeatherIconFactory.boundedSize(10_000))
        assertEquals("999", WidgetWeatherIconFactory.normalizedCode("../secret"))
        assertEquals("999", WidgetWeatherIconFactory.normalizedCode(null))
        assertEquals("100", WidgetWeatherIconFactory.normalizedCode("100"))
    }
}
