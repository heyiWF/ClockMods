package com.clockmods.ultimate.compose

import android.content.SharedPreferences
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.clockmods.R
import com.clockmods.pro.timer.CountdownKeypad
import com.clockmods.pro.timer.CountdownTimers
import com.clockmods.ultimate.navigation.PredictiveAlertDialog
import kotlinx.coroutines.delay

@Composable
internal fun CountdownScreen(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember(context) { CountdownTimers(context) }
    var timers by remember(store) { mutableStateOf(store.all()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith("countdown")) {
                timers = store.all()
                now = System.currentTimeMillis()
            }
        }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        timers = store.all()
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(store, timers, lifecycle) {
        if (timers.any { it.running }) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                timers.filter { it.running && it.deadline <= now }.forEach { store.complete(it.id) }
                delay(250)
            }
        }
    }
    fun start(duration: Long, label: String) {
        store.startNew(duration, label)
        list.requestScrollToItem(0)
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 30, 60).forEach { minutes ->
                        AssistChip(onClick = { start(minutes * 60_000L, "") },
                            label = { Text(stringResource(R.string.timer_minutes, minutes)) })
                    }
                }
                Button(onClick = { adding = true }) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.timer_custom))
                }
            }
            LazyColumn(state = list, verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)) {
                items(timers, key = { it.id }) { timer ->
                    CountdownCard(timer, timer.remainingAt(now), store, Modifier.animateItem(), onRename = { renaming = timer.id })
                }
            }
        }
    }
    if (adding) CountdownInput(onDismiss = { adding = false }, onStart = { duration, label ->
        start(duration, label)
        adding = false
    })
    timers.find { it.id == renaming }?.let { timer ->
        var label by rememberSaveable(timer.id) { mutableStateOf(timer.label) }
        PredictiveAlertDialog(onDismissRequest = { renaming = null }, title = { Text(stringResource(R.string.countdown_label)) },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, label = { Text(stringResource(R.string.countdown_label)) }) },
            confirmButton = { TextButton(onClick = { store.rename(timer.id, label); renaming = null }) { Text(stringResource(R.string.apply)) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun CountdownCard(timer: CountdownTimers.Timer, remaining: Long, store: CountdownTimers, modifier: Modifier, onRename: () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClickLabel = stringResource(R.string.countdown_label), onClick = onRename),
                    contentAlignment = Alignment.CenterStart) {
                    Text(timer.name(LocalContext.current), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = { store.remove(timer.id) }) { Icon(Icons.Default.Close, stringResource(R.string.countdown_delete)) }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                val time = CountdownTimers.time(remaining)
                val size = (maxWidth.value / (time.length * .62f) / LocalDensity.current.fontScale).coerceIn(12f, 52f / LocalDensity.current.fontScale)
                RollingTime(time, size)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { store.reset(timer.id) }) { Icon(Icons.Default.RestartAlt, stringResource(R.string.timer_reset)) }
                }
                Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { (remaining.toFloat() / timer.duration.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.size(72.dp), strokeWidth = 4.dp)
                    FilledIconButton(onClick = { store.toggle(timer.id) }, modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (timer.running) Color(0xFFF4C95D) else MaterialTheme.colorScheme.primary,
                            contentColor = if (timer.running) Color(0xFF302500) else MaterialTheme.colorScheme.onPrimary)) {
                        Crossfade(timer.running, label = "countdown control") { running ->
                            Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                stringResource(if (running) R.string.timer_pause else R.string.timer_start))
                        }
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    androidx.compose.animation.AnimatedVisibility(timer.running, enter = fadeIn() + slideInHorizontally { -it / 4 },
                        exit = fadeOut() + slideOutHorizontally { -it / 4 }) {
                        FilledTonalButton(onClick = { store.addMinute(timer.id) }, enabled = timer.running,
                            contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text(stringResource(R.string.countdown_minute), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownInput(onDismiss: () -> Unit, onStart: (Long, String) -> Unit) {
    var digits by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    PredictiveAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.countdown_new)) },
        text = {
            AndroidView(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                factory = { context -> CountdownKeypad(context, digits, label, colors.surfaceContainerHighest.toArgb(),
                    colors.onSurface.toArgb()) { value, name -> digits = value; label = name } })
        },
        confirmButton = {
            TextButton(enabled = CountdownTimers.keypadMillis(digits) > 0, onClick = { onStart(CountdownTimers.keypadMillis(digits), label) }) {
                Text(stringResource(R.string.timer_start))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
