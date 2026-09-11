package com.clockmods.ultimate.clock;

import android.content.Context;
import android.content.SharedPreferences;

import com.clockmods.sdk.clock.WorldClockEntry;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persists the world-clock feature state and its ordered city selection. */
public final class WorldClockRepository {
    public static final String PREFERENCES_NAME = "clockmods_world_clock";
    public static final boolean DEFAULT_ENABLED = false;
    public static final int MAX_SELECTED = 6;
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CITIES = "cities";
    private final SharedPreferences preferences;

    public WorldClockRepository(Context context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    WorldClockRepository(SharedPreferences preferences) {
        if (preferences == null) throw new IllegalArgumentException("preferences required");
        this.preferences = preferences;
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public List<WorldClockEntry> getSelected() {
        String encoded = preferences.getString(KEY_CITIES, null);
        if (encoded == null) return new ArrayList<>(WorldClockCatalog.defaults());
        try {
            return decode(encoded);
        } catch (Exception ignored) {
            return new ArrayList<>(WorldClockCatalog.defaults());
        }
    }

    public void save(List<WorldClockEntry> entries) {
        preferences.edit().putString(KEY_CITIES, encode(entries)).apply();
    }

    static String encode(List<WorldClockEntry> entries) {
        JSONArray array = new JSONArray();
        Set<String> seen = new LinkedHashSet<>();
        if (entries != null) {
            for (WorldClockEntry entry : entries) {
                if (entry == null || !seen.add(entry.getId())) continue;
                array.put(entry.getId());
                if (array.length() == MAX_SELECTED) break;
            }
        }
        return array.toString();
    }

    static List<WorldClockEntry> decode(String encoded) throws Exception {
        JSONArray array = new JSONArray(encoded);
        List<WorldClockEntry> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int index = 0; index < array.length(); index++) {
            WorldClockEntry entry = WorldClockCatalog.find(array.optString(index, ""));
            if (entry != null && seen.add(entry.getId())) result.add(entry);
            if (result.size() == MAX_SELECTED) break;
        }
        return result;
    }
}
