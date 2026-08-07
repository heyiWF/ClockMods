package com.clockmods.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HolidayRepository {
    public static final class HolidayStatus {
        public final String name;
        public final String date;
        public final boolean offDay;

        HolidayStatus(String name, String date, boolean offDay) {
            this.name = name; this.date = date; this.offDay = offDay;
        }
    }

    private final Map<String, HolidayStatus> statuses;

    public HolidayRepository(Context context) {
        Map<String, HolidayStatus> loaded = new HashMap<>();
        for (int year = 2025; year <= 2027; year++) {
            try (InputStream input = context.getAssets().open("holidays/" + year + ".json")) {
                loaded.putAll(parse(read(input)));
            } catch (Exception ignored) { }
        }
        statuses = Collections.unmodifiableMap(loaded);
    }

    public HolidayStatus statusOn(String date) { return statuses.get(date); }

    static Map<String, HolidayStatus> parse(String value) throws Exception {
        JSONObject root = new JSONObject(value);
        JSONArray days = root.getJSONArray("days");
        Map<String, HolidayStatus> output = new HashMap<>();
        for (int index = 0; index < days.length(); index++) {
            JSONObject day = days.getJSONObject(index);
            String name = day.getString("name").trim();
            String date = day.getString("date").trim();
            if (name.length() == 0 || !date.matches("\\d{4}-\\d{2}-\\d{2}")) continue;
            output.put(date, new HolidayStatus(name, date, day.getBoolean("isOffDay")));
        }
        return output;
    }

    private static String read(InputStream input) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line);
        }
        return output.toString();
    }
}