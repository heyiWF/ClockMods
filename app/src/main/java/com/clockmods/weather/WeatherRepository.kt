package com.clockmods.weather

import android.content.Context
import android.content.SharedPreferences
import com.clockmods.background.ClockPreferences
import org.json.JSONObject

class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCached(source: String, locationId: String): WeatherModels.WeatherDisplayData? {
        val value = preferences.getString("data", null) ?: return null
        return try {
            val json = JSONObject(value)
            if (source != json.optString("source", "automatic")) return null
            if (source == "manual" && locationId != json.optString("locationId")) return null
            if (currentLanguage() != json.optString("lang")) return null
            WeatherModels.WeatherDisplayData(json.optString("locationId"), json.optString("city"), json.optString("district"), json.optString("text"), json.optString("icon"), json.optString("temperature"), json.optLong("updatedAt"), readDetail(json))
        } catch (_: Exception) { null }
    }

    fun save(data: WeatherModels.WeatherDisplayData, source: String) {
        try {
            val json = JSONObject()
            putNullable(json, "locationId", data.locationId)
            putNullable(json, "city", data.city)
            putNullable(json, "district", data.district)
            putNullable(json, "text", data.text)
            putNullable(json, "icon", data.icon)
            putNullable(json, "temperature", data.temperature)
            json.put("updatedAt", data.updatedAt).put("source", source).put("lang", currentLanguage())
            data.detail?.let { json.put("detail", writeDetail(it)) }
            preferences.edit().putString("data", json.toString()).apply()
            appContext.sendBroadcast(android.content.Intent("com.clockmods.widget.WEATHER_CHANGED").setComponent(android.content.ComponentName(appContext.packageName, "com.clockmods.widget.update.WidgetDataChangedReceiver")))
        } catch (_: Exception) { }
    }

    private fun currentLanguage() = ClockPreferences(appContext).getClockLanguage()
    private fun readDetail(json: JSONObject): WeatherModels.WeatherDetail? = json.optJSONObject("detail")?.let { detail -> WeatherModels.WeatherDetail(nullable(detail, "feelsLike"), nullable(detail, "humidity"), nullable(detail, "windDir"), nullable(detail, "windScale"), nullable(detail, "precip"), nullable(detail, "warning"), nullable(detail, "aqiValue"), nullable(detail, "aqiCategory")) }
    private fun writeDetail(detail: WeatherModels.WeatherDetail) = JSONObject().also { json ->
        putNullable(json, "feelsLike", detail.feelsLike)
        putNullable(json, "humidity", detail.humidity)
        putNullable(json, "windDir", detail.windDir)
        putNullable(json, "windScale", detail.windScale)
        putNullable(json, "precip", detail.precip)
        putNullable(json, "warning", detail.warning)
        putNullable(json, "aqiValue", detail.aqiValue)
        putNullable(json, "aqiCategory", detail.aqiCategory)
    }
    private fun putNullable(json: JSONObject, key: String, value: String?) {
        if (value == null) json.remove(key) else json.put(key, value)
    }
    private fun nullable(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.optString(key)
    companion object { private const val PREFS = "weather_cache" }
}
