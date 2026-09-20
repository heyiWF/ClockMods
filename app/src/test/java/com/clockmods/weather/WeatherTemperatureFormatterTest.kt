package com.clockmods.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTemperatureFormatterTest {
    @Test fun keepsCelsiusValuesAndAddsCelsiusSymbol() {
        assertEquals("25", WeatherTemperatureFormatter.numeric("25", WeatherTemperatureFormatter.UNIT_CELSIUS))
        assertEquals("25\u2103", WeatherTemperatureFormatter.format("25", WeatherTemperatureFormatter.UNIT_CELSIUS))
    }

    @Test fun convertsCelsiusToRoundedFahrenheit() {
        assertEquals("32", WeatherTemperatureFormatter.numeric("0", WeatherTemperatureFormatter.UNIT_FAHRENHEIT))
        assertEquals("77", WeatherTemperatureFormatter.numeric("25", WeatherTemperatureFormatter.UNIT_FAHRENHEIT))
        assertEquals("75\u2109", WeatherTemperatureFormatter.format("24", WeatherTemperatureFormatter.UNIT_FAHRENHEIT))
    }

    @Test fun preservesPlaceholdersAndNormalizesUnknownUnits() {
        assertEquals("--\u2109", WeatherTemperatureFormatter.format("--", WeatherTemperatureFormatter.UNIT_FAHRENHEIT))
        assertEquals(WeatherTemperatureFormatter.UNIT_CELSIUS, WeatherTemperatureFormatter.normalizeUnit("kelvin"))
        assertEquals("Feels 77\u2109", WeatherTemperatureFormatter.replaceUnit("Feels 77\u2103", WeatherTemperatureFormatter.UNIT_FAHRENHEIT))
    }
}
