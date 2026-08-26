package com.clockmods.weather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Formats temperatures returned by QWeather for display. */
public final class WeatherTemperatureFormatter {
    public static final String UNIT_CELSIUS = "celsius";
    public static final String UNIT_FAHRENHEIT = "fahrenheit";

    // Keep this source file ASCII-only; Java resolves these escapes at runtime.
    public static final String CELSIUS_SYMBOL = "\u2103";
    public static final String FAHRENHEIT_SYMBOL = "\u2109";

    private WeatherTemperatureFormatter() { }

    public static String normalizeUnit(String unit) {
        if (unit == null) return UNIT_CELSIUS;
        String normalized = unit.trim().toLowerCase(Locale.US);
        return UNIT_FAHRENHEIT.equals(normalized) ? UNIT_FAHRENHEIT : UNIT_CELSIUS;
    }

    public static boolean isFahrenheit(String unit) {
        return UNIT_FAHRENHEIT.equals(normalizeUnit(unit));
    }

    public static String symbol(String unit) {
        return isFahrenheit(unit) ? FAHRENHEIT_SYMBOL : CELSIUS_SYMBOL;
    }

    /** Converts a raw Celsius value to a display value without a unit suffix. */
    public static String numeric(String celsius, String unit) {
        if (celsius == null) return "";
        String raw = celsius.trim();
        if (raw.length() == 0 || !isFahrenheit(unit)) return raw;
        try {
            BigDecimal value = new BigDecimal(raw);
            return value.multiply(BigDecimal.valueOf(9L))
                    .divide(BigDecimal.valueOf(5L), 0, RoundingMode.HALF_UP)
                    .add(BigDecimal.valueOf(32L))
                    .toPlainString();
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    /** Converts a raw Celsius value and appends the selected unit symbol. */
    public static String format(String celsius, String unit) {
        String value = numeric(celsius, unit);
        return value.length() == 0 ? value : value + symbol(unit);
    }

    /** Replaces common Celsius/Fahrenheit suffixes in a localized formatted string. */
    public static String replaceUnit(String formatted, String unit) {
        if (formatted == null || formatted.length() == 0) return formatted;
        String symbol = symbol(unit);
        return formatted.replace("\u2103", symbol)
                .replace("\u2109", symbol)
                .replace("\u00b0C", symbol)
                .replace("\u00b0F", symbol);
    }
}
