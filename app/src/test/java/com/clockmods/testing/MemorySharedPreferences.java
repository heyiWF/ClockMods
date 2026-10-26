package com.clockmods.testing;

import android.content.SharedPreferences;
import java.util.*;

public final class MemorySharedPreferences implements SharedPreferences {
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
