package com.clockmods.ultimate.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ClockPreferences
import com.clockmods.background.FontCatalog
import com.clockmods.calendar.CalendarMonth
import com.clockmods.calendar.HolidayRepository
import com.clockmods.pro.LunarAlmanac
import com.clockmods.pro.schedule.ScheduleItem
import com.clockmods.pro.schedule.ScheduleStore
import com.clockmods.ui.ClockTypefaceResolver
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherTemperatureFormatter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private data class CalendarCellInfo(
    val day: CalendarMonth.Day,
    val lunar: String,
    val festivals: List<String>,
    val holiday: HolidayRepository.HolidayStatus?,
    val suitable: List<String>,
    val avoid: List<String>,
    val hasSchedule: Boolean,
)

private data class CalendarTypography(
    val family: String,
    val weight: Int,
    val emphasizedWeight: Int,
    val displayFamily: FontFamily,
    val emphasizedDisplayFamily: FontFamily,
    val chineseFamily: FontFamily,
    val emphasizedChineseFamily: FontFamily,
    val timeScale: Float,
    val dateScale: Float,
    val supportingScale: Float,
) {
    fun timeStyle(base: TextStyle, text: String, emphasized: Boolean = false): TextStyle =
        style(base, text, timeScale / ClockPreferences.DEFAULT_TIME_FONT_SCALE, emphasized)

    fun dateStyle(base: TextStyle, text: String, emphasized: Boolean = false): TextStyle =
        style(base, text, dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE, emphasized)

    fun supportingStyle(base: TextStyle, text: String, emphasized: Boolean = false): TextStyle =
        style(base, text, supportingScale, emphasized)

    private fun style(
        base: TextStyle,
        text: String,
        scale: Float,
        emphasized: Boolean,
    ): TextStyle = base.copy(
        fontSize = base.fontSize * scale,
        fontFamily = when {
            containsChinese(text) && emphasized -> emphasizedChineseFamily
            containsChinese(text) -> chineseFamily
            emphasized -> emphasizedDisplayFamily
            else -> displayFamily
        },
        fontWeight = FontWeight(if (emphasized) emphasizedWeight else weight),
    )
}

private fun containsChinese(text: String): Boolean {
    var offset = 0
    while (offset < text.length) {
        val codePoint = text.codePointAt(offset)
        if (ClockTypefaceResolver.isChinese(codePoint)) return true
        offset += Character.charCount(codePoint)
    }
    return false
}

private fun dayKey(year: Int, month: Int, day: Int): String = "$year-$month-$day"

@Composable
internal fun CalendarScreen(
    modifier: Modifier,
    refreshGeneration: Int,
    onOpenCalendarSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val preferences = remember(context, refreshGeneration) { ClockPreferences(context) }
    val scheduleStore = remember(context) { ScheduleStore(context) }
    val holidayRepository = remember(context) { HolidayRepository(context) }
    val timeZone = remember(refreshGeneration) {
        preferences.getTimeZoneId().takeIf(String::isNotBlank)?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
    }
    var todayMillis by remember(timeZone) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeZone) {
        while (true) {
            val now = System.currentTimeMillis()
            todayMillis = now
            val nextMidnight = Calendar.getInstance(timeZone).apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            kotlinx.coroutines.delay((nextMidnight - now + 250L).coerceAtLeast(1_000L))
        }
    }
    val today = remember(timeZone, todayMillis) {
        Calendar.getInstance(timeZone).apply { timeInMillis = todayMillis }
    }
    val todayKey = dayKey(
        today.get(Calendar.YEAR),
        today.get(Calendar.MONTH),
        today.get(Calendar.DAY_OF_MONTH),
    )
    var year by rememberSaveable { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var selectedKey by rememberSaveable {
        mutableStateOf(todayKey)
    }
    var previouslyObservedToday by rememberSaveable(timeZone.id) { mutableStateOf(todayKey) }
    LaunchedEffect(todayKey) {
        if (todayKey != previouslyObservedToday && selectedKey == previouslyObservedToday) {
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH)
            selectedKey = todayKey
        }
        previouslyObservedToday = todayKey
    }
    var scheduleRevision by rememberSaveable { mutableIntStateOf(0) }
    val theme = remember(preferences.getCalendarTheme()) {
        CalendarThemeCatalog.resolve(preferences.getCalendarTheme())
    }
    val typographyScope = remember(theme.id) { ClockPreferences.calendarScope(theme.id) }
    val typography = remember(context, preferences, typographyScope, refreshGeneration) {
        val family = preferences.getFontFamily(typographyScope)
        val option = FontCatalog.optionFor(family)
        val weight = option.nearestWeight(preferences.getFontWeight(typographyScope))
        val emphasizedWeight = option.emphasizedWeight(weight)
        CalendarTypography(
            family = family,
            weight = weight,
            emphasizedWeight = emphasizedWeight,
            displayFamily = FontFamily(ClockTypefaceResolver.resolve(context, family, weight)),
            emphasizedDisplayFamily = FontFamily(
                ClockTypefaceResolver.resolve(context, family, emphasizedWeight),
            ),
            chineseFamily = FontFamily(
                ClockTypefaceResolver.resolveSupporting(context, family, weight, true),
            ),
            emphasizedChineseFamily = FontFamily(
                ClockTypefaceResolver.resolveSupporting(context, family, emphasizedWeight, true),
            ),
            timeScale = preferences.getTimeFontScale(typographyScope),
            dateScale = preferences.getDateFontScale(typographyScope),
            supportingScale = preferences.getSupportingFontScale(typographyScope),
        )
    }
    val monthData = remember(year, month, refreshGeneration, todayKey) {
        CalendarMonth.create(
            year,
            month,
            timeZone,
            todayMillis,
            preferences.getCalendarWeekStart(),
        )
    }
    val weatherRepository = remember(context) { BackgroundRepository(context) }
    val weatherState = rememberWeatherState(weatherRepository, refreshGeneration)
    val weatherDetailLabels = WeatherModels.WeatherDetail.DetailLabels(
        stringResource(R.string.weather_feels_format),
        stringResource(R.string.weather_humidity_format),
        stringResource(R.string.weather_wind_scale_format),
        stringResource(R.string.weather_precip_format),
        stringResource(R.string.weather_air_format),
        stringResource(R.string.weather_warning_suffix),
    )
    var clockTick by remember(todayMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockTick = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }
    val weatherText = if (weatherRepository.isWeatherEnabled()) {
        formatWeatherState(
            weatherState,
            weatherRepository.getWeatherTemperatureUnit(),
            weatherRepository.isWeatherDetailed(),
            weatherDetailLabels,
            clockTick,
        )
    } else {
        ""
    }
    val scheduleByDay = remember(monthData, refreshGeneration, scheduleRevision) {
        monthData.days.associate { day ->
            dayKey(day.year, day.month, day.dayOfMonth) to
                scheduleStore.itemsFor(day.year, day.month, day.dayOfMonth).isNotEmpty()
        }
    }
    val cells = remember(monthData, scheduleByDay, refreshGeneration) {
        monthData.days.map { day ->
            val almanac = LunarAlmanac.of(day.year, day.month, day.dayOfMonth)
            val date = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                day.year,
                day.month + 1,
                day.dayOfMonth,
            )
            CalendarCellInfo(
                day = day,
                lunar = almanac.shortLabel(),
                festivals = almanac.festivals(),
                holiday = holidayRepository.statusOn(date),
                suitable = almanac.suitable(),
                avoid = almanac.avoid(),
                hasSchedule = scheduleByDay[dayKey(day.year, day.month, day.dayOfMonth)] == true,
            )
        }
    }
    val selected = cells.firstOrNull {
        selectedKey == dayKey(it.day.year, it.day.month, it.day.dayOfMonth)
    } ?: cells.first { it.day.currentMonth }
    var selectedSchedule by remember { mutableStateOf(emptyList<ScheduleItem>()) }
    var editingItem by remember { mutableStateOf<ScheduleItem?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    fun reloadSelectedSchedule() {
        selectedSchedule = scheduleStore.itemsFor(
            selected.day.year,
            selected.day.month,
            selected.day.dayOfMonth,
        ).toList()
    }
    LaunchedEffect(selected.day.year, selected.day.month, selected.day.dayOfMonth, refreshGeneration) {
        reloadSelectedSchedule()
    }
    val locale = LocaleManager.resolveLocale(context)
    val monthTitle = remember(year, month, locale, refreshGeneration) {
        val formatter = SimpleDateFormat(
            if (preferences.isClockUseEnglish()) "MMMM yyyy" else "yyyy年M月",
            locale,
        ).apply { this.timeZone = timeZone }
        formatter.format(
            Calendar.getInstance(timeZone).apply {
                clear()
                set(year, month, 1)
            }.time,
        )
    }
    fun moveMonth(offset: Int) {
        val cursor = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, 1)
            add(Calendar.MONTH, offset)
        }
        val newYear = cursor.get(Calendar.YEAR)
        val newMonth = cursor.get(Calendar.MONTH)
        year = newYear
        month = newMonth
        selectedKey = dayKey(newYear, newMonth, 1)
    }
    val weekdays = stringArrayResource(R.array.calendar_weekday_names).toList().let { values ->
        if (preferences.getCalendarWeekStart() == Calendar.MONDAY) values.drop(1) + values.first()
        else values
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(theme.backgroundStart), Color(theme.backgroundEnd))))
            .pointerInput(year, month) {
                var dragDistance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { _, amount -> dragDistance += amount },
                    onDragEnd = {
                        if (abs(dragDistance) > 90f) moveMonth(if (dragDistance < 0f) 1 else -1)
                    },
                )
            },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp)) {
            // The paper theme carries the month toolbar inside its panel (like the reference
            // "宣纸水墨" layout), so the shared screen header is skipped for it.
            if (theme.layout != CalendarLayout.CARD_GRID) {
                CalendarMonthHeader(
                    monthTitle = monthTitle,
                    theme = theme,
                    typography = typography,
                    onPrevious = { moveMonth(-1) },
                    onToday = {
                        val now = Calendar.getInstance(timeZone)
                        year = now.get(Calendar.YEAR)
                        month = now.get(Calendar.MONTH)
                        selectedKey = dayKey(year, month, now.get(Calendar.DAY_OF_MONTH))
                    },
                    onNext = { moveMonth(1) },
                    onOpenSettings = onOpenCalendarSettings,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // Resolved once here: reading maxWidth/maxHeight deeper inside the layout lambdas
                // trips the implicit-receiver rule (several nested layout scopes are in play), and
                // a single boolean also documents the real branching condition.
                val isLandscape = maxWidth > maxHeight
                val selectedKeyForCell: (CalendarCellInfo) -> Boolean = { cell ->
                    selectedKey == dayKey(cell.day.year, cell.day.month, cell.day.dayOfMonth)
                }
                val selectCell: (CalendarCellInfo) -> Unit = { cell ->
                    selectedKey = dayKey(cell.day.year, cell.day.month, cell.day.dayOfMonth)
                }
                val highlightWeekends = preferences.isCalendarHighlightWeekends()
                val selectionCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    CalendarSelectionCard(
                        selected,
                        theme,
                        selectedSchedule,
                        typography,
                        weatherText = weatherText,
                        onAddSchedule = { editingItem = null; editorVisible = true },
                        onEditSchedule = { editingItem = it; editorVisible = true },
                        modifier = cardModifier,
                    )
                }
                val grid: @Composable (Modifier) -> Unit = { gridModifier ->
                    CalendarMonthGrid(
                        cells = cells,
                        weekdays = weekdays,
                        theme = theme,
                        typography = typography,
                        highlightWeekends = highlightWeekends,
                        isSelected = selectedKeyForCell,
                        onSelect = selectCell,
                        modifier = gridModifier,
                    )
                }
                when (theme.layout) {
                    CalendarLayout.WEEK_AGENDA -> {
                        Column(Modifier.fillMaxSize()) {
                            WeekAgendaStrip(
                                cells = cells.filter { it.day.currentMonth },
                                theme = theme,
                                typography = typography,
                                highlightWeekends = highlightWeekends,
                                isSelected = selectedKeyForCell,
                                onSelect = selectCell,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp, max = 104.dp),
                            )
                            selectionCard(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp))
                        }
                    }
                    CalendarLayout.SPLIT_PANEL -> {
                        Column(Modifier.fillMaxSize()) {
                            if (isLandscape) {
                                Row(Modifier.fillMaxWidth().weight(1f)) {
                                    grid(Modifier.weight(.64f).fillMaxHeight())
                                    Column(
                                        Modifier.weight(.36f).fillMaxHeight().padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CalendarWeatherStrip(
                                            theme = theme,
                                            typography = typography,
                                            weatherText = weatherText,
                                            locationText = weatherLocationText(weatherState),
                                            cityLabel = stringResource(R.string.ultimate_calendar_weather_label),
                                        )
                                        selectionCard(Modifier.fillMaxWidth().weight(1f))
                                    }
                                }
                            } else {
                                CalendarWeatherStrip(
                                    theme = theme,
                                    typography = typography,
                                    weatherText = weatherText,
                                    locationText = weatherLocationText(weatherState),
                                    cityLabel = stringResource(R.string.ultimate_calendar_weather_label),
                                )
                                grid(Modifier.fillMaxWidth().weight(1f))
                                selectionCard(
                                    Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(vertical = 8.dp),
                                )
                            }
                            AlmanacMarquee(
                                theme = theme,
                                typography = typography,
                                suitableLabel = stringResource(R.string.ultimate_calendar_almanac_suitable),
                                avoidLabel = stringResource(R.string.ultimate_calendar_almanac_avoid),
                                suitable = selected.suitable,
                                avoid = selected.avoid,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                    CalendarLayout.HERO_MONTH -> {
                        if (isLandscape) {
                            Row(Modifier.fillMaxSize()) {
                                MonthHero(
                                    monthTitle = monthTitle,
                                    theme = theme,
                                    typography = typography,
                                    weatherText = weatherText,
                                    modifier = Modifier
                                        .weight(.32f)
                                        .fillMaxHeight()
                                        .padding(end = 12.dp),
                                )
                                Column(Modifier.weight(.68f).fillMaxHeight()) {
                                    grid(Modifier.fillMaxWidth().weight(1f))
                                    selectionCard(Modifier.fillMaxWidth().heightIn(max = 190.dp).padding(top = 8.dp))
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                MonthHero(
                                    monthTitle = monthTitle,
                                    theme = theme,
                                    typography = typography,
                                    weatherText = weatherText,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp, max = 132.dp),
                                )
                                grid(Modifier.fillMaxWidth().weight(1f))
                                selectionCard(Modifier.fillMaxWidth().heightIn(max = 210.dp).padding(vertical = 8.dp))
                            }
                        }
                    }
                    CalendarLayout.CARD_GRID -> {
                        // "宣纸水墨": the whole month lives on one warm cream paper panel
                        // (toolbar + weekday header + 7x6 grid viewport + footer), mirroring
                        // calendar_dashboard_month.xml.
                        if (isLandscape) {
                            Row(Modifier.fillMaxSize().padding(4.dp)) {
                                CalendarPaperPanel(
                                    monthTitle = monthTitle,
                                    theme = theme,
                                    typography = typography,
                                    onPrevious = { moveMonth(-1) },
                                    onToday = {
                                        val now = Calendar.getInstance(timeZone)
                                        year = now.get(Calendar.YEAR)
                                        month = now.get(Calendar.MONTH)
                                        selectedKey = dayKey(
                                            year,
                                            month,
                                            now.get(Calendar.DAY_OF_MONTH),
                                        )
                                    },
                                    onNext = { moveMonth(1) },
                                    onOpenSettings = onOpenCalendarSettings,
                                    modifier = Modifier.weight(.64f).fillMaxHeight(),
                                ) {
                                    Column(Modifier.fillMaxSize()) {
                                        grid(Modifier.fillMaxWidth().weight(1f))
                                        CalendarDateFooter(
                                            day = selected.day,
                                            theme = theme,
                                            typography = typography,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                selectionCard(Modifier.weight(.36f).fillMaxHeight().padding(8.dp))
                            }
                        } else {
                            Column(Modifier.fillMaxSize().padding(4.dp)) {
                                CalendarPaperPanel(
                                    monthTitle = monthTitle,
                                    theme = theme,
                                    typography = typography,
                                    onPrevious = { moveMonth(-1) },
                                    onToday = {
                                        val now = Calendar.getInstance(timeZone)
                                        year = now.get(Calendar.YEAR)
                                        month = now.get(Calendar.MONTH)
                                        selectedKey = dayKey(
                                            year,
                                            month,
                                            now.get(Calendar.DAY_OF_MONTH),
                                        )
                                    },
                                    onNext = { moveMonth(1) },
                                    onOpenSettings = onOpenCalendarSettings,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                ) {
                                    Column(Modifier.fillMaxSize()) {
                                        grid(Modifier.fillMaxWidth().weight(1f))
                                        CalendarDateFooter(
                                            day = selected.day,
                                            theme = theme,
                                            typography = typography,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                selectionCard(
                                    Modifier.fillMaxWidth().heightIn(max = 250.dp).padding(bottom = 8.dp),
                                )
                            }
                        }
                    }
                    CalendarLayout.DASHBOARD -> {
                        if (isLandscape) {
                            Row(Modifier.fillMaxSize()) {
                                grid(Modifier.weight(.62f).fillMaxHeight())
                                selectionCard(Modifier.weight(.38f).fillMaxHeight().padding(8.dp))
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                grid(Modifier.fillMaxWidth().weight(1f))
                                selectionCard(
                                    Modifier.fillMaxWidth().heightIn(max = 250.dp).padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorVisible) {
        ScheduleEditorDialog(
            existing = editingItem,
            use24Hour = preferences.isUse24Hour(),
            onDismiss = { editorVisible = false },
            onDelete = { item ->
                scheduleStore.remove(selected.day.year, selected.day.month, selected.day.dayOfMonth, item.id)
                scheduleRevision++
                editorVisible = false
                reloadSelectedSchedule()
            },
            onSave = { item, title, hour, minute ->
                scheduleStore.save(
                    selected.day.year,
                    selected.day.month,
                    selected.day.dayOfMonth,
                    item?.id,
                    title,
                    hour,
                    minute,
                ).also { saved ->
                    if (saved) {
                        scheduleRevision++
                        editorVisible = false
                        reloadSelectedSchedule()
                    }
                }
            },
        )
    }
}

/**
 * The "宣纸水墨" month panel: a single warm cream paper card that hosts the month toolbar,
 * the 7x6 grid viewport and the footer almanac line, mirroring `calendar_dashboard_month.xml`
 * (`@drawable/calendar_dashboard_panel` + toolbar + weekdays + grid viewport + footer carousel).
 */
@Composable
private fun CalendarPaperPanel(
    monthTitle: String,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(theme.cornerRadiusDp.dp.coerceAtLeast(8.dp)),
        color = Color(theme.panel),
        border = if (theme.panelStroke != 0) BorderStroke(1.dp, Color(theme.panelStroke)) else null,
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.calendar_previous_month),
                        tint = Color(theme.text),
                    )
                }
                Text(
                    monthTitle,
                    style = typography.timeStyle(
                        MaterialTheme.typography.headlineSmall,
                        monthTitle,
                        emphasized = true,
                    ),
                    color = Color(theme.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    IconButton(onClick = onToday) {
                        Icon(
                            Icons.Default.Today,
                            stringResource(R.string.calendar_today),
                            tint = Color(theme.accent),
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            stringResource(R.string.calendar_next_month),
                            tint = Color(theme.text),
                        )
                    }
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                stringResource(R.string.calendar_settings_accessibility),
                                tint = Color(theme.text),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                content()
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthTitle: String,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.calendar_previous_month),
                tint = Color(theme.text),
            )
        }
        Text(
            monthTitle,
            style = typography.timeStyle(if (theme.flatGrid) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineSmall
            }, monthTitle, emphasized = true),
            color = Color(theme.text),
        )
        Row {
            IconButton(onClick = onToday) {
                Icon(
                    Icons.Default.Today,
                    stringResource(R.string.calendar_today),
                    tint = Color(theme.accent),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    stringResource(R.string.calendar_next_month),
                    tint = Color(theme.text),
                )
            }
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        stringResource(R.string.calendar_settings_accessibility),
                        tint = Color(theme.text),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDateFooter(
    day: CalendarMonth.Day,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    modifier: Modifier = Modifier,
) {
    val calendar = remember(day) {
        Calendar.getInstance().apply { clear(); set(day.year, day.month, day.dayOfMonth) }
    }
    val almanac = remember(day) { LunarAlmanac.of(day.year, day.month, day.dayOfMonth) }
    val text = remember(day, calendar) {
        val fullDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val natural = almanac.naturalLabel()
        if (natural.isBlank()) fullDate else "$fullDate $natural"
    }
    Text(
        text,
        modifier = modifier.padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color(theme.text),
        style = typography.dateStyle(MaterialTheme.typography.bodyMedium, text),
    )
}

@Composable
private fun CalendarMonthGrid(
    cells: List<CalendarCellInfo>,
    weekdays: List<String>,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    highlightWeekends: Boolean,
    isSelected: (CalendarCellInfo) -> Boolean,
    onSelect: (CalendarCellInfo) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    label,
                    Modifier.weight(1f).padding(vertical = 5.dp),
                    textAlign = TextAlign.Center,
                    style = typography.dateStyle(MaterialTheme.typography.labelLarge, label),
                    color = Color(theme.weekday),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                week.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        selected = isSelected(cell),
                        highlightWeekends = highlightWeekends,
                        theme = theme,
                        typography = typography,
                        onSelect = { onSelect(cell) },
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekAgendaStrip(
    cells: List<CalendarCellInfo>,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    highlightWeekends: Boolean,
    isSelected: (CalendarCellInfo) -> Boolean,
    onSelect: (CalendarCellInfo) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        cells.forEach { cell ->
            val selected = isSelected(cell)
            val weekdayLabel = remember(cell.day.dayOfWeek) {
                weekdayShortLabel(cell.day.dayOfWeek)
            }
            Surface(
                onClick = { onSelect(cell) },
                shape = RoundedCornerShape(theme.cornerRadiusDp.dp.coerceAtLeast(8.dp)),
                color = if (selected) Color(theme.selectionFill) else Color.Transparent,
                border = if (selected) BorderStroke(1.5.dp, Color(theme.selectionStroke)) else null,
                modifier = Modifier.width(58.dp).heightIn(min = 78.dp, max = 100.dp),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        weekdayLabel,
                        style = typography.supportingStyle(
                            MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            weekdayLabel,
                        ),
                        color = Color(theme.secondary),
                    )
                    val dayText = cell.day.dayOfMonth.toString()
                    Text(
                        dayText,
                        style = typography.timeStyle(
                            MaterialTheme.typography.titleMedium,
                            dayText,
                            emphasized = cell.day.today || selected,
                        ),
                        color = if (cell.day.today) Color(theme.today) else Color(theme.day),
                    )
                    val supporting = cell.festivals.firstOrNull() ?: cell.lunar
                    Text(
                        supporting,
                        maxLines = 1,
                        style = typography.supportingStyle(
                            MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            supporting,
                        ),
                        color = Color(theme.secondary),
                    )
                }
            }
        }
    }
}

private fun weekdayShortLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "日"
    Calendar.MONDAY -> "一"
    Calendar.TUESDAY -> "二"
    Calendar.WEDNESDAY -> "三"
    Calendar.THURSDAY -> "四"
    Calendar.FRIDAY -> "五"
    else -> "六"
}

@Composable
private fun CalendarDayCell(
    cell: CalendarCellInfo,
    selected: Boolean,
    highlightWeekends: Boolean,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    onSelect: () -> Unit,
    modifier: Modifier,
) {
    val todayDescription = stringResource(
        R.string.calendar_day_today_accessibility,
        cell.day.dayOfMonth,
        cell.lunar,
    )
    val dayDescription = stringResource(
        R.string.calendar_day_accessibility,
        cell.day.dayOfMonth,
        cell.lunar,
    )
    val weekend = cell.day.dayOfWeek == Calendar.SATURDAY || cell.day.dayOfWeek == Calendar.SUNDAY
    val shape = if (theme.flatGrid) RoundedCornerShape(0.dp) else RoundedCornerShape(theme.cornerRadiusDp.dp)
    val todayFill = theme.todayFill.takeIf { it != 0 }
    val container = when {
        selected -> Color(theme.selectionFill)
        cell.day.today -> todayFill?.let(::Color) ?: Color(theme.panel)
        theme.flatGrid -> Color.Transparent
        else -> Color(theme.panel)
    }
    val border = when {
        selected -> BorderStroke(1.dp, Color(theme.selectionStroke))
        theme.panelStroke != 0 && !theme.flatGrid -> BorderStroke(1.dp, Color(theme.panelStroke))
        else -> null
    }
    Card(
        onClick = onSelect,
        modifier = modifier.semantics {
            contentDescription = if (cell.day.today) todayDescription else dayDescription
        },
        shape = shape,
        border = border,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.fillMaxSize().padding(if (theme.flatGrid) 1.dp else 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val dayText = cell.day.dayOfMonth.toString()
            val holidayLabel = cell.holiday?.let {
                stringResource(if (it.offDay) R.string.calendar_day_status_off else R.string.calendar_day_status_work)
            }
            // The 班/休 badge rides to the top-right of the day number, as in the reference card.
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    dayText,
                    color = when {
                        selected || cell.day.today -> Color(theme.today)
                        !cell.day.currentMonth -> Color(theme.secondary).copy(alpha = .45f)
                        weekend && highlightWeekends -> Color(theme.weekend)
                        else -> Color(theme.day)
                    },
                    style = typography.timeStyle(
                        MaterialTheme.typography.bodyLarge,
                        dayText,
                        emphasized = cell.day.today || selected,
                    ),
                )
                if (holidayLabel != null) {
                    Text(
                        holidayLabel,
                        color = Color(if (cell.holiday!!.offDay) theme.restBadge else theme.workBadge),
                        style = typography.supportingStyle(
                            MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            holidayLabel,
                            emphasized = true,
                        ),
                        modifier = Modifier.padding(start = 1.dp, top = 1.dp),
                    )
                }
                if (cell.hasSchedule) {
                    Box(
                        Modifier
                            .padding(start = 1.dp, top = 3.dp)
                            .size(4.dp)
                            .background(Color(theme.accent), CircleShape),
                    )
                }
            }
            val carouselLabels = buildList {
                add(cell.lunar)
                addAll(cell.festivals)
            }
            LunarCarouselText(
                labels = carouselLabels,
                color = if (selected || cell.day.today) Color(theme.today).copy(alpha = .9f)
                    else Color(theme.secondary),
                style = typography.supportingStyle(
                    MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    carouselLabels.firstOrNull().orEmpty(),
                ),
            )
        }
    }
}

@Composable
private fun CalendarSelectionCard(
    info: CalendarCellInfo,
    theme: ComposeCalendarTheme,
    scheduleItems: List<ScheduleItem>,
    typography: CalendarTypography,
    weatherText: String,
    onAddSchedule: () -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendar = remember(info) {
        Calendar.getInstance().apply { clear(); set(info.day.year, info.day.month, info.day.dayOfMonth) }
    }
    val almanac = remember(info) {
        LunarAlmanac.of(info.day.year, info.day.month, info.day.dayOfMonth)
    }
    val title = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (theme.flatGrid) 0.dp else theme.cornerRadiusDp.dp),
        color = Color(theme.panel),
        border = if (theme.panelStroke != 0) BorderStroke(1.dp, Color(theme.panelStroke)) else null,
    ) {
        Column(
            Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = typography.dateStyle(
                    MaterialTheme.typography.titleMedium,
                    title,
                    emphasized = true,
                ),
                color = Color(theme.text),
            )
            val naturalLabel = almanac.naturalLabel()
            Text(
                naturalLabel,
                style = typography.supportingStyle(MaterialTheme.typography.bodyMedium, naturalLabel),
                color = Color(theme.secondary),
            )
            if (theme.showWeather && weatherText.isNotBlank()) {
                Text(
                    weatherText,
                    style = typography.supportingStyle(
                        MaterialTheme.typography.bodyMedium,
                        weatherText,
                        emphasized = true,
                    ),
                    color = Color(theme.accent),
                )
            }
            val events = buildList {
                addAll(info.festivals)
                info.holiday?.name?.takeIf(String::isNotBlank)?.let(::add)
            }.distinct()
            if (events.isNotEmpty()) {
                val eventText = events.joinToString(" · ")
                Text(
                    eventText,
                    style = typography.supportingStyle(MaterialTheme.typography.bodyMedium, eventText),
                    color = Color(theme.accent),
                )
            }
            if (!theme.marqueeAlmanac && info.suitable.isNotEmpty()) {
                AlmanacLine(
                    label = stringResource(R.string.ultimate_calendar_almanac_suitable),
                    items = info.suitable,
                    theme = theme,
                    typography = typography,
                    emphasis = Color(theme.suitable),
                )
            }
            if (!theme.marqueeAlmanac && info.avoid.isNotEmpty()) {
                AlmanacLine(
                    label = stringResource(R.string.ultimate_calendar_almanac_avoid),
                    items = info.avoid,
                    theme = theme,
                    typography = typography,
                    emphasis = Color(theme.avoid),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val scheduleTitle = stringResource(R.string.ultimate_schedule_title)
                Text(
                    scheduleTitle,
                    style = typography.supportingStyle(
                        MaterialTheme.typography.bodyMedium,
                        scheduleTitle,
                        emphasized = true,
                    ),
                    color = Color(theme.text),
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAddSchedule) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    val addLabel = stringResource(R.string.ultimate_schedule_add)
                    Text(
                        addLabel,
                        style = typography.supportingStyle(
                            MaterialTheme.typography.labelLarge,
                            addLabel,
                        ),
                    )
                }
            }
            if (scheduleItems.isEmpty()) {
                val emptyLabel = stringResource(R.string.ultimate_schedule_empty)
                Text(
                    emptyLabel,
                    color = Color(theme.secondary),
                    style = typography.supportingStyle(
                        MaterialTheme.typography.bodySmall,
                        emptyLabel,
                    ),
                )
            } else {
                Column(Modifier.fillMaxWidth()) {
                    scheduleItems.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            val timeLabel = item.timeLabel().ifBlank { stringResource(R.string.ultimate_schedule_all_day) }
                            Text(
                                timeLabel,
                                color = Color(theme.accent),
                                modifier = Modifier.width(62.dp),
                                style = typography.supportingStyle(
                                    MaterialTheme.typography.bodyMedium,
                                    timeLabel,
                                ),
                            )
                            Text(
                                item.title,
                                Modifier.weight(1f),
                                color = Color(theme.text),
                                maxLines = 2,
                                style = typography.supportingStyle(
                                    MaterialTheme.typography.bodyMedium,
                                    item.title,
                                ),
                            )
                            IconButton(onClick = { onEditSchedule(item) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ultimate_schedule_dialog_edit), tint = Color(theme.secondary))
                            }
                        }
                    }
             }
         }
     }
 }
}

/** A single 宜/忌 row: a coloured label chip followed by items that scroll horizontally. */
@Composable
private fun AlmanacLine(
    label: String,
    items: List<String>,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    emphasis: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = emphasis.copy(alpha = .16f),
            modifier = Modifier.size(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    label,
                    color = emphasis,
                    style = typography.supportingStyle(
                        MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        label,
                        emphasized = true,
                    ),
                )
            }
        }
        val body = items.take(10).joinToString(" · ")
        // The list itself is plain weight; when it overflows one line it scrolls horizontally
        // instead of being ellipsised.
        MarqueeText(
            text = body,
            color = emphasis,
            style = typography.supportingStyle(
                MaterialTheme.typography.bodyMedium,
                body,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/** A single-line 宜/忌 footer that scrolls its combined text horizontally. */
@Composable
private fun AlmanacMarquee(
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    suitableLabel: String,
    avoidLabel: String,
    suitable: List<String>,
    avoid: List<String>,
    modifier: Modifier = Modifier,
) {
    if (suitable.isEmpty() && avoid.isEmpty()) return
    val text = buildString {
        if (suitable.isNotEmpty()) {
            append(suitableLabel).append(' ').append(suitable.take(10).joinToString(" · "))
        }
        if (suitable.isNotEmpty() && avoid.isNotEmpty()) append("      ")
        if (avoid.isNotEmpty()) {
            append(avoidLabel).append(' ').append(avoid.take(10).joinToString(" · "))
        }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(theme.cornerRadiusDp.dp.coerceAtLeast(8.dp)),
        color = Color(theme.panel),
        border = if (theme.panelStroke != 0) BorderStroke(1.dp, Color(theme.panelStroke)) else null,
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            MarqueeText(
                text = text,
                color = Color(theme.suitable),
                style = typography.supportingStyle(
                    MaterialTheme.typography.bodyMedium,
                    text,
                ),
            )
        }
    }
}

/**
 * Shared phase clock for every per-cell calendar label carousel, mirroring the Java
 * `CalendarCarouselTimeline`: all cells advance on the same boundary so their vertical
 * transitions share one frame phase.
 */
private object CalendarCarouselTimeline {
    const val HOLD_MS = 3_000L
    const val TRANSITION_MS = 200L
    const val CYCLE_MS = HOLD_MS + TRANSITION_MS
}

/**
 * The lunar line under a day number. When the day carries a solar term or festival the label
 * rolls vertically between the lunar day and that festival, matching the reference 宣纸 card;
 * a plain lunar day stays still.
 *
 * <p>Faithful port of the Java `CalendarLabelCarouselView`: the motion is a pure upward slide
 * of one line height (never a horizontal shift), driven by the shared [CalendarCarouselTimeline].
 * A label longer than [MAX_STATIC_CHARS] characters scrolls horizontally while it is shown;
 * shorter labels stay centred (never scroll).</p>
 */
@Composable
private fun LunarCarouselText(
    labels: List<String>,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val distinct = remember(labels) { labels.distinct().filter { it.isNotEmpty() } }
    if (distinct.isEmpty()) return
    if (distinct.size == 1) {
        LunarCarouselLine(distinct.first(), color, style, modifier)
        return
    }
    val density = LocalDensity.current
    val lineHeight = with(density) {
        val fontSizePx = style.fontSize.toPx()
        val multiplier = if (style.lineHeight.isSpecified && style.lineHeight.value > 0f) {
            style.lineHeight.toPx() / fontSizePx
        } else {
            1.2f
        }
        (fontSizePx * multiplier).coerceAtLeast(1f)
    }
    var phase by remember { mutableFloatStateOf(0f) }
    var cycle by remember { mutableIntStateOf(0) }
    LaunchedEffect(distinct) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = (now - start) / 1_000_000L
            // Keep the raw cycle count separately: phase is wrapped to [0, CYCLE_MS) so that the
            // hold/slide math stays simple, but the label index must advance over the LONG-RUN
            // total, otherwise it would stick on the first pair forever.
            cycle = (elapsed / CalendarCarouselTimeline.CYCLE_MS).toInt()
            phase = (elapsed % CalendarCarouselTimeline.CYCLE_MS).toFloat()
        }
    }
    val index = ((cycle % distinct.size) + distinct.size) % distinct.size
    val holding = phase < CalendarCarouselTimeline.HOLD_MS
    val progress = if (holding) 0f else
        ((phase - CalendarCarouselTimeline.HOLD_MS) / CalendarCarouselTimeline.TRANSITION_MS)
            .coerceIn(0f, 1f)
    val nextIndex = (index + 1) % distinct.size
    val slide = progress * lineHeight
    Box(
        modifier
            .height(with(density) { lineHeight.toDp() })
            .clipToBounds(),
    ) {
        LunarCarouselLine(
            distinct[index],
            color,
            style,
            Modifier.graphicsLayer { translationY = -slide },
        )
        if (!holding) {
            LunarCarouselLine(
                distinct[nextIndex],
                color,
                style,
                Modifier.graphicsLayer { translationY = lineHeight - slide },
            )
        }
    }
}

/** One line of a [LunarCarouselText]: centred when short, marquee-scrolled when long. */
@Composable
private fun LunarCarouselLine(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val scrollable = text.codePointCount(0, text.length) > MAX_STATIC_CHARS
    if (!scrollable) {
        Text(
            text,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            style = style,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    Box(modifier.fillMaxWidth().clipToBounds()) {
        MarqueeText(text, color, style, modifier = Modifier.fillMaxWidth())
    }
}

/** A label scrolls horizontally only when it is longer than this many characters. */
private const val MAX_STATIC_CHARS = 3

/**
 * A minimal single-line horizontal marquee with no external dependency.  When the text fits it
 * sits still; when it overflows it scrolls left continuously and wraps seamlessly.
 */
@Composable
private fun MarqueeText(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var containerWidth by remember { mutableIntStateOf(0) }
    var textWidth by remember { mutableIntStateOf(0) }
    val overflow = textWidth > containerWidth && containerWidth > 0
    val transition = rememberInfiniteTransition(label = "almanac-marquee")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ((textWidth + containerWidth).coerceAtLeast(1) * 18).coerceAtLeast(4_000),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "almanac-marquee-progress",
    )
    val gapPx = with(density) { 48.dp.toPx() }
    val offsetPx = if (overflow) {
        val span = (textWidth + gapPx).toFloat()
        -(progress * span)
    } else {
        0f
    }
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width },
    ) {
        Row(Modifier.wrapContentWidth().graphicsLayer { translationX = offsetPx }) {
            Text(
                text,
                color = color,
                maxLines = 1,
                softWrap = false,
                style = style,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .onSizeChanged { textWidth = it.width },
            )
            if (overflow) {
                Text(
                    text,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    style = style,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarWeatherStrip(
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    weatherText: String,
    locationText: String,
    cityLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(theme.cornerRadiusDp.dp.coerceAtLeast(8.dp)),
        color = Color(theme.panel),
        border = if (theme.panelStroke != 0) BorderStroke(1.dp, Color(theme.panelStroke)) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                cityLabel,
                color = Color(theme.secondary),
                style = typography.supportingStyle(
                    MaterialTheme.typography.labelMedium,
                    cityLabel,
                ),
            )
            val body = when {
                weatherText.isNotBlank() && locationText.isNotBlank() -> "$locationText $weatherText"
                weatherText.isNotBlank() -> weatherText
                else -> "—"
            }
            AnimatedContent(
                targetState = body,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "calendar-weather",
            ) { value ->
                Text(
                    value,
                    Modifier.weight(1f, fill = false),
                    color = Color(theme.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.supportingStyle(
                        MaterialTheme.typography.bodyMedium,
                        value,
                        emphasized = true,
                    ),
                )
            }
        }
    }
}

/** Oversized month word-mark panel used by the HERO_MONTH layout. */
@Composable
private fun MonthHero(
    monthTitle: String,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    weatherText: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                monthTitle,
                color = Color(theme.text),
                maxLines = 3,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                style = typography.timeStyle(
                    MaterialTheme.typography.displaySmall,
                    monthTitle,
                    emphasized = true,
                ),
            )
            if (weatherText.isNotBlank()) {
                Text(
                    weatherText,
                    color = Color(theme.accent),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.supportingStyle(
                        MaterialTheme.typography.bodyMedium,
                        weatherText,
                    ),
                )
            }
        }
    }
}

private fun weatherLocationText(state: WeatherModels.WeatherState?): String {
    val data = state?.data ?: return ""
    return WeatherModels.locationText(data.city, data.district)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
    existing: ScheduleItem?,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onDelete: (ScheduleItem) -> Unit,
    onSave: (ScheduleItem?, String, Int, Int) -> Boolean,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var hasTime by rememberSaveable(existing?.id) { mutableStateOf(existing?.hasTime() == true) }
    var selectedHour by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.hour?.takeIf { it >= 0 } ?: 9) }
    var selectedMinute by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.minute?.takeIf { it >= 0 } ?: 0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var attemptedEmpty by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var saveRejected by rememberSaveable(existing?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.ultimate_schedule_dialog_add else R.string.ultimate_schedule_dialog_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(200)
                        attemptedEmpty = false
                        saveRejected = false
                    },
                    label = { Text(stringResource(R.string.ultimate_schedule_dialog_title_hint)) },
                    singleLine = true,
                    isError = attemptedEmpty,
                )
                if (saveRejected) {
                    Text(
                        stringResource(
                            R.string.ultimate_schedule_limit_reached,
                            ScheduleStore.MAX_ITEMS_PER_DAY,
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { showTimePicker = true }) {
                    val locale = LocalConfiguration.current.locales[0]
                    Text(
                        if (hasTime) {
                            String.format(locale, "%02d:%02d", selectedHour, selectedMinute)
                        } else {
                            stringResource(R.string.ultimate_schedule_all_day)
                        },
                    )
                }
                if (hasTime) {
                    TextButton(onClick = { hasTime = false }) { Text(stringResource(R.string.ultimate_schedule_dialog_time)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().isBlank()) attemptedEmpty = true
                else {
                    saveRejected = !onSave(
                        existing,
                        title.trim(),
                        if (hasTime) selectedHour else ScheduleItem.TIME_NONE,
                        if (hasTime) selectedMinute else ScheduleItem.TIME_NONE,
                    )
                }
            }) { Text(stringResource(R.string.ultimate_schedule_save)) }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text(stringResource(R.string.ultimate_schedule_dialog_delete))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
            }
        },
    )
    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = use24Hour)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.ultimate_schedule_dialog_time)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = state.hour
                    selectedMinute = state.minute
                    hasTime = true
                    showTimePicker = false
                }) { Text(stringResource(R.string.ultimate_apply)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.ultimate_cancel)) } },
        )
    }
}
