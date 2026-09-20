package com.clockmods.ultimate

import com.clockmods.ultimate.settings.UltimateSettingsActivity
import com.clockmods.ultimate.settings.UltimateSubSettingsActivity
import com.clockmods.widget.config.WidgetConfigActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentCompatibilityTest {
    @Test
    fun upgradeSensitiveActivityClassNamesRemainStable() {
        assertEquals(
            "com.clockmods.ultimate.UltimateMainActivity",
            UltimateMainActivity::class.java.name,
        )
        assertEquals(
            "com.clockmods.ultimate.SetupWizardActivity",
            SetupWizardActivity::class.java.name,
        )
        assertEquals(
            "com.clockmods.ultimate.settings.UltimateSettingsActivity",
            UltimateSettingsActivity::class.java.name,
        )
        assertEquals(
            "com.clockmods.ultimate.settings.UltimateSubSettingsActivity",
            UltimateSubSettingsActivity::class.java.name,
        )
        assertEquals(
            "com.clockmods.widget.config.WidgetConfigActivity",
            WidgetConfigActivity::class.java.name,
        )
    }

    @Test
    fun legacyEntrypointsDelegateToComposeActivities() {
        assertTrue(ComposeMainActivity::class.java.isAssignableFrom(UltimateMainActivity::class.java))
        assertTrue(
            UltimateSettingsActivity::class.java.isAssignableFrom(
                UltimateSubSettingsActivity::class.java,
            ),
        )
    }
}
