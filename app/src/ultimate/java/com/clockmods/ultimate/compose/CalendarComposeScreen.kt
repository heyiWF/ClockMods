package com.clockmods.ultimate.compose

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.background.FontCatalog
import com.clockmods.calendar.CalendarMonth
import com.clockmods.calendar.HolidayRepository
import com.clockmods.pro.LunarAlmanac
import com.clockmods.pro.schedule.ScheduleItem
import com.clockmods.pro.schedule.ScheduleStore
import com.clockmods.ui.ClockTypefaceResolver
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
internal fun CalendarScreen(modifier: Modifier, refreshGeneration: Int) {
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
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
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
            )
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val selectedKeyForCell: (CalendarCellInfo) -> Boolean = { cell ->
                    selectedKey == dayKey(cell.day.year, cell.day.month, cell.day.dayOfMonth)
                }
                val selectCell: (CalendarCellInfo) -> Unit = { cell ->
                    selectedKey = dayKey(cell.day.year, cell.day.month, cell.day.dayOfMonth)
                }
                val selectionCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    CalendarSelectionCard(
                        selected,
                        theme,
                        selectedSchedule,
                        typography,
                        onAddSchedule = { editingItem = null; editorVisible = true },
                        onEditSchedule = { editingItem = it; editorVisible = true },
                        modifier = cardModifier,
                    )
                }
                when {
                    theme.id == ComposeCalendarTheme.ID_AGENDA -> {
                        Column(Modifier.fillMaxSize()) {
                            AgendaMonthStrip(
                                cells = cells.filter { it.day.currentMonth },
                                theme = theme,
                                typography = typography,
                                highlightWeekends = preferences.isCalendarHighlightWeekends(),
                                isSelected = selectedKeyForCell,
                                onSelect = selectCell,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp, max = 96.dp),
                            )
                            selectionCard(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp))
                        }
                    }
                    maxWidth > maxHeight -> {
                        Row(Modifier.fillMaxSize()) {
                            CalendarMonthGrid(
                                cells = cells,
                                weekdays = weekdays,
                                theme = theme,
                                typography = typography,
                                highlightWeekends = preferences.isCalendarHighlightWeekends(),
                                isSelected = selectedKeyForCell,
                                onSelect = selectCell,
                                modifier = Modifier.weight(.62f).fillMaxHeight(),
                            )
                            selectionCard(Modifier.weight(.38f).fillMaxHeight().padding(8.dp))
                        }
                    }
                    else -> {
                        Column(Modifier.fillMaxSize()) {
                            CalendarMonthGrid(
                                cells = cells,
                                weekdays = weekdays,
                                theme = theme,
                                typography = typography,
                                highlightWeekends = preferences.isCalendarHighlightWeekends(),
                                isSelected = selectedKeyForCell,
                                onSelect = selectCell,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            selectionCard(
                                Modifier.fillMaxWidth().heightIn(max = 250.dp).padding(vertical = 8.dp),
                            )
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

@Composable
private fun CalendarMonthHeader(
    monthTitle: String,
    theme: ComposeCalendarTheme,
    typography: CalendarTypography,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
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
        }
    }
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
private fun AgendaMonthStrip(
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.forEach { cell ->
            CalendarDayCell(
                cell = cell,
                selected = isSelected(cell),
                highlightWeekends = highlightWeekends,
                theme = theme,
                typography = typography,
                onSelect = { onSelect(cell) },
                modifier = Modifier.width(64.dp).heightIn(min = 72.dp, max = 92.dp).padding(2.dp),
            )
        }
    }
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
    val container = when {
        selected -> Color(theme.selectionFill)
        cell.day.today -> Color(theme.todayFill.takeIf { it != 0 } ?: theme.panel)
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
            Text(
                dayText,
                color = when {
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
            val supportingLabel = cell.festivals.firstOrNull() ?: cell.lunar
            Text(
                supportingLabel,
                maxLines = 1,
                style = typography.supportingStyle(
                    MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    supportingLabel,
                ),
                color = Color(theme.secondary),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                cell.holiday?.let {
                    val holidayLabel = stringResource(if (it.offDay) R.string.calendar_day_status_off else R.string.calendar_day_status_work)
                    Text(
                        holidayLabel,
                        color = Color(if (it.offDay) theme.restBadge else theme.workBadge),
                        style = typography.supportingStyle(
                            MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            holidayLabel,
                            emphasized = true,
                        ),
                    )
                }
                if (cell.hasSchedule) Box(Modifier.size(4.dp).background(Color(theme.accent), CircleShape))
            }
        }
    }
}

@Composable
private fun CalendarSelectionCard(
    info: CalendarCellInfo,
    theme: ComposeCalendarTheme,
    scheduleItems: List<ScheduleItem>,
    typography: CalendarTypography,
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
            if (info.suitable.isNotEmpty()) {
                val suitableText = stringResource(R.string.calendar_suitable_prefix) +
                    info.suitable.take(8).joinToString(" · ")
                Text(
                    suitableText,
                    style = typography.supportingStyle(MaterialTheme.typography.bodyMedium, suitableText),
                    color = Color(theme.suitable),
                )
            }
            if (info.avoid.isNotEmpty()) {
                val avoidText = stringResource(R.string.calendar_avoid_prefix) +
                    info.avoid.take(8).joinToString(" · ")
                Text(
                    avoidText,
                    style = typography.supportingStyle(MaterialTheme.typography.bodyMedium, avoidText),
                    color = Color(theme.avoid),
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
