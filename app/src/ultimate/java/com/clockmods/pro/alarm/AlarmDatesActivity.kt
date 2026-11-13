package com.clockmods.pro.alarm

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.ui.compose.ClockModsTheme
import java.time.*
import java.time.format.*

class AlarmDatesActivity : AlarmUi() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { ClockModsTheme { Dates(remember(revision) { AlarmSpec(draft) }) } }
    }
    @OptIn(ExperimentalLayoutApi::class)
    @Composable private fun Dates(alarm: AlarmSpec) {
        val exclusions = intent.getStringExtra(MODE) == "excluded"
        val dates = if (exclusions) alarm.excludedDates else alarm.extraDates
        var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
        val month = YearMonth.parse(monthKey)
        val locale = LocalConfiguration.current.locales[0]
        fun change(date: LocalDate) = edit { val selected = if (exclusions) it.excludedDates else it.extraDates; if (!selected.remove(date)) selected.add(date) }
        AlarmScaffold(stringResource(if (exclusions) R.string.alarm_excluded_dates else R.string.alarm_extra_dates), ::finish, ::returnDraft) { insets ->
            AlarmColumn(insets) {
                Text(stringResource(R.string.alarm_date_count, dates.size), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(month.format(DateTimeFormatter.ofPattern(if (locale.language == "zh") "yyyy年M月" else "LLLL yyyy", locale)),
                                Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.alarm_previous_month)) }
                            IconButton(onClick = { monthKey = month.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.alarm_next_month)) }
                        }
                        Row { for (day in 1..7) Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                            Text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } }
                        val offset = month.atDay(1).dayOfWeek.value - 1
                        val rows = (offset + month.lengthOfMonth() + 6) / 7
                        for (week in 0 until rows) Row(Modifier.fillMaxWidth()) {
                            for (column in 0..6) {
                                val day = week * 7 + column - offset + 1
                                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (day in 1..month.lengthOfMonth()) {
                                        val date = month.atDay(day)
                                        val selected = dates.contains(date)
                                        val description = alarmDate(date) + ", " + stringResource(if (selected) R.string.alarm_selected else R.string.alarm_unselected)
                                        Surface(selected = selected, onClick = { change(date) }, shape = CircleShape,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            border = if (date == LocalDate.now() && !selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.fillMaxSize().padding(2.dp).semantics { contentDescription = description }) {
                                            Box(contentAlignment = Alignment.Center) { Text(day.toString(), style = MaterialTheme.typography.bodyLarge) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.alarm_dates_help), Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.forEach { date -> InputChip(selected = true, onClick = { change(date) }, label = { Text(alarmDate(date)) }, trailingIcon = { Icon(Icons.Outlined.Close, stringResource(R.string.alarm_remove_date, alarmDate(date)), Modifier.size(18.dp)) }) }
                }
                if (dates.isNotEmpty()) TextButton(onClick = { edit { if (exclusions) it.excludedDates.clear() else it.extraDates.clear() } }) { Text(stringResource(R.string.alarm_clear_dates)) }
            }
        }
    }
}
