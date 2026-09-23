package com.clockmods.ultimate.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal data class CalendarCellInfo(
    val day: CalendarMonth.Day,
    val lunar: String,
    val festivals: List<String>,
    val holiday: HolidayRepository.HolidayStatus?,
    val suitable: List<String>,
    val avoid: List<String>,
    val hasSchedule: Boolean,
)

internal data class CalendarTypography(
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
    onToggleChrome: () -> Unit = {},
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
    val weatherState = if (theme.showWeather) rememberWeatherState(weatherRepository, refreshGeneration) else null
    var clockTick by remember(todayMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockTick = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
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
    val adjacentCells: (Int) -> List<CalendarCellInfo> = { direction ->
        val cursor = Calendar.getInstance(timeZone).apply {
            clear()
            if (theme.layout == CalendarLayout.AGENDA) {
                set(selected.day.year, selected.day.month, selected.day.dayOfMonth)
                add(Calendar.DAY_OF_MONTH, direction * 7)
            } else {
                set(year, month, 1)
                add(Calendar.MONTH, direction)
            }
        }
        val page = if (theme.layout == CalendarLayout.AGENDA) {
            CalendarMonth.createWeek(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH),
                cursor.get(Calendar.DAY_OF_MONTH), timeZone, todayMillis,
                preferences.getCalendarWeekStart())
        } else {
            CalendarMonth.create(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH), timeZone,
                todayMillis, preferences.getCalendarWeekStart())
        }
        page.days.map { day ->
            val almanac = LunarAlmanac.of(day.year, day.month, day.dayOfMonth)
            val date = String.format(Locale.US, "%04d-%02d-%02d", day.year,
                day.month + 1, day.dayOfMonth)
            CalendarCellInfo(day, almanac.shortLabel(), almanac.festivals(),
                holidayRepository.statusOn(date), almanac.suitable(), almanac.avoid(), false)
        }
    }
    var selectedSchedule by remember { mutableStateOf(emptyList<ScheduleItem>()) }
    var monthPickerVisible by rememberSaveable { mutableStateOf(false) }
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
        val anchor = Calendar.getInstance(timeZone).apply { clear(); set(year, month, 1) }
        com.clockmods.ui.DateFormatter.format(
            if (preferences.isClockUseEnglish()) "MMMM yyyy" else "yyyy年M月", anchor,
            LocaleManager.dateLang(preferences.getClockLanguage()))
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
        selectedKey = dayKey(newYear, newMonth, selected.day.dayOfMonth.coerceAtMost(cursor.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    val weekdays = stringArrayResource(R.array.calendar_weekday_names).toList().let { values ->
        if (preferences.getCalendarWeekStart() == Calendar.MONDAY) values.drop(1) + values.first()
        else values
    }

    fun goToday() {
        year = today.get(Calendar.YEAR)
        month = today.get(Calendar.MONTH)
        selectedKey = todayKey
    }
    fun selectDay(cell: CalendarCellInfo) {
        val key = dayKey(cell.day.year, cell.day.month, cell.day.dayOfMonth)
        selectedKey = key
        year = cell.day.year
        month = cell.day.month
    }
    fun movePage(offset: Int) {
        if (theme.layout != CalendarLayout.AGENDA) { moveMonth(offset); return }
        val cursor = Calendar.getInstance(timeZone).apply {
            clear()
            set(selected.day.year, selected.day.month, selected.day.dayOfMonth)
            add(Calendar.DAY_OF_MONTH, offset * 7)
        }
        year = cursor.get(Calendar.YEAR)
        month = cursor.get(Calendar.MONTH)
        selectedKey = dayKey(year, month, cursor.get(Calendar.DAY_OF_MONTH))
    }
    UltimateCalendarLayout(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onToggleChrome() })
        }, theme = theme, typography = typography,
        preferences = preferences, cells = cells, selected = selected,
        adjacentCells = adjacentCells,
        weekdays = weekdays, monthTitle = monthTitle, timeZone = timeZone,
        clockTick = clockTick, weatherState = weatherState,
        refreshGeneration = refreshGeneration, scheduleItems = selectedSchedule,
        onPrevious = { movePage(-1) }, onNext = { movePage(1) },
        onToday = ::goToday, onSelect = ::selectDay,
        onMonthPicker = { monthPickerVisible = true },
        onAddSchedule = { editingItem = null; editorVisible = true },
        onEditSchedule = { editingItem = it; editorVisible = true },
    )
    if (monthPickerVisible) {
        CalendarMonthPicker(year, month, onDismiss = { monthPickerVisible = false }) { y, m ->
            year = y
            month = m
            selectedKey = dayKey(y, m, 1)
            monthPickerVisible = false
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
internal fun LunarCarouselText(
    labels: List<String>,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val distinct = remember(labels) { labels.distinct().filter { it.isNotEmpty() } }
    if (distinct.isEmpty()) return
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
        phase = 0f
        cycle = 0
        if (distinct.size == 1) return@LaunchedEffect
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
    // Static and cycling labels must share the same line box and baseline.
    val lineStyle = style.copy(lineHeight = with(density) { lineHeight.toSp() })
    Box(
        modifier
            .height(with(density) { lineHeight.toDp() })
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        LunarCarouselLine(
            distinct[index],
            color,
            lineStyle,
            Modifier.graphicsLayer { translationY = -slide },
        )
        if (!holding) {
            LunarCarouselLine(
                distinct[nextIndex],
                color,
                lineStyle,
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
internal fun MarqueeText(
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
