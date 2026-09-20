package com.clockmods.ultimate.clock

import android.content.SharedPreferences
import com.clockmods.sdk.clock.ClockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UltimateClockPreferencesTest {
    private lateinit var stored: MemorySharedPreferences
    private lateinit var preferences: UltimateClockPreferences

    @Before
    fun setUp() {
        stored = MemorySharedPreferences()
        preferences = UltimateClockPreferences(stored)
    }

    @Test
    fun defaultsMatchSettingsContract() {
        assertEquals("clockmods_ultimate_style", UltimateClockPreferences.PREFERENCES_NAME)
        assertEquals("style_id", UltimateClockPreferences.KEY_STYLE_ID)
        assertEquals("second_motion", UltimateClockPreferences.KEY_SECOND_MOTION)
        assertEquals("background_mode", UltimateClockPreferences.KEY_BACKGROUND_MODE)
        assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER, preferences.getStyleId())
        assertEquals(ClockState.SecondHandMotion.SWEEP, preferences.getSecondHandMotion())
        assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME, preferences.getBackgroundMode())
    }

    @Test
    fun settingsMotionStringsMapToSdkValues() {
        assertStoredMotion("smooth", ClockState.SecondHandMotion.SWEEP)
        assertStoredMotion("tick", ClockState.SecondHandMotion.TICK)
        assertStoredMotion("off", ClockState.SecondHandMotion.OFF)
        assertStoredMotion("unknown", UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION)
    }

    @Test
    fun sdkMotionValuesPersistAsSettingsStrings() {
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.SWEEP)
        assertEquals("smooth", stored.getString(UltimateClockPreferences.KEY_SECOND_MOTION, null))
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.TICK)
        assertEquals("tick", stored.getString(UltimateClockPreferences.KEY_SECOND_MOTION, null))
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF)
        assertEquals("off", stored.getString(UltimateClockPreferences.KEY_SECOND_MOTION, null))
    }

    @Test
    fun dottedStyleIdsRoundTripAndBlankValuesUseDefault() {
        preferences.setStyleId(UltimateClockStyles.STYLE_ORBIT_NEON)
        assertEquals("orbit.neon", stored.getString(UltimateClockPreferences.KEY_STYLE_ID, null))
        assertEquals("orbit.neon", preferences.getStyleId())
        preferences.setStyleId("   ")
        assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID, preferences.getStyleId())
    }

    @Test
    fun styleIdNormalizationRejectsEmptyAndOversizedValues() {
        assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID, UltimateClockPreferences.normalizeStyleId(null))
        assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID, UltimateClockPreferences.normalizeStyleId("  "))
        assertEquals("partner.meridian", UltimateClockPreferences.normalizeStyleId("  partner.meridian  "))
        assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID, UltimateClockPreferences.normalizeStyleId("a".repeat(121)))
    }

    @Test
    fun restoreDefaultsWritesAllSettings() {
        preferences.setStyleId(UltimateClockStyles.STYLE_DIGITAL_GRID)
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF)
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE)
        preferences.restoreDefaults()
        assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID, preferences.getStyleId())
        assertEquals(UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION, preferences.getSecondHandMotion())
        assertEquals(UltimateClockPreferences.DEFAULT_BACKGROUND_MODE, preferences.getBackgroundMode())
    }

    @Test
    fun backgroundModeAcceptsOnlyThreeStableValues() {
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_COLOR)
        assertEquals(UltimateClockPreferences.BACKGROUND_MODE_COLOR, preferences.getBackgroundMode())
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE)
        assertEquals(UltimateClockPreferences.BACKGROUND_MODE_IMAGE, preferences.getBackgroundMode())
        preferences.setBackgroundMode("unknown")
        assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME, preferences.getBackgroundMode())
    }

    @Test
    fun palettesPersistIndependentlyAndResetWithDefaults() {
        val orbit = UltimateClockStyles.STYLE_ORBIT
        val dual = UltimateClockStyles.STYLE_DUAL_BLOCKS
        preferences.setPalette(
            orbit,
            ClockPalette(0xFFFFFFFF.toInt(), 0xFF123456.toInt(), 0xFF000000.toInt())
                .withGaussianBlur(true).withBlurStrength(75).withBlurBrightness(68),
        )
        preferences.setPalette(dual, ClockPalette(0xFF345678.toInt(), 0xFFEEEEEE.toInt(), 0xFFFF0000.toInt()))
        val reopened = UltimateClockPreferences(stored)
        assertTrue(reopened.getPalette(orbit).gaussianBlur)
        assertEquals(75, reopened.getPalette(orbit).blurStrength)
        assertEquals(68, reopened.getPalette(orbit).blurBrightness)
        assertEquals(ClockPalette.DEFAULT.blurStrength, reopened.getPalette(dual).blurStrength)
        assertEquals(ClockPalette.DEFAULT.blurBrightness, reopened.getPalette(dual).blurBrightness)
        assertFalse(reopened.getPalette(dual).gaussianBlur)
        reopened.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_COLOR)
        assertTrue(reopened.getPalette(orbit).gaussianBlur)
        assertEquals(0xFFFFFFFF.toInt(), reopened.getPalette(orbit).background)
        assertEquals(0xFF000000.toInt(), reopened.getPalette(orbit).accent)
        assertEquals(0xFFEEEEEE.toInt(), reopened.getPalette(dual).panel)
        assertEquals(ClockPalette.DEFAULT.background, reopened.getPalette(UltimateClockStyles.STYLE_BLEND).background)
        reopened.restoreDefaults()
        assertFalse(reopened.getPalette(orbit).gaussianBlur)
        assertEquals(ClockPalette.DEFAULT.blurStrength, reopened.getPalette(orbit).blurStrength)
        assertEquals(ClockPalette.DEFAULT.blurBrightness, reopened.getPalette(orbit).blurBrightness)
        assertEquals(ClockPalette.DEFAULT.background, reopened.getPalette(orbit).background)
        assertEquals(ClockPalette.DEFAULT.panel, reopened.getPalette(dual).panel)
    }

    private fun assertStoredMotion(storedValue: String, expected: ClockState.SecondHandMotion) {
        stored.edit().putString(UltimateClockPreferences.KEY_SECOND_MOTION, storedValue).commit()
        assertEquals(expected, preferences.getSecondHandMotion())
    }

    private class MemorySharedPreferences : SharedPreferences {
        private val values = HashMap<String, Any?>()

        override fun getAll(): Map<String, *> = HashMap(values)
        override fun getString(key: String?, defaultValue: String?): String? = values[key] as? String ?: defaultValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defaultValues: Set<String>?): Set<String>? =
            (values[key] as? Set<String>)?.toHashSet() ?: defaultValues
        override fun getInt(key: String?, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getLong(key: String?, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getFloat(key: String?, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getBoolean(key: String?, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = MemoryEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private inner class MemoryEditor : SharedPreferences.Editor {
            private val updates = HashMap<String, Any?>()
            private val removals = HashSet<String>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)
            override fun putStringSet(key: String?, value: Set<String>?): SharedPreferences.Editor = put(key, value?.toHashSet())
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)
            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removals += key
                return this
            }
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) values.clear()
                removals.forEach(values::remove)
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            }
        }
    }
}
