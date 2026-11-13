package com.clockmods.pro.alarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.navigation.PredictiveAlertDialog as AlertDialog
import org.json.JSONObject
import java.time.ZoneId

class AlarmEditorActivity : AlarmUi() {
    private var childMode: String? = null
    private val child = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) runCatching {
            val changed = AlarmSpec.fromJson(JSONObject(result.data!!.getStringExtra(DRAFT)!!))
            edit { when (childMode) {
                "extra" -> { it.extraDates.clear(); it.extraDates.addAll(changed.extraDates) }
                "excluded" -> { it.excludedDates.clear(); it.excludedDates.addAll(changed.excludedDates) }
                else -> { it.cycleLength = changed.cycleLength; it.cycleStart = changed.cycleStart; changed.cycleMinutes.copyInto(it.cycleMinutes) }
            } }
        }
    }
    private val ringtone = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = androidx.core.content.IntentCompat.getParcelableExtra(result.data!!, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            edit { it.ringtone = if (uri == null) "silent" else if (RingtoneManager.isDefault(uri)) "default" else uri.toString() }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); childMode = state?.getString("childMode")
        if (state == null) {
            if (intent.hasExtra(AlarmScheduler.EXTRA_ID)) {
                draft = AlarmStore(this).find(intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0)) ?: run { finish(); return }
            } else draft.onceDate = AlarmPlan.nextDate(draft.hour, draft.minute, System.currentTimeMillis(), ZoneId.systemDefault())
        }
        setContent { ClockModsTheme { Editor(remember(revision) { AlarmSpec(draft) }) } }
    }
    override fun onSaveInstanceState(state: Bundle) { state.putString("childMode", childMode); super.onSaveInstanceState(state) }
    private fun openChild(mode: String) {
        childMode = mode
        child.launch(Intent(this, if (mode == "shift") AlarmShiftActivity::class.java else AlarmDatesActivity::class.java).putExtra(DRAFT, json()).putExtra(MODE, mode))
    }
    private fun chooseRingtone() {
        val current = if (draft.ringtone == "silent") null else if (draft.ringtone == "default") RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else Uri.parse(draft.ringtone)
        val picker = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        try { ringtone.launch(picker) } catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this, R.string.alarm_ringtone_unavailable, Toast.LENGTH_LONG).show() }
    }
    private fun save() {
        if (draft.repeat == AlarmSpec.Repeat.WEEKLY && draft.weekdays == 0 && draft.extraDates.isEmpty()) {
            Toast.makeText(this, R.string.alarm_choose_days, Toast.LENGTH_LONG).show(); return
        }
        draft.enabled = true; draft.snoozeAt = 0; draft.lastFiredAt = 0
        if (AlarmScheduler.nextTrigger(draft, this, System.currentTimeMillis()) == 0L) {
            Toast.makeText(this, R.string.alarm_no_next, Toast.LENGTH_LONG).show(); return
        }
        AlarmStore(this).save(draft); AlarmScheduler.schedule(this, draft); setResult(RESULT_OK); finish()
    }
    @Composable private fun Editor(alarm: AlarmSpec) {
        var dialog by rememberSaveable { mutableStateOf<String?>(null) }
        var label by rememberSaveable { mutableStateOf("") }
        AlarmScaffold(stringResource(if (alarm.id == 0L) R.string.alarm_add else R.string.alarm_edit_title), ::finish, ::save) { insets ->
            AlarmColumn(insets) {
                AlarmTimeHeader(alarm.hour * 60 + alarm.minute) { dialog = "time" }
                AlarmRow(Icons.Outlined.Repeat, stringResource(R.string.alarm_repeat), stringResource(AlarmLabels.REPEATS[alarm.repeat.ordinal]), { dialog = "repeat" })
                if (alarm.repeat == AlarmSpec.Repeat.WEEKLY) AlarmWeekdays(alarm.weekdays) { value -> edit { it.weekdays = value } }
                if (alarm.repeat == AlarmSpec.Repeat.ONCE) AlarmRow(Icons.Outlined.CalendarMonth, stringResource(R.string.alarm_once_date), alarmDate(alarm.onceDate), { dialog = "date" })
                if (alarm.repeat == AlarmSpec.Repeat.SHIFT) AlarmRow(Icons.Outlined.WorkOutline, stringResource(R.string.alarm_shift), stringResource(R.string.alarm_days, alarm.cycleLength), { openChild("shift") })
                if (alarm.repeat == AlarmSpec.Repeat.HOLIDAYS || alarm.repeat == AlarmSpec.Repeat.WORKDAYS)
                    Text(stringResource(R.string.alarm_holiday_coverage), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                AlarmRow(Icons.Outlined.EventAvailable, stringResource(R.string.alarm_extra_dates), stringResource(R.string.alarm_date_count, alarm.extraDates.size), { openChild("extra") })
                AlarmRow(Icons.Outlined.EventBusy, stringResource(R.string.alarm_excluded_dates), stringResource(R.string.alarm_date_count, alarm.excludedDates.size), { openChild("excluded") })
                Spacer(Modifier.height(12.dp))
                AlarmRow(Icons.Outlined.Label, stringResource(R.string.alarm_label), alarm.label, { label = alarm.label; dialog = "label" })
                AlarmRow(Icons.Outlined.NotificationsNone, stringResource(R.string.alarm_ringtone), AlarmLabels.ringtone(this@AlarmEditorActivity, alarm), ::chooseRingtone)
                AlarmRow(Icons.Outlined.Vibration, stringResource(R.string.alarm_vibrate), onClick = { edit { it.vibrate = !it.vibrate } }, trailing = {
                    Switch(alarm.vibrate, { value -> edit { it.vibrate = value } })
                })
                AlarmRow(Icons.Outlined.Snooze, stringResource(R.string.alarm_snooze), stringResource(R.string.timer_minutes, alarm.snoozeMinutes), { dialog = "snooze" })
                if (alarm.id != 0L) { Spacer(Modifier.height(16.dp)); TextButton(onClick = { dialog = "delete" }) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.alarm_delete), color = MaterialTheme.colorScheme.error)
                } }
            }
        }
        when (dialog) {
            "time" -> AlarmTimeDialog(alarm.hour * 60 + alarm.minute, ClockPreferences(this).isUse24Hour(), { dialog = null }) { value ->
                edit { val old = it.hour * 60 + it.minute; it.hour = value / 60; it.minute = value % 60
                    for (index in it.cycleMinutes.indices) if (it.cycleMinutes[index] == old) it.cycleMinutes[index] = value
                    if (it.repeat == AlarmSpec.Repeat.ONCE) it.onceDate = AlarmPlan.nextDate(it.hour, it.minute, System.currentTimeMillis(), ZoneId.systemDefault()) }
                dialog = null
            }
            "date" -> AlarmDateDialog(alarm.onceDate, { dialog = null }) { date -> edit { it.onceDate = date }; dialog = null }
            "snooze" -> AlarmNumberDialog(stringResource(R.string.alarm_snooze), alarm.snoozeMinutes, 1, 60, { dialog = null }) { number -> edit { it.snoozeMinutes = number }; dialog = null }
            "repeat" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.alarm_repeat)) }, text = {
                Column { AlarmSpec.Repeat.values().forEach { repeat ->
                    Row(Modifier.fillMaxWidth().clickable { edit { it.repeat = repeat }; dialog = null }.padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(alarm.repeat == repeat, null); Spacer(Modifier.width(12.dp)); Text(stringResource(AlarmLabels.REPEATS[repeat.ordinal]))
                    }
                } }
            }, confirmButton = {}, dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } })
            "label" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.alarm_label)) },
                text = { OutlinedTextField(label, { label = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { edit { it.label = label.trim() }; dialog = null }) { Text(stringResource(R.string.apply)) } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } })
            "delete" -> AlertDialog(onDismissRequest = { dialog = null }, text = { Text(stringResource(R.string.alarm_delete_confirm)) },
                confirmButton = { TextButton(onClick = {
                    AlarmScheduler.cancel(this, alarm.id); val store = AlarmStore(this); store.delete(alarm.id)
                    if (store.isRinging) startService(AlarmDelivery.command(this, alarm.id, AlarmDelivery.STOP)); finish()
                }) { Text(stringResource(R.string.alarm_delete), color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } })
        }
    }
}
