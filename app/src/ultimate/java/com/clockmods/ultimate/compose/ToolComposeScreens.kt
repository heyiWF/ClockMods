package com.clockmods.ultimate.compose

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.pro.alarm.AlarmNotifications
import com.clockmods.pro.alarm.AlarmScheduler
import com.clockmods.pro.alarm.AlarmStore
import com.clockmods.pro.timer.TimerScheduler
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TimerScreen(modifier: Modifier, pomodoro: Boolean) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("pro_timers", Context.MODE_PRIVATE)
    }
    val prefix = if (pomodoro) "pomodoro_" else "countdown_"
    val mode = if (pomodoro) "pomodoro" else "countdown"
    val defaultDuration = if (pomodoro) 25 * 60_000L else 10 * 60_000L
    var duration by rememberSaveable {
        mutableLongStateOf(preferences.getLong(prefix + "duration", defaultDuration))
    }
    var remaining by rememberSaveable {
        mutableLongStateOf(preferences.getLong(prefix + "remaining", duration))
    }
    var deadline by rememberSaveable {
        mutableLongStateOf(preferences.getLong(prefix + "deadline", 0L))
    }
    var running by rememberSaveable {
        mutableStateOf(preferences.getBoolean(prefix + "running", false))
    }
    var phase by rememberSaveable {
        mutableIntStateOf(preferences.getInt(prefix + "phase", 0))
    }
    var customDialog by rememberSaveable { mutableStateOf(false) }

    fun persist() {
        preferences.edit()
            .putLong(prefix + "duration", duration)
            .putLong(prefix + "remaining", remaining)
            .putLong(prefix + "deadline", deadline)
            .putBoolean(prefix + "running", running)
            .putInt(prefix + "phase", phase)
            .apply()
    }
    fun setDuration(value: Long) {
        TimerScheduler.cancel(context, mode)
        running = false
        duration = value
        remaining = value
        deadline = 0L
        persist()
    }
    fun advancePomodoro() {
        TimerScheduler.cancel(context, mode)
        phase = (phase + 1) % 3
        duration = when (phase) {
            0 -> 25 * 60_000L
            1 -> 5 * 60_000L
            else -> 15 * 60_000L
        }
        remaining = duration
        deadline = 0L
        running = false
        persist()
    }

    LaunchedEffect(running, deadline) {
        while (running) {
            remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining == 0L) {
                running = false
                if (pomodoro) advancePomodoro() else persist()
                break
            }
            kotlinx.coroutines.delay(200L)
        }
    }
    val latestDuration by rememberUpdatedState(duration)
    val latestRemaining by rememberUpdatedState(remaining)
    val latestDeadline by rememberUpdatedState(deadline)
    val latestRunning by rememberUpdatedState(running)
    val latestPhase by rememberUpdatedState(phase)
    DisposableEffect(Unit) {
        onDispose {
            preferences.edit()
                .putLong(prefix + "duration", latestDuration)
                .putLong(prefix + "remaining", latestRemaining)
                .putLong(prefix + "deadline", latestDeadline)
                .putBoolean(prefix + "running", latestRunning)
                .putInt(prefix + "phase", latestPhase)
                .apply()
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (pomodoro) {
            Text(
                stringResource(
                    when (phase) {
                        0 -> R.string.pomodoro_focus
                        1 -> R.string.pomodoro_short_break
                        else -> R.string.pomodoro_long_break
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            formatDuration(remaining, pomodoro),
            fontSize = if (pomodoro) 64.sp else 54.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (running) {
                        remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
                        running = false
                        TimerScheduler.cancel(context, mode)
                    } else if (remaining > 0L) {
                        deadline = System.currentTimeMillis() + remaining
                        running = true
                        TimerScheduler.schedule(context, mode, deadline)
                    }
                    persist()
                },
                enabled = remaining > 0L,
            ) {
                Icon(
                    if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (running) R.string.timer_pause else R.string.timer_start))
            }
            OutlinedButton(onClick = {
                TimerScheduler.cancel(context, mode)
                running = false
                if (pomodoro) {
                    phase = 0
                    duration = defaultDuration
                }
                remaining = duration
                deadline = 0L
                persist()
            }) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.timer_reset))
            }
            if (pomodoro) {
                OutlinedButton(onClick = ::advancePomodoro) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.timer_skip))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val presets = if (pomodoro) listOf(5L, 15L, 25L) else listOf(5L, 10L, 30L, 60L)
            presets.forEach { minutes ->
                FilterChip(
                    selected = duration == minutes * 60_000L,
                    onClick = { setDuration(minutes * 60_000L) },
                    label = { Text(stringResource(R.string.timer_minutes, minutes)) },
                )
            }
            if (!pomodoro) {
                FilterChip(
                    selected = false,
                    onClick = { customDialog = true },
                    label = { Text(stringResource(R.string.timer_custom)) },
                )
            }
        }
    }
    if (customDialog) {
        CustomDurationDialog(
            initialMillis = duration,
            onDismiss = { customDialog = false },
            onConfirm = {
                setDuration(it)
                customDialog = false
            },
        )
    }
}

@Composable
private fun CustomDurationDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val totalSeconds = (initialMillis / 1000L)
        .coerceAtMost(99L * 3600L + 59L * 60L + 59L)
    var hours by rememberSaveable { mutableStateOf((totalSeconds / 3600L).toString()) }
    var minutes by rememberSaveable { mutableStateOf((totalSeconds / 60L % 60L).toString()) }
    var seconds by rememberSaveable { mutableStateOf((totalSeconds % 60L).toString()) }
    val duration = customDurationMillis(
        hours.toIntOrNull(),
        minutes.toIntOrNull(),
        seconds.toIntOrNull(),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_custom_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = stringResource(R.string.timer_hours),
                    modifier = Modifier.weight(1f),
                )
                DurationField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = stringResource(R.string.timer_minutes_unit),
                    modifier = Modifier.weight(1f),
                )
                DurationField(
                    value = seconds,
                    onValueChange = { seconds = it },
                    label = stringResource(R.string.timer_seconds),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(duration) },
                enabled = duration > 0L,
            ) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun DurationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(2)) },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

internal fun customDurationMillis(hours: Int?, minutes: Int?, seconds: Int?): Long {
    if (hours == null || minutes == null || seconds == null) return 0L
    if (hours !in 0..99 || minutes !in 0..59 || seconds !in 0..59) return 0L
    return (hours * 3600L + minutes * 60L + seconds) * 1000L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmScreen(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember { AlarmStore(context) }
    val use24Hour = remember { ClockPreferences(context).isUse24Hour() }
    var enabled by remember { mutableStateOf(store.enabled()) }
    var hour by remember { mutableIntStateOf(store.hour()) }
    var minute by remember { mutableIntStateOf(store.minute()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var exactAlarmAccess by remember { mutableStateOf(AlarmScheduler.canScheduleExact(context)) }
    var fullScreenIntentAccess by remember {
        mutableStateOf(AlarmNotifications.canUseFullScreenIntent(context))
    }
    var notificationAccess by remember {
        mutableStateOf(AlarmNotifications.canPostNotifications(context))
    }
    val exactAlarmAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        exactAlarmAccess = AlarmScheduler.canScheduleExact(context)
        if (enabled) AlarmScheduler.schedule(context, hour, minute)
    }
    val fullScreenIntentAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        fullScreenIntentAccess = AlarmNotifications.canUseFullScreenIntent(context)
        notificationAccess = AlarmNotifications.canPostNotifications(context)
    }
    val notificationAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notificationAccess = AlarmNotifications.canPostNotifications(context)
    }
    val pickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = use24Hour,
    )
    val nextTrigger = if (enabled) {
        AlarmScheduler.nextTrigger(hour, minute, System.currentTimeMillis())
    } else 0L

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format(
                            LocalConfiguration.current.locales[0],
                            "%02d:%02d",
                            hour,
                            minute,
                        ),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        store.setEnabled(it)
                        if (it) {
                            AlarmScheduler.schedule(context, hour, minute)
                            exactAlarmAccess = AlarmScheduler.canScheduleExact(context)
                        } else AlarmScheduler.cancel(context)
                    })
                }
                Text(
                    if (enabled) {
                        stringResource(
                            R.string.alarm_next_trigger,
                            DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.SHORT,
                            ).format(Date(nextTrigger)),
                        )
                    } else stringResource(R.string.alarm_disabled),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.AccessAlarm, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.alarm_choose_time))
                }
                if (enabled && !exactAlarmAccess) {
                    Text(
                        stringResource(R.string.alarm_exact_access_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            val request = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            )
                            runCatching { exactAlarmAccessLauncher.launch(request) }
                        },
                    ) {
                        Text(stringResource(R.string.alarm_grant_exact_access))
                    }
                }
                if (enabled && !fullScreenIntentAccess) {
                    Text(
                        stringResource(R.string.alarm_full_screen_access_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            AlarmNotifications.fullScreenIntentSettingsIntent(context)?.let { request ->
                                runCatching { fullScreenIntentAccessLauncher.launch(request) }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.alarm_grant_full_screen_access))
                    }
                }
                if (enabled && !notificationAccess) {
                    Text(
                        stringResource(R.string.alarm_notification_access_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                runCatching {
                                    notificationAccessLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                }
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                    )
                                }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.alarm_grant_notification_access))
                    }
                }
            }
        }
    }
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.alarm_choose_time)) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = pickerState.hour
                    minute = pickerState.minute
                    enabled = true
                    store.save(hour, minute, true)
                    AlarmScheduler.schedule(context, hour, minute)
                    exactAlarmAccess = AlarmScheduler.canScheduleExact(context)
                    showPicker = false
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun StopwatchScreen(modifier: Modifier) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("pro_stopwatch", Context.MODE_PRIVATE)
    }
    var running by rememberSaveable {
        mutableStateOf(preferences.getBoolean("running", false))
    }
    var accumulated by rememberSaveable {
        mutableLongStateOf(preferences.getLong("accumulated", 0L))
    }
    val initialRealtime = SystemClock.elapsedRealtime()
    val initialWall = System.currentTimeMillis()
    var startedAtRealtime by rememberSaveable {
        mutableLongStateOf(
            preferences.getLong("started_at_realtime", 0L)
                .takeIf { running && it > 0L } ?: initialRealtime,
        )
    }
    var startedAtWall by rememberSaveable {
        mutableLongStateOf(
            preferences.getLong("started_at_wall", 0L)
                .takeIf { running && it > 0L } ?: initialWall,
        )
    }
    fun calculateElapsed(): Long = StopwatchTimebase.elapsed(
        accumulatedMillis = accumulated,
        running = running,
        startedAtRealtimeMillis = startedAtRealtime,
        startedAtWallMillis = startedAtWall,
        nowRealtimeMillis = SystemClock.elapsedRealtime(),
        nowWallMillis = System.currentTimeMillis(),
    )
    var elapsed by rememberSaveable { mutableLongStateOf(calculateElapsed()) }
    val laps = remember {
        mutableStateListOf<Long>().apply {
            preferences.getString("laps", "").orEmpty().split(',')
                .mapNotNull(String::toLongOrNull)
                .forEach(::add)
        }
    }
    fun persist() {
        preferences.edit()
            .putLong("accumulated", accumulated)
            .putBoolean("running", running)
            .putLong("started_at_realtime", if (running) startedAtRealtime else 0L)
            .putLong("started_at_wall", if (running) startedAtWall else 0L)
            .putString("laps", laps.joinToString(","))
            .apply()
    }
    LaunchedEffect(running) {
        while (running) {
            elapsed = calculateElapsed()
            kotlinx.coroutines.delay(32L)
        }
    }

    LaunchedEffect(Unit) {
        // Migrates a running stopwatch saved by the View implementation, which had no time base.
        if (running && preferences.getLong("started_at_realtime", 0L) <= 0L) persist()
    }

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            formatStopwatch(elapsed),
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                if (running) {
                    accumulated = calculateElapsed()
                    elapsed = accumulated
                    running = false
                } else {
                    startedAtRealtime = SystemClock.elapsedRealtime()
                    startedAtWall = System.currentTimeMillis()
                    running = true
                }
                persist()
            }) {
                Icon(
                    if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (running) R.string.timer_pause else R.string.timer_start))
            }
            OutlinedButton(
                onClick = {
                    val value = calculateElapsed()
                    if (value > 0L) {
                        laps.add(0, value)
                        persist()
                    }
                },
                enabled = running || elapsed > 0L,
            ) {
                Icon(Icons.Default.Flag, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.stopwatch_lap))
            }
            OutlinedButton(onClick = {
                running = false
                accumulated = 0L
                elapsed = 0L
                startedAtRealtime = 0L
                startedAtWall = 0L
                laps.clear()
                persist()
            }) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.timer_reset))
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f).widthIn(max = 560.dp)) {
            itemsIndexed(laps) { index, lap ->
                Text(
                    stringResource(
                        R.string.stopwatch_lap_value,
                        laps.size - index,
                        formatStopwatch(lap),
                    ),
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private fun formatDuration(millis: Long, pomodoro: Boolean): String {
    val total = (millis + 999L) / 1000L
    return if (pomodoro) {
        String.format(Locale.US, "%02d:%02d", total / 60L, total % 60L)
    } else {
        String.format(
            Locale.US,
            "%02d:%02d:%02d",
            total / 3600L,
            total / 60L % 60L,
            total % 60L,
        )
    }
}

private fun formatStopwatch(millis: Long): String {
    val centiseconds = millis / 10L
    return String.format(
        Locale.US,
        "%02d:%02d.%02d",
        centiseconds / 6000L,
        centiseconds / 100L % 60L,
        centiseconds % 100L,
    )
}
