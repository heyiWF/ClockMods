package com.clockmods.weather;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import static com.clockmods.weather.WeatherModels.WeatherDisplayData;

public final class WeatherRepository {
    private static final String PREFS = "weather_cache";
    private final SharedPreferences preferences;
    public WeatherRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public WeatherDisplayData getCached(String source, String locationId) {
        String value = preferences.getString("data", null);
        if (value == null) return null;
        try {
            JSONObject json = new JSONObject(value);
            String cachedSource = json.optString("source", "automatic");
            if (!source.equals(cachedSource)) return null;
            if ("manual".equals(source) && !locationId.equals(json.optString("locationId"))) return null;
            return new WeatherDisplayData(json.optString("locationId"), json.optString("city"),
                    json.optString("district"), json.optString("text"), json.optString("icon"),
                    json.optString("temperature"), json.optLong("updatedAt"));
        } catch (Exception ignored) { return null; }
    }
    public void save(WeatherDisplayData data, String source) {
        try {
            JSONObject json = new JSONObject();
            json.put("locationId", data.locationId).put("city", data.city).put("district", data.district)
                    .put("text", data.text).put("icon", data.icon).put("temperature", data.temperature)
                    .put("updatedAt", data.updatedAt).put("source", source);
            preferences.edit().putString("data", json.toString()).apply();
        } catch (Exception ignored) { }
    }
}