package com.clockmods.widget

import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.store.WidgetConfigStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WidgetConfigStoreTest {
    @Test
    fun roundTripAndProviderAuthority() {
        val config = WidgetConfig.builder(42, WidgetKind.WEATHER)
            .themeId("paper.warm")
            .backgroundAlpha(102)
            .showSeconds(true)
            .useSystemTimeFormat(false)
            .use24Hour(false)
            .timeZoneId("America/New_York")
            .useSystemTimeZone(false)
            .darkText(true)
            .build()
        val json = WidgetConfigStore.encode(config)
        assertEquals(
            json,
            WidgetConfigStore.encode(WidgetConfigStore.decode(json, 42, WidgetKind.WEATHER)),
        )
        val authoritative = WidgetConfigStore.decode(json, 43, WidgetKind.ANALOG)
        assertEquals(43, authoritative.appWidgetId)
        assertEquals(WidgetKind.ANALOG, authoritative.kind)
        assertFalse(authoritative.showSeconds)
    }

    @Test
    fun migrationMissingFieldsAndDamage() {
        for (raw in arrayOf<String?>(null, "{broken", "{}", "{\"schemaVersion\":0}", "{\"schemaVersion\":99}")) {
            val config = WidgetConfigStore.decode(raw, 5, WidgetKind.CALENDAR)
            assertEquals(1, config.schemaVersion)
            assertEquals(5, config.appWidgetId)
            assertEquals(WidgetKind.CALENDAR, config.kind)
            assertEquals("system.dynamic", config.themeId)
        }
        assertEquals(
            "paper.warm",
            WidgetConfigStore.decode("{\"themeId\":\"paper.warm\"}", 1, WidgetKind.DIGITAL).themeId,
        )
    }
}
