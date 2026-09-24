package com.clockmods.ultimate.compose

import android.app.Activity
import android.content.res.Configuration
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.clockmods.R
import com.clockmods.background.BackgroundRepository
import com.clockmods.pro.chime.HourlyChimeController
import com.clockmods.ultimate.AntiBurnPreferences
import com.clockmods.ultimate.ComposeMainActivity
import com.clockmods.ultimate.clock.UltimateClockPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar
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
    onOpenCalendarSettings: () -> Unit = onOpenSettings,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.CLOCK) }
    var chromeVisible by rememberSaveable { mutableStateOf(false) }
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
    // Clock and calendar share the original full-screen presentation; a background tap reveals navigation.
    val immersive = (destination == Destination.CLOCK || destination == Destination.CALENDAR) && !chromeVisible
    val navigationType = when {
        immersive -> NavigationSuiteType.None
        useNavigationRail -> NavigationSuiteType.NavigationRail
        else -> NavigationSuiteType.NavigationBar
    }
    val view = LocalView.current
    LaunchedEffect(immersive) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? Activity)?.window ?: return@onDispose
            WindowCompat.getInsetsController(window, view)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(destination) {
        if (destination == Destination.CLOCK) chromeVisible = false
    }

    Box(Modifier.fillMaxSize()) {
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
                        // Calendar settings remain available when the user reveals the navigation chrome.
                        if (destination != Destination.CLOCK && (destination != Destination.CALENDAR || chromeVisible)) {
                            TopAppBar(
                                title = { Text(stringResource(destination.labelRes)) },
                                actions = {
                                    IconButton(onClick = if (destination == Destination.CALENDAR) onOpenCalendarSettings else onOpenSettings) {
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
                            modifier = Modifier.padding(if (immersive) PaddingValues(0.dp) else padding),
                            refreshGeneration = refreshGeneration,
                            onOpenSettings = onOpenSettings,
                            onToggleChrome = { chromeVisible = !chromeVisible },
                        )
                        Destination.CALENDAR ->
                            CalendarScreen(
                                Modifier.padding(if (immersive) PaddingValues(0.dp) else padding),
                                refreshGeneration,
                                onToggleChrome = { chromeVisible = !chromeVisible },
                            )
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
        ChimeHost(refreshGeneration)
    }
}

/** Activity-wide cue layer. Its fast ticker only recomposes this overlay. */
@Composable
private fun ChimeHost(refreshGeneration: Int) {
    val context = LocalContext.current
    val repository = remember(context) { BackgroundRepository(context) }
    val styleId = remember(context, refreshGeneration) {
        UltimateClockPreferences(context).getStyleId()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resumed) {
        while (resumed) {
            tick = System.currentTimeMillis()
            delay(1_000L - tick % 1_000L)
        }
    }
    val calendar = remember(tick, refreshGeneration) {
        Calendar.getInstance(HourlyChimeController.resolveTimeZone(repository.getTimeZoneId()))
            .apply { timeInMillis = tick }
    }
    ChimeIndicator(repository, calendar, resumed, styleId)
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
