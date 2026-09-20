package com.clockmods.widget

import com.clockmods.weather.WeatherModels
import com.clockmods.widget.render.WidgetTextFormatter
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetTextFormatterTest {
    @Test
    fun formatsRespectZoneAndLocale() {
        val now = Instant.parse("2026-09-18T17:00:00Z").toEpochMilli()
        val shanghai = TimeZone.getTimeZone("Asia/Shanghai")
        val newYork = TimeZone.getTimeZone("America/New_York")
        assertEquals("9月19日", WidgetTextFormatter.formatGregorianDate(now, shanghai, Locale.CHINA))
        assertEquals("Sep 18", WidgetTextFormatter.formatGregorianDate(now, newYork, Locale.ENGLISH))
        assertEquals("星期六", WidgetTextFormatter.formatWeekday(now, shanghai, Locale.TAIWAN))
        assertEquals("HH:mm:ss", WidgetTextFormatter.timePattern(true, true))
        assertEquals("h:mm a", WidgetTextFormatter.timePattern(false, false))
        assertEquals("h:mm a", WidgetTextFormatter.timePattern(Locale.ENGLISH, false, false))
        assertEquals("HH:mm:ss", WidgetTextFormatter.timePattern(Locale.CHINA, true, true))
        assertNotEquals(
            WidgetTextFormatter.formatLunar(now, shanghai, Locale.CHINA),
            WidgetTextFormatter.formatLunar(now, newYork, Locale.CHINA),
        )
        assertEquals("A · B", WidgetTextFormatter.join("", null, "A", "B", "A"))
        assertEquals("", WidgetTextFormatter.formatWeatherSummary(null, true))
        val data = WeatherModels.WeatherDisplayData("1", null, null, "Clear", "100", "20", now)
        assertEquals("Clear", WidgetTextFormatter.formatWeatherSummary(data, true))
        for (day in 1..30) {
            val text = WidgetTextFormatter.formatHolidayAndSolarTerm(
                now + day * 86_400_000L,
                shanghai,
                Locale.CHINA,
            )
            assertFalse(text.startsWith(" · "))
            assertFalse(text.endsWith(" · "))
        }
    }
}
