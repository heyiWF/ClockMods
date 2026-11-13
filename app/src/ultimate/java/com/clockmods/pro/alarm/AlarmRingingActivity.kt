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
                    Text(alarm?.label?.takeIf { it.isNotBlank() } ?: stringResource(R.string.alarm_ringing),
                        style = MaterialTheme.typography.headlineMedium)
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        TimeText(android.text.format.DateFormat.getTimeFormat(this@AlarmRingingActivity).format(Date(now)),
                            fontSize = 64.sp, maxLines = 1)
                    }
                    Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { command(AlarmDelivery.SNOOZE) }, Modifier.weight(1f).heightIn(min = 72.dp)) {
                            Text(stringResource(R.string.alarm_snooze), style = MaterialTheme.typography.titleLarge)
                        }
                        Button(onClick = { command(AlarmDelivery.STOP) }, Modifier.weight(1f).heightIn(min = 72.dp)) {
                            Text(stringResource(R.string.alarm_dismiss), style = MaterialTheme.typography.titleLarge)
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
        if (alarm == null) finish()
    }
    private fun command(action: String) { alarm?.let { startService(AlarmDelivery.command(this, it.id, action)) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); refresh() }
    override fun onDestroy() { if (::preferences.isInitialized) preferences.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
}
