package com.clockmods.pro.alarm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ui.compose.TimeText
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AlarmShiftActivity : AlarmUi() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { ClockModsTheme { Shift(remember(revision) { AlarmSpec(draft) }) } } }
    private fun save() {
        if ((0 until draft.cycleLength).none { draft.cycleMinutes[it] >= 0 }) {
            Toast.makeText(this, R.string.alarm_shift_empty, Toast.LENGTH_LONG).show(); return
        }
        returnDraft()
    }
    @Composable private fun Shift(alarm: AlarmSpec) {
        var dialog by rememberSaveable { mutableStateOf<String?>(null) }
        var editingDay by rememberSaveable { mutableIntStateOf(-1) }
        val locale = LocalConfiguration.current.locales[0]
        val elapsed = ChronoUnit.DAYS.between(alarm.cycleStart, LocalDate.now())
        val today = if (elapsed < 0) -1 else (elapsed % alarm.cycleLength).toInt()
        AlarmScaffold(stringResource(R.string.alarm_shift), ::finish, ::save) { insets ->
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxWidth(), contentPadding = PaddingValues(24.dp, 8.dp, 24.dp, 24.dp)) {
                    item {
                        AlarmRow(Icons.Outlined.Repeat, stringResource(R.string.alarm_cycle_length), stringResource(R.string.alarm_days, alarm.cycleLength), { dialog = "length" })
                        AlarmRow(Icons.Outlined.CalendarMonth, stringResource(R.string.alarm_cycle_start), alarmDate(alarm.cycleStart), { dialog = "start" })
                        Text(stringResource(R.string.alarm_shift_help), Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(alarm.cycleLength, key = { it }) { day ->
                        val label = stringResource(R.string.alarm_cycle_day, day + 1)
                        Surface(onClick = { editingDay = day }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(label + if (day == today) " · " + stringResource(R.string.alarm_today) else "", style = MaterialTheme.typography.bodyLarge)
                                    val minutes = alarm.cycleMinutes[day]
                                    TimeText(if (minutes < 0) stringResource(R.string.alarm_rest_day) else String.format(locale, "%02d:%02d", minutes / 60, minutes % 60), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(alarm.cycleMinutes[day] >= 0, { enabled -> edit { it.cycleMinutes[day] = if (enabled) it.hour * 60 + it.minute else -1 } }, Modifier.semantics { contentDescription = label })
                            }
                        }
                    }
                }
            }
        }
        if (dialog == "length") AlarmNumberDialog(stringResource(R.string.alarm_cycle_length), alarm.cycleLength, 2, 62, { dialog = null }) { length -> edit { it.cycleLength = length }; dialog = null }
        if (dialog == "start") AlarmDateDialog(alarm.cycleStart, { dialog = null }) { date -> edit { it.cycleStart = date }; dialog = null }
        if (editingDay >= 0) AlarmTimeDialog(alarm.cycleMinutes[editingDay].takeIf { it >= 0 } ?: (alarm.hour * 60 + alarm.minute), ClockPreferences(this).isUse24Hour(), { editingDay = -1 }) { minutes ->
            edit { it.cycleMinutes[editingDay] = minutes }; editingDay = -1
        }
    }
}
