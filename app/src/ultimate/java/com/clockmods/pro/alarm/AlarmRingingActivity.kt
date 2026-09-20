package com.clockmods.pro.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.compose.ClockModsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Full-screen Compose alarm surface shown over the lock screen. */
class AlarmRingingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase) ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AlarmStore(this).enabled()) {
            finish()
            return
        }
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            ClockModsTheme(darkTheme = true, dynamicColor = false) {
                AlarmRingingScreen(onDismiss = ::dismissAlarm)
            }
        }
    }

    private fun dismissAlarm() {
        stopService(Intent(this, AlarmRingingService::class.java))
        AlarmStore(this).setRinging(false)
        AlarmNotifications.cancel(this)
        finishAndRemoveTask()
    }

    companion object {
        internal const val EXTRA_START_RINGING =
            "com.clockmods.pro.alarm.extra.START_RINGING"
    }
}

@Composable
private fun AlarmRingingScreen(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { ClockPreferences(context) }
    val locale = remember(preferences) { LocaleManager.resolveLocale(context) }
    val timeZone = remember(preferences) {
        preferences.getTimeZoneId().takeIf(String::isNotBlank)?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L - now % 1_000L)
        }
    }
    val time = remember(now, timeZone, locale, preferences) {
        SimpleDateFormat(if (preferences.isUse24Hour()) "HH:mm" else "h:mm a", locale).run {
            this.timeZone = timeZone
            format(Date(now))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            time,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 80.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.alarm_ringing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.alarm_dismiss))
        }
    }
}
