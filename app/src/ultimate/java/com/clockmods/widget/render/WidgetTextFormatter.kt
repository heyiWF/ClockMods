package com.clockmods.widget.render

import android.text.format.DateFormat
import com.clockmods.calendar.LunarCalendar
import com.clockmods.pro.LunarAlmanac
import com.clockmods.weather.WeatherModels
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WidgetTextFormatter {
    @JvmStatic fun format(now: Long, zone: TimeZone, locale: Locale, pattern: String): String =
        SimpleDateFormat(pattern, locale).apply { timeZone = zone }.format(Date(now))

    @JvmStatic fun formatGregorianDate(now: Long, zone: TimeZone, locale: Locale): String =
        format(now, zone, locale, if (locale.language == "zh") "M月d日" else "MMM d")

    @JvmStatic fun formatWeekday(now: Long, zone: TimeZone, locale: Locale): String =
        format(now, zone, locale, "EEEE")

    @JvmStatic fun timePattern(use24: Boolean, seconds: Boolean): String =
        (if (use24) "HH:mm" else "hh:mm") + (if (seconds) ":ss" else "") + (if (use24) "" else " a")

    @JvmStatic fun timePattern(locale: Locale, use24: Boolean, seconds: Boolean): String {
        val skeleton = (if (use24) "HH" else "hh") + "mm" + (if (seconds) "ss" else "")
        return DateFormat.getBestDateTimePattern(locale, skeleton).takeUnless { it.isNullOrBlank() }
            ?: timePattern(use24, seconds)
    }

    @JvmStatic fun formatLunar(now: Long, zone: TimeZone, locale: Locale): String {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now }
        return LunarCalendar.formatNatural(calendar)
    }

    @JvmStatic fun formatHolidayAndSolarTerm(now: Long, zone: TimeZone, locale: Locale): String {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now }
        return join(*LunarAlmanac.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).festivals().toTypedArray())
    }

    @JvmStatic fun formatWeatherSummary(data: WeatherModels.WeatherDisplayData?, includeLocation: Boolean): String {
        if (data == null) return ""
        return join(if (includeLocation) WeatherModels.locationText(data.city, data.district) else "", data.text)
    }

    @JvmStatic fun join(vararg values: String?): String = values.asSequence()
        .filterNotNull()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(" · ")
}
