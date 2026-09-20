package com.clockmods.weather

import java.util.Collections

object WeatherModels {
    class WeatherDisplayData(
        @JvmField val locationId: String?, @JvmField val city: String?, @JvmField val district: String?,
        @JvmField val text: String?, @JvmField val icon: String?, @JvmField val temperature: String?,
        @JvmField val updatedAt: Long, @JvmField val detail: WeatherDetail? = null,
    ) {
        constructor(locationId: String?, city: String?, district: String?, text: String?, icon: String?, temperature: String?, updatedAt: Long) : this(locationId, city, district, text, icon, temperature, updatedAt, null)
        fun satisfies(detailedRequired: Boolean): Boolean = !detailedRequired || detail != null
    }

    class WeatherDetail(
        @JvmField val feelsLike: String?, @JvmField val humidity: String?, @JvmField val windDir: String?, @JvmField val windScale: String?,
        @JvmField val precip: String?, @JvmField val warning: String?, @JvmField val aqiValue: String?, @JvmField val aqiCategory: String?,
    ) {
        class DetailLabels(
            @JvmField val feelsFormat: String, @JvmField val humidityFormat: String, @JvmField val windScaleFormat: String,
            @JvmField val precipFormat: String, @JvmField val airFormat: String, @JvmField val warningSuffix: String,
        )
        fun carouselItems(labels: DetailLabels, temperatureUnit: String?): List<String> {
            val items = ArrayList<String>()
            if (present(feelsLike)) items += String.format(WeatherTemperatureFormatter.replaceUnit(labels.feelsFormat, temperatureUnit) ?: labels.feelsFormat, WeatherTemperatureFormatter.numeric(feelsLike, temperatureUnit))
            if (present(humidity)) items += String.format(labels.humidityFormat, humidity)
            if (present(windDir) || present(windScale)) { var wind = if (present(windDir)) windDir!! else ""; if (present(windScale)) wind += (if (wind.isNotEmpty()) " " else "") + String.format(labels.windScaleFormat, windScale); items += wind }
            if (hasPrecipitation()) items += String.format(labels.precipFormat, precip)
            warning?.split("\n")?.take(20)?.forEach { item -> val trimmed = item.trim(); if (trimmed.isNotEmpty()) items += if (trimmed.contains(labels.warningSuffix.trim())) trimmed else trimmed + labels.warningSuffix }
            if (present(aqiValue)) items += (String.format(labels.airFormat, aqiValue) + if (present(aqiCategory)) " $aqiCategory" else "")
            return items
        }
        private fun hasPrecipitation() = present(precip) && (precip!!.toDoubleOrNull() ?: 0.0) > 0.0
        private fun present(value: String?) = !value.isNullOrBlank()
    }

    class DailyForecast(@JvmField val fxDate: String, @JvmField val tempMin: String?, @JvmField val tempMax: String?, @JvmField val iconDay: String?, @JvmField val textDay: String?, @JvmField val windDirDay: String?, @JvmField val windScaleDay: String?, @JvmField val humidity: String?)
    class DailyForecastData(@JvmField val locationId: String?, @JvmField val city: String?, @JvmField val district: String?, @JvmField val updatedAt: Long, entries: List<DailyForecast>) {
        @JvmField val entries: List<DailyForecast> = Collections.unmodifiableList(ArrayList(entries))
        fun findByDate(date: String?): DailyForecast? = entries.firstOrNull { it.fxDate == date }
    }
    enum class Status { IDLE, LOADING, SUCCESS, PERMISSION_DENIED, LOCATION_UNAVAILABLE, NETWORK_ERROR, API_ERROR, CONFIG_ERROR }
    class WeatherState(@JvmField val status: Status, @JvmField val data: WeatherDisplayData?, @JvmField val message: String?) {
        companion object { @JvmStatic fun of(status: Status, message: String?) = WeatherState(status, null, message) }
    }
    class DailyForecastState(@JvmField val status: Status, @JvmField val data: DailyForecastData?, @JvmField val message: String?) {
        companion object { @JvmStatic fun of(status: Status, message: String?) = DailyForecastState(status, null, message) }
    }
    @JvmStatic fun locationText(city: String?, district: String?): String { if (city == null) return district ?: ""; val display = if (city.endsWith("市")) city.dropLast(1) else city; if (district.isNullOrEmpty() || city == district || display == district) return display; return if (containsHan(display) || containsHan(district)) display + district else "$district, $display" }
    private fun containsHan(text: String) = text.any { it.code in 0x4E00..0x9FFF }
    @JvmStatic fun intervals(): List<Int> = Collections.unmodifiableList(listOf(10, 30, 60, 180, 360, 720))
}
