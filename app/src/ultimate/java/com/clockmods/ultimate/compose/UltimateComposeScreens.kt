package com.clockmods.ultimate.compose

import android.content.res.Configuration
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.ultimate.AntiBurnPreferences
import com.clockmods.ultimate.ComposeMainActivity
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private enum class Destination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    CLOCK(R.string.pro_page_clock, Icons.Default.AccessTime),
    CALENDAR(R.string.pro_page_calendar, Icons.Default.CalendarMonth),
    POMODORO(R.string.pro_page_pomodoro, Icons.Default.Timer),
    ALARM(R.string.pro_page_alarm, Icons.Default.Alarm),
    COUNTDOWN(R.string.pro_page_countdown, Icons.Default.HourglassBottom),
    STOPWATCH(R.string.pro_page_stopwatch, Icons.Default.AccessAlarm),
}

/** Compose-native application shell shared by every Ultimate destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateApp(
    refreshGeneration: Int,
    requestedDestination: String? = null,
    navigationRequestGeneration: Int = 0,
    onOpenSettings: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.CLOCK) }
    LaunchedEffect(navigationRequestGeneration) {
        destination = when (requestedDestination) {
            ComposeMainActivity.DESTINATION_CALENDAR -> Destination.CALENDAR
            ComposeMainActivity.DESTINATION_CLOCK -> Destination.CLOCK
            else -> return@LaunchedEffect
        }
    }
    val configuration = LocalConfiguration.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    val useNavigationRail = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        with(density) { windowWidth.toDp() >= 600.dp }
    val navigationType = if (useNavigationRail) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
        layoutType = navigationType,
        navigationSuiteItems = {
            Destination.entries.forEach { item ->
                item(
                    selected = destination == item,
                    onClick = { destination = item },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = stringResource(item.labelRes),
                        )
                    },
                    label = { Text(stringResource(item.labelRes), maxLines = 1) },
                    alwaysShowLabel = false,
                )
            }
        },
    ) {
        AntiBurnSurface(refreshGeneration = refreshGeneration) { antiBurnModifier ->
            Scaffold(
                modifier = antiBurnModifier,
                topBar = {
                    if (destination != Destination.CLOCK) {
                        TopAppBar(
                            title = { Text(stringResource(destination.labelRes)) },
                            actions = {
                                IconButton(onClick = onOpenSettings) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = stringResource(
                                            R.string.open_settings_accessibility,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                },
            ) { padding ->
                when (destination) {
                    Destination.CLOCK -> ClockScreen(
                        modifier = Modifier.padding(padding),
                        refreshGeneration = refreshGeneration,
                        onOpenSettings = onOpenSettings,
                    )
                    Destination.CALENDAR ->
                        CalendarScreen(Modifier.padding(padding), refreshGeneration)
                    Destination.POMODORO ->
                        TimerScreen(Modifier.padding(padding), pomodoro = true)
                    Destination.COUNTDOWN ->
                        TimerScreen(Modifier.padding(padding), pomodoro = false)
                    Destination.ALARM -> AlarmScreen(Modifier.padding(padding))
                    Destination.STOPWATCH -> StopwatchScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

/** Mirrors the old View controller while keeping the navigation chrome stationary and usable. */
@Composable
private fun AntiBurnSurface(
    refreshGeneration: Int,
    content: @Composable (Modifier) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val preferences = remember(context, refreshGeneration) { AntiBurnPreferences(context) }
    val enabled = preferences.isEnabled()
    val periodMillis = preferences.getPeriodMinutes() * 60_000L
    val amplitudePx = with(density) { preferences.getAmplitudeDp().dp.toPx() }
    val autoDim = preferences.isAutoDim()
    var elapsedRealtime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(enabled, periodMillis, amplitudePx) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            elapsedRealtime = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }

    val progress = if (enabled) {
        (elapsedRealtime % periodMillis).toFloat() / periodMillis.toFloat()
    } else {
        0f
    }
    val angle = progress * PI.toFloat() * 2f
    val translationX = if (enabled) sin(angle) * amplitudePx else 0f
    val translationY = if (enabled) {
        sin(angle * 2f + PI.toFloat() / 3f) * amplitudePx * .72f
    } else {
        0f
    }
    val scaleX = if (enabled && contentSize.width > 0) {
        1f + amplitudePx * 2f / contentSize.width
    } else {
        1f
    }
    val scaleY = if (enabled && contentSize.height > 0) {
        1f + amplitudePx * 2f / contentSize.height
    } else {
        1f
    }

    Box(Modifier.fillMaxSize()) {
        content(
            Modifier
                .fillMaxSize()
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    this.translationX = translationX
                    this.translationY = translationY
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                },
        )
        if (enabled && autoDim) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .alpha(.14f),
            )
        }
    }
}
