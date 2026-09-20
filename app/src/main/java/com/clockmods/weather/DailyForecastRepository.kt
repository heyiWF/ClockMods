package com.clockmods.weather

import android.content.Context
import android.content.SharedPreferences
import com.clockmods.background.ClockPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DailyForecastRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun getCached(source: String, locationId: String, now: Long, timeZone: TimeZone): WeatherModels.DailyForecastData? {
        val value = preferences.getString("data", null) ?: return null
        return try {
            val json = JSONObject(value)
            if (source != json.optString("source") || (source == "manual" && locationId != json.optString("locationId")) || currentLanguage() != json.optString("lang")) return null
            read(json).takeIf { isReusable(it, now, timeZone) }
        } catch (_: Exception) { null }
    }
    fun save(data: WeatherModels.DailyForecastData, source: String) {
        try {
            val entries = JSONArray()
            data.entries.forEach { entry ->
                entries.put(JSONObject().also { json ->
                    json.put("fxDate", entry.fxDate)
                    putNullable(json, "tempMin", entry.tempMin)
                    putNullable(json, "tempMax", entry.tempMax)
                    putNullable(json, "iconDay", entry.iconDay)
                    putNullable(json, "textDay", entry.textDay)
                    putNullable(json, "windDirDay", entry.windDirDay)
                    putNullable(json, "windScaleDay", entry.windScaleDay)
                    putNullable(json, "humidity", entry.humidity)
                })
            }
            val json = JSONObject()
            json.put("source", source)
            putNullable(json, "locationId", data.locationId)
            putNullable(json, "city", data.city)
            putNullable(json, "district", data.district)
            json.put("updatedAt", data.updatedAt).put("lang", currentLanguage()).put("entries", entries)
            preferences.edit().putString("data", json.toString()).apply()
        } catch (_: Exception) { }
    }
    private fun currentLanguage() = ClockPreferences(appContext).getClockLanguage()
    private fun putNullable(json: JSONObject, key: String, value: String?) {
        if (value == null) json.remove(key) else json.put(key, value)
    }
    companion object {
        const val MAX_AGE_MS = 6L * 60L * 60L * 1000L
        private const val PREFS = "daily_forecast_cache"
        @JvmStatic fun isReusable(data: WeatherModels.DailyForecastData?, now: Long, timeZone: TimeZone): Boolean {
            if (data == null || data.updatedAt > now || now - data.updatedAt > MAX_AGE_MS) return false
            val updated = Calendar.getInstance(timeZone).apply { timeInMillis = data.updatedAt }
            val current = Calendar.getInstance(timeZone).apply { timeInMillis = now }
            if (updated.get(Calendar.YEAR) != current.get(Calendar.YEAR) || updated.get(Calendar.DAY_OF_YEAR) != current.get(Calendar.DAY_OF_YEAR)) return false
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
            repeat(3) { if (data.findByDate(format.format(current.time)) == null) return false; current.add(Calendar.DAY_OF_MONTH, 1) }
            return true
        }
        private fun read(json: JSONObject): WeatherModels.DailyForecastData {
            val values = json.getJSONArray("entries"); val entries = ArrayList<WeatherModels.DailyForecast>()
            for (index in 0 until values.length()) { val entry = values.getJSONObject(index); entries += WeatherModels.DailyForecast(entry.getString("fxDate"), entry.optString("tempMin"), entry.optString("tempMax"), entry.optString("iconDay"), entry.optString("textDay"), entry.optString("windDirDay"), entry.optString("windScaleDay"), entry.optString("humidity")) }
            return WeatherModels.DailyForecastData(json.optString("locationId"), json.optString("city"), json.optString("district"), json.optLong("updatedAt"), entries)
        }
    }
}
