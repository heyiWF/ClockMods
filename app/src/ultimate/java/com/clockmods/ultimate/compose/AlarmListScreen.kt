package com.clockmods.ultimate.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clockmods.R
import com.clockmods.pro.alarm.*
import com.clockmods.ui.compose.TimeText
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AlarmListScreen(modifier: Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val store = remember(context) { AlarmStore(context) }
    var revision by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            AlarmScheduler.rescheduleAll(context); revision++
        } }
        lifecycle.addObserver(observer)
        val preferences = context.getSharedPreferences("pro_alarm", 0)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "alarms_v2" || key == "ringing_id") revision++
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { lifecycle.removeObserver(observer); preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val editor = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { revision++ }
    val alarms = remember(revision) { store.all() }
    val ringing = remember(revision) { store.isRinging() }
    Scaffold(modifier, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { editor.launch(Intent(context, AlarmEditorActivity::class.java)) },
            icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(R.string.alarm_add)) })
    }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { insets ->
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ringing) item {
            Button(onClick = { context.startActivity(Intent(context, AlarmRingingActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.alarm_open_to_dismiss))
            }
        }
        if (!AlarmScheduler.canScheduleExact(context)) item { AccessButton(R.string.alarm_exact_access_required, 0) }
        if (!AlarmAccess.fullScreen(context)) item { AccessButton(R.string.alarm_full_screen_access_required, 1) }
        if (!AlarmAccess.notifications(context)) item { AccessButton(R.string.alarm_notification_access_required, 2) }
        if (alarms.isEmpty()) item { Text(stringResource(R.string.alarm_empty), Modifier.padding(vertical = 24.dp)) }
        items(alarms, key = { it.id }) { alarm ->
            Card(onClick = { editor.launch(Intent(context, AlarmEditorActivity::class.java).putExtra(AlarmScheduler.EXTRA_ID, alarm.id)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (alarm.label.isNotEmpty()) alarm.label else AlarmLabels.repeat(context, alarm), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeText(String.format(locale, "%02d:%02d", alarm.hour, alarm.minute), Modifier.weight(1f), fontSize = 56.sp)
                        Switch(checked = alarm.enabled || alarm.snoozeAt > System.currentTimeMillis(), onCheckedChange = {
                            AlarmAccess.enabled(context, alarm, it); revision++
                        })
                    }
                    val next = AlarmScheduler.nextTrigger(alarm, context, System.currentTimeMillis())
                    TimeText(if (next > 0) stringResource(R.string.alarm_next_trigger,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(next)))
                        else stringResource(if (alarm.enabled) R.string.alarm_no_next else R.string.alarm_disabled),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
}
}

@Composable
private fun AccessButton(message: Int, kind: Int) {
    val context = LocalContext.current
    OutlinedButton(onClick = { AlarmAccess.settings(context, kind) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(message))
    }
}
