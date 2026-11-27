package com.clockmods.ultimate.compose

import com.clockmods.ui.compose.TimeText

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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.pro.alarm.AlarmNotifications
import com.clockmods.pro.alarm.AlarmScheduler
import com.clockmods.pro.alarm.AlarmStore
import com.clockmods.pro.timer.PomodoroSequence
import com.clockmods.pro.timer.SequenceEditorView
import com.clockmods.pro.timer.TimerCompletion
import com.clockmods.pro.timer.TimerScheduler
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PomodoroScreen(modifier: Modifier) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("pro_timers", Context.MODE_PRIVATE)
    }
    val prefix = "pomodoro_"
    val mode = "pomodoro"
    var sequence by remember(preferences) {
        mutableStateOf(PomodoroSequence.load(preferences))
    }
    val defaultDuration = sequence[0].duration()
    var sequenceDraft by rememberSaveable { mutableStateOf<String?>(null) }
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

    DisposableEffect(preferences, prefix) {
        fun reload() {
            running = preferences.getBoolean(prefix + "running", false)
            sequence = PomodoroSequence.load(preferences)
            phase = PomodoroSequence.index(preferences, sequence)
            duration = preferences.getLong(prefix + "duration", defaultDuration)
            remaining = preferences.getLong(prefix + "remaining", duration)
            deadline = preferences.getLong(prefix + "deadline", 0L)
        }
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.startsWith(prefix) == true) reload()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        reload()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun persist() {
        preferences.edit().remove(TimerCompletion.PENDING_PHASE)
            .putLong(prefix + "duration", duration)
            .putLong(prefix + "remaining", remaining)
            .putLong(prefix + "deadline", deadline)
            .putBoolean(prefix + "running", running)
            .putInt(prefix + "phase", phase)
            .apply()
    }
    fun advancePomodoro() {
        TimerScheduler.cancel(context, mode)
        phase = (phase + 1) % sequence.size
        duration = sequence[phase].duration()
        remaining = duration
        deadline = 0L
        running = false
        persist()
    }

    LaunchedEffect(running, deadline) {
        while (running) {
            remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining == 0L || !preferences.getBoolean(prefix + "running", false)) {
                com.clockmods.pro.timer.TimerReceiver.complete(context, mode)
                running = preferences.getBoolean(prefix + "running", false)
                phase = preferences.getInt(prefix + "phase", 0)
                duration = preferences.getLong(prefix + "duration", defaultDuration)
                remaining = preferences.getLong(prefix + "remaining", duration)
                deadline = preferences.getLong(prefix + "deadline", 0L)
                break
            }
            kotlinx.coroutines.delay(200L)
        }
    }
    // Actions persist immediately. Never write a stale UI snapshot on disposal after the
    // alarm receiver has completed a timer while this screen was in the background.

    ToolWorkspace(
        modifier = modifier,
        time = formatPomodoro(remaining),
        rollDigits = true,
        phase = stringResource(TimerCompletion.phaseLabel(sequence[phase.coerceIn(sequence.indices)].label)),
        details = {
            FlowRow(Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { sequenceDraft = PomodoroSequence.encode(sequence) },
                    label = { Text(stringResource(R.string.pomodoro_edit_sequence)) })
            }
        },
        actions = {
            ToolActions(running, enabled = remaining > 0,
                onToggle = {
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
                onReset = {
                    TimerScheduler.cancel(context, mode)
                    running = false
                    phase = 0
                    duration = defaultDuration
                    remaining = duration
                    deadline = 0L
                    persist()
                },
                extra = Icons.Default.SkipNext,
                extraLabel = stringResource(R.string.timer_skip), onExtra = ::advancePomodoro)
        },
    )
    sequenceDraft?.let { draft ->
        val dismiss = { sequenceDraft = null }
        androidx.compose.ui.window.Dialog(onDismissRequest = dismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            com.clockmods.ultimate.navigation.PredictiveDialogBack(dismiss)
            val scheme = MaterialTheme.colorScheme
            androidx.compose.material3.Surface(
                modifier = Modifier.widthIn(max = 608.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = scheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.pomodoro_edit_sequence), style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx -> SequenceEditorView(ctx, draft,
                            intArrayOf(scheme.primaryContainer.toArgb(), scheme.secondaryContainer.toArgb(), scheme.tertiaryContainer.toArgb()),
                            intArrayOf(scheme.onPrimaryContainer.toArgb(), scheme.onSecondaryContainer.toArgb(), scheme.onTertiaryContainer.toArgb()),
                            { sequenceDraft = it }, dismiss,
                            { steps -> PomodoroSequence.apply(ctx, steps); dismiss() },
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmScreen(modifier: Modifier) { AlarmListScreen(modifier) }

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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var animateLapPlacement by remember { mutableStateOf(false) }
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

    ToolWorkspace(
        modifier = modifier,
        time = formatStopwatch(elapsed),
        detailsVisible = laps.isNotEmpty(),
        details = { StopwatchLaps(laps, listState, animateLapPlacement, Modifier.fillMaxSize()) },
        actions = {
            ToolActions(running, showSecondary = running || elapsed > 0,
                onToggle = {
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
                },
                onReset = {
                    running = false
                    accumulated = 0L
                    elapsed = 0L
                    startedAtRealtime = 0L
                    startedAtWall = 0L
                    laps.clear()
                    listState.requestScrollToItem(0)
                    persist()
                },
                extra = Icons.Default.Flag, extraLabel = stringResource(R.string.stopwatch_lap),
                onExtra = {
                    val value = calculateElapsed()
                    if (value > 0L) {
                        val atTop = !listState.canScrollBackward
                        animateLapPlacement = atTop && laps.isNotEmpty()
                        laps.add(0, value)
                        if (atTop) {
                            // Override key-based anchoring so placement alone moves the old rows.
                            listState.requestScrollToItem(0)
                        } else {
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                        persist()
                    }
                })
        },
    )
}

private fun formatPomodoro(millis: Long): String {
    val total = (millis + 999L) / 1000L
    return String.format(Locale.US, "%02d:%02d", total / 60L, total % 60L)
}

internal fun formatStopwatch(millis: Long): String {
    val centiseconds = millis / 10L
    return String.format(
        Locale.US,
        "%02d:%02d.%02d",
        centiseconds / 6000L,
        centiseconds / 100L % 60L,
        centiseconds % 100L,
    )
}
