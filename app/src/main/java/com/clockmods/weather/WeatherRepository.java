package com.clockmods.weather;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import static com.clockmods.weather.WeatherModels.WeatherDetail;
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
                    json.optString("temperature"), json.optLong("updatedAt"), readDetail(json));
        } catch (Exception ignored) { return null; }
    }
    public void save(WeatherDisplayData data, String source) {
        try {
            JSONObject json = new JSONObject();
            json.put("locationId", data.locationId).put("city", data.city).put("district", data.district)
                    .put("text", data.text).put("icon", data.icon).put("temperature", data.temperature)
                    .put("updatedAt", data.updatedAt).put("source", source);
            if (data.detail != null) json.put("detail", writeDetail(data.detail));
            preferences.edit().putString("data", json.toString()).apply();
        } catch (Exception ignored) { }
    }
    private static WeatherDetail readDetail(JSONObject json) {
        JSONObject detail = json.optJSONObject("detail");
        if (detail == null) return null;
        return new WeatherDetail(
                nullable(detail, "feelsLike"), nullable(detail, "humidity"),
                nullable(detail, "windDir"), nullable(detail, "windScale"),
                nullable(detail, "precip"), nullable(detail, "warning"),
                nullable(detail, "aqiValue"), nullable(detail, "aqiCategory"));
    }
    private static JSONObject writeDetail(WeatherDetail detail) throws org.json.JSONException {
        JSONObject json = new JSONObject();
        json.put("feelsLike", detail.feelsLike).put("humidity", detail.humidity)
                .put("windDir", detail.windDir).put("windScale", detail.windScale)
                .put("precip", detail.precip).put("warning", detail.warning)
                .put("aqiValue", detail.aqiValue).put("aqiCategory", detail.aqiCategory);
        return json;
    }
    private static String nullable(JSONObject json, String key) {
        return json.isNull(key) ? null : json.optString(key, null);
    }
}