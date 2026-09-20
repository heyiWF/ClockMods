package com.clockmods.widget.render

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.calendar.HolidayRepository
import com.clockmods.weather.QWeatherConfig
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherRefreshUseCase
import com.clockmods.weather.WeatherRepository
import com.clockmods.weather.WeatherTemperatureFormatter
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.model.WidgetSizeClass
import com.clockmods.widget.model.WidgetThemeSpec
import com.clockmods.widget.store.WidgetConfigStore
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/** Only this class owns widget view IDs. No clock is painted into a bitmap. */
object WidgetRemoteViewsFactory {
    @Volatile private var holidayRepository: HolidayRepository? = null

    private val digitalLayouts = arrayOf(
        intArrayOf(
            R.layout.widget_digital_compact, R.layout.widget_digital_compact_serif,
            R.layout.widget_digital_compact_mono, R.layout.widget_digital_compact_roboto,
            R.layout.widget_digital_compact_gsansdisplay, R.layout.widget_digital_compact_gsanstext,
            R.layout.widget_digital_compact_sfprodisplay, R.layout.widget_digital_compact_sfprorounded,
            R.layout.widget_digital_compact_inter, R.layout.widget_digital_compact_lato,
            R.layout.widget_digital_compact_lora, R.layout.widget_digital_compact_notosans,
            R.layout.widget_digital_compact_bitcount,
        ),
        intArrayOf(
            R.layout.widget_digital_wide, R.layout.widget_digital_wide_serif,
            R.layout.widget_digital_wide_mono, R.layout.widget_digital_wide_roboto,
            R.layout.widget_digital_wide_gsansdisplay, R.layout.widget_digital_wide_gsanstext,
            R.layout.widget_digital_wide_sfprodisplay, R.layout.widget_digital_wide_sfprorounded,
            R.layout.widget_digital_wide_inter, R.layout.widget_digital_wide_lato,
            R.layout.widget_digital_wide_lora, R.layout.widget_digital_wide_notosans,
            R.layout.widget_digital_wide_bitcount,
        ),
        intArrayOf(
            R.layout.widget_digital_tall, R.layout.widget_digital_tall_serif,
            R.layout.widget_digital_tall_mono, R.layout.widget_digital_tall_roboto,
            R.layout.widget_digital_tall_gsansdisplay, R.layout.widget_digital_tall_gsanstext,
            R.layout.widget_digital_tall_sfprodisplay, R.layout.widget_digital_tall_sfprorounded,
            R.layout.widget_digital_tall_inter, R.layout.widget_digital_tall_lato,
            R.layout.widget_digital_tall_lora, R.layout.widget_digital_tall_notosans,
            R.layout.widget_digital_tall_bitcount,
        ),
    )

    private val analogLayouts = arrayOf(
        intArrayOf(
            R.layout.widget_analog_compact, R.layout.widget_analog_compact_serif,
            R.layout.widget_analog_compact_mono, R.layout.widget_analog_compact_roboto,
            R.layout.widget_analog_compact_gsansdisplay, R.layout.widget_analog_compact_gsanstext,
            R.layout.widget_analog_compact_sfprodisplay, R.layout.widget_analog_compact_sfprorounded,
            R.layout.widget_analog_compact_inter, R.layout.widget_analog_compact_lato,
            R.layout.widget_analog_compact_lora, R.layout.widget_analog_compact_notosans,
            R.layout.widget_analog_compact_bitcount,
        ),
        intArrayOf(
            R.layout.widget_analog_medium, R.layout.widget_analog_medium_serif,
            R.layout.widget_analog_medium_mono, R.layout.widget_analog_medium_roboto,
            R.layout.widget_analog_medium_gsansdisplay, R.layout.widget_analog_medium_gsanstext,
            R.layout.widget_analog_medium_sfprodisplay, R.layout.widget_analog_medium_sfprorounded,
            R.layout.widget_analog_medium_inter, R.layout.widget_analog_medium_lato,
            R.layout.widget_analog_medium_lora, R.layout.widget_analog_medium_notosans,
            R.layout.widget_analog_medium_bitcount,
        ),
        intArrayOf(
            R.layout.widget_analog_large, R.layout.widget_analog_large_serif,
            R.layout.widget_analog_large_mono, R.layout.widget_analog_large_roboto,
            R.layout.widget_analog_large_gsansdisplay, R.layout.widget_analog_large_gsanstext,
            R.layout.widget_analog_large_sfprodisplay, R.layout.widget_analog_large_sfprorounded,
            R.layout.widget_analog_large_inter, R.layout.widget_analog_large_lato,
            R.layout.widget_analog_large_lora, R.layout.widget_analog_large_notosans,
            R.layout.widget_analog_large_bitcount,
        ),
    )

    private val weatherLayouts = arrayOf(
        intArrayOf(
            R.layout.widget_weather_compact, R.layout.widget_weather_compact_serif,
            R.layout.widget_weather_compact_mono, R.layout.widget_weather_compact_roboto,
            R.layout.widget_weather_compact_gsansdisplay, R.layout.widget_weather_compact_gsanstext,
            R.layout.widget_weather_compact_sfprodisplay, R.layout.widget_weather_compact_sfprorounded,
            R.layout.widget_weather_compact_inter, R.layout.widget_weather_compact_lato,
            R.layout.widget_weather_compact_lora, R.layout.widget_weather_compact_notosans,
            R.layout.widget_weather_compact_bitcount,
        ),
        intArrayOf(
            R.layout.widget_weather_wide, R.layout.widget_weather_wide_serif,
            R.layout.widget_weather_wide_mono, R.layout.widget_weather_wide_roboto,
            R.layout.widget_weather_wide_gsansdisplay, R.layout.widget_weather_wide_gsanstext,
            R.layout.widget_weather_wide_sfprodisplay, R.layout.widget_weather_wide_sfprorounded,
            R.layout.widget_weather_wide_inter, R.layout.widget_weather_wide_lato,
            R.layout.widget_weather_wide_lora, R.layout.widget_weather_wide_notosans,
            R.layout.widget_weather_wide_bitcount,
        ),
        intArrayOf(
            R.layout.widget_weather_large, R.layout.widget_weather_large_serif,
            R.layout.widget_weather_large_mono, R.layout.widget_weather_large_roboto,
            R.layout.widget_weather_large_gsansdisplay, R.layout.widget_weather_large_gsanstext,
            R.layout.widget_weather_large_sfprodisplay, R.layout.widget_weather_large_sfprorounded,
            R.layout.widget_weather_large_inter, R.layout.widget_weather_large_lato,
            R.layout.widget_weather_large_lora, R.layout.widget_weather_large_notosans,
            R.layout.widget_weather_large_bitcount,
        ),
    )

    private val calendarLayouts = arrayOf(
        intArrayOf(
            R.layout.widget_calendar_compact, R.layout.widget_calendar_compact_serif,
            R.layout.widget_calendar_compact_mono, R.layout.widget_calendar_compact_roboto,
            R.layout.widget_calendar_compact_gsansdisplay, R.layout.widget_calendar_compact_gsanstext,
            R.layout.widget_calendar_compact_sfprodisplay, R.layout.widget_calendar_compact_sfprorounded,
            R.layout.widget_calendar_compact_inter, R.layout.widget_calendar_compact_lato,
            R.layout.widget_calendar_compact_lora, R.layout.widget_calendar_compact_notosans,
            R.layout.widget_calendar_compact_bitcount,
        ),
        intArrayOf(
            R.layout.widget_calendar_wide, R.layout.widget_calendar_wide_serif,
            R.layout.widget_calendar_wide_mono, R.layout.widget_calendar_wide_roboto,
            R.layout.widget_calendar_wide_gsansdisplay, R.layout.widget_calendar_wide_gsanstext,
            R.layout.widget_calendar_wide_sfprodisplay, R.layout.widget_calendar_wide_sfprorounded,
            R.layout.widget_calendar_wide_inter, R.layout.widget_calendar_wide_lato,
            R.layout.widget_calendar_wide_lora, R.layout.widget_calendar_wide_notosans,
            R.layout.widget_calendar_wide_bitcount,
        ),
        intArrayOf(
            R.layout.widget_calendar_large, R.layout.widget_calendar_large_serif,
            R.layout.widget_calendar_large_mono, R.layout.widget_calendar_large_roboto,
            R.layout.widget_calendar_large_gsansdisplay, R.layout.widget_calendar_large_gsanstext,
            R.layout.widget_calendar_large_sfprodisplay, R.layout.widget_calendar_large_sfprorounded,
            R.layout.widget_calendar_large_inter, R.layout.widget_calendar_large_lato,
            R.layout.widget_calendar_large_lora, R.layout.widget_calendar_large_notosans,
            R.layout.widget_calendar_large_bitcount,
        ),
    )

    private fun holidays(context: Context): HolidayRepository {
        return holidayRepository ?: synchronized(this) {
            holidayRepository ?: HolidayRepository(context.applicationContext).also { holidayRepository = it }
        }
    }

    @JvmStatic fun createResponsive(context: Context, id: Int, kind: WidgetKind): RemoteViews {
        val config = WidgetConfigStore(context).getOrDefault(id, kind)
        val sizes = linkedMapOf<SizeF, RemoteViews>()
        val bounds = arrayOf(floatArrayOf(80f, 56f), floatArrayOf(180f, 100f), floatArrayOf(260f, 100f), floatArrayOf(180f, 180f), floatArrayOf(260f, 180f))
        bounds.forEach { bound ->
            sizes[SizeF(bound[0], bound[1])] = create(
                context,
                config,
                WidgetSizeClassResolver.resolve(bound[0], bound[1]),
            )
        }
        return RemoteViews(sizes)
    }

    @JvmStatic fun createForSize(context: Context, id: Int, kind: WidgetKind, size: WidgetSizeClass): RemoteViews =
        create(context, WidgetConfigStore(context).getOrDefault(id, kind), size)

    @JvmStatic fun create(context: Context, config: WidgetConfig, size: WidgetSizeClass): RemoteViews {
        val theme = WidgetThemeRegistry.resolve(context, config.themeId)
        val tier = when (size) {
            WidgetSizeClass.COMPACT, WidgetSizeClass.SMALL -> 0
            WidgetSizeClass.WIDE -> 1
            else -> 2
        }
        val themeColumn = when (theme.fontLayoutVariant) {
            WidgetThemeSpec.Font.SERIF -> 1
            WidgetThemeSpec.Font.MONOSPACE -> 2
            else -> 0
        }
        val column = WidgetFontRegistry.columnOf(config.fontId, themeColumn)
        val layouts = when (config.kind) {
            WidgetKind.DIGITAL -> digitalLayouts
            WidgetKind.ANALOG -> analogLayouts
            WidgetKind.WEATHER -> weatherLayouts
            WidgetKind.CALENDAR -> calendarLayouts
        }
        val views = RemoteViews(context.packageName, layouts[tier][column])
        val primary = if (config.themeId == "transparent.clean" && config.darkText) 0xff16212c.toInt() else theme.primaryTextColor
        val secondary = if (config.themeId == "transparent.clean") primary else theme.secondaryTextColor
        views.setImageViewResource(R.id.widget_background, theme.backgroundDrawableRes)
        views.setInt(R.id.widget_background, "setImageAlpha", config.backgroundAlpha)
        views.setInt(R.id.widget_settings, "setColorFilter", primary)
        views.setOnClickPendingIntent(R.id.widget_root, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.tap(config.tapAction)))
        views.setOnClickPendingIntent(R.id.widget_settings, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.Action.CONFIG))

        val now = System.currentTimeMillis()
        val zone: TimeZone = config.zone()
        val locale = context.resources.configuration.locales[0]
        val compact = size == WidgetSizeClass.COMPACT
        val date = WidgetTextFormatter.join(
            if (config.showDate) WidgetTextFormatter.formatGregorianDate(now, zone, locale) else "",
            if (config.showWeekday) WidgetTextFormatter.formatWeekday(now, zone, locale) else "",
        )
        text(views, R.id.widget_date, date, secondary, 12 * config.textScale, date.isNotEmpty() && (!compact || config.kind == WidgetKind.CALENDAR || config.kind == WidgetKind.DIGITAL))
        val lunar = if (config.showLunar) WidgetTextFormatter.formatLunar(now, zone, locale) else ""
        text(views, R.id.widget_lunar, lunar, secondary, 12 * config.textScale, config.showLunar && !compact && (tier == 2 || config.kind == WidgetKind.CALENDAR))
        var holiday = WidgetTextFormatter.formatHolidayAndSolarTerm(now, zone, locale)
        val day = holidays(context).statusOn(WidgetTextFormatter.format(now, zone, Locale.ROOT, "yyyy-MM-dd"))
        if (day != null) holiday = WidgetTextFormatter.join(holiday, day.name + " " + context.getString(if (day.offDay) R.string.widget_rest else R.string.widget_work))
        text(views, R.id.widget_holiday, holiday, secondary, 12 * config.textScale, config.showLunar && tier == 2 && holiday.isNotEmpty())
        text(views, R.id.widget_zone, zone.id.replace('_', ' '), secondary, 11 * config.textScale, config.showLocation && tier == 2 && config.kind != WidgetKind.WEATHER)
        arrayOf(R.id.widget_date, R.id.widget_lunar, R.id.widget_holiday).forEach { view ->
            views.setOnClickPendingIntent(view, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.Action.CALENDAR))
        }

        when (config.kind) {
            WidgetKind.DIGITAL, WidgetKind.WEATHER -> {
                val twelve = WidgetTextFormatter.timePattern(locale, if (config.useSystemTimeFormat) false else config.use24Hour, config.showSeconds)
                val twentyFour = WidgetTextFormatter.timePattern(locale, config.useSystemTimeFormat || config.use24Hour, config.showSeconds)
                views.setCharSequence(R.id.widget_time, "setFormat12Hour", twelve)
                views.setCharSequence(R.id.widget_time, "setFormat24Hour", twentyFour)
                views.setString(R.id.widget_time, "setTimeZone", if (config.useSystemTimeZone) null else config.timeZoneId)
                views.setTextColor(R.id.widget_time, primary)
                val clockHeight = if (config.kind == WidgetKind.WEATHER) {
                    if (compact) 28f else if (tier == 2) 48f else 36f
                } else {
                    if (compact) 32f else if (tier == 2) 68f else 48f
                }
                if (!(compact && config.kind == WidgetKind.DIGITAL)) {
                    views.setViewLayoutHeight(R.id.widget_time, clockHeight * config.textScale, TypedValue.COMPLEX_UNIT_DIP)
                }
                views.setOnClickPendingIntent(R.id.widget_time, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.Action.CLOCK))
            }
            WidgetKind.ANALOG -> {
                val dialIndex = when (config.themeId) {
                    "paper.warm" -> 2
                    "neon.night" -> 3
                    "instrument.dark" -> 1
                    else -> 0
                }
                val dials = intArrayOf(R.drawable.widget_analog_0_dial, R.drawable.widget_analog_1_dial, R.drawable.widget_analog_2_dial, R.drawable.widget_analog_3_dial)
                val hours = intArrayOf(R.drawable.widget_analog_0_hour, R.drawable.widget_analog_1_hour, R.drawable.widget_analog_2_hour, R.drawable.widget_analog_3_hour)
                val minutes = intArrayOf(R.drawable.widget_analog_0_minute, R.drawable.widget_analog_1_minute, R.drawable.widget_analog_2_minute, R.drawable.widget_analog_3_minute)
                views.setIcon(R.id.widget_analog, "setDial", Icon.createWithResource(context, dials[dialIndex]))
                views.setIcon(R.id.widget_analog, "setHourHand", Icon.createWithResource(context, hours[dialIndex]))
                views.setIcon(R.id.widget_analog, "setMinuteHand", Icon.createWithResource(context, minutes[dialIndex]))
                views.setIcon(R.id.widget_analog, "setSecondHand", null)
                views.setColorStateList(R.id.widget_analog, "setDialTintList", ColorStateList.valueOf(primary))
                views.setColorStateList(R.id.widget_analog, "setHourHandTintList", ColorStateList.valueOf(primary))
                views.setColorStateList(R.id.widget_analog, "setMinuteHandTintList", ColorStateList.valueOf(if (config.themeId == "transparent.clean") primary else theme.accentColor))
                views.setString(R.id.widget_analog, "setTimeZone", if (config.useSystemTimeZone) null else config.timeZoneId)
                views.setOnClickPendingIntent(R.id.widget_analog, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.Action.CLOCK))
            }
            WidgetKind.CALENDAR -> {
                text(views, R.id.widget_day, WidgetTextFormatter.format(now, zone, locale, "d"), primary, (if (tier == 0) 46 else 64) * config.textScale, true)
                views.setOnClickPendingIntent(R.id.widget_day, WidgetPendingIntentFactory.create(context, config.appWidgetId, WidgetPendingIntentFactory.Action.CALENDAR))
            }
        }

        if (config.kind == WidgetKind.WEATHER) {
            val preferences = ClockPreferences(context)
            val data = WeatherRepository(context).getCached(preferences.getWeatherLocationMode(), preferences.getWeatherLocationId())
            val setup = !QWeatherConfig.isConfigured() || (data == null && !WeatherRefreshUseCase.hasLocation(context, preferences))
            val temperature = if (data == null) "—" else WeatherTemperatureFormatter.format(data.temperature, preferences.getWeatherTemperatureUnit())
            text(views, R.id.widget_temperature, temperature, primary, 24 * config.textScale, true)
            val summary = if (data == null) "" else WidgetTextFormatter.join(
                if (config.showLocation) WeatherModels.locationText(data.city, data.district) else "",
                if (config.showWeatherDescription) data.text else "",
            )
            text(views, R.id.widget_weather_summary, summary, secondary, 12 * config.textScale, !compact && summary.isNotEmpty())
            val status = if (data == null) context.getString(if (setup) R.string.widget_weather_setup else R.string.widget_weather_retry)
            else context.getString(R.string.widget_weather_updated, WidgetTextFormatter.format(data.updatedAt, zone, locale, "MM-dd HH:mm"))
            text(views, R.id.widget_weather_status, status, secondary, 11 * config.textScale, data == null || !compact)
            views.setImageViewBitmap(R.id.widget_weather_icon, WidgetWeatherIconFactory.render(context, data?.icon ?: "999", preferences.isWeatherIconFill(), if (config.themeId == "transparent.clean") primary else theme.iconColor, dp(context, 40)))
            views.setOnClickPendingIntent(R.id.widget_weather_region, WidgetPendingIntentFactory.create(context, config.appWidgetId, if (setup) WidgetPendingIntentFactory.Action.WEATHER else WidgetPendingIntentFactory.Action.REFRESH))
        }

        if (config.themeId == "system.dynamic") {
            arrayOf(R.id.widget_date, R.id.widget_lunar, R.id.widget_holiday, R.id.widget_zone).forEach { views.setColor(it, "setTextColor", R.color.widget_secondary) }
            views.setColor(R.id.widget_settings, "setColorFilter", R.color.widget_primary)
            if (config.kind == WidgetKind.DIGITAL || config.kind == WidgetKind.WEATHER) views.setColor(R.id.widget_time, "setTextColor", R.color.widget_primary)
            if (config.kind == WidgetKind.CALENDAR) views.setColor(R.id.widget_day, "setTextColor", R.color.widget_primary)
            if (config.kind == WidgetKind.WEATHER) {
                views.setColor(R.id.widget_temperature, "setTextColor", R.color.widget_primary)
                views.setColor(R.id.widget_weather_summary, "setTextColor", R.color.widget_secondary)
                views.setColor(R.id.widget_weather_status, "setTextColor", R.color.widget_secondary)
            }
        }
        return views
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun text(views: RemoteViews, id: Int, value: String, color: Int, size: Float, visible: Boolean) {
        views.setTextViewText(id, value)
        views.setTextColor(id, color)
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, size)
        views.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)
    }
}
