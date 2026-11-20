package com.clockmods.pro.alarm

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ui.compose.TimeText
import kotlinx.coroutines.delay
import java.util.Date

class AlarmRingingActivity : ComponentActivity() {
    private var alarm by mutableStateOf<AlarmSpec?>(null)
    private var swapped by mutableStateOf(false)
    private lateinit var preferences: SharedPreferences
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "ringing_id") refresh()
    }
    override fun attachBaseContext(base: Context) = super.attachBaseContext(LocaleManager.wrap(base) ?: base)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val store = AlarmStore(this)
        store.pendingRings().keys.firstOrNull()?.let { id ->
            androidx.core.content.ContextCompat.startForegroundService(this,
                Intent(this, AlarmRingingService::class.java).putExtra(AlarmScheduler.EXTRA_ID, id))
        }
        preferences = getSharedPreferences("pro_alarm", MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(listener)
        refresh()
        setContent { ClockModsTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val current = alarm ?: return@Column
                    Text(if (current.isTimer) stringResource(if (current.countdown) R.string.pro_page_countdown else R.string.pro_page_pomodoro)
                        else AlarmDelivery.title(this@AlarmRingingActivity, current),
                        style = MaterialTheme.typography.headlineMedium)
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        val time = if (current.isTimer) {
                            if (current.timerDuration > 0) AlarmDelivery.duration(current) else current.label
                        } else {
                            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                            LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
                            android.text.format.DateFormat.getTimeFormat(this@AlarmRingingActivity).format(Date(now))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (current.isTimer && !current.countdown && current.timerDuration > 0)
                                Text(stringResource(AlarmDelivery.phaseLabel(current)), style = MaterialTheme.typography.titleLarge)
                            val size = (this@BoxWithConstraints.maxWidth.value / (time.length.coerceAtLeast(1) * .65f) /
                                androidx.compose.ui.platform.LocalDensity.current.fontScale).coerceAtMost(72f)
                            TimeText(time, fontSize = size.sp, maxLines = 1)
                        }
                    }
                    Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val actions = when {
                            current.isTimer && !current.countdown && current.completedDeadline != 0L -> listOf(AlarmDelivery.STOP, AlarmDelivery.NEXT)
                            current.isTimer -> listOf(AlarmDelivery.STOP)
                            swapped -> listOf(AlarmDelivery.STOP, AlarmDelivery.SNOOZE)
                            else -> listOf(AlarmDelivery.SNOOZE, AlarmDelivery.STOP)
                        }
                        actions.forEach { action ->
                            if (action == AlarmDelivery.SNOOZE) {
                                FilledTonalButton(onClick = { command(action) }, Modifier.weight(1f).heightIn(min = 72.dp)) {
                                    Text(stringResource(R.string.alarm_snooze), style = MaterialTheme.typography.titleLarge)
                                }
                            } else {
                                Button(onClick = { command(action) }, Modifier.weight(1f).heightIn(min = 72.dp)) {
                                    Text(stringResource(if (action == AlarmDelivery.NEXT) R.string.pomodoro_next else AlarmDelivery.stopLabel(current)),
                                        style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
        } }
    }
    private fun refresh() {
        val store = AlarmStore(this)
        val id = store.ringingId().takeIf { it != 0L } ?: store.pendingRings().keys.firstOrNull()
        alarm = id?.let(store::find)
        swapped = id?.let { AlarmPreferences.swapped(this, it) } ?: false
        if (alarm == null) finish()
    }
    private fun volumeSnooze(key: Int): Boolean =
        alarm?.isTimer == false &&
            (key == android.view.KeyEvent.KEYCODE_VOLUME_UP || key == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || key == android.view.KeyEvent.KEYCODE_VOLUME_MUTE) &&
            AlarmPreferences.enabled(this, AlarmPreferences.VOLUME)
    override fun onKeyDown(key: Int, event: android.view.KeyEvent): Boolean {
        if (volumeSnooze(key)) { if (event.repeatCount == 0) command(AlarmDelivery.SNOOZE); return true }
        return super.onKeyDown(key, event)
    }
    override fun onKeyUp(key: Int, event: android.view.KeyEvent): Boolean = volumeSnooze(key) || super.onKeyUp(key, event)
    private fun command(action: String) { alarm?.let { startService(AlarmDelivery.command(this, it.id, action)) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); refresh() }
    override fun onDestroy() { if (::preferences.isInitialized) preferences.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
}
