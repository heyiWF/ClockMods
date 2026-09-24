package com.clockmods.ultimate.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.calendar.LunarCalendar
import com.clockmods.pro.CalendarDashboardSizing as Sizing
import com.clockmods.pro.LunarAlmanac
import com.clockmods.pro.schedule.ScheduleItem
import com.clockmods.ui.ClockTypefaceResolver
import com.clockmods.ui.ClockTimeText
import com.clockmods.ui.WeatherIcon
import com.clockmods.weather.DailyForecastController
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherTemperatureFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

private val LocalMonthPanelHeight = staticCompositionLocalOf { 0f }

/** Ultimate 1abc6b8 geometry, rendered with Compose. No per-day cards or shared detail sidebar. */
@Composable
internal fun UltimateCalendarLayout(
    modifier: Modifier, theme: ComposeCalendarTheme, typography: CalendarTypography,
    preferences: ClockPreferences, cells: List<CalendarCellInfo>, selected: CalendarCellInfo,
    adjacentCells: (Int) -> List<CalendarCellInfo>,
    weekdays: List<String>, monthTitle: String, timeZone: TimeZone, clockTick: () -> Long,
    weatherState: WeatherModels.WeatherState?, refreshGeneration: Int,
    scheduleItems: List<ScheduleItem>, onPrevious: () -> Unit, onNext: () -> Unit,
    onToday: () -> Unit, onSelect: (CalendarCellInfo) -> Unit, onMonthPicker: () -> Unit,
    onAddSchedule: () -> Unit, onEditSchedule: (ScheduleItem) -> Unit,
) {
    val context = LocalContext.current
    var forecast by remember { mutableStateOf<WeatherModels.DailyForecastData?>(null) }
    val forecastController = remember(context) { DailyForecastController(context) { state ->
        if (state.data != null) forecast = state.data
    } }
    DisposableEffect(theme.showWeather, refreshGeneration, weatherState?.status) {
        forecast = null
        if (theme.showWeather && preferences.isWeatherEnabled()) forecastController.start()
        onDispose(forecastController::stop)
    }
    DisposableEffect(forecastController) { onDispose(forecastController::shutdown) }
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(theme.backgroundStart), Color(theme.backgroundEnd)))).semantics { testTagsAsResourceId = true }.testTag("calendar:${theme.id}")) {
        val landscape = maxWidth > maxHeight
        val gutter = if (landscape) 8.dp else 5.dp
        val grid: @Composable (Modifier) -> Unit = { gridModifier ->
            CalendarSwipePager("${theme.id}:${selected.day.year}-${selected.day.month}",
                gridModifier, onPrevious, onNext,
                adjacent = { direction ->
                    val page = adjacentCells(direction)
                    val targetDay = selected.day.dayOfMonth.coerceAtMost(
                        page.last { it.day.currentMonth }.day.dayOfMonth)
                    val selectedDay = page.firstOrNull {
                        it.day.currentMonth && it.day.dayOfMonth == targetDay
                    } ?: page.first { it.day.currentMonth }
                    val preview: @Composable () -> Unit = {
                        OriginalMonthGrid(page, weekdays, selectedDay, theme, typography,
                            preferences.isCalendarHighlightWeekends(), {}, Modifier.fillMaxSize())
                    }
                    preview
                },
                current = { OriginalMonthGrid(cells, weekdays, selected, theme, typography,
                    preferences.isCalendarHighlightWeekends(), onSelect, Modifier.fillMaxSize()) })
        }
        val showAttribution = theme.showWeather && preferences.isWeatherEnabled()
        Box(Modifier.fillMaxSize().padding(bottom = if (showAttribution) 20.dp else 0.dp)) {
            when (theme.layout) {
                CalendarLayout.DASHBOARD, CalendarLayout.WALL -> {
                    val panel: @Composable (Modifier) -> Unit = { panelModifier ->
                        OriginalMonthPanel(theme, typography, selected, monthTitle,
                            onPrevious, onNext, onToday, onMonthPicker, panelModifier, grid)
                    }
                    if (theme.layout == CalendarLayout.WALL) panel(Modifier.fillMaxSize().padding(gutter))
                    else if (landscape) {
                        Row(Modifier.fillMaxSize().padding(gutter), horizontalArrangement = Arrangement.spacedBy(gutter)) {
                            DashboardReadings(theme, typography, preferences, weatherState, forecast,
                                clockTick, timeZone, true, gutter.value,
                                Modifier.weight(1f).fillMaxHeight())
                            panel(Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        Column(Modifier.fillMaxSize().padding(gutter), verticalArrangement = Arrangement.spacedBy(gutter)) {
                            DashboardReadings(theme, typography, preferences, weatherState, forecast,
                                clockTick, timeZone, false, gutter.value,
                                Modifier.weight(.46f).fillMaxWidth())
                            panel(Modifier.weight(.54f).fillMaxWidth())
                        }
                    }
                }
                CalendarLayout.POSTER -> PosterCalendar(theme, typography, selected, landscape, onToday, grid)
                CalendarLayout.AGENDA -> AgendaCalendar(theme, typography, preferences, cells, selected,
                    adjacentCells,
                    monthTitle, timeZone, landscape, weatherState, forecast, scheduleItems,
                    onPrevious, onNext, onToday, onSelect, onMonthPicker, onAddSchedule, onEditSchedule)
            }
        }
        if (showAttribution) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp),
                contentAlignment = Alignment.Center) {
                WeatherAttribution(Color(theme.backgroundEnd))
            }
        }
    }
}

private fun Modifier.calendarPanel(theme: ComposeCalendarTheme): Modifier {
    val shape = RoundedCornerShape(theme.cornerRadiusDp.dp)
    return clip(shape).background(Color(theme.panel)).then(
        if (theme.panelStroke != 0) Modifier.border(1.dp, Color(theme.panelStroke), shape) else Modifier)
}

@Composable
private fun CalendarText(text: String, size: Float, color: Int, typography: CalendarTypography,
    modifier: Modifier = Modifier, emphasized: Boolean = false, maxLines: Int = 1,
    date: Boolean = false) {
    Text(text, modifier, color = Color(color), maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = if (date) typography.dateStyle(TextStyle(fontSize = size.sp), text, emphasized)
            else typography.supportingStyle(TextStyle(fontSize = size.sp, lineHeight = (size * 1.18f).sp),
                text, emphasized))
}

@Composable
private fun OriginalMonthPanel(theme: ComposeCalendarTheme, typography: CalendarTypography,
    selected: CalendarCellInfo, monthTitle: String, previous: () -> Unit, next: () -> Unit,
    today: () -> Unit, picker: () -> Unit, modifier: Modifier,
    grid: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier.calendarPanel(theme).padding(if (LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) 5.dp else 10.dp).testTag("month-panel")) {
        val h = maxHeight.value
        val toolbar = Sizing.monthToolbarHeight(h, 1f)
        val footer = Sizing.monthFooterHeight(h, 1f)
        val dateFactor = typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE
        val titleSize = min(Sizing.monthTitleSize(toolbar, 1f), toolbar * .68f / dateFactor)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(toolbar.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(previous, Modifier.size(toolbar.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month), tint = Color(theme.secondary))
                }
                Text(monthTitle, Modifier.weight(1f).clickable(onClick = picker).testTag("month-title"), textAlign = TextAlign.Center,
                    color = Color(theme.text), maxLines = 1,
                    style = typography.dateStyle(TextStyle(fontSize = titleSize.sp), monthTitle))
                IconButton(today, Modifier.size(toolbar.dp)) {
                    Icon(Icons.Default.CalendarMonth, stringResource(R.string.calendar_today), tint = Color(theme.accent))
                }
                IconButton(next, Modifier.size(toolbar.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month), tint = Color(theme.secondary))
                }
            }
            CompositionLocalProvider(LocalMonthPanelHeight provides h) {
                grid(Modifier.fillMaxWidth().weight(1f))
            }
            CalendarFooter(selected, theme, typography, footer, Modifier.fillMaxWidth().height(footer.dp))
        }
    }
}

@Composable
private fun OriginalMonthGrid(cells: List<CalendarCellInfo>, weekdays: List<String>, selection: CalendarCellInfo,
    theme: ComposeCalendarTheme, typography: CalendarTypography, highlightWeekends: Boolean,
    onSelect: (CalendarCellInfo) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.testTag("month-grid")) {
        val header = if (theme.flatGrid) 22f else Sizing.monthWeekdayHeight(LocalMonthPanelHeight.current, 1f)
        val weekdayBase = if (theme.flatGrid) Sizing.posterWeekdaySize(maxWidth.value / 7, header, 1f)
            else Sizing.monthWeekdaySize(maxWidth.value / 7, header, 1f)
        val weekdaySize = min(weekdayBase,
            header * .8f / (typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(header.dp), verticalAlignment = Alignment.CenterVertically) {
                weekdays.forEachIndexed { index, label ->
                    val weekend = cells.getOrNull(index)?.day?.dayOfWeek in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
                    Text(label, Modifier.weight(1f), color = Color(if (weekend && highlightWeekends) theme.weekend else theme.weekday),
                        textAlign = TextAlign.Center, style = typography.dateStyle(TextStyle(fontSize = weekdaySize.sp), label))
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    week.forEach { cell -> OriginalDayCell(cell, cell.day == selection.day, theme, typography,
                        highlightWeekends, { onSelect(cell) }, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun OriginalDayCell(cell: CalendarCellInfo, selected: Boolean, theme: ComposeCalendarTheme,
    typography: CalendarTypography, highlightWeekends: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val description = stringResource(if (cell.day.today) R.string.calendar_day_today_accessibility else R.string.calendar_day_accessibility,
        cell.day.dayOfMonth, cell.lunar)
    BoxWithConstraints(modifier.alpha(if (cell.day.currentMonth) 1f else if (theme.flatGrid) .18f else .38f)
        .then(if (selected && !theme.flatGrid) Modifier.background(Color(theme.selectionFill), shape)
            .border(1.dp, Color(theme.selectionStroke), shape) else Modifier)
        .clickable(onClick = onSelect).semantics { contentDescription = description; this.selected = selected }
        .testTag("day:${cell.day.year}-${cell.day.month + 1}-${cell.day.dayOfMonth}")
        .padding(horizontal = if (theme.flatGrid) 0.dp else 1.dp, vertical = if (theme.flatGrid) 0.dp else 2.dp), contentAlignment = Alignment.Center) {
        val w = maxWidth.value
        val h = maxHeight.value
        val number = if (theme.flatGrid) Sizing.posterCellNumberSize(w, h, 1f) else Sizing.monthDaySize(w, h + 4, 1f)
        val lunar = Sizing.monthLunarSize(w, h + 4, 1f)
        val labelHeight = if (theme.flatGrid) Sizing.posterMarkHeight(number, 1f) else Sizing.monthLabelHeight(h + 4, lunar)
        val numberHeight = (if (theme.flatGrid) h - labelHeight else min(number * 1.5f, h - labelHeight)).coerceAtLeast(1f)
        val weekend = cell.day.dayOfWeek in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val color = when { cell.day.today -> theme.today; weekend && highlightWeekends -> theme.weekend; else -> theme.day }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CalendarNumber(cell, color, theme, typography, if (theme.flatGrid) number else 0f,
                Modifier.fillMaxWidth().height(numberHeight.dp))
            if (theme.flatGrid) {
                Canvas(Modifier.fillMaxWidth().height(labelHeight.dp)) {
                    if (selected) drawLine(Color(if (cell.day.today) theme.today else theme.selectionStroke),
                        androidx.compose.ui.geometry.Offset(size.width * .22f, size.height / 2),
                        androidx.compose.ui.geometry.Offset(size.width * .78f, size.height / 2), maxOf(1.5.dp.toPx(), size.height * .3f))
                    else if (cell.day.today) drawCircle(Color(theme.today), maxOf(1.dp.toPx(), min(size.width, size.height) * .22f))
                }
            } else Box(Modifier.fillMaxWidth().height(labelHeight.dp), contentAlignment = Alignment.Center) {
                LunarCarouselText(listOf(cell.lunar) + cell.festivals,
                    Color(if (cell.festivals.isEmpty()) theme.secondary else theme.text),
                    typography.supportingStyle(TextStyle(fontSize = lunar.sp), cell.lunar))
            }
        }
    }
}

/** Same ink-centred digit, halo and raised rest/work badge as CalendarDayNumberView. */
@Composable
private fun CalendarNumber(cell: CalendarCellInfo, color: Int, theme: ComposeCalendarTheme,
    typography: CalendarTypography, pinnedSize: Float, modifier: Modifier, emphasized: Boolean = false) {
    val context = LocalContext.current
    val face = remember(typography, emphasized) { ClockTypefaceResolver.resolve(context, typography.family,
        if (emphasized) typography.emphasizedWeight else typography.weight) }
    val badge = if (theme.flatGrid || theme.layout == CalendarLayout.AGENDA) "" else cell.holiday?.let {
        stringResource(if (it.offDay) R.string.calendar_day_status_off else R.string.calendar_day_status_work) }.orEmpty()
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG) }
    val badgePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG) }
    val rect = remember { Rect() }
    Canvas(modifier) {
        val day = cell.day.dayOfMonth.toString()
        paint.typeface = face
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (if (pinnedSize > 0) pinnedSize * density else Sizing.monthDaySize(size.width, size.height, density)) *
            typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE
        paint.getTextBounds(day, 0, day.length, rect)
        val heightFit = size.height * .72f / rect.height().coerceAtLeast(1)
        val widthFit = size.width * .72f / paint.measureText(day).coerceAtLeast(1f)
        paint.textSize *= minOf(1f, heightFit, widthFit)
        if (badge.isNotEmpty()) {
            badgePaint.typeface = face
            badgePaint.textSize = paint.textSize * .48f
            val rightExtent = paint.measureText(day) / 2f + 2.dp.toPx() +
                badgePaint.measureText(badge)
            paint.textSize *= min(1f, (size.width / 2f - 1.dp.toPx()).coerceAtLeast(1f) /
                rightExtent.coerceAtLeast(1f))
        }
        paint.getTextBounds(day, 0, day.length, rect)
        val baseline = size.height / 2 - (rect.top + rect.bottom) / 2f
        if (cell.day.today && theme.todayFill != 0 && !theme.flatGrid && theme.layout != CalendarLayout.AGENDA) {
            drawCircle(Color(theme.todayFill), min(paint.textSize * .78f, min(size.width, size.height) / 2))
        }
        paint.color = color
        drawContext.canvas.nativeCanvas.drawText(day, size.width / 2, baseline, paint)
        if (badge.isNotEmpty()) {
            badgePaint.typeface = face; badgePaint.textSize = paint.textSize * .48f
            badgePaint.color = if (cell.holiday?.offDay == true) theme.restBadge else theme.workBadge
            drawContext.canvas.nativeCanvas.drawText(badge, size.width / 2 + paint.measureText(day) / 2 + 2.dp.toPx(),
                baseline + paint.ascent() * .42f, badgePaint)
        }
    }
}

@Composable
private fun CalendarFooter(cell: CalendarCellInfo, theme: ComposeCalendarTheme, typography: CalendarTypography,
    height: Float, modifier: Modifier) {
    CalendarFooterCarousel(cell, calendarDateLine(cell), theme, typography, height, modifier)
}

@Composable
private fun calendarDateLine(cell: CalendarCellInfo): String {
    val date = Calendar.getInstance().apply { clear(); set(cell.day.year, cell.day.month, cell.day.dayOfMonth) }
    val prefs = ClockPreferences(LocalContext.current)
    val solar = com.clockmods.ui.DateFormatter.format(
        if (prefs.isClockUseEnglish()) prefs.getDatePatternEn() else prefs.getDatePatternCn(), date,
        com.clockmods.LocaleManager.dateLang(prefs.getClockLanguage()))
    return stringResource(R.string.calendar_selected_date, solar, LunarAlmanac.of(cell.day.year, cell.day.month, cell.day.dayOfMonth).naturalLabel())
}

@Composable
private fun PosterCalendar(theme: ComposeCalendarTheme, typography: CalendarTypography, selection: CalendarCellInfo,
    landscape: Boolean, today: () -> Unit, grid: @Composable (Modifier) -> Unit) {
    val masthead: @Composable (Modifier) -> Unit = { modifier ->
        val date = remember(selection.day) { Calendar.getInstance().apply {
            clear(); set(selection.day.year, selection.day.month, selection.day.dayOfMonth)
        } }
        val lunar = remember(selection.day) { LunarCalendar.formatNatural(date) }
        Column(modifier.testTag("poster-masthead")) {
            CalendarText(lunar, if (landscape) 17f else 15f, theme.secondary, typography,
                Modifier.fillMaxWidth().testTag("poster-selected-lunar"))
            if (selection.festivals.isNotEmpty()) {
                CalendarText(selection.festivals.joinToString(" · "), if (landscape) 14f else 12f,
                    theme.accent, typography, Modifier.fillMaxWidth().testTag("poster-selected-festivals"))
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PosterWordmark(selection.day.year, selection.day.month, theme, typography,
                    Modifier.fillMaxSize().padding(end = if (landscape) 18.dp else 0.dp))
                Text(stringResource(R.string.calendar_today), Modifier.align(if (landscape) Alignment.BottomStart else Alignment.BottomEnd)
                    .clickable(onClick = today).padding(horizontal = 10.dp, vertical = 12.dp),
                    color = Color(theme.accent), fontSize = 11.sp, letterSpacing = 1.76.sp)
            }
        }
    }
    if (landscape) Row(Modifier.fillMaxSize().padding(start = 30.dp, top = 18.dp, end = 30.dp, bottom = 16.dp)) {
        masthead(Modifier.weight(.36f).fillMaxHeight())
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(theme.secondary).copy(alpha = .32f)))
        Spacer(Modifier.width(20.dp))
        grid(Modifier.weight(.64f).fillMaxHeight())
    } else Column(Modifier.fillMaxSize().padding(start = 26.dp, top = 20.dp, end = 26.dp, bottom = 18.dp)) {
        masthead(Modifier.fillMaxWidth().weight(.30f))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(theme.secondary).copy(alpha = .32f)))
        Spacer(Modifier.height(10.dp))
        grid(Modifier.fillMaxWidth().weight(.70f))
    }
}

@Composable
private fun PosterWordmark(year: Int, month: Int, theme: ComposeCalendarTheme,
    typography: CalendarTypography, modifier: Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val monthText = if (locale.language == "zh") listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")[month]
        else SimpleDateFormat("MMMM", locale).format(Calendar.getInstance().apply { clear(); set(year, month, 1) }.time)
    val monthPaint = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight) } }
    val yearPaint = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.weight) } }
    Canvas(modifier.semantics { contentDescription = "$monthText $year" }) {
        val dateFactor = typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE
        val monthBound = Sizing.posterWordmarkSize(size.width, size.height, density)
        monthPaint.textSize = monthBound * dateFactor
        monthPaint.textSize *= min(1f, size.width * .94f / monthPaint.measureText(monthText))
        yearPaint.textSize = Sizing.posterYearSize(monthBound, size.height, density) * dateFactor
        yearPaint.textSize *= min(1f, size.width * .30f / yearPaint.measureText(year.toString()))
        val initialHeight = monthPaint.descent() - monthPaint.ascent() +
            monthPaint.textSize * .12f + yearPaint.descent() - yearPaint.ascent()
        val heightFit = min(1f, size.height * .94f / initialHeight.coerceAtLeast(1f))
        monthPaint.textSize *= heightFit
        yearPaint.textSize *= heightFit
        monthPaint.color = theme.text; yearPaint.color = theme.secondary
        val mh = monthPaint.descent() - monthPaint.ascent()
        val yh = yearPaint.descent() - yearPaint.ascent()
        val gap = monthPaint.textSize * .12f
        val top = (size.height - mh - gap - yh) / 2
        drawContext.canvas.nativeCanvas.drawText(monthText, 0f, top - monthPaint.ascent(), monthPaint)
        drawContext.canvas.nativeCanvas.drawText(year.toString(), 0f, top + mh + gap - yearPaint.ascent(), yearPaint)
    }
}

@Composable
private fun DashboardReadings(theme: ComposeCalendarTheme, typography: CalendarTypography,
    preferences: ClockPreferences, weather: WeatherModels.WeatherState?, forecast: WeatherModels.DailyForecastData?,
    tick: () -> Long, zone: TimeZone, landscape: Boolean, gutter: Float, modifier: Modifier) {
    Column(modifier.testTag("dashboard-readings"), verticalArrangement = Arrangement.spacedBy(gutter.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(if (landscape) 1.06f else .98f).calendarPanel(theme)) {
            val statusScale = minOf(
                fitStatusPillScale(preferences.getStatusIconScale(), maxWidth.value - 24f),
                ((maxHeight.value - 20f) / 68f).coerceAtLeast(ClockPreferences.MIN_STATUS_ICON_SCALE),
            )
            val clockHeight = maxHeight.value -
                if (preferences.isShowStatusIcons()) 34f * statusScale + 10f else 0f
            val clockSize = minOf(
                Sizing.clockTimeSize(maxWidth.value, clockHeight, preferences.isShowSeconds(), 1f),
                maxWidth.value / if (preferences.isShowSeconds()) 5.3f else 3.7f,
            )
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 5.dp)) {
                if (preferences.isShowStatusIcons()) DeviceStatusPill(rememberDeviceStatus(), statusScale, true,
                    Color(preferences.getTimeColor()), Color(theme.panel))
                val clock = Calendar.getInstance(zone).apply { timeInMillis = tick() }
                val time = SimpleDateFormat(if (preferences.isUse24Hour()) "HH:mm" else "hh:mm", Locale.US).apply { timeZone = zone }.format(clock.time)
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.Center) {
                    StableCalendarTime(time, clockSize, Color(preferences.getTimeColor()), typography,
                        Modifier.alignByBaseline())
                    if (preferences.isShowSeconds()) Column(Modifier.alignBy(androidx.compose.ui.layout.LastBaseline)) {
                        if (!preferences.isUse24Hour()) CalendarText(SimpleDateFormat("a", LocalConfiguration.current.locales[0]).format(clock.time), clockSize * .25f, theme.secondary, typography)
                        val seconds = String.format(Locale.US, ":%02d", clock.get(Calendar.SECOND))
                        StableCalendarTime(seconds, clockSize * .46f, Color(theme.accent), typography)
                    }
                }
                }
            }
        }
        if (preferences.isWeatherEnabled()) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(if (landscape) .94f else .89f).calendarPanel(theme)) {
            val temperatureSize = Sizing.weatherTemperatureSize(maxWidth.value, maxHeight.value, 1f)
            val iconSize = Sizing.weatherIconSize(maxWidth.value, maxHeight.value, 1f)
            val summarySize = Sizing.weatherSummarySize(maxWidth.value, maxHeight.value, 1f)
            val data = weather?.data
            Row(Modifier.fillMaxSize().padding(horizontal = Sizing.spacing(maxWidth.value * .02f, 1f, 12f).dp,
                vertical = Sizing.spacing(maxHeight.value * .035f, 1f, 8f).dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(.8f).offset(x = if (landscape) 0.dp else 6.dp), contentAlignment = Alignment.Center) {
                    if (weather?.status == WeatherModels.Status.LOADING && data == null) CircularProgressIndicator(Modifier.size(iconSize.dp), color = Color(theme.accent))
                    else CalendarWeatherIcon(data?.icon, theme.weatherIcon, preferences, Modifier.size(iconSize.dp))
                }
                Column(Modifier.weight(2.2f).offset(x = (-6).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        CalendarText(temperature(data?.temperature, preferences), temperatureSize, theme.text, typography, Modifier.offset(y = 5.dp), emphasized = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CalendarText(stringResource(R.string.calendar_feels_like), temperatureSize * .26f, theme.secondary, typography)
                            CalendarText(temperature(data?.detail?.feelsLike, preferences), temperatureSize * .53f, theme.accent, typography)
                        }
                    }
                    val summary = listOfNotNull(data?.let { WeatherModels.locationText(it.city, it.district) }, data?.text,
                        data?.detail?.windDir, data?.detail?.windScale?.let { stringResource(R.string.weather_wind_scale_format, it) },
                        data?.detail?.humidity?.let { stringResource(R.string.weather_humidity_format, it) }).joinToString(" ")
                        .ifBlank { weather?.message.orEmpty() }
                    CalendarText(summary, summarySize, theme.text, typography, Modifier.padding(top = 4.dp))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(if (landscape) 1f else .98f).calendarPanel(theme)) {
            val side = Sizing.spacing(maxWidth.value * .012f, 1f, 8f)
            val cw = (maxWidth.value - side * 2) / 3
            val h = maxHeight.value
            val headingSize = Sizing.forecastHeadingSize(cw, h, 1f)
            val bodySize = Sizing.forecastTextSize(cw, h, 1f)
            val iconSize = Sizing.forecastIconSize(cw, h, 1f)
            val headings = listOf(R.string.calendar_today, R.string.calendar_tomorrow, R.string.calendar_after_tomorrow)
            Row(Modifier.fillMaxSize().padding(horizontal = side.dp)) {
                repeat(3) { index ->
                    val date = Calendar.getInstance(zone).apply { timeInMillis = tick(); add(Calendar.DAY_OF_MONTH, index) }
                    val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(date.time)
                    val entry = forecast?.findByDate(key)
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CalendarText(stringResource(headings[index]), headingSize, if (index == 0) theme.accent else theme.text, typography, emphasized = true)
                            CalendarText(entry?.textDay.orEmpty(), headingSize, theme.text, typography)
                        }
                        Spacer(Modifier.height(Sizing.spacing(h * .05f, 1f, 15f).dp))
                        CalendarWeatherIcon(entry?.iconDay, if (index == 0) theme.weatherIcon else theme.forecastIcon, preferences, Modifier.size(iconSize.dp))
                        Spacer(Modifier.height(Sizing.spacing(h * .05f, 1f, 15f).dp))
                        CalendarText(if (entry == null) "—" else temperatureRange(entry, preferences), bodySize, theme.text, typography)
                        if (entry != null) CalendarText(listOfNotNull(
                            entry.humidity?.let { stringResource(R.string.weather_humidity_format, it) }, entry.windDirDay,
                            entry.windScaleDay?.let { stringResource(R.string.weather_wind_scale_format, it) }).joinToString(" "),
                            Sizing.forecastDetailSize(cw, h, 1f), theme.secondary, typography,
                            Modifier.padding(top = Sizing.spacing(h * .024f, 1f, 8f).dp))
                    }
                }
            }
        }
        }
    }
}

private fun temperature(value: String?, preferences: ClockPreferences): String =
    if (value.isNullOrBlank()) "—" else WeatherTemperatureFormatter.numeric(value, preferences.getWeatherTemperatureUnit()) +
        WeatherTemperatureFormatter.symbol(preferences.getWeatherTemperatureUnit())

@Composable
private fun alignedClockText(text: String, typography: CalendarTypography): androidx.compose.ui.text.AnnotatedString {
    val context = LocalContext.current
    val shift = remember(typography) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight)
            textSize = 1000f
        }
        com.clockmods.ui.ClockTimeText.colonBaselineOffset(paint) / paint.ascent()
    }
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        text.forEachIndexed { index, char -> if (char == ':') addStyle(
            androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift(shift)), index, index + 1) }
    }
}

@Composable
private fun StableCalendarTime(
    time: String, size: Float, color: Color, typography: CalendarTypography,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val style = typography.timeStyle(TextStyle(fontSize = size.sp), time, true)
    val paint = remember(context, typography, style.fontSize, density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight)
            textSize = with(density) { style.fontSize.toPx() }
        }
    }
    Row(modifier.clearAndSetSemantics { contentDescription = time }) {
        time.forEachIndexed { index, character ->
            val width = (ClockTimeText.slotWidth(time, index, paint) / density.density).dp
            Text(
                alignedClockText(character.toString(), typography),
                Modifier.width(width).alignByBaseline(),
                color = color,
                style = style,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun temperatureRange(entry: WeatherModels.DailyForecast, preferences: ClockPreferences): String =
    WeatherTemperatureFormatter.replaceUnit(stringResource(R.string.weather_temperature_range_format,
        WeatherTemperatureFormatter.numeric(entry.tempMin, preferences.getWeatherTemperatureUnit()),
        WeatherTemperatureFormatter.numeric(entry.tempMax, preferences.getWeatherTemperatureUnit())), preferences.getWeatherTemperatureUnit()).orEmpty()

@Composable
private fun CalendarWeatherIcon(code: String?, color: Int, preferences: ClockPreferences, modifier: Modifier) {
    val context = LocalContext.current
    val icon = remember(code, preferences.isWeatherIconFill()) { WeatherIcon.load(context, code ?: "999", preferences.isWeatherIconFill()) }
    val paint = remember(color) { Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color } }
    Canvas(modifier) { icon?.draw(drawContext.canvas.nativeCanvas, 0f, 0f, min(size.width, size.height), paint) }
}

@Composable
private fun AgendaCalendar(theme: ComposeCalendarTheme, typography: CalendarTypography, preferences: ClockPreferences,
    cells: List<CalendarCellInfo>, selection: CalendarCellInfo,
    adjacentCells: (Int) -> List<CalendarCellInfo>, title: String, zone: TimeZone, landscape: Boolean,
    weather: WeatherModels.WeatherState?, forecast: WeatherModels.DailyForecastData?, schedule: List<ScheduleItem>,
    previous: () -> Unit, next: () -> Unit, today: () -> Unit, select: (CalendarCellInfo) -> Unit,
    picker: () -> Unit, add: () -> Unit, edit: (ScheduleItem) -> Unit) {
    val week = cells.chunked(7).first { selection in it }
    val cursor = Calendar.getInstance(zone).apply { firstDayOfWeek = preferences.getCalendarWeekStart(); clear(); set(selection.day.year, selection.day.month, selection.day.dayOfMonth) }
    val subtitle = stringResource(R.string.calendar_week_of_year, cursor.get(Calendar.WEEK_OF_YEAR))
    val panelHeight = LocalConfiguration.current.screenHeightDp - if (landscape) 28f else 32f
    val chromeSize = Sizing.monthTitleSize(Sizing.monthToolbarHeight(panelHeight, 1f), 1f)
    val supportSize = Sizing.agendaSubheadingSize(Sizing.monthFooterHeight(panelHeight, 1f), 1f)
    val header: @Composable () -> Unit = {
        Column { CalendarText(title, min(chromeSize, 36f * ClockPreferences.DEFAULT_DATE_FONT_SCALE / typography.dateScale),
                theme.text, typography, Modifier.clickable(onClick = picker).testTag("month-title"),
                emphasized = true, date = true)
            CalendarText(subtitle, supportSize, theme.secondary, typography, Modifier.padding(top = 3.dp)) }
    }
    val actions: @Composable (Float) -> Unit = { statusScale ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            CalendarText(stringResource(R.string.calendar_today), supportSize, theme.accent, typography,
                Modifier.clickable(onClick = today).padding(horizontal = 8.dp).heightIn(min = if (landscape) 32.dp else 36.dp))
            if (preferences.isShowStatusIcons()) DeviceStatusPill(rememberDeviceStatus(), statusScale,
                true, Color(theme.text), Color(theme.backgroundStart))
        }
    }
    val strip: @Composable (Modifier) -> Unit = { stripModifier ->
        val stripPage: @Composable (List<CalendarCellInfo>, CalendarCellInfo?, Boolean) -> Unit =
            { page, selectedDay, interactive ->
                val click: (CalendarCellInfo) -> Unit = if (interactive) select else { _ -> }
                if (landscape) Column(Modifier.fillMaxSize()) {
                    page.forEach { cell -> AgendaStripCell(cell, cell.day == selectedDay?.day,
                        theme, typography, true, preferences.isCalendarHighlightWeekends(),
                        { click(cell) }, Modifier.weight(1f).fillMaxWidth()) }
                } else Row(Modifier.fillMaxSize()) {
                    page.forEach { cell -> AgendaStripCell(cell, cell.day == selectedDay?.day,
                        theme, typography, false, preferences.isCalendarHighlightWeekends(),
                        { click(cell) }, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        CalendarSwipePager("agenda:${week.first().day.year}-${week.first().day.month}-${week.first().day.dayOfMonth}",
            stripModifier.testTag("week-strip"), previous, next,
            adjacent = { direction ->
                val page = adjacentCells(direction)
                val selectedDay = page.firstOrNull { it.day.dayOfWeek == selection.day.dayOfWeek }
                val preview: @Composable () -> Unit = { stripPage(page, selectedDay, false) }
                preview
            },
            current = { stripPage(week, selection, true) })
    }
    if (landscape) Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(.36f).fillMaxHeight()) { header(); Spacer(Modifier.height(12.dp)); strip(Modifier.weight(1f).fillMaxWidth()) }
        Column(Modifier.weight(.64f).fillMaxHeight()) { BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                actions(fitStatusPillScale(preferences.getStatusIconScale(), maxWidth.value - 60f))
            }
            Spacer(Modifier.height(8.dp)); AgendaCard(selection, theme, typography, preferences, weather, forecast, schedule, add, edit, Modifier.fillMaxWidth().weight(1f)) }
    } else Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth.value
            if (preferences.isShowStatusIcons() && maxWidth < 420.dp) {
                Column(Modifier.fillMaxWidth()) {
                    header()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        actions(fitStatusPillScale(preferences.getStatusIconScale(), availableWidth - 60f))
                    }
                }
            } else {
                val statusScale = fitStatusPillScale(preferences.getStatusIconScale(), availableWidth - 110f)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { header() }
                    actions(statusScale)
                }
            }
        }
        Spacer(Modifier.height(14.dp)); strip(Modifier.fillMaxWidth().height(90.dp)); Spacer(Modifier.height(14.dp))
        AgendaCard(selection, theme, typography, preferences, weather, forecast, schedule, add, edit, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun AgendaStripCell(cell: CalendarCellInfo, selected: Boolean, theme: ComposeCalendarTheme,
    typography: CalendarTypography, rail: Boolean, weekends: Boolean, click: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val context = LocalContext.current
    val labels = context.resources.getStringArray(R.array.calendar_weekday_names)
    BoxWithConstraints(modifier.then(if (selected) Modifier.background(Color(theme.selectionFill), shape)
        .border(1.dp, Color(theme.selectionStroke), shape) else Modifier).clickable(onClick = click)
        .testTag("week-day:${cell.day.dayOfMonth}").semantics { this.selected = selected }) {
        val number = Sizing.agendaStripNumberSize(maxWidth.value, maxHeight.value, 1f)
        val label = Sizing.agendaStripLunarSize(maxWidth.value, maxHeight.value, 1f)
        val dayColor = when { cell.day.today -> theme.today; weekends && cell.day.dayOfWeek in listOf(1, 7) -> theme.weekend; else -> theme.day }
        val weekdayColor = if (weekends && cell.day.dayOfWeek in listOf(1, 7)) theme.weekend else theme.weekday
        val lunarItems = cell.festivals.ifEmpty { listOf(cell.lunar) }
        val lunarColor = if (cell.festivals.isEmpty()) theme.secondary else theme.accent
        val markerColor = if (cell.day.today) Color(theme.today) else Color.Transparent
        if (rail) Row(Modifier.fillMaxSize().padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(2.5.dp).fillMaxHeight().background(markerColor))
            Spacer(Modifier.width(6.dp))
            CalendarText(labels[cell.day.dayOfWeek - 1], label, weekdayColor, typography, Modifier.weight(1.1f))
            CalendarNumber(cell, dayColor, theme, typography, number, Modifier.weight(2.6f).fillMaxHeight(), true)
            Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) { CalendarText(lunarItems.first(), label, lunarColor, typography) }
            Spacer(Modifier.width(6.dp))
        } else Column(Modifier.fillMaxSize().padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CalendarText(labels[cell.day.dayOfWeek - 1], label, weekdayColor, typography)
            CalendarNumber(cell, dayColor, theme, typography, number, Modifier.fillMaxWidth().weight(1f), true)
            LunarCarouselText(lunarItems, Color(lunarColor), typography.supportingStyle(TextStyle(fontSize = label.sp), cell.lunar))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(2.5.dp).background(markerColor))
        }
    }
}

@Composable
private fun AgendaCard(cell: CalendarCellInfo, theme: ComposeCalendarTheme, typography: CalendarTypography,
    preferences: ClockPreferences, weather: WeatherModels.WeatherState?, forecast: WeatherModels.DailyForecastData?,
    schedule: List<ScheduleItem>, add: () -> Unit, edit: (ScheduleItem) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.calendarPanel(theme).padding(18.dp).testTag("agenda-card")) {
        val titleSize = Sizing.agendaDetailTitleSize(maxWidth.value, maxHeight.value, 1f)
        val body = Sizing.agendaDetailBodySize(maxWidth.value, maxHeight.value, 1f)
        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", cell.day.year, cell.day.month + 1, cell.day.dayOfMonth)
        val entry = forecast?.findByDate(dateKey)
        val data = if (cell.day.today) weather?.data else null
        Column(Modifier.fillMaxSize()) {
            CalendarText(calendarDateLine(cell),
                min(titleSize, 36f * ClockPreferences.DEFAULT_DATE_FONT_SCALE / typography.dateScale),
                theme.text, typography,
                emphasized = true, maxLines = 2, date = true)
            if (cell.festivals.isNotEmpty()) CalendarText(cell.festivals.joinToString(" · "), body, theme.accent, typography, Modifier.padding(top = 10.dp), maxLines = 2)
            if (data != null || entry != null) {
                Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(theme.panelStroke)); Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalendarWeatherIcon(data?.icon ?: entry?.iconDay, theme.weatherIcon, preferences, Modifier.size(40.dp))
                    if (data != null) CalendarText(temperature(data.temperature, preferences), 22f, theme.text, typography, emphasized = true)
                    CalendarText(if (entry == null) data?.text.orEmpty() else "${entry.textDay.orEmpty()} ${temperatureRange(entry, preferences)}", body, theme.secondary, typography, Modifier.weight(1f), maxLines = 2)
                }
            }
            Spacer(Modifier.height(14.dp))
            PinnedAlmanac(stringResource(R.string.calendar_suitable_prefix), cell.suitable, theme.suitable, body, typography)
            Spacer(Modifier.height(6.dp))
            PinnedAlmanac(stringResource(R.string.calendar_avoid_prefix), cell.avoid, theme.avoid, body, typography)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CalendarText(stringResource(R.string.ultimate_schedule_title), body, theme.text, typography, Modifier.weight(1f), true)
                TextButton(onClick = add) { Text(stringResource(R.string.ultimate_schedule_add), color = Color(theme.accent)) }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (schedule.isEmpty()) CalendarText(stringResource(R.string.ultimate_schedule_empty), body * 1.08f, theme.secondary, typography)
                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    schedule.forEach { item -> CalendarText("${item.timeLabel()}  ${item.title}", body * 1.08f, theme.text, typography,
                        Modifier.fillMaxWidth().clickable { edit(item) }.padding(vertical = 8.dp), maxLines = 2) }
                }
            }
        }
    }
}

@Composable
private fun PinnedAlmanac(prefix: String, items: List<String>, color: Int, textSizeSp: Float, typography: CalendarTypography) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val glyphPaint = remember(typography, prefix) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size((textSizeSp * 1.55f).dp)) {
            val radius = minOf(this.size.width, this.size.height) / 2f
            drawCircle(Color(color).copy(alpha = ALMANAC_BADGE_BACKGROUND_ALPHA), radius)
            glyphPaint.color = color
            glyphPaint.textSize = minOf(
                textSizeSp * .85f * typography.supportingScale * density * fontScale,
                radius * 1.25f,
            )
            drawCenteredAlmanacGlyph(drawContext.canvas.nativeCanvas, prefix,
                this.size.width / 2f, this.size.height / 2f, glyphPaint)
        }
        MarqueeText(items.joinToString(" · "), Color(color),
            typography.supportingStyle(TextStyle(fontSize = textSizeSp.sp), items.first()), Modifier.weight(1f))
    }
}

@Composable
internal fun CalendarMonthPicker(year: Int, month: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    var y by rememberSaveable { mutableStateOf(year.toString()) }
    var m by rememberSaveable { mutableStateOf((month + 1).toString()) }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("month-picker"), title = { Text(stringResource(R.string.calendar_month_picker_title)) },
        text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(y, { y = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), label = { Text(stringResource(R.string.calendar_year)) })
            OutlinedTextField(m, { m = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text(stringResource(R.string.calendar_month)) })
        } }, confirmButton = { TextButton(enabled = y.toIntOrNull() in 1..9999 && m.toIntOrNull() in 1..12,
            onClick = { onPick(y.toInt(), m.toInt() - 1) }) { Text(stringResource(R.string.ultimate_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) } })
}
