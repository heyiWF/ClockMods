package com.clockmods.weather

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object WeatherTemperatureFormatter {
    const val UNIT_CELSIUS = "celsius"
    const val UNIT_FAHRENHEIT = "fahrenheit"
    const val CELSIUS_SYMBOL = "\u2103"
    const val FAHRENHEIT_SYMBOL = "\u2109"

    @JvmStatic
    fun normalizeUnit(unit: String?): String =
        if (unit?.trim()?.lowercase(Locale.US) == UNIT_FAHRENHEIT) UNIT_FAHRENHEIT else UNIT_CELSIUS

    @JvmStatic
    fun isFahrenheit(unit: String?): Boolean = UNIT_FAHRENHEIT == normalizeUnit(unit)

    @JvmStatic
    fun symbol(unit: String?): String = if (isFahrenheit(unit)) FAHRENHEIT_SYMBOL else CELSIUS_SYMBOL

    @JvmStatic
    fun numeric(celsius: String?, unit: String?): String {
        if (celsius == null) return ""
        val raw = celsius.trim()
        if (raw.isEmpty() || !isFahrenheit(unit)) return raw
        return try {
            BigDecimal(raw).multiply(BigDecimal.valueOf(9L))
                .divide(BigDecimal.valueOf(5L), 0, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(32L)).toPlainString()
        } catch (_: NumberFormatException) {
            raw
        }
    }

    @JvmStatic
    fun format(celsius: String?, unit: String?): String {
        val value = numeric(celsius, unit)
        return if (value.isEmpty()) value else value + symbol(unit)
    }

    @JvmStatic
    fun replaceUnit(formatted: String?, unit: String?): String? {
        if (formatted.isNullOrEmpty()) return formatted
        val replacement = symbol(unit)
        return formatted.replace("\u2103", replacement).replace("\u2109", replacement)
            .replace("\u00b0C", replacement).replace("\u00b0F", replacement)
    }
}
