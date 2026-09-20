package com.clockmods.widget

import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigTest {
    @Test
    fun defaultsAndNormalization() {
        for (kind in WidgetKind.values()) {
            val config = WidgetConfig.builder(8, kind).build()
            assertEquals(kind, config.kind)
            assertEquals(8, config.appWidgetId)
            assertFalse(config.showSeconds)
            assertTrue(config.useSystemTimeZone)
            assertTrue(config.useSystemTimeFormat)
            assertEquals(WidgetConfig.FONT_THEME, config.fontId)
        }
        val config = WidgetConfig.builder(1, WidgetKind.ANALOG)
            .themeId("bad")
            .timeZoneId("bad")
            .useSystemTimeZone(false)
            .showSeconds(true)
            .backgroundAlpha(900)
            .textScale(Float.NaN)
            .tapAction("bad")
            .build()
        assertEquals("system.dynamic", config.themeId)
        assertTrue(config.useSystemTimeZone)
        assertFalse(config.showSeconds)
        assertEquals(255, config.backgroundAlpha)
        assertEquals(1f, config.textScale, 0f)
        assertEquals("open_clock", config.tapAction)
        assertEquals(.85f, config.toBuilder().textScale(-5f).build().textScale, 0f)
        assertEquals(0, config.toBuilder().backgroundAlpha(-1).build().backgroundAlpha)
        assertEquals(1.2f, config.toBuilder().textScale(Float.POSITIVE_INFINITY).build().textScale, 0f)
    }

    @Test
    fun fontIdIsValidatedAgainstTheCatalog() {
        val config = WidgetConfig.builder(1, WidgetKind.DIGITAL).fontId("no.such.font").build()
        assertEquals(WidgetConfig.FONT_THEME, config.fontId)
        for (id in WidgetConfig.FONT_IDS) {
            assertEquals(id, config.toBuilder().fontId(id).build().fontId)
        }
    }

    @Test
    fun fixedZoneSurvivesSystemChanges() {
        val config = WidgetConfig.builder(1, WidgetKind.DIGITAL)
            .timeZoneId("America/New_York")
            .useSystemTimeZone(false)
            .showSeconds(true)
            .build()
        assertEquals("America/New_York", config.zone().id)
        assertTrue(config.showSeconds)
    }
}
