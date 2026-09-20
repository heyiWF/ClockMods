package com.clockmods.weather

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert
import org.junit.Test

class DailyForecastRepositoryTest {
    private val zone = TimeZone.getTimeZone("Asia/Shanghai")

    @Test fun cacheRequiresThreeDatesAndExpiresAtMidnightOrSixHours() {
        val updatedAt = millis(2026, Calendar.JULY, 28, 20, 0)
        val complete = data(updatedAt, "2026-07-28", "2026-07-29", "2026-07-30")
        Assert.assertTrue(DailyForecastRepository.isReusable(complete, millis(2026, Calendar.JULY, 28, 23, 59), zone))
        Assert.assertFalse(DailyForecastRepository.isReusable(complete, millis(2026, Calendar.JULY, 29, 0, 1), zone))
        Assert.assertFalse(
            DailyForecastRepository.isReusable(
                data(updatedAt, "2026-07-28", "2026-07-30"),
                millis(2026, Calendar.JULY, 28, 21, 0),
                zone,
            ),
        )
        Assert.assertFalse(DailyForecastRepository.isReusable(complete, millis(2026, Calendar.JULY, 29, 2, 1), zone))
    }

    private fun data(updatedAt: Long, vararg dates: String): WeatherModels.DailyForecastData {
        val entries = dates.map {
            WeatherModels.DailyForecast(it, "20", "30", "100", "晴", "", "", "")
        }
        return WeatherModels.DailyForecastData("101280601", "深圳市", "南山区", updatedAt, entries)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
