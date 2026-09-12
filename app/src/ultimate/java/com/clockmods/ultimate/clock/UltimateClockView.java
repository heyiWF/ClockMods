package com.clockmods.ultimate.clock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import com.clockmods.R;
import com.clockmods.LocaleManager;
import com.clockmods.background.BackgroundDimSchedule;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;
import com.clockmods.calendar.LunarCalendar;
import com.clockmods.sdk.clock.ClockBackground;
import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleMetadata;
import com.clockmods.sdk.clock.ClockStyleRegistry;
import com.clockmods.sdk.clock.ClockThemeTokens;
import com.clockmods.sdk.clock.WorldClockEntry;
import com.clockmods.time.NetworkTimeProvider;
import com.clockmods.ui.DateFormatter;
import com.clockmods.weather.WeatherModels;
import com.clockmods.weather.WeatherModels.WeatherState;
import com.clockmods.weather.WeatherTemperatureFormatter;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lifecycle-aware host for SDK clock styles. Styles draw only through the Canvas contract; this
 * class owns time, preferences, accessibility, and foreground/background frame scheduling.
 */
public class UltimateClockView extends FrameLayout {
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MINUTE_REFRESH_MILLIS = 60_000L;
    private static final long REDUCED_MOTION_CACHE_MILLIS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!shouldRunFrames()) return;
            invalidate();
            scheduleNextFrame();
        }
    };

    private ClockStyleRegistry styleRegistry;
    private UltimateClockPreferences preferences;
    private String styleId = UltimateClockPreferences.DEFAULT_STYLE_ID;
    private ClockState.SecondHandMotion secondHandMotion =
            UltimateClockPreferences.DEFAULT_SECOND_HAND_MOTION;
    private boolean followSystemReducedMotion =
            UltimateClockPreferences.DEFAULT_FOLLOW_SYSTEM_REDUCED_MOTION;
    private boolean manualReducedMotion;
    private String backgroundMode = UltimateClockPreferences.DEFAULT_BACKGROUND_MODE;
    private boolean showSeconds = true;
    private boolean use24Hour;
    private Locale locale;
    private TimeZone timeZone;
    private String customDateText = "";
    private String customTimeZoneText = "";
    private String weatherText = "";
    private String customMessage = "";
    private String statusText = "";
    private String lastContentDescription;
    private BackgroundRepository backgroundRepository;
    private final ClockTypography typography = new ClockTypography();
    private ClockThemeTokens paletteTokens;
    private WorldClockRepository worldClockRepository;
    private List<WorldClockEntry> worldClocks = new ArrayList<WorldClockEntry>();
    private final HorizontalScrollView worldClockScroller;
    private final View worldClockCards;
    private ClockState worldClockState;
    private ClockRenderContext worldClockContext;
    private ClockThemeTokens worldClockTheme;
    private GestureDetector gestureDetector;
    private String fontFamily = ClockPreferences.DEFAULT_FONT_FAMILY;
    private int fontWeight = FontCatalog.DEFAULT_WEIGHT;
    private float timeScale = 1f;
    private float dateScale = 1f;
    private float supportingScale = 1f;
    private final Object workerLock = new Object();
    private ExecutorService imageExecutor;
    private NetworkTimeProvider networkTimeProvider;
    private Bitmap backgroundBitmap;
    private int loadedWidth;
    private int loadedHeight;
    private String datePattern = ClockPreferences.DEFAULT_DATE_PATTERN_CN;
    private DateFormatter.Lang dateLanguage = DateFormatter.Lang.CHINESE;
    private boolean showLunar;
    private boolean requestedRunning;
    private boolean attached;
    private boolean windowVisible = true;
    private boolean windowFocused = true;
    private long reducedMotionCheckedAt;
    private boolean systemReducedMotion;
    private float bottomOverlayInset;

    public UltimateClockView(Context context) {
        this(context, null);
    }

    public UltimateClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        worldClockScroller = new HorizontalScrollView(context) {
            @Override public boolean onInterceptTouchEvent(MotionEvent event) {
                return hasScrollableCities() && super.onInterceptTouchEvent(event);
            }

            @Override public boolean onTouchEvent(MotionEvent event) {
                return hasScrollableCities() && super.onTouchEvent(event);
            }

            private boolean hasScrollableCities() {
                return canScrollHorizontally(-1) || canScrollHorizontally(1);
            }
        };
        worldClockScroller.setHorizontalScrollBarEnabled(false);
        worldClockScroller.setHorizontalFadingEdgeEnabled(false);
        worldClockScroller.setClipToPadding(false);
        worldClockScroller.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        worldClockCards = new View(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int width = (int) Math.ceil(UltimateClockStyles.worldClockContentWidth(
                        worldClocks.size(), UltimateClockView.this.getMeasuredWidth(),
                        UltimateClockView.this.getMeasuredHeight(),
                        getResources().getDisplayMetrics().density));
                setMeasuredDimension(width, MeasureSpec.getSize(heightSpec));
            }

            @Override protected void onDraw(Canvas canvas) {
                if (worldClockState != null && worldClockContext != null) {
                    UltimateClockStyles.drawWorldClockCards(canvas, worldClockContext,
                            worldClockState, worldClockTheme, getHeight());
                }
            }
        };
        worldClockScroller.addView(worldClockCards, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        addView(worldClockScroller, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        styleRegistry = UltimateClockStyles.sharedRegistry();
        preferences = new UltimateClockPreferences(context);
        locale = Locale.getDefault();
        timeZone = TimeZone.getDefault();
        use24Hour = DateFormat.is24HourFormat(context);
        reloadPreferences();
        setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** Applies the shared Ultimate settings contract without replacing the host registry. */
    public final void reloadPreferences() {
        if (preferences == null) return;
        styleId = preferences.getStyleId();
        secondHandMotion = preferences.getSecondHandMotion();
        followSystemReducedMotion = preferences.isFollowSystemReducedMotion();
        backgroundMode = preferences.getBackgroundMode();
        if (worldClockRepository == null) {
            worldClockRepository = new WorldClockRepository(getContext());
        }
        ClockStyle resolved = styleRegistry.resolveForApi(styleId, Build.VERSION.SDK_INT);
        worldClocks = worldClockRepository.isEnabled()
                && resolved.getMetadata().getCapabilities().supports(
                ClockStyleCapabilities.Capability.WORLD_CLOCK)
                ? worldClockRepository.getSelected() : new ArrayList<WorldClockEntry>();
        worldClockScroller.scrollTo(0, 0);
        worldClockScroller.setVisibility(worldClocks.isEmpty() ? GONE : VISIBLE);
        requestLayout();
        reloadTypography();
        invalidateAndReschedule();
    }

    /**
     * Reads the font chosen for the current style. Done here rather than in {@code onDraw} so no
     * frame ever touches SharedPreferences; the style id is part of the key, so switching styles
     * has to re-read it.
     */
    private void reloadTypography() {
        BackgroundRepository repository = backgroundRepository != null
                ? backgroundRepository : new BackgroundRepository(getContext());
        fontFamily = repository.getFontFamily(styleId);
        fontWeight = repository.getFontWeight(styleId);
        timeScale = repository.getTimeFontScale(styleId)
                / ClockPreferences.DEFAULT_TIME_FONT_SCALE;
        dateScale = repository.getDateFontScale(styleId)
                / ClockPreferences.DEFAULT_DATE_FONT_SCALE;
        supportingScale = repository.getSupportingFontScale(styleId);
        ClockThemeTokens base = styleRegistry.resolveForApi(styleId, Build.VERSION.SDK_INT).getThemeTokens();
        paletteTokens = ClockPalette.supports(styleId)
                ? preferences.getPalette(styleId).applyTo(base) : base;
        typography.invalidate();
    }

    public void setPreferences(UltimateClockPreferences value) {
        if (value == null) throw new IllegalArgumentException("preferences must not be null");
        preferences = value;
        reloadPreferences();
    }

    public UltimateClockPreferences getPreferences() {
        return preferences;
    }

    public void setStyleRegistry(ClockStyleRegistry registry) {
        if (registry == null || registry.size() == 0) {
            throw new IllegalArgumentException("A non-empty style registry is required");
        }
        styleRegistry = registry;
        reloadTypography();
        invalidate();
    }

    public ClockStyleRegistry getStyleRegistry() {
        return styleRegistry;
    }

    public void setStyleId(String value) {
        styleId = value == null ? UltimateClockPreferences.DEFAULT_STYLE_ID : value.trim();
        if (styleId.length() == 0) styleId = UltimateClockPreferences.DEFAULT_STYLE_ID;
        if (preferences != null) preferences.setStyleId(styleId);
        reloadTypography();
        requestLayout();
        invalidate();
    }

    public String getStyleId() {
        return styleId;
    }

    public void setSecondHandMotion(ClockState.SecondHandMotion value) {
        secondHandMotion = value == null ? ClockState.SecondHandMotion.TICK : value;
        if (preferences != null) preferences.setSecondHandMotion(secondHandMotion);
        invalidateAndReschedule();
    }

    public ClockState.SecondHandMotion getSecondHandMotion() {
        return secondHandMotion;
    }

    public void setFollowSystemReducedMotion(boolean follow) {
        followSystemReducedMotion = follow;
        reducedMotionCheckedAt = 0L;
        if (preferences != null) preferences.setFollowSystemReducedMotion(follow);
        invalidateAndReschedule();
    }

    public boolean isFollowSystemReducedMotion() {
        return followSystemReducedMotion;
    }

    /** Forces a reduced-motion frame policy when the host has its own accessibility setting. */
    public void setReducedMotion(boolean reducedMotion) {
        manualReducedMotion = reducedMotion;
        invalidateAndReschedule();
    }

    public void setShowSeconds(boolean show) {
        showSeconds = show;
        invalidateAndReschedule();
    }

    public boolean isShowSeconds() {
        return showSeconds;
    }

    public void setUse24Hour(boolean use24Hour) {
        this.use24Hour = use24Hour;
        invalidate();
    }

    public boolean isUse24Hour() {
        return use24Hour;
    }

    public void setLocale(Locale value) {
        locale = value == null ? Locale.getDefault() : value;
        invalidate();
    }

    public Locale getLocale() {
        return locale;
    }

    public void setTimeZone(TimeZone value) {
        timeZone = value == null ? TimeZone.getDefault() : (TimeZone) value.clone();
        invalidate();
    }

    public void setTimeZoneId(String id) {
        if (id == null || id.trim().length() == 0) setTimeZone(TimeZone.getDefault());
        else setTimeZone(TimeZone.getTimeZone(id.trim()));
    }

    public TimeZone getTimeZone() {
        return (TimeZone) timeZone.clone();
    }

    /** An empty value restores automatic locale-aware date formatting. */
    public void setDateText(String value) {
        customDateText = value == null ? "" : value.trim();
        invalidate();
    }

    public void setTimeZoneText(String value) {
        customTimeZoneText = value == null ? "" : value.trim();
        invalidate();
    }

    public void setWeatherText(String value) {
        weatherText = value == null ? "" : value.trim();
        invalidate();
    }

    public void setStatusText(String value) {
        statusText = value == null ? "" : value.trim();
        invalidate();
    }

    /** Compatibility bridge for the existing weather controller used by the Ultimate fragment. */
    public void setWeatherState(WeatherState state) {
        if (state == null) {
            setWeatherText("");
            return;
        }
        if (state.data != null) {
            WeatherModels.WeatherDisplayData data = state.data;
            StringBuilder text = new StringBuilder();
            String location = WeatherModels.locationText(data.city, data.district);
            appendPart(text, location);
            appendPart(text, WeatherTemperatureFormatter.format(data.temperature,
                    weatherTemperatureUnit()));
            appendPart(text, data.text);
            setWeatherText(text.toString());
        } else {
            setWeatherMessage(state.message);
        }
    }

    public void setWeatherMessage(String message) {
        setWeatherText(message);
    }

    private String weatherTemperatureUnit() {
        return backgroundRepository == null
                ? new ClockPreferences(getContext()).getWeatherTemperatureUnit()
                : backgroundRepository.getWeatherTemperatureUnit();
    }

    /** Reserves room for a host overlay without shrinking the rendered background. */
    public void setBottomOverlayInset(float inset) {
        bottomOverlayInset = Math.max(0f, inset);
        requestLayout();
        invalidate();
    }

    /** The background repository still supplies clock settings; style renderers own their surface. */
    public void setBackgroundRepository(BackgroundRepository repository) {
        backgroundRepository = repository;
        if (repository != null) {
            reloadTypography();
            setShowSeconds(repository.isShowSeconds());
            setUse24Hour(repository.isUse24Hour());
            String zone = repository.getTimeZoneId();
            setTimeZoneId(zone);
            String language = repository.getClockLanguage();
            boolean english = ClockPreferences.LANGUAGE_ENGLISH.equals(language);
            setLocale(english ? Locale.ENGLISH
                    : ClockPreferences.LANGUAGE_TRADITIONAL.equals(language)
                            ? Locale.TRADITIONAL_CHINESE : Locale.SIMPLIFIED_CHINESE);
            dateLanguage = LocaleManager.dateLang(language);
            datePattern = english ? repository.getDatePatternEn() : repository.getDatePatternCn();
            showLunar = repository.isShowLunar();
            customDateText = "";
            setStatusText("");
            String message = repository.getCustomMessage();
            customMessage = message == null ? "" : message.trim();
            configureNetworkTime(repository);
        }
        reloadPreferences();
        requestBackgroundReload();
        invalidate();
    }

    public BackgroundRepository getBackgroundRepository() {
        return backgroundRepository;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reducedMotionCheckedAt = 0L;
        synchronized (workerLock) {
            attached = true;
            imageExecutor = Executors.newSingleThreadExecutor();
            networkTimeProvider = new NetworkTimeProvider();
        }
        if (backgroundRepository != null) configureNetworkTime(backgroundRepository);
        requestBackgroundReload();
        updateFrameLoop();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        cancelTicker();
        ExecutorService executor;
        NetworkTimeProvider provider;
        synchronized (workerLock) {
            executor = imageExecutor;
            imageExecutor = null;
            provider = networkTimeProvider;
            networkTimeProvider = null;
        }
        if (executor != null) executor.shutdownNow();
        if (provider != null) provider.shutdown();
        replaceBackgroundBitmap(null, 0, 0);
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != loadedWidth || height != loadedHeight) requestBackgroundReload();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        windowVisible = visibility == View.VISIBLE;
        updateFrameLoop();
    }

    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        windowFocused = hasWindowFocus;
        updateFrameLoop();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this) {
            windowVisible = visibility == View.VISIBLE;
            updateFrameLoop();
        }
    }

    /** Starts frame delivery; callers should pair this with {@link #stop()} from onPause. */
    public void start() {
        requestedRunning = true;
        updateFrameLoop();
    }

    /** Stops all scheduled invalidations immediately. */
    public void stop() {
        requestedRunning = false;
        cancelTicker();
    }

    public boolean isRunning() {
        return requestedRunning && shouldRunFrames();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        long now = currentTimeMillis();
        boolean reduced = manualReducedMotion ||
                (followSystemReducedMotion && isSystemReducedMotion());
        ClockStyle style = styleRegistry.resolveForApi(styleId, Build.VERSION.SDK_INT);
        ClockState.SecondHandMotion motion = effectiveSecondHandMotion(style, reduced);
        ClockState state = ClockState.builder(now)
                .timeZone(timeZone)
                .locale(locale)
                .use24Hour(use24Hour)
                .showSeconds(showSeconds)
                .secondHandMotion(motion)
                .dateText(dateText(now))
                // The main face no longer exposes a GMT/zone badge. The actual zone remains in
                // the accessibility description and in the world-clock editor.
                .timeZoneText("")
                .weatherText(combinedWeatherText())
                .statusText(statusText)
                .worldClocks(worldClocks)
                .timeScale(timeScale)
                .dateScale(dateScale)
                .supportingScale(supportingScale)
                .build();
        ClockRenderContext renderContext = new ClockRenderContext(0f, 0f, getWidth(), getHeight(),
                getResources().getDisplayMetrics().density,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f,
                            getResources().getDisplayMetrics()), now, reduced,
                createBackground(now), bottomOverlayInset, 0f, true);
        ClockThemeTokens theme = typography.apply(getContext(), paletteTokens, styleId,
                fontFamily, fontWeight);
        worldClockState = state;
        worldClockContext = renderContext;
        worldClockTheme = theme;
        worldClockCards.invalidate();
        int saveCount = canvas.save();
        try {
            style.getRenderer().render(canvas, renderContext, state, theme);
        } finally {
            canvas.restoreToCount(saveCount);
        }
        updateContentDescription(state, style);
    }

    /** Observe double taps before dispatching the stream to the native city scroller. */
    public void setGestureDetector(GestureDetector detector) {
        gestureDetector = detector;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (gestureDetector != null) gestureDetector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && worldClockScroller.getVisibility() == VISIBLE
                && worldClockStripBounds().contains(event.getX(), event.getY())
                && getParent() != null) {
            // Keep the outer destination pager from stealing city gestures, even when they fit.
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        boolean handled = super.dispatchTouchEvent(event);
        if ((event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return handled;
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (worldClockScroller.getVisibility() == GONE) return;
        RectF strip = worldClockStripBounds();
        float density = getResources().getDisplayMetrics().density;
        float faceHeight = strip.top - Math.min(density * 8f, getMeasuredHeight() * .025f);
        int inset = Math.round(UltimateClockStyles.worldClockContentInset(styleId,
                getMeasuredWidth(), faceHeight, density));
        worldClockScroller.setPadding(inset, 0, inset, 0);
        worldClockScroller.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round(strip.height()), MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (worldClockScroller.getVisibility() == GONE) return;
        RectF strip = worldClockStripBounds();
        worldClockScroller.layout(0, Math.round(strip.top), getMeasuredWidth(),
                Math.round(strip.top) + worldClockScroller.getMeasuredHeight());
    }

    private RectF worldClockStripBounds() {
        return UltimateClockStyles.worldClockStripBounds(0f, 0f, getMeasuredWidth(), getMeasuredHeight(),
                getResources().getDisplayMetrics().density, bottomOverlayInset);
    }

    private void invalidateAndReschedule() {
        invalidate();
        updateFrameLoop();
    }

    private void updateFrameLoop() {
        cancelTicker();
        if (shouldRunFrames()) {
            ticker.run();
        }
    }

    private boolean shouldRunFrames() {
        return requestedRunning && attached && windowVisible && windowFocused
                && getVisibility() == View.VISIBLE && getWidth() > 0 && getHeight() > 0;
    }

    private void scheduleNextFrame() {
        if (!shouldRunFrames()) return;
        long delay;
        boolean reduced = manualReducedMotion
                || (followSystemReducedMotion && isSystemReducedMotion());
        ClockStyle style = styleRegistry.resolveForApi(styleId, Build.VERSION.SDK_INT);
        ClockState.SecondHandMotion motion = effectiveSecondHandMotion(style, reduced);
        if (motion == ClockState.SecondHandMotion.OFF) {
            long now = currentTimeMillis();
            delay = MINUTE_REFRESH_MILLIS - (now % MINUTE_REFRESH_MILLIS);
        } else if (motion == ClockState.SecondHandMotion.SWEEP) {
            postOnAnimation(ticker);
            return;
        } else {
            long now = currentTimeMillis();
            delay = MILLIS_PER_SECOND - (now % MILLIS_PER_SECOND);
        }
        handler.postDelayed(ticker, Math.max(1L, delay));
    }

    private boolean isSystemReducedMotion() {
        long now = SystemClock.uptimeMillis();
        if (reducedMotionCheckedAt != 0L
                && now - reducedMotionCheckedAt < REDUCED_MOTION_CACHE_MILLIS) {
            return systemReducedMotion;
        }
        boolean reduced = false;
        if (Build.VERSION.SDK_INT >= 26) {
            reduced = !ValueAnimator.areAnimatorsEnabled();
        } else if (Build.VERSION.SDK_INT >= 17) {
            try {
                reduced = Settings.Global.getFloat(getContext().getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
            } catch (RuntimeException ignored) {
                reduced = false;
            }
        }
        systemReducedMotion = reduced;
        reducedMotionCheckedAt = now;
        return reduced;
    }

    private ClockState.SecondHandMotion effectiveSecondHandMotion(ClockStyle style,
            boolean reducedMotion) {
        return resolveSecondHandMotion(style, showSeconds, secondHandMotion, reducedMotion);
    }

    static ClockState.SecondHandMotion resolveSecondHandMotion(ClockStyle style,
            boolean showSeconds, ClockState.SecondHandMotion requestedMotion,
            boolean reducedMotion) {
        ClockStyleCapabilities capabilities = style.getMetadata().getCapabilities();
        if (!showSeconds
                || !capabilities.supports(ClockStyleCapabilities.Capability.SECONDS)) {
            return ClockState.SecondHandMotion.OFF;
        }
        // Motion choices describe a physical second hand. Digital-only faces intentionally hide
        // that setting, so an OFF value saved by a previous analog face must not silently remove
        // their numeric seconds. The separate "show seconds" preference remains authoritative.
        if (style.getMetadata().getKind() == ClockStyleMetadata.Kind.DIGITAL) {
            return ClockState.SecondHandMotion.TICK;
        }
        ClockState.SecondHandMotion safeMotion = requestedMotion == null
                ? ClockState.SecondHandMotion.TICK : requestedMotion;
        if (safeMotion == ClockState.SecondHandMotion.OFF) {
            return ClockState.SecondHandMotion.OFF;
        }
        if (safeMotion == ClockState.SecondHandMotion.SWEEP
                && (reducedMotion || !capabilities.supports(
                        ClockStyleCapabilities.Capability.SMOOTH_SECONDS))) {
            return ClockState.SecondHandMotion.TICK;
        }
        return safeMotion;
    }

    private void cancelTicker() {
        handler.removeCallbacks(ticker);
        removeCallbacks(ticker);
    }

    private String dateText(long now) {
        if (customDateText.length() > 0) return customDateText;
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(now);
        String date = DateFormatter.format(datePattern, calendar, dateLanguage);
        if (!showLunar) return date;
        String lunar = LunarCalendar.format(calendar);
        return lunar.length() == 0 ? date : date + " / " + lunar;
    }

    private String timeZoneText() {
        if (customTimeZoneText.length() > 0) return customTimeZoneText;
        return timeZone.getDisplayName(timeZone.inDaylightTime(new Date()), TimeZone.SHORT, locale);
    }

    private long currentTimeMillis() {
        NetworkTimeProvider provider = networkTimeProvider;
        return provider == null ? System.currentTimeMillis() : provider.currentTimeMillis();
    }

    private void configureNetworkTime(BackgroundRepository repository) {
        NetworkTimeProvider provider = networkTimeProvider;
        if (provider == null || repository == null) return;
        provider.setEnabled(repository.isUseNetworkTime());
        provider.setSyncIntervalMinutes(repository.getSyncIntervalMinutes());
    }

    private ClockBackground createBackground(long now) {
        BackgroundRepository repository = backgroundRepository;
        if (repository == null) return null;
        boolean dimmed = shouldDimBackground(now);
        if (UltimateClockPreferences.BACKGROUND_MODE_IMAGE.equals(backgroundMode)) {
            if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
                return ClockBackground.image(backgroundBitmap, repository.getCurrentColor(), dimmed);
            }
            return ClockBackground.color(repository.getCurrentColor(), dimmed);
        }
        if (UltimateClockPreferences.BACKGROUND_MODE_COLOR.equals(backgroundMode)) {
            return ClockBackground.color(repository.getCurrentColor(), dimmed);
        }
        return ClockBackground.theme(dimmed);
    }

    private String combinedWeatherText() {
        if (weatherText.length() == 0) return customMessage;
        if (customMessage.length() == 0) return weatherText;
        return weatherText + " / " + customMessage;
    }

    private boolean shouldDimBackground(long now) {
        BackgroundRepository repository = backgroundRepository;
        if (repository == null) return false;
        if (repository.isDimBackground()) return true;
        if (!repository.isScheduleDimBackground()) return false;
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(now);
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
                + calendar.get(Calendar.MINUTE);
        return BackgroundDimSchedule.isActive(currentMinutes,
                repository.getDimStartMinutes(), repository.getDimEndMinutes());
    }

    private void requestBackgroundReload() {
        final BackgroundRepository repository = backgroundRepository;
        if (repository == null || getWidth() <= 0 || getHeight() <= 0) return;
        final int requestedWidth = getWidth();
        final int requestedHeight = getHeight();
        synchronized (workerLock) {
            if (!attached || imageExecutor == null || imageExecutor.isShutdown()) return;
            imageExecutor.execute(() -> {
                Bitmap loaded = null;
                if (ClockPreferences.MODE_IMAGE.equals(repository.getBackgroundMode())) {
                    try {
                        loaded = repository.loadImage(requestedWidth, requestedHeight);
                    } catch (IOException | OutOfMemoryError ignored) {
                        loaded = null;
                    }
                }
                final Bitmap result = loaded;
                handler.post(() -> {
                    if (attached) {
                        replaceBackgroundBitmap(result, requestedWidth, requestedHeight);
                    } else if (result != null && !result.isRecycled()) {
                        result.recycle();
                    }
                });
            });
        }
    }

    private void replaceBackgroundBitmap(Bitmap bitmap, int width, int height) {
        if (backgroundBitmap != null && backgroundBitmap != bitmap
                && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        backgroundBitmap = bitmap;
        loadedWidth = width;
        loadedHeight = height;
        invalidate();
    }

    private void updateContentDescription(ClockState state, ClockStyle style) {
        Calendar calendar = state.newCalendar();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String time = String.format(locale, "%02d:%02d", hour, calendar.get(Calendar.MINUTE));
        if (!use24Hour) {
            int display = hour % 12;
            if (display == 0) display = 12;
            time = String.format(locale, "%02d:%02d", display, calendar.get(Calendar.MINUTE));
        }
        StringBuilder description = new StringBuilder(style.getMetadata().getName())
                .append(", ").append(time)
                .append(", ").append(state.getDateText())
                .append(", ").append(timeZoneText());
        appendDescription(description, state.getWeatherText());
        appendDescription(description, state.getStatusText());
        appendDescription(description,
                getContext().getString(R.string.open_settings_accessibility));
        String value = description.toString();
        if (!value.equals(lastContentDescription)) {
            lastContentDescription = value;
            setContentDescription(value);
        }
    }

    private static void appendPart(StringBuilder target, String value) {
        if (value != null && value.trim().length() > 0) {
            if (target.length() > 0) target.append(' ');
            target.append(value.trim());
        }
    }

    private static void appendDescription(StringBuilder target, String value) {
        if (value != null && value.length() > 0) target.append(", ").append(value);
    }
}
