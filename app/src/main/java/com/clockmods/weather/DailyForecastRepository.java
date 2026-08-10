package com.clockmods.weather;

import android.content.Context;
import android.content.SharedPreferences;

import com.clockmods.background.ClockPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.clockmods.weather.WeatherModels.DailyForecast;
import static com.clockmods.weather.WeatherModels.DailyForecastData;

public final class DailyForecastRepository {
    static final long MAX_AGE_MS = 6L * 60L * 60L * 1000L;
    private static final String PREFS = "daily_forecast_cache";
    private final Context appContext;
    private final SharedPreferences preferences;

    public DailyForecastRepository(Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public DailyForecastData getCached(String source, String locationId, long now, TimeZone timeZone) {
        String value = preferences.getString("data", null);
        if (value == null) return null;
        try {
            JSONObject json = new JSONObject(value);
            if (!source.equals(json.optString("source"))) return null;
            if ("manual".equals(source) && !locationId.equals(json.optString("locationId"))) return null;
            // Drop forecast text cached in a different language after a language switch.
            if (!currentLanguage().equals(json.optString("lang"))) return null;
            DailyForecastData data = read(json);
            return isReusable(data, now, timeZone) ? data : null;
        } catch (Exception ignored) { return null; }
    }

    public void save(DailyForecastData data, String source) {
        try {
            JSONObject json = new JSONObject();
            json.put("source", source).put("locationId", data.locationId)
                    .put("city", data.city).put("district", data.district)
                    .put("updatedAt", data.updatedAt).put("lang", currentLanguage());
            JSONArray entries = new JSONArray();
            for (DailyForecast entry : data.entries) {
                entries.put(new JSONObject().put("fxDate", entry.fxDate)
                        .put("tempMin", entry.tempMin).put("tempMax", entry.tempMax)
                        .put("iconDay", entry.iconDay).put("textDay", entry.textDay)
                        .put("windDirDay", entry.windDirDay).put("windScaleDay", entry.windScaleDay)
                        .put("humidity", entry.humidity));
            }
            json.put("entries", entries);
            preferences.edit().putString("data", json.toString()).apply();
        } catch (Exception ignored) { }
    }

    private String currentLanguage() {
        return new ClockPreferences(appContext).getClockLanguage();
    }

    static boolean isReusable(DailyForecastData data, long now, TimeZone timeZone) {
        if (data == null || data.updatedAt > now || now - data.updatedAt > MAX_AGE_MS) return false;
        Calendar updated = Calendar.getInstance(timeZone);
        updated.setTimeInMillis(data.updatedAt);
        Calendar current = Calendar.getInstance(timeZone);
        current.setTimeInMillis(now);
        if (updated.get(Calendar.YEAR) != current.get(Calendar.YEAR)
                || updated.get(Calendar.DAY_OF_YEAR) != current.get(Calendar.DAY_OF_YEAR)) return false;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(timeZone);
        for (int offset = 0; offset < 3; offset++) {
            if (data.findByDate(format.format(current.getTime())) == null) return false;
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
        return true;
    }

    private static DailyForecastData read(JSONObject json) throws Exception {
        JSONArray values = json.getJSONArray("entries");
        List<DailyForecast> entries = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            JSONObject entry = values.getJSONObject(index);
            entries.add(new DailyForecast(entry.getString("fxDate"), entry.optString("tempMin"),
                    entry.optString("tempMax"), entry.optString("iconDay"),
                    entry.optString("textDay"), entry.optString("windDirDay"),
                    entry.optString("windScaleDay"), entry.optString("humidity")));
        }
        return new DailyForecastData(json.optString("locationId"), json.optString("city"),
                json.optString("district"), json.optLong("updatedAt"), entries);
    }
}