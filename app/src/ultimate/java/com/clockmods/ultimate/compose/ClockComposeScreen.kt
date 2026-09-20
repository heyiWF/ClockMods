package com.clockmods.ultimate.compose

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.BackgroundDimSchedule
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ClockPreferences
import com.clockmods.calendar.LunarCalendar
import com.clockmods.pro.chime.HourlyChimeController
import com.clockmods.sdk.clock.ClockBackground
import com.clockmods.sdk.clock.ClockOverlayBounds
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.sdk.clock.WorldClockEntry
import com.clockmods.time.NetworkTimeProvider
import com.clockmods.ui.DateFormatter
import com.clockmods.ui.ClockTimeFormatter
import com.clockmods.ui.WeatherIcon
import com.clockmods.ultimate.clock.ClockPalette
import com.clockmods.ultimate.clock.ClockMotionResolver
import com.clockmods.ultimate.clock.ClockTypography
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.ultimate.clock.WorldClockRepository
import com.clockmods.weather.WeatherController
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherTemperatureFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.hypot

private const val WEATHER_DETAIL_HOLD_MILLIS = 3_000L
private const val QWEATHER_WEBSITE = "https://www.qweather.com"

@Composable
internal fun ClockScreen(
    modifier: Modifier,
    refreshGeneration: Int,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { BackgroundRepository(context) }
    val appearance = remember(context, refreshGeneration) { UltimateClockPreferences(context) }
    val networkTime = remember { NetworkTimeProvider() }
    val worldClockRepository = remember(context) { WorldClockRepository(context) }
    val styleId = appearance.getStyleId()
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(Unit) { onDispose(networkTime::shutdown) }
    LaunchedEffect(refreshGeneration) {
        networkTime.setEnabled(repository.isUseNetworkTime())
        networkTime.setSyncIntervalMinutes(repository.getSyncIntervalMinutes())
        while (true) {
            tick = networkTime.currentTimeMillis()
            kotlinx.coroutines.delay(250L)
        }
    }

    val locale = remember(refreshGeneration) {
        when (repository.getClockLanguage()) {
            ClockPreferences.LANGUAGE_ENGLISH -> Locale.ENGLISH
            ClockPreferences.LANGUAGE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            else -> Locale.SIMPLIFIED_CHINESE
        }
    }
    val zone = remember(refreshGeneration) {
        repository.getTimeZoneId().takeIf(String::isNotBlank)?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
    }
    val calendar = remember(tick, zone, locale) {
        Calendar.getInstance(zone, locale).apply { timeInMillis = tick }
    }
    val datePattern = if (repository.isClockUseEnglish()) {
        repository.getDatePatternEn()
    } else {
        repository.getDatePatternCn()
    }
    val solarDate = DateFormatter.format(
        datePattern,
        calendar,
        LocaleManager.dateLang(repository.getClockLanguage()),
    )
    val dateText = if (!repository.isShowLunar()) solarDate else {
        LunarCalendar.format(calendar).takeIf(String::isNotBlank)?.let { "$solarDate / $it" }
            ?: solarDate
    }
    val weatherState = rememberWeatherState(repository, refreshGeneration)
    val weatherDetailLabels = WeatherModels.WeatherDetail.DetailLabels(
        stringResource(R.string.weather_feels_format),
        stringResource(R.string.weather_humidity_format),
        stringResource(R.string.weather_wind_scale_format),
        stringResource(R.string.weather_precip_format),
        stringResource(R.string.weather_air_format),
        stringResource(R.string.weather_warning_suffix),
    )
    val weatherText = formatWeatherState(
        weatherState,
        repository.getWeatherTemperatureUnit(),
        repository.isWeatherDetailed(),
        weatherDetailLabels,
        tick,
    )
    val supportingText = listOf(weatherText, repository.getCustomMessage())
        .filter(String::isNotBlank)
        .joinToString(" / ")
    val deviceStatus = rememberDeviceStatus()
    val showStatusIcons = repository.isShowStatusIcons()
    val statusText = if (showStatusIcons) {
        buildString {
            append(if (deviceStatus.connected) "Network" else "Offline")
            if (deviceStatus.batteryPercent >= 0) append(" · ${deviceStatus.batteryPercent}%")
        }
    } else ""
    val worldClocks = remember(refreshGeneration) {
        if (worldClockRepository.isEnabled()) worldClockRepository.getSelected() else emptyList()
    }
    var statusOverlay by remember { mutableStateOf<ClockOverlayBounds?>(null) }
    val density = LocalDensity.current
    val bottomOverlayInset = with(density) { 36.dp.toPx() }
    LaunchedEffect(showStatusIcons) {
        if (!showStatusIcons) statusOverlay = null
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(onOpenSettings) {
                detectTapGestures(onDoubleTap = { onOpenSettings() })
            },
    ) {
        ClockCanvas(
            modifier = Modifier.fillMaxSize(),
            context = context,
            timeMillis = tick,
            styleId = appearance.getStyleId(),
            showSeconds = repository.isShowSeconds(),
            use24Hour = repository.isUse24Hour(),
            locale = locale,
            timeZone = zone,
            dateText = dateText,
            weatherText = supportingText,
            statusText = statusText,
            worldClocks = worldClocks,
            repository = repository,
            appearance = appearance,
            refreshGeneration = refreshGeneration,
            bottomOverlayInset = bottomOverlayInset,
            statusOverlay = statusOverlay,
        )
        if (showStatusIcons) {
            DeviceStatusPill(
                status = deviceStatus,
                scale = repository.getStatusIconScale(),
                transparent = styleId == UltimateClockStyles.STYLE_PRO_CLASSIC,
                contentColor = Color(repository.getTimeColor()),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInParent()
                        statusOverlay = ClockOverlayBounds(
                            bounds.left, bounds.top, bounds.right, bounds.bottom,
                        )
                    },
            )
        }
        if (repository.isWeatherEnabled()) {
            WeatherAttribution(
                state = weatherState,
                fill = repository.isWeatherIconFill(),
                dynamicColor = repository.isWeatherIconDynamicColor(),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
        Text(
            text = stringResource(R.string.open_settings_accessibility),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).alpha(.42f),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 10.sp,
        )
        ChimeIndicator(repository = repository, calendar = calendar)
    }
}

@Composable
private fun ClockCanvas(
    modifier: Modifier,
    context: Context,
    timeMillis: Long,
    styleId: String,
    showSeconds: Boolean,
    use24Hour: Boolean,
    locale: Locale,
    timeZone: TimeZone,
    dateText: String,
    weatherText: String,
    statusText: String,
    worldClocks: List<WorldClockEntry>,
    repository: BackgroundRepository,
    appearance: UltimateClockPreferences,
    refreshGeneration: Int,
    bottomOverlayInset: Float,
    statusOverlay: ClockOverlayBounds?,
) {
    val registry = remember { UltimateClockStyles.sharedRegistry() }
    val style = remember(styleId) {
        registry.resolveForApi(styleId, android.os.Build.VERSION.SDK_INT)
    }
    val baseTheme = remember(styleId) { style.getThemeTokens() }
    val paletteTheme = remember(styleId, refreshGeneration) {
        if (ClockPalette.supports(styleId)) appearance.getPalette(styleId).applyTo(baseTheme)
        else baseTheme
    }
    val preferenceTheme = remember(styleId, refreshGeneration, paletteTheme) {
        applyProClassicPreferenceColors(
            styleId,
            paletteTheme,
            repository.getTimeColor(),
            repository.getDateColor(),
        )
    }
    val theme = remember(styleId, refreshGeneration, preferenceTheme) {
        ClockTypography().apply(context, preferenceTheme, styleId)
    }
    val density = LocalDensity.current
    val backgroundMode = appearance.getBackgroundMode()
    var backgroundBitmap by remember(backgroundMode, refreshGeneration) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(backgroundMode, refreshGeneration) {
        backgroundBitmap = if (backgroundMode == UltimateClockPreferences.BACKGROUND_MODE_IMAGE) {
            val metrics = context.resources.displayMetrics
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { repository.loadImage(metrics.widthPixels, metrics.heightPixels) }
                    .getOrNull()
            }
        } else null
    }
    val dimmed = shouldDimBackground(repository, timeMillis, timeZone, locale)
    val clockBackground = when (backgroundMode) {
        UltimateClockPreferences.BACKGROUND_MODE_IMAGE ->
            ClockBackground.image(backgroundBitmap, repository.getCurrentColor(), dimmed)
        UltimateClockPreferences.BACKGROUND_MODE_COLOR ->
            ClockBackground.color(repository.getCurrentColor(), dimmed)
        else -> ClockBackground.theme(dimmed)
    }
    val effectiveMotion = ClockMotionResolver.resolve(
        style,
        showSeconds,
        appearance.getSecondHandMotion(),
    )
    val worldClockSupported = style.getMetadata().getCapabilities().supports(
        ClockStyleCapabilities.Capability.WORLD_CLOCK,
    )
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var worldClockScroll by remember(styleId, worldClocks) { mutableFloatStateOf(0f) }
    val scrollMaximum = if (worldClockSupported && worldClocks.isNotEmpty() &&
        canvasSize.width > 0 && canvasSize.height > 0
    ) {
        val width = canvasSize.width.toFloat()
        val height = canvasSize.height.toFloat()
        val strip = UltimateClockStyles.worldClockStripBounds(
            0f, 0f, width, height, density.density, bottomOverlayInset,
        )
        val faceHeight = strip.top - minOf(density.density * 8f, height * .025f)
        val inset = UltimateClockStyles.worldClockContentInset(
            styleId, width, faceHeight, density.density,
        )
        (UltimateClockStyles.worldClockContentWidth(
            worldClocks.size, width, height, density.density,
        ) + inset * 2f - strip.width()).coerceAtLeast(0f)
    } else 0f
    LaunchedEffect(scrollMaximum) {
        worldClockScroll = worldClockScroll.coerceIn(0f, scrollMaximum)
    }

    val transitionProgress = remember { Animatable(1f) }
    val transitionKey = if (showSeconds) timeMillis / 1_000L else timeMillis / 60_000L
    val animateTime = styleId == UltimateClockStyles.STYLE_PRO_CLASSIC &&
        repository.isAnimateTimeChanges()
    LaunchedEffect(transitionKey, animateTime, refreshGeneration) {
        if (animateTime) {
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(1f, tween(durationMillis = 220))
        } else {
            transitionProgress.snapTo(1f)
        }
    }
    val transitionType = repository.getTimeTransition()
    val canvasModifier = modifier
        .onSizeChanged { canvasSize = it }
        .pointerInput(worldClocks, scrollMaximum, bottomOverlayInset) {
            var draggingStrip = false
            detectHorizontalDragGestures(
                onDragStart = { position ->
                    val strip = UltimateClockStyles.worldClockStripBounds(
                        0f, 0f, size.width.toFloat(), size.height.toFloat(),
                        density.density, bottomOverlayInset,
                    )
                    draggingStrip = worldClockSupported && scrollMaximum > 0f &&
                        position.y in strip.top..strip.bottom
                },
                onDragCancel = { draggingStrip = false },
                onDragEnd = { draggingStrip = false },
                onHorizontalDrag = { change, amount ->
                    if (draggingStrip) {
                        change.consume()
                        worldClockScroll = (worldClockScroll - amount).coerceIn(0f, scrollMaximum)
                    }
                },
            )
        }

    Canvas(canvasModifier) {
        val state = ClockState.builder(timeMillis)
            .timeZone(timeZone)
            .locale(locale)
            .use24Hour(use24Hour)
            .showSeconds(showSeconds)
            .blinkColon(repository.isBlinkColon())
            .smallSeconds(repository.isSmallSeconds())
            .portraitStacked(repository.isPortraitStacked())
            .dateLunarDualLine(repository.isDateLunarDualLine())
            .secondHandMotion(effectiveMotion)
            .dateText(dateText)
            .timeZoneText(
                timeZone.getDisplayName(
                    timeZone.inDaylightTime(Date(timeMillis)),
                    TimeZone.SHORT,
                    locale,
                ),
            )
            .weatherText(weatherText)
            .statusText(statusText)
            .worldClocks(worldClocks)
            .timeScale(timeFontScaleForStyle(repository, styleId))
            .dateScale(dateFontScaleForStyle(repository, styleId))
            .supportingScale(repository.getSupportingFontScale(styleId))
            .timeTransition(clockTimeTransition(transitionType))
            .timeTransitionProgress(if (animateTime) transitionProgress.value else 1f)
            .build()
        val renderContext = ClockRenderContext(
            0f,
            0f,
            size.width,
            size.height,
            density.density,
            density.fontScale * density.density,
            timeMillis,
            clockBackground,
            bottomOverlayInset,
            worldClockScroll,
            false,
            statusOverlay,
        )
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val save = canvas.save()
            try {
                style.getRenderer().render(canvas, renderContext, state, theme)
            } finally {
                canvas.restoreToCount(save)
            }
        }
    }
}

internal fun clockTimeTransition(value: String?): ClockState.TimeTransition = when (value) {
    ClockPreferences.TRANSITION_SLIDE_UP -> ClockState.TimeTransition.SLIDE_UP
    ClockPreferences.TRANSITION_SLIDE_DOWN -> ClockState.TimeTransition.SLIDE_DOWN
    ClockPreferences.TRANSITION_SCALE -> ClockState.TimeTransition.SCALE
    ClockPreferences.TRANSITION_FLIP -> ClockState.TimeTransition.FLIP
    else -> ClockState.TimeTransition.FADE
}

internal fun applyProClassicPreferenceColors(
    styleId: String,
    theme: ClockThemeTokens,
    timeColor: Int,
    dateColor: Int,
): ClockThemeTokens {
    if (styleId != UltimateClockStyles.STYLE_PRO_CLASSIC) return theme
    return theme.toBuilder()
        .primaryTextColor(timeColor)
        .secondaryTextColor(dateColor)
        .build()
}

private fun timeFontScaleForStyle(repository: BackgroundRepository, styleId: String): Float {
    val value = if (styleId == UltimateClockStyles.STYLE_PRO_CLASSIC) {
        repository.getTimeFontScale()
    } else {
        repository.getTimeFontScale(styleId)
    }
    return value / ClockPreferences.DEFAULT_TIME_FONT_SCALE
}

private fun dateFontScaleForStyle(repository: BackgroundRepository, styleId: String): Float {
    val value = if (styleId == UltimateClockStyles.STYLE_PRO_CLASSIC) {
        repository.getDateFontScale()
    } else {
        repository.getDateFontScale(styleId)
    }
    return value / ClockPreferences.DEFAULT_DATE_FONT_SCALE
}

private fun shouldDimBackground(
    repository: BackgroundRepository,
    now: Long,
    timeZone: TimeZone,
    locale: Locale,
): Boolean {
    if (repository.isDimBackground()) return true
    if (!repository.isScheduleDimBackground()) return false
    val calendar = Calendar.getInstance(timeZone, locale).apply { timeInMillis = now }
    return BackgroundDimSchedule.isActive(
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
        repository.getDimStartMinutes(),
        repository.getDimEndMinutes(),
    )
}

@Composable
private fun rememberWeatherState(
    repository: BackgroundRepository,
    refreshGeneration: Int,
): WeatherModels.WeatherState? {
    val context = LocalContext.current
    var state by remember { mutableStateOf<WeatherModels.WeatherState?>(null) }
    var permissionGeneration by remember { mutableIntStateOf(0) }
    val controller = remember(context) {
        WeatherController(
            context,
            object : WeatherController.Listener {
                override fun onWeatherState(newState: WeatherModels.WeatherState) {
                    state = newState
                }
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionGeneration++
        controller.refreshNow()
    }
    val enabled = repository.isWeatherEnabled()
    val automatic = repository.getWeatherLocationMode() != ClockPreferences.WEATHER_LOCATION_MANUAL
    LaunchedEffect(enabled, automatic, refreshGeneration) {
        if (enabled && automatic &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }
    DisposableEffect(enabled, refreshGeneration, permissionGeneration) {
        if (enabled) controller.start(repository.getWeatherIntervalMinutes()) else {
            controller.stop()
            state = null
        }
        onDispose(controller::stop)
    }
    DisposableEffect(Unit) { onDispose(controller::shutdown) }
    return state
}

internal fun formatWeatherState(
    state: WeatherModels.WeatherState?,
    temperatureUnit: String,
    detailed: Boolean,
    labels: WeatherModels.WeatherDetail.DetailLabels,
    nowMillis: Long,
): String {
    state ?: return ""
    val data = state.data ?: return state.message.orEmpty()
    val summary = buildList {
        WeatherModels.locationText(data.city, data.district).takeIf(String::isNotBlank)?.let(::add)
        WeatherTemperatureFormatter.format(data.temperature, temperatureUnit)
            .takeIf(String::isNotBlank)?.let(::add)
        data.text?.takeIf(String::isNotBlank)?.let(::add)
    }.joinToString(" ")
    if (!detailed) return summary
    val carousel = buildList {
        summary.takeIf(String::isNotBlank)?.let(::add)
        addAll(data.detail?.carouselItems(labels, temperatureUnit).orEmpty())
    }
    if (carousel.isEmpty()) return summary
    val index = ((nowMillis.coerceAtLeast(0L) / WEATHER_DETAIL_HOLD_MILLIS) % carousel.size).toInt()
    return carousel[index]
}

@Composable
private fun WeatherAttribution(
    state: WeatherModels.WeatherState?,
    fill: Boolean,
    dynamicColor: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconCode = state?.data?.icon
    val icon = remember(context, iconCode, fill) { WeatherIcon.load(context, iconCode, fill) }
    val attribution = stringResource(R.string.weather_attribution)
    val iconColor = if (dynamicColor) MaterialTheme.colorScheme.primary else Color.White
    Row(
        modifier.clickable {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(QWEATHER_WEBSITE)),
                )
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Canvas(Modifier.size(16.dp)) {
                drawIntoCanvas { target ->
                    icon.draw(
                        target.nativeCanvas,
                        0f,
                        0f,
                        size.minDimension,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iconColor.toArgb() },
                    )
                }
            }
        }
        Text(
            text = attribution,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            fontSize = 10.sp,
        )
    }
}

private data class DeviceStatus(val connected: Boolean, val batteryPercent: Int)

@Composable
private fun rememberDeviceStatus(): DeviceStatus {
    val context = LocalContext.current
    fun readStatus(batteryIntent: Intent? = null): DeviceStatus {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork)
        val connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val battery = batteryIntent
            ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        return DeviceStatus(connected, percent)
    }
    var status by remember { mutableStateOf(readStatus()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiveContext: Context?, intent: Intent?) {
                status = readStatus(intent.takeIf { it?.action == Intent.ACTION_BATTERY_CHANGED })
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return status
}

@Composable
private fun DeviceStatusPill(
    status: DeviceStatus,
    scale: Float,
    transparent: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val normalizedScale = ClockPreferences.normalizeStatusIconScale(scale)
    Card(
        modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (transparent) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = .82f)
            },
            contentColor = if (transparent) contentColor else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (transparent) 0.dp else 1.dp),
    ) {
        Row(
            Modifier.padding(
                horizontal = 10.dp * normalizedScale,
                vertical = 7.dp * normalizedScale,
            ),
            horizontalArrangement = Arrangement.spacedBy(7.dp * normalizedScale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (status.connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp * normalizedScale),
            )
            if (status.batteryPercent >= 0) {
                Icon(
                    Icons.Default.BatteryFull,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp * normalizedScale),
                )
                Text(
                    "${status.batteryPercent}%",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize * normalizedScale,
                )
            }
        }
    }
}

@Composable
private fun ChimeIndicator(repository: BackgroundRepository, calendar: Calendar) {
    val candidateChimeAt = HourlyChimeController.upcomingChimeAtMillis(
        calendar,
        repository.isHourlyChimeEnabled(),
        repository.isHalfHourChimeEnabled(),
    )
    var chimeAtMillis by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val progress = remember { Animatable(1f) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(candidateChimeAt) {
        if (candidateChimeAt != Long.MIN_VALUE && candidateChimeAt != chimeAtMillis) {
            chimeAtMillis = candidateChimeAt
        }
    }
    LaunchedEffect(chimeAtMillis) {
        if (chimeAtMillis == Long.MIN_VALUE) return@LaunchedEffect
        val chimeAt = Calendar.getInstance(calendar.timeZone).apply {
            timeInMillis = chimeAtMillis
        }
        val targetMinute = chimeAt.get(Calendar.HOUR_OF_DAY) * 60 + chimeAt.get(Calendar.MINUTE)
        val quiet = repository.isHourlyChimeQuietEnabled() && HourlyChimeController.isQuietAtMinute(
            targetMinute,
            repository.getHourlyChimeQuietStart(),
            repository.getHourlyChimeQuietEnd(),
        )
        if (quiet) return@LaunchedEffect
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 5_000))
        visible = false
    }
    if (!visible) return

    val expansion = easeOutQuint((progress.value / 0.52f).coerceIn(0f, 1f))
    val textProgress = smoothStep(0.28f, 0.48f, progress.value)
    val opacity = 1f - smoothStep(0.82f, 1f, progress.value)
    val chimeAt = Calendar.getInstance(calendar.timeZone).apply { timeInMillis = chimeAtMillis }
    val text = ClockTimeFormatter.formatHourlyChime(
        chimeAt.get(Calendar.HOUR_OF_DAY),
        chimeAt.get(Calendar.MINUTE),
        repository.isUse24Hour(),
        repository.isClockUseEnglish(),
    )
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(chimeAtMillis) {
                detectTapGestures(onTap = { visible = false })
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFF4C430).copy(alpha = opacity),
                radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f * expansion,
                center = center,
            )
        }
        Text(
            text = text,
            modifier = Modifier.graphicsLayer {
                alpha = textProgress * opacity
                val scale = 0.94f + 0.06f * easeOutQuint(textProgress)
                scaleX = scale
                scaleY = scale
            },
            color = Color.Black,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
    }
}

private fun smoothStep(start: Float, end: Float, value: Float): Float {
    val normalized = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}

private fun easeOutQuint(value: Float): Float {
    val remaining = 1f - value
    return 1f - remaining * remaining * remaining * remaining * remaining
}
