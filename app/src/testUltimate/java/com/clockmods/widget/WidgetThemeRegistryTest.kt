package com.clockmods.widget

import com.clockmods.widget.model.WidgetConfig
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetThemeRegistryTest {
    @Test
    fun stableThemeIdsAndLayoutResourcesExist() {
        val ids = WidgetConfig.THEME_IDS.toHashSet()
        assertEquals(6, ids.size)
        assertTrue(ids.contains("system.dynamic"))
        assertTrue(ids.contains("transparent.clean"))
        val root = Paths.get("src/ultimate/res/layout")
        for (kind in arrayOf("digital", "analog", "weather", "calendar")) {
            assertTrue(Files.exists(root.resolve("widget_${kind}_compact.xml")))
            assertTrue(Files.exists(root.resolve("widget_${kind}_compact_serif.xml")))
            assertTrue(Files.exists(root.resolve("widget_${kind}_compact_mono.xml")))
        }
    }
}
