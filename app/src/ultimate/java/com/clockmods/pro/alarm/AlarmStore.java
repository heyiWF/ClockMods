package com.clockmods.pro.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

/** Versioned list; preserves the former daily alarm once, including its enabled state. */
public final class AlarmStore {
    private static final Object LOCK = new Object();
    private final SharedPreferences preferences;
    private final Context context;
    public AlarmStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = context.getSharedPreferences("pro_alarm", Context.MODE_PRIVATE);
        synchronized (LOCK) {
            if (!preferences.contains("alarms_v2")) {
                List<AlarmSpec> migrated = new ArrayList<>();
                if (preferences.contains("hour") || preferences.contains("enabled")) {
                    AlarmSpec old = new AlarmSpec(); old.id = 1;
                    old.hour = preferences.getInt("hour", 7); old.minute = preferences.getInt("minute", 30);
                    old.enabled = preferences.getBoolean("enabled", false);
                    old.repeat = AlarmSpec.Repeat.WEEKLY; old.weekdays = 127;
                    migrated.add(old);
                }
                write(migrated);
            }
        }
    }
    public List<AlarmSpec> all() { return readList("alarms_v2"); }
    public List<AlarmSpec> timerAlerts() { return readList("timer_alerts"); }
    private List<AlarmSpec> readList(String key) {
        synchronized (LOCK) {
            List<AlarmSpec> out = new ArrayList<>();
            try {
                JSONArray items = new JSONArray(preferences.getString(key, "[]"));
                for (int i = 0; i < items.length(); i++) {
                    try { out.add(AlarmSpec.fromJson(items.getJSONObject(i))); } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
            return out;
        }
    }
    public AlarmSpec find(long id) { for (AlarmSpec alarm : id < 0 ? timerAlerts() : all()) if (alarm.id == id) return alarm; return null; }
    public void save(AlarmSpec alarm) {
        synchronized (LOCK) {
            List<AlarmSpec> items = alarm.id < 0 ? timerAlerts() : all();
            if (alarm.id == 0) {
                long next = preferences.getLong("next_alarm_id", 2);
                for (AlarmSpec item : items) next = Math.max(next, item.id + 1);
                alarm.id = next; preferences.edit().putLong("next_alarm_id", next + 1).apply();
            }
            items.removeIf(item -> item.id == alarm.id); items.add(new AlarmSpec(alarm));
            items.sort((a, b) -> { int time = Integer.compare(a.hour * 60 + a.minute, b.hour * 60 + b.minute);
                return time != 0 ? time : Long.compare(a.id, b.id); });
            write(alarm.id < 0 ? "timer_alerts" : "alarms_v2", items);
        }
    }
    public void delete(long id) { synchronized (LOCK) { List<AlarmSpec> items = id < 0 ? timerAlerts() : all(); items.removeIf(item -> item.id == id); write(id < 0 ? "timer_alerts" : "alarms_v2", items); } }
    private void write(List<AlarmSpec> items) { write("alarms_v2", items); }
    private void write(String key, List<AlarmSpec> items) {
        JSONArray array = new JSONArray();
        try { for (AlarmSpec item : items) array.put(item.toJson()); }
        catch (Exception e) { throw new IllegalStateException("Cannot serialize alarm", e); }
        preferences.edit().putString(key, array.toString()).apply();
    }
    public java.util.LinkedHashMap<Long, Long> pendingRings() {
        synchronized (LOCK) {
            java.util.LinkedHashMap<Long, Long> result = new java.util.LinkedHashMap<>();
            try {
                JSONArray array = new JSONArray(preferences.getString("pending_rings", "[]"));
                long now = System.currentTimeMillis();
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject value = array.getJSONObject(i);
                    long id = value.getLong("id"), until = value.getLong("until");
                    if (until > now && find(id) != null) result.put(id, until);
                }
            } catch (Exception ignored) { }
            return result;
        }
    }
    public void rememberRing(long id) {
        synchronized (LOCK) {
            java.util.LinkedHashMap<Long, Long> pending = pendingRings();
            if (!pending.containsKey(id)) preferences.edit().putBoolean("ring_swap_" + id,
                    AlarmPreferences.enabled(context, AlarmPreferences.RANDOM) && java.util.concurrent.ThreadLocalRandom.current().nextBoolean()).apply();
            pending.putIfAbsent(id, System.currentTimeMillis() + 10 * 60_000L); writeRings(pending);
        }
    }
    public void forgetRing(long id) {
        synchronized (LOCK) { java.util.LinkedHashMap<Long, Long> pending = pendingRings(); pending.remove(id); preferences.edit().remove("ring_swap_" + id).apply(); writeRings(pending); }
    }
    public void clearRings() { synchronized (LOCK) { writeRings(new java.util.LinkedHashMap<>()); setRingingId(0); } }
    private void writeRings(java.util.LinkedHashMap<Long, Long> pending) {
        JSONArray array = new JSONArray();
        try { for (java.util.Map.Entry<Long, Long> entry : pending.entrySet())
            array.put(new org.json.JSONObject().put("id", entry.getKey()).put("until", entry.getValue()));
        } catch (Exception e) { throw new IllegalStateException(e); }
        preferences.edit().putString("pending_rings", array.toString()).apply();
    }
    public boolean isRinging() { return preferences.getLong("ringing_id", 0) != 0; }
    public long ringingId() { return preferences.getLong("ringing_id", 0); }
    public void setRingingId(long id) { preferences.edit().putLong("ringing_id", id).putBoolean("ringing", id != 0).apply(); }
    public void setRinging(boolean value) { if (!value) setRingingId(0); }
    // Source compatibility for old integrations. New UI always addresses alarms by ID.
    private AlarmSpec first() { List<AlarmSpec> items = all(); return items.isEmpty() ? new AlarmSpec() : items.get(0); }
    public int hour() { return first().hour; }
    public int minute() { return first().minute; }
    public boolean enabled() { for (AlarmSpec item : all()) if (item.enabled) return true; return false; }
    public void save(int hour, int minute, boolean enabled) { AlarmSpec item = first(); item.hour = hour; item.minute = minute; item.enabled = enabled; save(item); }
    public void setEnabled(boolean enabled) { AlarmSpec item = first(); item.enabled = enabled; save(item); }
}
