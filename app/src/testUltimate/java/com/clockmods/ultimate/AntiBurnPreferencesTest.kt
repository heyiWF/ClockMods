package com.clockmods.ultimate

import com.clockmods.test.MemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiBurnPreferencesTest {
    @Test
    fun defaultsStartDisabledWithDocumentedValues() {
        assertFalse(AntiBurnPreferences.DEFAULT_ENABLED)
        assertEquals(10, AntiBurnPreferences.DEFAULT_PERIOD_MINUTES)
        assertEquals(4f, AntiBurnPreferences.DEFAULT_AMPLITUDE_DP, 0f)
        assertTrue(AntiBurnPreferences.DEFAULT_AUTO_DIM)
    }

    @Test
    fun periodAndAmplitudeAreBoundedToSupportedControls() {
        assertEquals(1, AntiBurnPreferences.normalizePeriod(1))
        assertEquals(10, AntiBurnPreferences.normalizePeriod(12))
        assertEquals(30, AntiBurnPreferences.normalizePeriod(30))
        assertEquals(60, AntiBurnPreferences.normalizePeriod(60))
        assertEquals(0f, AntiBurnPreferences.normalizeAmplitude(-1f), 0f)
        assertEquals(12f, AntiBurnPreferences.normalizeAmplitude(13f), 0f)
        assertEquals(4f, AntiBurnPreferences.normalizeAmplitude(Float.NaN), 0f)
    }

    @Test
    fun restoreDefaultsClearsEveryOverride() {
        val preferences = AntiBurnPreferences(MemorySharedPreferences())
        preferences.setEnabled(true)
        preferences.setPeriodMinutes(30)
        preferences.setAmplitudeDp(11f)
        preferences.setAutoDim(false)

        preferences.restoreDefaults()

        assertEquals(AntiBurnPreferences.DEFAULT_ENABLED, preferences.isEnabled())
        assertEquals(AntiBurnPreferences.DEFAULT_PERIOD_MINUTES, preferences.getPeriodMinutes())
        assertEquals(AntiBurnPreferences.DEFAULT_AMPLITUDE_DP, preferences.getAmplitudeDp(), 0f)
        assertEquals(AntiBurnPreferences.DEFAULT_AUTO_DIM, preferences.isAutoDim())
    }
}
