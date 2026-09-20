package com.clockmods.weather

import android.content.Context
import com.clockmods.background.ClockPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class QWeatherClient @JvmOverloads constructor(context: Context, private val host: String, private val timeoutMs: Int = 15000) {
    private val socketFactory = TlsSocketFactory.create(context.applicationContext)
    private val lang = apiLang(ClockPreferences(context).getClockLanguage())
    @Throws(Exception::class) fun fetch(latitude: Double, longitude: Double) = fetch(latitude, longitude, false)
    @Throws(Exception::class) fun fetch(latitude: Double, longitude: Double, detailed: Boolean): WeatherModels.WeatherDisplayData { val location = getLocation(latitude, longitude); return fetchNow(location.getString("id"), location.optString("adm2", ""), location.optString("name", ""), optDouble(location, "lat", latitude), optDouble(location, "lon", longitude), detailed) }
    @Throws(Exception::class) fun fetchLocation(locationId: String, city: String, district: String) = fetchNow(locationId, city, district, Double.NaN, Double.NaN, false)
    @Throws(Exception::class) fun fetchLocation(locationId: String, city: String, district: String, latitude: Double, longitude: Double, detailed: Boolean) = fetchNow(locationId, city, district, latitude, longitude, detailed)
    @Throws(Exception::class) fun fetchDaily(latitude: Double, longitude: Double): WeatherModels.DailyForecastData { val location = getLocation(latitude, longitude); return fetchDailyLocation(location.getString("id"), location.optString("adm2", ""), location.optString("name", "")) }
    @Throws(Exception::class) fun fetchDailyLocation(locationId: String, city: String, district: String) = parseDailyForecast(request("/v7/weather/3d?location=${urlEncode(locationId)}&lang=$lang"), locationId, city, district, System.currentTimeMillis())
    private fun fetchNow(locationId: String, city: String, district: String, latitude: Double, longitude: Double, detailed: Boolean): WeatherModels.WeatherDisplayData { val now = request("/v7/weather/now?location=${urlEncode(locationId)}&lang=$lang").getJSONObject("now"); return WeatherModels.WeatherDisplayData(locationId, city, district, now.optString("text", ""), now.optString("icon", ""), now.optString("temp", "--"), System.currentTimeMillis(), if (detailed) buildDetail(now, latitude, longitude) else null) }
    private fun buildDetail(now: JSONObject, latitude: Double, longitude: Double): WeatherModels.WeatherDetail { val warning = if (!latitude.isNaN() && !longitude.isNaN()) fetchWarning(latitude, longitude) else null; val aqi: Array<String?> = if (!latitude.isNaN() && !longitude.isNaN()) fetchAirQuality(latitude, longitude) else arrayOfNulls(2); return WeatherModels.WeatherDetail(now.optString("feelsLike", ""), now.optString("humidity", ""), now.optString("windDir", ""), now.optString("windScale", ""), now.optString("precip", ""), warning, aqi[0], aqi[1]) }
    private fun fetchWarning(latitude: Double, longitude: Double): String? = try { formatWarnings(requestRaw("/weatheralert/v1/current/${formatCoordinate(latitude)}/${formatCoordinate(longitude)}?lang=$lang").optJSONArray("alerts"), lang) } catch (_: Exception) { null }
    private fun fetchAirQuality(latitude: Double, longitude: Double): Array<String?> {
        return try {
            val indexes = requestRaw("/airquality/v1/current/${formatCoordinate(latitude)}/${formatCoordinate(longitude)}?lang=$lang").optJSONArray("indexes") ?: return arrayOfNulls(2)
            if (indexes.length() == 0) return arrayOfNulls(2)
            var index = indexes.getJSONObject(0)
            for (i in 0 until indexes.length()) if (indexes.getJSONObject(i).optString("code") == "qaqi") { index = indexes.getJSONObject(i); break }
            arrayOf(index.optString("aqiDisplay", "").takeIf(String::isNotEmpty), index.optString("category", "").takeIf(String::isNotEmpty))
        } catch (_: Exception) { arrayOfNulls(2) }
    }
    private fun getLocation(latitude: Double, longitude: Double): JSONObject { val locations = request("/geo/v2/city/lookup?location=${formatLocation(latitude, longitude)}&range=cn&number=1&lang=$lang").getJSONArray("location"); if (locations.length() == 0) throw IOException("No QWeather location"); return locations.getJSONObject(0) }
    private fun request(path: String): JSONObject = requestRaw(path).also { if (it.optString("code") != "200") throw IOException("QWeather error: ${it.optString("code")}") }
    private fun requestRaw(path: String): JSONObject { val connection = URL("https://$host$path").openConnection() as HttpURLConnection; try { if (connection is HttpsURLConnection && socketFactory != null) connection.sslSocketFactory = socketFactory; connection.requestMethod = "GET"; connection.connectTimeout = timeoutMs; connection.readTimeout = timeoutMs; connection.setRequestProperty("Authorization", "Bearer " + QWeatherSigner.token(QWeatherConfig.credentialId(), QWeatherConfig.developerId(), QWeatherConfig.projectId(), QWeatherConfig.privateKeyBase64(), System.currentTimeMillis() / 1000L)); val status = connection.responseCode; val response = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: throw IOException("Empty QWeather response"); val json = JSONObject(response); if (status !in 200..299) throw IOException("QWeather error: ${json.optString("code", status.toString())}"); return json } finally { connection.disconnect() } }

    companion object {
        @JvmStatic fun apiLang(clockLanguage: String?): String = when (clockLanguage) { ClockPreferences.LANGUAGE_ENGLISH -> "en"; ClockPreferences.LANGUAGE_TRADITIONAL -> "zh-hant"; else -> "zh" }
        @JvmStatic @Throws(Exception::class) fun parseDailyForecast(body: JSONObject, locationId: String, city: String, district: String, updatedAt: Long): WeatherModels.DailyForecastData { val daily = body.optJSONArray("daily") ?: throw IOException("Empty daily forecast"); val entries = ArrayList<WeatherModels.DailyForecast>(); for (index in 0 until daily.length()) { val day = daily.optJSONObject(index) ?: continue; val date = day.optString("fxDate", "").trim(); if (date.isNotEmpty()) entries += WeatherModels.DailyForecast(date, day.optString("tempMin", ""), day.optString("tempMax", ""), day.optString("iconDay", ""), day.optString("textDay", ""), day.optString("windDirDay", ""), day.optString("windScaleDay", ""), day.optString("humidity", "")) }; if (entries.isEmpty()) throw IOException("Empty daily forecast"); return WeatherModels.DailyForecastData(locationId, city, district, updatedAt, entries) }
        @JvmStatic fun formatWarnings(alerts: JSONArray?): String? = formatWarnings(alerts, "zh")
        @JvmStatic fun formatWarnings(alerts: JSONArray?, lang: String): String? { if (alerts == null || alerts.length() == 0) return null; val values = ArrayList<String>(); for (index in 0 until minOf(alerts.length(), 20)) alerts.optJSONObject(index)?.let { formatWarning(it, lang)?.takeIf(String::isNotEmpty)?.let(values::add) }; return values.joinToString("\n").takeIf(String::isNotEmpty) }
        @JvmStatic fun formatWarning(alert: JSONObject): String? = formatWarning(alert, "zh")
        @JvmStatic fun formatWarning(alert: JSONObject, lang: String): String? { alert.optString("headline", "").trim().takeIf(String::isNotEmpty)?.let { return it }; val event = alert.optJSONObject("eventType")?.optString("name", "")?.trim().orEmpty(); if (event.isEmpty()) return null; val code = alert.optJSONObject("color")?.optString("code", "")?.trim().orEmpty(); if (lang == "en") return event + warningColorEnglish(code).let { if (it.isEmpty()) " Warning" else " $it Warning" }; val traditional = lang == "zh-hant"; return event + warningColorChinese(code, traditional) + if (traditional) "預警" else "预警" }
        private fun warningColorEnglish(code: String): String = when (code.lowercase(Locale.US)) { "white" -> "White"; "gray" -> "Gray"; "green" -> "Green"; "blue" -> "Blue"; "yellow" -> "Yellow"; "amber" -> "Amber"; "orange" -> "Orange"; "red" -> "Red"; "purple" -> "Purple"; "black" -> "Black"; else -> code }
        private fun warningColorChinese(code: String, traditional: Boolean): String { val common = mapOf("white" to "白色", "gray" to "灰色", "amber" to "琥珀色", "orange" to "橙色", "black" to "黑色"); val simplified = mapOf("green" to "绿色", "blue" to "蓝色", "yellow" to "黄色", "red" to "红色", "purple" to "紫色"); val traditionalMap = mapOf("green" to "綠色", "blue" to "藍色", "yellow" to "黃色", "red" to "紅色", "purple" to "紫色"); val lower = code.lowercase(Locale.US); return common[lower] ?: (if (traditional) traditionalMap[lower] else simplified[lower]) ?: code }
        @JvmStatic fun formatLocation(latitude: Double, longitude: Double) = String.format(Locale.US, "%.2f,%.2f", longitude, latitude)
        @JvmStatic fun formatCoordinate(value: Double) = String.format(Locale.US, "%.2f", value)
        private fun optDouble(objectValue: JSONObject, key: String, fallback: Double) = objectValue.optString(key, "").toDoubleOrNull() ?: fallback
        private fun urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")
    }
}
