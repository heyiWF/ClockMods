package com.clockmods.pro.alarm;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/** A saved alarm, independent of any scheduled or ringing occurrence. */
public final class AlarmSpec {
    public enum Repeat { ONCE, WEEKLY, HOLIDAYS, WORKDAYS, SHIFT }
    public long id;
    public int hour = 7, minute = 30;
    public boolean enabled = true, vibrate = true;
    public String label = "", ringtone = "default";
    public Repeat repeat = Repeat.ONCE;
    /** Monday is bit 0; Sunday is bit 6. */
    public int weekdays = 31;
    public LocalDate onceDate = LocalDate.now(), cycleStart = LocalDate.now();
    public int cycleLength = 2;
    /** Minutes since midnight; -1 is a rest day. */
    public final int[] cycleMinutes = new int[62];
    public final Set<LocalDate> extraDates = new TreeSet<>(), excludedDates = new TreeSet<>();
    public int snoozeMinutes = 10;
    public long snoozeAt, scheduledAt, lastFiredAt;

    public AlarmSpec() { Arrays.fill(cycleMinutes, 450); }
    public AlarmSpec(AlarmSpec other) {
        this();
        try { read(other.toJson()); } catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static AlarmSpec fromJson(JSONObject json) throws Exception {
        AlarmSpec result = new AlarmSpec(); result.read(json); return result;
    }
    private void read(JSONObject json) throws Exception {
        id = json.optLong("id");
        hour = clamp(json.optInt("hour", 7), 0, 23); minute = clamp(json.optInt("minute", 30), 0, 59);
        enabled = json.optBoolean("enabled", true); vibrate = json.optBoolean("vibrate", true);
        label = json.optString("label", ""); ringtone = json.optString("ringtone", "default");
        try { repeat = Repeat.valueOf(json.optString("repeat", "ONCE")); } catch (Exception ignored) { repeat = Repeat.ONCE; }
        weekdays = json.optInt("weekdays", 31) & 127;
        onceDate = date(json.optString("onceDate"), LocalDate.now());
        cycleStart = date(json.optString("cycleStart"), LocalDate.now());
        cycleLength = clamp(json.optInt("cycleLength", 2), 2, 62);
        JSONArray cycle = json.optJSONArray("cycleMinutes");
        for (int i = 0; i < 62; i++) cycleMinutes[i] = cycle == null ? hour * 60 + minute
                : clamp(cycle.optInt(i, hour * 60 + minute), -1, 1439);
        readDates(json.optJSONArray("extraDates"), extraDates);
        readDates(json.optJSONArray("excludedDates"), excludedDates);
        snoozeMinutes = clamp(json.optInt("snoozeMinutes", 10), 1, 60);
        snoozeAt = json.optLong("snoozeAt"); scheduledAt = json.optLong("scheduledAt");
        lastFiredAt = json.optLong("lastFiredAt");
    }
    public JSONObject toJson() throws Exception {
        JSONObject out = new JSONObject().put("id", id).put("hour", hour).put("minute", minute)
                .put("enabled", enabled).put("vibrate", vibrate).put("label", label).put("ringtone", ringtone)
                .put("repeat", repeat.name()).put("weekdays", weekdays).put("onceDate", onceDate.toString())
                .put("cycleStart", cycleStart.toString()).put("cycleLength", cycleLength)
                .put("snoozeMinutes", snoozeMinutes).put("snoozeAt", snoozeAt)
                .put("scheduledAt", scheduledAt).put("lastFiredAt", lastFiredAt);
        JSONArray cycle = new JSONArray(), extra = new JSONArray(), excluded = new JSONArray();
        for (int value : cycleMinutes) cycle.put(value);
        for (LocalDate value : extraDates) extra.put(value.toString());
        for (LocalDate value : excludedDates) excluded.put(value.toString());
        return out.put("cycleMinutes", cycle).put("extraDates", extra).put("excludedDates", excluded);
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static LocalDate date(String value, LocalDate fallback) {
        try { return LocalDate.parse(value); } catch (Exception ignored) { return fallback; }
    }
    private static void readDates(JSONArray values, Set<LocalDate> out) {
        out.clear(); if (values == null) return;
        for (int i = 0; i < values.length(); i++) {
            LocalDate date = date(values.optString(i), null); if (date != null) out.add(date);
        }
    }
}
