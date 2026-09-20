package com.clockmods.ultimate.compose

import com.clockmods.background.ClockPreferences
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherTemperatureFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ClockPreferenceCompatibilityTest {
    private val baseTheme = ClockThemeTokens.builder()
        .primaryTextColor(0xFF010203.toInt())
        .secondaryTextColor(0xFF040506.toInt())
        .build()
    private val weatherLabels = WeatherModels.WeatherDetail.DetailLabels(
        "Feels %s\u2103",
        "Humidity %s%%",
        "Force %s",
        "Precip %s mm",
        "AQI %s",
        " Warning",
    )

    @Test
    fun legacyTransitionNamesMapToRendererValues() {
        val mappings = listOf(
            ClockPreferences.TRANSITION_FADE to ClockState.TimeTransition.FADE,
            ClockPreferences.TRANSITION_SLIDE_UP to ClockState.TimeTransition.SLIDE_UP,
            ClockPreferences.TRANSITION_SLIDE_DOWN to ClockState.TimeTransition.SLIDE_DOWN,
            ClockPreferences.TRANSITION_SCALE to ClockState.TimeTransition.SCALE,
            ClockPreferences.TRANSITION_FLIP to ClockState.TimeTransition.FLIP,
            "unsupported" to ClockState.TimeTransition.FADE,
            null to ClockState.TimeTransition.FADE,
        )

        mappings.forEach { (stored, expected) ->
            assertEquals(stored, expected, clockTimeTransition(stored))
        }
    }

    @Test
    fun proClassicUsesLegacyTimeAndDateColors() {
        val resolved = applyProClassicPreferenceColors(
            UltimateClockStyles.STYLE_PRO_CLASSIC,
            baseTheme,
            0xFF112233.toInt(),
            0xFF445566.toInt(),
        )

        assertEquals(0xFF112233.toInt(), resolved.getPrimaryTextColor())
        assertEquals(0xFF445566.toInt(), resolved.getSecondaryTextColor())
    }

    @Test
    fun otherStylesKeepTheirThemeColors() {
        val resolved = applyProClassicPreferenceColors(
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            baseTheme,
            0xFF112233.toInt(),
            0xFF445566.toInt(),
        )

        assertSame(baseTheme, resolved)
    }

    @Test
    fun basicWeatherIgnoresDetailedFieldsWhenDetailIsDisabled() {
        val state = weatherState(
            detail = WeatherModels.WeatherDetail(
                "27", "40", "NE", "2", "1.2", "Storm Warning", "35", "Good",
            ),
        )

        assertEquals(
            "Bao'an, Shenzhen 26\u2103 Clear",
            formatWeatherState(
                state,
                WeatherTemperatureFormatter.UNIT_CELSIUS,
                false,
                weatherLabels,
                15_000L,
            ),
        )
    }

    @Test
    fun detailedWeatherRotatesSummaryAndDetailItemsAtHoldInterval() {
        val state = weatherState(
            detail = WeatherModels.WeatherDetail(
                "27", "40", null, null, null, null, null, null,
            ),
        )

        val frames = (0..3).map { index ->
            formatWeatherState(
                state,
                WeatherTemperatureFormatter.UNIT_CELSIUS,
                true,
                weatherLabels,
                index * 3_000L,
            )
        }

        assertEquals(
            listOf(
                "Bao'an, Shenzhen 26\u2103 Clear",
                "Feels 27\u2103",
                "Humidity 40%",
                "Bao'an, Shenzhen 26\u2103 Clear",
            ),
            frames,
        )
    }

    @Test
    fun weatherFormattingHandlesEmptyAndMessageOnlyStates() {
        assertEquals(
            "",
            formatWeatherState(
                null,
                WeatherTemperatureFormatter.UNIT_CELSIUS,
                true,
                weatherLabels,
                0L,
            ),
        )
        assertEquals(
            "Waiting for location",
            formatWeatherState(
                WeatherModels.WeatherState.of(
                    WeatherModels.Status.LOCATION_UNAVAILABLE,
                    "Waiting for location",
                ),
                WeatherTemperatureFormatter.UNIT_CELSIUS,
                true,
                weatherLabels,
                0L,
            ),
        )
        assertEquals(
            "",
            formatWeatherState(
                weatherState(city = null, district = null, text = null, temperature = null),
                WeatherTemperatureFormatter.UNIT_CELSIUS,
                true,
                weatherLabels,
                0L,
            ),
        )
    }

    private fun weatherState(
        city: String? = "Shenzhen",
        district: String? = "Bao'an",
        text: String? = "Clear",
        temperature: String? = "26",
        detail: WeatherModels.WeatherDetail? = null,
    ) = WeatherModels.WeatherState(
        WeatherModels.Status.SUCCESS,
        WeatherModels.WeatherDisplayData(
            "101280601",
            city,
            district,
            text,
            "100",
            temperature,
            1L,
            detail,
        ),
        null,
    )
}
