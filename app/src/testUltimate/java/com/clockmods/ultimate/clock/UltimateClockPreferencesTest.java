package com.clockmods.ultimate.clock;

import android.content.SharedPreferences;

import com.clockmods.sdk.clock.ClockState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UltimateClockPreferencesTest {
    private MemorySharedPreferences stored;
    private UltimateClockPreferences preferences;

    @Before
    public void setUp() {
        stored = new MemorySharedPreferences();
        preferences = new UltimateClockPreferences(stored);
    }

    @Test
    public void defaultsMatchSettingsContract() {
        Assert.assertEquals("clockmods_ultimate_style",
                UltimateClockPreferences.PREFERENCES_NAME);
        Assert.assertEquals("style_id", UltimateClockPreferences.KEY_STYLE_ID);
        Assert.assertEquals("second_motion", UltimateClockPreferences.KEY_SECOND_MOTION);
        Assert.assertEquals("follow_reduced_motion",
                UltimateClockPreferences.KEY_FOLLOW_REDUCED_MOTION);
        Assert.assertEquals("background_mode", UltimateClockPreferences.KEY_BACKGROUND_MODE);
        Assert.assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER,
                preferences.getStyleId());
        Assert.assertEquals(ClockState.SecondHandMotion.SWEEP,
                preferences.getSecondHandMotion());
        Assert.assertTrue(preferences.isFollowSystemReducedMotion());
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME,
                preferences.getBackgroundMode());
    }

    @Test
    public void settingsMotionStringsMapToSdkValues() {
        assertStoredMotion("smooth", ClockState.SecondHandMotion.SWEEP);
        assertStoredMotion("tick", ClockState.SecondHandMotion.TICK);
        assertStoredMotion("off", ClockState.SecondHandMotion.OFF);
        assertStoredMotion("unknown", UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION);
    }

    @Test
    public void sdkMotionValuesPersistAsSettingsStrings() {
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.SWEEP);
        Assert.assertEquals("smooth", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION, null));
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.TICK);
        Assert.assertEquals("tick", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION, null));
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF);
        Assert.assertEquals("off", stored.getString(
                UltimateClockPreferences.KEY_SECOND_MOTION, null));
    }

    @Test
    public void dottedStyleIdsRoundTripAndBlankValuesUseDefault() {
        preferences.setStyleId(UltimateClockStyles.STYLE_ORBIT_NEON);
        Assert.assertEquals("orbit.neon", stored.getString(
                UltimateClockPreferences.KEY_STYLE_ID, null));
        Assert.assertEquals("orbit.neon", preferences.getStyleId());

        preferences.setStyleId("   ");
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                preferences.getStyleId());
    }

    @Test
    public void styleIdNormalizationRejectsEmptyAndOversizedValues() {
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId(null));
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId("  "));
        Assert.assertEquals("partner.meridian",
                UltimateClockPreferences.normalizeStyleId("  partner.meridian  "));
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 121; i++) oversized.append('a');
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                UltimateClockPreferences.normalizeStyleId(oversized.toString()));
    }

    @Test
    public void restoreDefaultsWritesAllFourSettings() {
        preferences.setStyleId(UltimateClockStyles.STYLE_DIGITAL_GRID);
        preferences.setSecondHandMotion(ClockState.SecondHandMotion.OFF);
        preferences.setFollowSystemReducedMotion(false);
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE);

        preferences.restoreDefaults();

        Assert.assertEquals(UltimateClockPreferences.DEFAULT_STYLE_ID,
                preferences.getStyleId());
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION,
                preferences.getSecondHandMotion());
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_FOLLOW_SYSTEM_REDUCED_MOTION,
                preferences.isFollowSystemReducedMotion());
        Assert.assertEquals(UltimateClockPreferences.DEFAULT_BACKGROUND_MODE,
                preferences.getBackgroundMode());
    }

    @Test
    public void backgroundModeAcceptsOnlyThreeStableValues() {
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_COLOR);
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_COLOR,
                preferences.getBackgroundMode());
        preferences.setBackgroundMode(UltimateClockPreferences.BACKGROUND_MODE_IMAGE);
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_IMAGE,
                preferences.getBackgroundMode());
        preferences.setBackgroundMode("unknown");
        Assert.assertEquals(UltimateClockPreferences.BACKGROUND_MODE_THEME,
                preferences.getBackgroundMode());
    }

    private void assertStoredMotion(String storedValue, ClockState.SecondHandMotion expected) {
        stored.edit().putString(UltimateClockPreferences.KEY_SECOND_MOTION, storedValue).commit();
        Assert.assertEquals(expected, preferences.getSecondHandMotion());
    }

    private static final class MemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<String, Object>();

        @Override public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(new HashMap<String, Object>(values));
        }

        @Override public String getString(String key, String defaultValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defaultValue;
        }

        @SuppressWarnings("unchecked")
        @Override public Set<String> getStringSet(String key, Set<String> defaultValues) {
            Object value = values.get(key);
            return value instanceof Set
                    ? new HashSet<String>((Set<String>) value) : defaultValues;
        }

        @Override public int getInt(String key, int defaultValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defaultValue;
        }

        @Override public long getLong(String key, long defaultValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defaultValue;
        }

        @Override public float getFloat(String key, float defaultValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defaultValue;
        }

        @Override public boolean getBoolean(String key, boolean defaultValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defaultValue;
        }

        @Override public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override public Editor edit() {
            return new MemoryEditor();
        }

        @Override public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        @Override public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        private final class MemoryEditor implements Editor {
            private final Map<String, Object> updates = new HashMap<String, Object>();
            private final Set<String> removals = new HashSet<String>();
            private boolean clear;

            @Override public Editor putString(String key, String value) {
                updates.put(key, value);
                return this;
            }

            @Override public Editor putStringSet(String key, Set<String> value) {
                updates.put(key, value == null ? null : new HashSet<String>(value));
                return this;
            }

            @Override public Editor putInt(String key, int value) {
                updates.put(key, value);
                return this;
            }

            @Override public Editor putLong(String key, long value) {
                updates.put(key, value);
                return this;
            }

            @Override public Editor putFloat(String key, float value) {
                updates.put(key, value);
                return this;
            }

            @Override public Editor putBoolean(String key, boolean value) {
                updates.put(key, value);
                return this;
            }

            @Override public Editor remove(String key) {
                removals.add(key);
                return this;
            }

            @Override public Editor clear() {
                clear = true;
                return this;
            }

            @Override public boolean commit() {
                apply();
                return true;
            }

            @Override public void apply() {
                if (clear) values.clear();
                for (String key : removals) values.remove(key);
                for (Map.Entry<String, Object> entry : updates.entrySet()) {
                    if (entry.getValue() == null) values.remove(entry.getKey());
                    else values.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
