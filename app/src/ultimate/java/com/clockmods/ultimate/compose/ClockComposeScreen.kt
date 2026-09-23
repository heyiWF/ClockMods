package com.clockmods.ultimate.compose

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import com.clockmods.ultimate.clock.ClockPalette
import com.clockmods.ultimate.clock.ClockMotionResolver
import com.clockmods.ultimate.clock.ClockTypography
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.ultimate.clock.ClockDigitTransitionTracker
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

/** Preview geometry: a fixed mid-morning instant with the seconds hand parked at 30. */
private const val PREVIEW_HOUR = 10
private const val PREVIEW_MINUTE = 9
private const val PREVIEW_SECOND = 30

/** Ink/theme for a live preview: palette applied to the style's tokens, then the host typography. */
@Composable
internal fun previewClockTheme(
    context: Context,
    styleId: String,
    palette: ClockPalette,
    repository: BackgroundRepository,
): ClockThemeTokens {
    val style = remember(styleId) {
        UltimateClockStyles.sharedRegistry().resolveForApi(styleId, android.os.Build.VERSION.SDK_INT)
    }
    val timeColor = repository.getTimeColor()
    val dateColor = repository.getDateColor()
    return remember(context, styleId, palette, timeColor, dateColor) {
        val tokens = style.getThemeTokens()
        val themed = if (ClockPalette.supports(styleId)) palette.applyTo(tokens) else tokens
        ClockTypography().apply(context,
            applyProClassicPreferenceColors(styleId, themed, timeColor, dateColor), styleId)
    }
}

/**
 * Resolves the surface the preview clock is painted on from the user's real background preferences,
 * so what the settings card shows matches the face: a parked image, a custom colour, or the theme
 * gradient, with the same dimming schedule applied. This is also what lets [GaussianGlass] engage —
 * it only blurs when the background actually carries an image.
 */
@Composable
internal fun previewClockBackground(
    repository: BackgroundRepository,
    appearance: UltimateClockPreferences,
    refreshGeneration: Int,
): ClockBackground {
    val context = LocalContext.current
    val backgroundMode = remember(appearance, refreshGeneration) { appearance.getBackgroundMode() }
    var bitmap by remember(backgroundMode, refreshGeneration) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(backgroundMode, refreshGeneration) {
        bitmap = if (backgroundMode == UltimateClockPreferences.BACKGROUND_MODE_IMAGE) {
            val metrics = context.resources.displayMetrics
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { repository.loadImage(metrics.widthPixels, metrics.heightPixels) }
                    .getOrNull()
            }
        } else null
    }
    val dimmed = remember(repository, refreshGeneration) {
        shouldDimBackground(repository, System.currentTimeMillis(), TimeZone.getDefault(), Locale.getDefault())
    }
    return when (backgroundMode) {
        UltimateClockPreferences.BACKGROUND_MODE_IMAGE ->
            ClockBackground.image(bitmap, repository.getCurrentColor(), dimmed)
        UltimateClockPreferences.BACKGROUND_MODE_COLOR ->
            ClockBackground.color(repository.getCurrentColor(), dimmed)
        else -> ClockBackground.theme(dimmed)
    }
}

/**
 * Draws [styleId] straight onto the supplied canvas with a frozen clock, so the settings surface
 * can show the theme reacting to a palette change while the user is still editing it. The clock is
 * painted over the user's real [background] (image included) and the style's own renderer is used,
 * so card gaussian blur behaves exactly as it does on the face.
 *
 * The face is composed at [hostWidth] x [hostHeight] — the real screen geometry — and then scaled
 * into the destination box. That is what keeps the preview honest: the image-anchored blur samples
 * the same pixels, the cards land in the same places, and the derived light/dark ink is identical
 * to the running clock instead of drifting to whatever region a tiny preview box would cover.
 */
internal fun renderClockPreview(
    canvas: android.graphics.Canvas,
    styleId: String,
    theme: ClockThemeTokens,
    background: ClockBackground,
    repository: BackgroundRepository,
    density: Float,
    timeZone: TimeZone,
    locale: Locale,
    hostWidth: Float,
    hostHeight: Float,
    fitToBounds: Boolean = false,
) {
    val area = canvas.clipBounds
    val width = area.width().toFloat()
    val height = area.height().toFloat()
    if (width <= 0f || height <= 0f) return
    val hostW = if (hostWidth > 0f) hostWidth else width
    val hostH = if (hostHeight > 0f) hostHeight else height
    val scale = if (fitToBounds) minOf(width / hostW, height / hostH)
        else maxOf(width / hostW, height / hostH)
    val translateX = area.left + (width - hostW * scale) * .5f
    val translateY = area.top + (height - hostH * scale) * .5f
    val style = UltimateClockStyles.sharedRegistry()
        .resolveForApi(styleId, android.os.Build.VERSION.SDK_INT)
    val calendar = Calendar.getInstance(timeZone, locale).apply {
        set(Calendar.HOUR_OF_DAY, PREVIEW_HOUR)
        set(Calendar.MINUTE, PREVIEW_MINUTE)
        set(Calendar.SECOND, PREVIEW_SECOND)
        set(Calendar.MILLISECOND, 0)
    }
    val now = calendar.timeInMillis
    val showSeconds = repository.isShowSeconds()
    val motion = ClockMotionResolver.resolve(style, showSeconds, ClockState.SecondHandMotion.TICK)
    val datePattern = if (repository.isClockUseEnglish()) repository.getDatePatternEn()
        else repository.getDatePatternCn()
    val solarDate = DateFormatter.format(
        datePattern, calendar, LocaleManager.dateLang(repository.getClockLanguage()),
    )
    val dateText = if (repository.isShowLunar()) {
        LunarCalendar.format(calendar).takeIf(String::isNotBlank)?.let { "$solarDate / $it" }
            ?: solarDate
    } else solarDate
    val state = ClockState.builder(now)
        .timeZone(timeZone)
        .locale(locale)
        .use24Hour(repository.isUse24Hour())
        .showSeconds(showSeconds)
        .blinkColon(repository.isBlinkColon())
        .smallSeconds(repository.isSmallSeconds())
        .portraitStacked(repository.isPortraitStacked())
        .dateLunarDualLine(repository.isDateLunarDualLine())
        .secondHandMotion(motion)
        .dateText(dateText)
        .timeZoneText(timeZone.getDisplayName(timeZone.inDaylightTime(Date(now)), TimeZone.SHORT, locale))
        .weatherText("24°")
        .worldClocks(emptyList())
        .timeScale(timeFontScaleForStyle(repository, styleId))
        .dateScale(dateFontScaleForStyle(repository, styleId))
        .supportingScale(repository.getSupportingFontScale(styleId))
        .build()
    // Compose at the host geometry so blur samples the same image region and cards keep their real
    // proportions; density is scaled so text stays proportional inside the shrunk face.
    val renderContext = ClockRenderContext(
        0f,
        0f,
        hostW,
        hostH,
        density,
        density,
        now,
        background,
        0f,
        0f,
        false,
        null,
    )
    val save = canvas.save()
    try {
        if (fitToBounds) {
            canvas.drawColor(if (background.usesThemeSurface()) theme.getBackgroundStartColor()
                else background.getColor())
        }
        canvas.translate(translateX, translateY)
        canvas.scale(scale, scale)
        style.getRenderer().render(canvas, renderContext, state, theme)
    } finally {
        canvas.restoreToCount(save)
    }
}

@Composable
internal fun ClockScreen(
    modifier: Modifier,
    refreshGeneration: Int,
    onOpenSettings: () -> Unit,
    onToggleChrome: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { BackgroundRepository(context) }
    val appearance = remember(context, refreshGeneration) { UltimateClockPreferences(context) }
    val networkTime = remember { NetworkTimeProvider() }
    val worldClockRepository = remember(context) { WorldClockRepository(context) }
    val styleId = appearance.getStyleId()
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // A sweep second hand is interpolated by millisecond, so the canvas redraws it on every
    // display frame and reads the clock directly rather than waiting for the 250 ms data tick
    // that drives the text overlays.
    val timeSource = remember { { networkTime.currentTimeMillis() } }

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
    var overlaySurface by remember { mutableStateOf(Color.Black) }
    val worldClocks = remember(refreshGeneration) {
        if (worldClockRepository.isEnabled()) worldClockRepository.getSelected() else emptyList()
    }
    var statusOverlay by remember { mutableStateOf<ClockOverlayBounds?>(null) }
    var faceSize by remember { mutableStateOf(IntSize.Zero) }
    var statusPillSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val bottomOverlayInset = with(density) { 36.dp.toPx() }
    LaunchedEffect(showStatusIcons) {
        if (!showStatusIcons) statusOverlay = null
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { faceSize = it }
            .pointerInput(onOpenSettings, onToggleChrome) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = { onOpenSettings() },
                )
            },
    ) {
        ClockCanvas(
            modifier = Modifier.fillMaxSize(),
            context = context,
            timeMillis = tick,
            timeSource = timeSource,
            styleId = appearance.getStyleId(),
            showSeconds = repository.isShowSeconds(),
            use24Hour = repository.isUse24Hour(),
            locale = locale,
            timeZone = zone,
            dateText = dateText,
            weatherText = supportingText,
            worldClocks = worldClocks,
            repository = repository,
            appearance = appearance,
            refreshGeneration = refreshGeneration,
            bottomOverlayInset = bottomOverlayInset,
            statusOverlay = statusOverlay,
            onOverlaySurface = { overlaySurface = it },
        )
        if (showStatusIcons) {
            // The pill is positioned from the same geometry the renderers use for their own
            // top-right metadata, so every theme keeps the two aligned instead of guessing.
            val faceWidth = faceSize.width.toFloat()
            val faceHeight = faceSize.height.toFloat()
            val placement = remember(styleId, faceWidth, faceHeight, statusPillSize, density.density) {
                if (faceWidth <= 0f || faceHeight <= 0f ||
                    statusPillSize.width <= 0 || statusPillSize.height <= 0
                ) {
                    null
                } else {
                    UltimateClockStyles.statusCapsuleBounds(
                        styleId,
                        faceWidth,
                        faceHeight,
                        density.density,
                        faceWidth * if (faceWidth >= faceHeight) .029f else .055f,
                        faceHeight * .037f,
                        statusPillSize.width.toFloat(),
                        statusPillSize.height.toFloat(),
                    )
                }
            }
            DeviceStatusPill(
                status = deviceStatus,
                scale = repository.getStatusIconScale(),
                transparent = styleId == UltimateClockStyles.STYLE_PRO_CLASSIC,
                contentColor = Color(repository.getTimeColor()),
                faceColor = overlaySurface,
                modifier = Modifier
                    .onSizeChanged { statusPillSize = it }
                    .then(
                        if (placement == null) {
                            Modifier
                        } else {
                            Modifier.offset {
                                IntOffset(placement[0].toInt(), placement[1].toInt())
                            }
                        },
                    )
                    .alpha(if (placement == null) 0f else 1f)
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
                faceColor = overlaySurface,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
        ChimeIndicator(repository = repository, calendar = calendar)
    }
}

/**
 * Live theme thumbnail for the settings surface. It renders through the very same
 * [ClockRenderer] the face uses, over the user's real background, so a palette edit is visible here
 * the moment it is made — no round trip into the clock and back. The aspect ratio follows the host
 * bounds, so the preview keeps whatever orientation the clock is configured for.
 */
@Composable
internal fun ClockPreviewCanvas(
    styleId: String,
    palette: ClockPalette,
    background: ClockBackground,
    repository: BackgroundRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hostSize = LocalWindowInfo.current.containerSize
    val locale = remember { Locale.SIMPLIFIED_CHINESE }
    val timeZone = remember { TimeZone.getDefault() }
    val theme = previewClockTheme(context, styleId, palette, repository)
    Canvas(modifier) {
        val canvas = drawContext.canvas.nativeCanvas
        renderClockPreview(
            canvas = canvas,
            styleId = styleId,
            theme = theme,
            background = background,
            repository = repository,
            density = density.density,
            timeZone = timeZone,
            locale = locale,
            hostWidth = hostSize.width.toFloat(),
            hostHeight = hostSize.height.toFloat(),
        )
    }
}

/**
 * A small self-contained clock face used by the style gallery tiles.
 *
 * The gallery uses the same resolved surface as the running clock. The caller shares one loaded
 * background across all thumbnails, so image mode does not load the wallpaper once per style.
 * The real host geometry keeps each miniature's proportions aligned with the clock face.
 */
@Composable
internal fun ClockStyleThumbnail(
    styleId: String,
    palette: ClockPalette,
    background: ClockBackground,
    repository: BackgroundRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hostSize = LocalWindowInfo.current.containerSize
    val locale = remember { Locale.SIMPLIFIED_CHINESE }
    val timeZone = remember { TimeZone.getDefault() }
    val theme = previewClockTheme(context, styleId, palette, repository)
    Canvas(modifier) {
        renderClockPreview(
            canvas = drawContext.canvas.nativeCanvas,
            styleId = styleId,
            theme = theme,
            background = background,
            repository = repository,
            density = density.density,
            timeZone = timeZone,
            locale = locale,
            hostWidth = hostSize.width.toFloat(),
            hostHeight = hostSize.height.toFloat(),
            fitToBounds = true,
        )
    }
}

@Composable
private fun ClockCanvas(
    modifier: Modifier,
    context: Context,
    timeMillis: Long,
    timeSource: () -> Long,
    styleId: String,
    showSeconds: Boolean,
    use24Hour: Boolean,
    locale: Locale,
    timeZone: TimeZone,
    dateText: String,
    weatherText: String,
    worldClocks: List<WorldClockEntry>,
    repository: BackgroundRepository,
    appearance: UltimateClockPreferences,
    refreshGeneration: Int,
    bottomOverlayInset: Float,
    statusOverlay: ClockOverlayBounds?,
    onOverlaySurface: (Color) -> Unit,
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
    // Host overlays sit on whatever the face is painted with, so they sample the same surface
    // the renderer uses instead of assuming the host window scheme describes it.
    val overlaySurface = remember(
        styleId, refreshGeneration, backgroundMode, backgroundBitmap, dimmed, paletteTheme,
    ) {
        val image = backgroundBitmap
        val surface = when {
            image != null && clockBackground.hasImage() -> averageColor(image)
            !clockBackground.usesThemeSurface() -> clockBackground.getColor()
            ClockPalette.supports(styleId) -> paletteTheme.getBackgroundStartColor()
            else -> theme.getBackgroundStartColor()
        }
        Color(if (dimmed) ClockPalette.mix(surface, 0xFF000000.toInt(), .4f) else surface)
    }
    LaunchedEffect(overlaySurface) { onOverlaySurface(overlaySurface) }
    val effectiveMotion = ClockMotionResolver.resolve(
        style,
        showSeconds,
        appearance.getSecondHandMotion(),
    )
    // A sweep hand advances by milliseconds, so a 250 ms data tick would quantise it into four
    // visible steps per second. Give the sweep path its own frame clock; every other motion keeps
    // using the data tick, which leaves the canvas idle between ticks.
    val smoothSeconds = effectiveMotion == ClockState.SecondHandMotion.SWEEP
    var animatedTime by remember { mutableLongStateOf(timeMillis) }
    LaunchedEffect(smoothSeconds, refreshGeneration) {
        if (!smoothSeconds) return@LaunchedEffect
        while (true) {
            withFrameMillis { animatedTime = timeSource() }
        }
    }
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

    val transitionKey = if (showSeconds) timeMillis / 1_000L else timeMillis / 60_000L
    val animateTime = repository.isAnimateTimeChanges()
    val transitionProgress = remember(styleId, transitionKey, animateTime) {
        Animatable(if (animateTime) 0f else 1f)
    }
    val digitTracker = remember(
        styleId, showSeconds, repository.isSmallSeconds(), repository.isPortraitStacked(),
        canvasSize.width >= canvasSize.height, animateTime,
    ) { ClockDigitTransitionTracker() }
    LaunchedEffect(transitionProgress) {
        if (animateTime) transitionProgress.animateTo(1f, tween(durationMillis = 280))
    }
    // Capture the outgoing line during composition. Starting the animation later in an effect
    // briefly paints the new line at full opacity before snapping back to the old one.
    val committedWeatherText = remember(styleId) { arrayOf(weatherText) }
    val previousWeatherText = remember(styleId, weatherText) { committedWeatherText[0] }
    val weatherProgress = remember(styleId, weatherText) {
        Animatable(if (previousWeatherText.isNotEmpty() && previousWeatherText != weatherText) 0f else 1f)
    }
    SideEffect { committedWeatherText[0] = weatherText }
    LaunchedEffect(weatherProgress) {
        if (weatherProgress.value < 1f) {
            weatherProgress.animateTo(1f, tween(durationMillis = 300))
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
        // Reading the frame clock inside the draw scope keeps the per-frame sweep redrawing to the
        // draw phase instead of recomposing the whole screen sixty times a second.
        val now = if (smoothSeconds) animatedTime else timeMillis
        val state = ClockState.builder(now)
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
                    timeZone.inDaylightTime(Date(now)),
                    TimeZone.SHORT,
                    locale,
                ),
            )
            .weatherText(weatherText)
            .worldClocks(worldClocks)
            .timeScale(timeFontScaleForStyle(repository, styleId))
            .dateScale(dateFontScaleForStyle(repository, styleId))
            .supportingScale(repository.getSupportingFontScale(styleId))
            .timeTransition(clockTimeTransition(transitionType))
            .timeTransitionProgress(if (animateTime) transitionProgress.value else 1f)
            .previousWeatherText(previousWeatherText)
            .weatherTransitionProgress(weatherProgress.value)
            .build()
        val renderContext = ClockRenderContext(
            0f,
            0f,
            size.width,
            size.height,
            density.density,
            density.fontScale * density.density,
            now,
            clockBackground,
            bottomOverlayInset,
            worldClockScroll,
            false,
            statusOverlay,
        )
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val save = canvas.save()
            val paintPool = UltimateClockStyles.RendererBase.PAINT_POOL.get()
            val previousTracker = paintPool.digitTracker
            digitTracker.beginFrame()
            paintPool.digitTracker = if (animateTime) digitTracker else null
            try {
                style.getRenderer().render(canvas, renderContext, state, theme)
            } finally {
                paintPool.digitTracker = previousTracker
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

/** Coarse average of a bitmap, used to pick readable ink for host overlays drawn over it. */
private fun averageColor(bitmap: Bitmap): Int {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return 0xFF000000.toInt()
    val stepX = maxOf(1, width / 24)
    val stepY = maxOf(1, height / 24)
    var red = 0L
    var green = 0L
    var blue = 0L
    var samples = 0L
    var y = stepY / 2
    while (y < height) {
        var x = stepX / 2
        while (x < width) {
            val pixel = bitmap.getPixel(x, y)
            red += (pixel ushr 16) and 255
            green += (pixel ushr 8) and 255
            blue += pixel and 255
            samples++
            x += stepX
        }
        y += stepY
    }
    if (samples == 0L) return 0xFF000000.toInt()
    return 0xFF000000.toInt() or
        ((red / samples).toInt() shl 16) or
        ((green / samples).toInt() shl 8) or
        (blue / samples).toInt()
}

@Composable
internal fun rememberWeatherState(
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
internal fun WeatherAttribution(faceColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // The bundled QWeather logotype already spells out the brand, so it replaces the plain text
    // label and is scaled to the same optical height as the 10sp caption it used to sit on.
    val logoHeight = with(density) { 11.dp }
    val suffix = stringResource(R.string.weather_attribution_suffix)
    // The caption sits on the live face, so its ink follows whatever the face is painted with
    // instead of the host window scheme, which says nothing about a photo or a theme gradient.
    val captionColor = Color(ClockPalette.foreground(faceColor.toArgb())).copy(alpha = .72f)
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
        Text(
            text = stringResource(R.string.weather_attribution_prefix),
            color = captionColor,
            fontSize = 10.sp,
        )
        Image(
            painter = painterResource(R.drawable.qweather_logo),
            contentDescription = stringResource(R.string.weather_attribution),
            colorFilter = ColorFilter.tint(captionColor),
            modifier = Modifier.height(logoHeight),
        )
        if (suffix.isNotBlank()) {
            Text(
                text = suffix,
                color = captionColor,
                fontSize = 10.sp,
            )
        }
    }
}

internal data class DeviceStatus(val connected: Boolean, val batteryPercent: Int)

@Composable
internal fun rememberDeviceStatus(): DeviceStatus {
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
internal fun DeviceStatusPill(
    status: DeviceStatus,
    scale: Float,
    transparent: Boolean,
    contentColor: Color,
    faceColor: Color,
    modifier: Modifier = Modifier,
) {
    val normalizedScale = ClockPreferences.normalizeStatusIconScale(scale)
    // A shallow frosted pane over the face: a soft white wash plus a hairline highlight instead
    // of an opaque card, so the clock stays visible underneath and the ink stays readable.
    val face = faceColor.toArgb()
    val container = if (transparent) {
        Color.Transparent
    } else {
        Color(ClockPalette.mix(face, 0xFFF6F8FC.toInt(), .18f)).copy(alpha = .42f)
    }
    val ink = if (transparent) contentColor else Color(ClockPalette.foreground(face))
    val hairline = Color(ClockPalette.mix(face, 0xFFFFFFFF.toInt(), .38f)).copy(alpha = .38f)
    Row(
        modifier
            .clip(RoundedCornerShape(percent = 50))
            .then(
                if (transparent) Modifier else Modifier
                    .background(container)
                    .border(1.dp, hairline, RoundedCornerShape(percent = 50)),
            )
            .padding(
                horizontal = 10.dp * normalizedScale,
                vertical = 7.dp * normalizedScale,
            ),
        horizontalArrangement = Arrangement.spacedBy(7.dp * normalizedScale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (status.connected) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(18.dp * normalizedScale),
        )
        if (status.batteryPercent >= 0) {
            Icon(
                Icons.Default.BatteryFull,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(18.dp * normalizedScale),
            )
            Text(
                "${status.batteryPercent}%",
                color = ink,
                fontSize = MaterialTheme.typography.labelMedium.fontSize * normalizedScale,
            )
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
            // The chime washes the whole face in gold, so its ink is picked against that gold
            // rather than hard-coded black.
            color = Color(ClockPalette.foreground(0xFFF4C430.toInt())),
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
