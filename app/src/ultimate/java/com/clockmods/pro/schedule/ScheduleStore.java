package com.clockmods.pro.schedule;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Daily schedule persistence. One {@link SharedPreferences} blob holds a JSON array keyed by civil
 * date ("yyyy-MM-dd", month 1-based), matching the format the forecast lookup already uses. Each
 * day costs one getter; a write rewrites only that day's array, keeping the per-page cost
 * proportional to a single day rather than to the full visible month.
 */
public final class ScheduleStore {
    /** Hard cap per day: the layout is bounded and a runaway array is a bug. */
    public static final int MAX_ITEMS_PER_DAY = 24;

    private static final String PREFS_NAME = "pro_schedule";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_HOUR = "hour";
    private static final String FIELD_MINUTE = "minute";

    private final SharedPreferences preferences;

    public ScheduleStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String keyFor(int year, int month0, int dayOfMonth) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, dayOfMonth);
    }

    /** Items for one day, sorted by time (all-day first). */
    public List<ScheduleItem> itemsFor(int year, int month0, int dayOfMonth) {
        String stored = preferences.getString(keyFor(year, month0, dayOfMonth), null);
        if (stored == null || stored.length() == 0) return new ArrayList<>();
        List<ScheduleItem> items = decode(stored);
        Collections.sort(items, Comparator.comparingInt(ScheduleItem::sortKey));
        return items;
    }

    public boolean hasItems(int year, int month0, int dayOfMonth) {
        return preferences.contains(keyFor(year, month0, dayOfMonth));
    }

    /**
     * Adds or replaces an item. When {@code id} is empty or not found, the item is appended; when
     * it matches an existing one, that entry is updated in place to preserve its position.
     */
    public void save(int year, int month0, int dayOfMonth, String id, String title,
            int hour, int minute) {
        String key = keyFor(year, month0, dayOfMonth);
        List<ScheduleItem> existing = itemsFor(year, month0, dayOfMonth);
        String targetId = (id == null || id.length() == 0) ? newId() : id;
        ScheduleItem replacement = new ScheduleItem(targetId, title, hour, minute);
        boolean replaced = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(targetId)) {
                existing.set(i, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            if (existing.size() >= MAX_ITEMS_PER_DAY) return;
            existing.add(replacement);
        }
        write(key, existing);
    }

    public void remove(int year, int month0, int dayOfMonth, String id) {
        String key = keyFor(year, month0, dayOfMonth);
        List<ScheduleItem> existing = itemsFor(year, month0, dayOfMonth);
        boolean removed = existing.removeIf(item -> item.id.equals(id));
        if (removed) write(key, existing);
    }

    private static String newId() {
        return Long.toHexString(System.currentTimeMillis()) + "_"
                + Long.toHexString(System.nanoTime());
    }

    private void write(String key, List<ScheduleItem> items) {
        JSONArray array = new JSONArray();
        for (ScheduleItem item : items) {
            JSONObject obj = new JSONObject();
            try {
                obj.put(FIELD_ID, item.id);
                obj.put(FIELD_TITLE, item.title);
                obj.put(FIELD_HOUR, item.hour);
                obj.put(FIELD_MINUTE, item.minute);
            } catch (JSONException ignored) {
                continue;
            }
            array.put(obj);
        }
        preferences.edit().putString(key, array.toString()).apply();
    }

    private static List<ScheduleItem> decode(String stored) {
        List<ScheduleItem> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length() && items.size() < MAX_ITEMS_PER_DAY; i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) continue;
                String id = obj.optString(FIELD_ID, "");
                String title = obj.optString(FIELD_TITLE, "");
                int hour = obj.optInt(FIELD_HOUR, ScheduleItem.TIME_NONE);
                int minute = obj.optInt(FIELD_MINUTE, ScheduleItem.TIME_NONE);
                if (id.length() == 0) id = newId();
                items.add(new ScheduleItem(id, title, hour, minute));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return items;
    }
}
