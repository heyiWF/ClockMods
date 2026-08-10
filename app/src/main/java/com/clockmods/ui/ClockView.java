package com.clockmods.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.LocaleManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.BackgroundDimSchedule;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.LunarCalendar;
import com.clockmods.time.NetworkTimeProvider;
import com.clockmods.weather.WeatherModels;
import com.clockmods.weather.WeatherModels.WeatherState;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClockView extends View {
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long TIME_TRANSITION_DURATION_MILLIS = 300L;
    private static final long WEATHER_DETAIL_HOLD_MILLIS = 3000L;
    private static final long WEATHER_DETAIL_SCROLL_PAUSE_MILLIS = 1000L;
    private static final long WEATHER_DETAIL_TRANSITION_MILLIS = 200L;
    private static final long WEATHER_DETAIL_FRAME_DELAY_MILLIS = 16L;
    private static final float WEATHER_DETAIL_SCROLL_DP_PER_SECOND = 40f;
    private static final float WEATHER_DETAIL_HORIZONTAL_PADDING_DP = 24f;
    private static final float SMALL_SECONDS_GAP_SPACE_FRACTION = 0.35f;
    // Fractions the main time size is fitted to: it never spans more than
    // TIME_MAX_WIDTH_FRACTION of the width nor TIME_HEIGHT_FRACTION of the height.
    // Exposed so overlays (e.g. the hourly chime) can reproduce the exact size.
    public static final float TIME_HEIGHT_FRACTION = 0.55f;
    public static final float TIME_MAX_WIDTH_FRACTION = 0.98f;
    private static final float SUPPORTING_TEXT_LETTER_SPACING = 0.025f;
    private static final int CLOCK_SHADOW_ALPHA = 0x66;
    private static final int DIM_BACKGROUND_OVERLAY_COLOR = 0x80000000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            // Schedule the next tick against the same clock source onDraw draws
            // from, so the seconds digit flips exactly on the network-time
            // boundary (not the system-clock boundary) when network time is on.
            NetworkTimeProvider timeProvider = networkTimeProvider;
            long now = timeProvider == null
                ? System.currentTimeMillis() : timeProvider.currentTimeMillis();
            long next = MILLIS_PER_SECOND - (now % MILLIS_PER_SECOND);
            handler.postDelayed(this, next);
        }
    };
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint secondsPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint periodPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix bitmapMatrix = new Matrix();
    private final Object workerLock = new Object();
    private ExecutorService imageExecutor;
    private NetworkTimeProvider networkTimeProvider;
    private boolean attached;
    private String datePatternCn = ClockPreferences.DEFAULT_DATE_PATTERN_CN;
    private String datePatternEn = ClockPreferences.DEFAULT_DATE_PATTERN_EN;
    private BackgroundRepository backgroundRepository;
    private Bitmap backgroundBitmap;
    private int loadedWidth;
    private int loadedHeight;
    private float timeFontScale = ClockPreferences.DEFAULT_TIME_FONT_SCALE;
    private float dateFontScale = ClockPreferences.DEFAULT_DATE_FONT_SCALE;
    private boolean blinkColon = ClockPreferences.DEFAULT_BLINK_COLON;
    private boolean animateTimeChanges = ClockPreferences.DEFAULT_ANIMATE_TIME_CHANGES;
    private String timeTransition = ClockPreferences.DEFAULT_TIME_TRANSITION;
    private boolean boldText = ClockPreferences.DEFAULT_BOLD_TEXT;
    private boolean showSeconds = ClockPreferences.DEFAULT_SHOW_SECONDS;
    private boolean showLunar = ClockPreferences.DEFAULT_SHOW_LUNAR;
    private boolean smallSeconds = ClockPreferences.DEFAULT_SMALL_SECONDS;
    private boolean use24Hour = ClockPreferences.DEFAULT_USE_24_HOUR;
    private boolean clockUseEnglish = ClockPreferences.DEFAULT_CLOCK_USE_ENGLISH;
    private DateFormatter.Lang dateLang = DateFormatter.Lang.CHINESE;
    private boolean portraitStacked = ClockPreferences.DEFAULT_PORTRAIT_STACKED;
    private boolean dateLunarDualLine = ClockPreferences.DEFAULT_DATE_LUNAR_DUAL_LINE;
    // Per-line animation state for the portrait stacked layout: index 0 = hours,
    // 1 = minutes, 2 = seconds. Each line animates its digit changes independently.
    private final String[] stackedText = new String[3];
    private final String[] stackedPrev = new String[3];
    private final long[] stackedAt = new long[3];
    private ClockTimeFormatter.DisplayTime displayedTime;
    private ClockTimeFormatter.DisplayTime previousTime;
    private long timeTransitionStartedAt;
    private float clockShadowRadius;
    private float clockShadowDy;
    private WeatherState weatherState;
    // Rotating carousel for the detailed-weather line (feels-like, humidity, …).
    private final Carousel weatherDetailCarousel = new Carousel();
    // Rotating carousel for the main supporting line when it hosts weather + a custom
    // message (or a message alone). Shares the exact timing/scroll logic of the detail line.
    private final Carousel messageCarousel = new Carousel();
    private String customMessage = ClockPreferences.DEFAULT_CUSTOM_MESSAGE;

    public ClockView(Context context) {
        this(context, null);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        clockShadowRadius = 8f * density;
        clockShadowDy = 2f * density;
        timePaint.setColor(0xFFFFFFFF);
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setShadowLayer(clockShadowRadius, 0f, clockShadowDy, 0x66000000);

        secondsPaint.setColor(0xFFFFFFFF);
        secondsPaint.setTextAlign(Paint.Align.CENTER);
        secondsPaint.setShadowLayer(clockShadowRadius, 0f, clockShadowDy, 0x66000000);

        periodPaint.setColor(0xFFFFFFFF);
        periodPaint.setTextAlign(Paint.Align.CENTER);
        periodPaint.setShadowLayer(clockShadowRadius, 0f, clockShadowDy, 0x66000000);

        datePaint.setColor(0xFFFFFFFF);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setShadowLayer(6f * density, 0f, 2f * density, 0x66000000);
    }

    public void setBackgroundRepository(BackgroundRepository repository) {
        backgroundRepository = repository;
        requestBackgroundReload();
        invalidate();
    }

    public void setWeatherState(WeatherState state) {
        weatherState = state;
        weatherDetailCarousel.setItems(
                (state != null && state.data != null && state.data.detail != null)
                        ? state.data.detail.carouselItems(detailLabels()) : null);
        invalidate();
    }

    /** Localized labels/units for the detailed weather carousel (follows interface language). */
    private WeatherModels.WeatherDetail.DetailLabels detailLabels() {
        return new WeatherModels.WeatherDetail.DetailLabels(
                getContext().getString(com.clockmods.R.string.weather_feels_format),
                getContext().getString(com.clockmods.R.string.weather_humidity_format),
                getContext().getString(com.clockmods.R.string.weather_wind_scale_format),
                getContext().getString(com.clockmods.R.string.weather_precip_format),
                getContext().getString(com.clockmods.R.string.weather_air_format),
                getContext().getString(com.clockmods.R.string.weather_warning_suffix));
    }

    public void setWeatherMessage(String message) {
        weatherState = WeatherState.of(WeatherModels.Status.IDLE, message);
        invalidate();
    }

    public void requestBackgroundReload() {
        if (backgroundRepository == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        final int requestedWidth = getWidth();
        final int requestedHeight = getHeight();
        synchronized (workerLock) {
            if (!attached || imageExecutor == null || imageExecutor.isShutdown()) return;
            imageExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap loaded = null;
                    if (ClockPreferences.MODE_IMAGE.equals(backgroundRepository.getBackgroundMode())) {
                        try {
                            loaded = backgroundRepository.loadImage(requestedWidth, requestedHeight);
                        } catch (IOException | OutOfMemoryError ignored) {
                            loaded = null;
                        }
                    }
                    final Bitmap result = loaded;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (attached) {
                                replaceBackgroundBitmap(result, requestedWidth, requestedHeight);
                            } else if (result != null && !result.isRecycled()) {
                                result.recycle();
                            }
                        }
                    });
                }
            });
        }
    }

    public void start() {
        handler.removeCallbacks(ticker);
        ticker.run();
    }

    public void stop() {
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != loadedWidth || height != loadedHeight) {
            requestBackgroundReload();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int backgroundColor = backgroundRepository == null ? 0xFF101418 : backgroundRepository.getCurrentColor();
        canvas.drawColor(backgroundColor);
        drawBackgroundImage(canvas, width, height);

        java.util.TimeZone timeZone = resolveTimeZone();
        Calendar now = Calendar.getInstance(timeZone);
        NetworkTimeProvider timeProvider = networkTimeProvider;
        now.setTimeInMillis(timeProvider == null
            ? System.currentTimeMillis() : timeProvider.currentTimeMillis());
        if (shouldDimBackground(now)) {
            canvas.drawColor(DIM_BACKGROUND_OVERLAY_COLOR);
        }

        applyTextStyles();

        ClockTimeFormatter.DisplayTime displayTime = ClockTimeFormatter.format(
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND),
            showSeconds, blinkColon, smallSeconds, use24Hour, clockUseEnglish);
        String dateText = DateFormatter.format(
                clockUseEnglish ? datePatternEn : datePatternCn, now, dateLang);
        String lunarText = showLunar ? LunarCalendar.format(now) : "";

        // Portrait stacked layout is a fully separate path; landscape is untouched.
        if (height >= width && portraitStacked) {
            drawStackedPortrait(canvas, now, dateText, lunarText, width, height);
            return;
        }

        String fullDate = lunarText.length() == 0 ? dateText : dateText + " " + lunarText;

        // Portrait always stacks the date and lunar lines; landscape shares a single line
        // unless the user opts into two rows. The width-based size is computed against
        // whichever string actually spans the full line so it fits the requested width %.
        boolean singleDateLine = lunarText.length() == 0 || (width > height && !dateLunarDualLine);
        String widestDateText = singleDateLine ? fullDate : longerOf(dateText, lunarText);
        applySupportingTypeface(widestDateText);

        timePaint.setTextSize(1f);
        secondsPaint.setTextSize(0.6f);
        periodPaint.setTextSize(0.3f);
        datePaint.setTextSize(1f);
        float measuredMainWidth = stableTextWidth(displayTime.mainText, timePaint);
        float leftAccessoryWidth = displayTime.hasPeriod()
            ? smallSecondsGapWidth() + periodPaint.measureText(displayTime.periodText) : 0f;
        float rightAccessoryWidth = 0f;
        if (displayTime.hasSmallSeconds()) {
            rightAccessoryWidth = smallSecondsGapWidth()
                + stableTextWidth(displayTime.secondsText, secondsPaint);
        }
        float measuredTimeWidth = ClockLayoutCalculator.calculateTimeGroupWidth(
                measuredMainWidth, leftAccessoryWidth, rightAccessoryWidth);
        float timeSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
            width, height, measuredTimeWidth, timeFontScale,
            TIME_HEIGHT_FRACTION, TIME_MAX_WIDTH_FRACTION);
        float dateSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
            width, height, measureSupportingText(widestDateText), dateFontScale, 0.14f, 0.92f);
        timePaint.setTextSize(timeSize);
        secondsPaint.setTextSize(timeSize * 0.6f);
        periodPaint.setTextSize(timeSize * 0.3f);
        datePaint.setTextSize(dateSize);

        float centerX = width / 2f;
        float timeCenterX = centerX + ClockLayoutCalculator.calculateMainCenterOffset(
            leftAccessoryWidth * timeSize, rightAccessoryWidth * timeSize);
        float centerY = height / 2f;
        Paint.FontMetrics timeMetrics = timePaint.getFontMetrics();
        Paint.FontMetrics dateMetrics = datePaint.getFontMetrics();
        float timeBaseline = centerY - (timeMetrics.ascent + timeMetrics.descent) / 2f;
        boolean portrait = height >= width;
        float gapFactor = portrait ? 0.9f : 0.35f;
        float gap = Math.max(portrait ? 32f : 12f, dateSize * gapFactor);
        float dateBaseline = timeBaseline + timeMetrics.ascent - gap - dateMetrics.descent;
        // Portrait stacks date/lunar and weather/detail as two rows each, so give those
        // rows more breathing room than a plain line (landscape keeps the tight default).
        float dateLineHeight = dateMetrics.descent - dateMetrics.ascent;
        float lunarRowGap = portrait ? dateLineHeight * 1.5f : dateLineHeight;   // 日期↔农历 行距（竖屏）
        float weatherDetailScale = portrait ? 1.35f : 1f;                        // 天气↔详细 行距倍数（竖屏）

        drawAnimatedTime(canvas, displayTime, timeCenterX, timeBaseline);
        drawWeather(canvas, centerX, timeBaseline, dateSize, timeMetrics, gap, weatherDetailScale);
        if (lunarText.length() == 0) {
            applySupportingTypeface(dateText);
            drawSupportingText(canvas, dateText, centerX, dateBaseline, Paint.Align.CENTER);
            return;
        }

        if (singleDateLine) {
            applySupportingTypeface(fullDate);
            drawSupportingText(canvas, fullDate, centerX, dateBaseline, Paint.Align.CENTER);
        } else {
            // Portrait, two date lines: the lunar row (nearest the time) keeps the full
            // gap above the time — matching the weather gap below it — and the Gregorian
            // date sits one (wider) row-gap above the lunar row.
            applySupportingTypeface(lunarText);
            drawSupportingText(canvas, lunarText, centerX, dateBaseline, Paint.Align.CENTER);
            applySupportingTypeface(dateText);
            drawSupportingText(canvas, dateText, centerX, dateBaseline - lunarRowGap,
                Paint.Align.CENTER);
        }
    }

    /**
     * Portrait "stacked" layout: the clock is drawn as large digits on separate
     * lines — hours, minutes and (optionally) seconds, top to bottom, with no colon.
     * The user's digit-change transition is preserved. Date, lunar date and weather
     * are still shown above/below the time block. Only entered when the view is
     * portrait and the stacked option is enabled; landscape uses the normal path.
     */
    private void drawStackedPortrait(Canvas canvas, Calendar now, String dateText,
            String lunarText, int width, int height) {
        int lines = showSeconds ? 3 : 2;
        // Detailed weather adds a second (carousel) line under the main weather line;
        // reserve room for it so both lines sit near the bottom with a wider gap.
        boolean weatherTwoLines = backgroundRepository != null
                && backgroundRepository.isWeatherEnabled()
                && backgroundRepository.isWeatherDetailed();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int displayHour = use24Hour ? hour : hour % 12;
        if (!use24Hour && displayHour == 0) {
            displayHour = 12;
        }
        String hoursText = String.format(Locale.CHINA, "%02d", displayHour);
        String minutesText = String.format(Locale.CHINA, "%02d", now.get(Calendar.MINUTE));
        String secondsText = String.format(Locale.CHINA, "%02d", now.get(Calendar.SECOND));

        float centerX = width / 2f;

        // Supporting text (date/lunar/weather) sizes and block heights are computed
        // first, so the digits can be capped to always leave room for them.
        datePaint.setTextSize(1f);
        String widestDate = lunarText.length() == 0 ? dateText : longerOf(dateText, lunarText);
        applySupportingTypeface(widestDate);
        float dateSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, measureSupportingText(widestDate), dateFontScale, 0.08f, 0.92f);
        datePaint.setTextSize(dateSize);
        Paint.FontMetrics dateMetrics = datePaint.getFontMetrics();
        float dateLineHeight = dateMetrics.descent - dateMetrics.ascent;
        float lunarGap = dateLineHeight * 1.5f;       // wider date <-> lunar row spacing
        float detailGapScale = 1.35f;                 // wider weather <-> detail row spacing
        boolean hasLunar = lunarText.length() > 0;
        // The bottom supporting line is shown when weather is enabled or a custom message
        // is set; the two-line (with detail carousel) variant only applies to weather.
        boolean hasMessage = backgroundRepository != null
                && backgroundRepository.getCustomMessage().length() > 0;
        boolean weatherShown = (backgroundRepository != null
                && backgroundRepository.isWeatherEnabled()) || hasMessage;
        float dateBlockHeight = hasLunar ? (lunarGap + dateLineHeight) : dateLineHeight;
        float weatherBlockHeight = !weatherShown ? 0f
                : (weatherTwoLines
                    ? (dateLineHeight + dateMetrics.descent) * detailGapScale + dateLineHeight
                    : dateLineHeight);

        float topSafe = height * 0.06f;
        float bottomSafe = height * 0.94f;
        float avail = bottomSafe - topSafe;
        // Date/weather stay a small fixed distance from the digits; they only spread
        // toward the edges when the time font grows, and never past the safe margins.
        float desiredGap = dateLineHeight * 1.6f;
        float minGap = dateLineHeight * 0.25f;

        // A two-digit group fills the requested width fraction, capped by a per-line
        // height fraction, then further capped so date + gaps + digits + weather never
        // exceed the usable height.
        timePaint.setTextSize(1f);
        float digitPairWidth = stableTextWidth("00", timePaint);
        float heightFraction = lines == 3
                ? (weatherTwoLines ? 0.13f : 0.18f)
                : (weatherTwoLines ? 0.20f : 0.28f);
        float lineSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, digitPairWidth, timeFontScale, heightFraction, 0.66f);
        // blockHeight ≈ blockFactor * lineSize; cap lineSize so the whole column fits.
        float blockFactor = (lines - 1) * 1.06f + 0.72f;
        float weatherReserve = weatherShown ? (weatherBlockHeight + minGap) : 0f;
        float maxBlockHeight = avail - dateBlockHeight - minGap - weatherReserve;
        lineSize = Math.min(lineSize, maxBlockHeight / blockFactor);
        timePaint.setTextSize(lineSize);
        Rect digitBounds = new Rect();
        timePaint.getTextBounds("0", 0, 1, digitBounds);
        float digitHeight = digitBounds.bottom - digitBounds.top;
        float lineAdvance = lineSize * 1.06f;
        float blockHeight = (lines - 1) * lineAdvance + digitHeight;

        // Equal gap above and below the digits (symmetric). It equals desiredGap when
        // there is spare room, and shrinks toward minGap as the digits grow; the whole
        // content column is then centered vertically within the safe area.
        float fitGap = ((weatherShown ? avail : (avail - weatherBlockHeight))
                - dateBlockHeight - blockHeight - weatherBlockHeight) / 2f;
        float gap = Math.max(minGap, Math.min(desiredGap, fitGap));
        float contentHeight = dateBlockHeight + gap + blockHeight
                + (weatherShown ? gap + weatherBlockHeight : 0f);
        float contentTop = Math.max(topSafe, topSafe + (avail - contentHeight) / 2f);

        float blockTop = contentTop + dateBlockHeight + gap;
        float firstBaseline = blockTop - digitBounds.top;
        float blockBottom = blockTop + blockHeight;

        long uptime = SystemClock.uptimeMillis();
        boolean animating = false;
        animating |= drawStackedLine(canvas, 0, hoursText, centerX, firstBaseline, uptime);
        animating |= drawStackedLine(canvas, 1, minutesText, centerX,
                firstBaseline + lineAdvance, uptime);
        if (lines == 3) {
            animating |= drawStackedLine(canvas, 2, secondsText, centerX,
                    firstBaseline + 2f * lineAdvance, uptime);
        } else {
            // Drop stale seconds state so re-enabling seconds starts clean.
            stackedText[2] = null;
            stackedPrev[2] = null;
        }
        if (animating) {
            postInvalidateDelayed(16L);
        }

        // Date (and lunar): the block bottom sits 'gap' above the hours.
        float dateBaseline = (blockTop - gap - dateBlockHeight) - dateMetrics.ascent;
        applySupportingTypeface(dateText);
        drawSupportingText(canvas, dateText, centerX, dateBaseline, Paint.Align.CENTER);
        if (hasLunar) {
            applySupportingTypeface(lunarText);
            drawSupportingText(canvas, lunarText, centerX, dateBaseline + lunarGap,
                    Paint.Align.CENTER);
        }

        // Weather: first row's top sits 'gap' below the seconds (symmetric with date).
        if (weatherShown) {
            drawWeather(canvas, centerX, blockBottom + gap, dateSize, new Paint.FontMetrics(),
                    0f, detailGapScale);
        }
    }

    /**
     * Draws one stacked time line at {@code baseline}, animating digit changes with
     * the configured transition. Returns {@code true} while an animation is running.
     */
    private boolean drawStackedLine(Canvas canvas, int index, String newText,
            float centerX, float baseline, long uptime) {
        if (!animateTimeChanges) {
            stackedText[index] = newText;
            stackedPrev[index] = null;
            drawStableText(canvas, newText, centerX, baseline, timePaint);
            return false;
        }
        if (stackedText[index] == null) {
            stackedText[index] = newText;
        } else if (!stackedText[index].equals(newText)) {
            stackedPrev[index] = stackedText[index];
            stackedText[index] = newText;
            stackedAt[index] = uptime;
        }
        if (stackedPrev[index] == null) {
            drawStableText(canvas, stackedText[index], centerX, baseline, timePaint);
            return false;
        }
        float progress = Math.min(1f,
                (uptime - stackedAt[index]) / (float) TIME_TRANSITION_DURATION_MILLIS);
        float eased = 1f - (float) Math.pow(1f - progress, 3);
        drawTextTransition(canvas, stackedPrev[index], stackedText[index], centerX, baseline,
                timePaint, eased, true, true);
        if (progress < 1f) {
            return true;
        }
        stackedPrev[index] = null;
        return false;
    }

        private void drawWeather(Canvas canvas, float centerX, float timeBaseline,
            float dateSize, Paint.FontMetrics timeMetrics, float gap, float detailGapScale) {
        if (backgroundRepository == null) return;
        boolean weatherEnabled = backgroundRepository.isWeatherEnabled();
        boolean hasMessage = customMessage != null && customMessage.length() > 0;

        // The main supporting line hosts, in priority order: the rich weather layout
        // (location + icon + condition) when weather is on and no message competes for the
        // line; a rotating carousel of [weather summary, message] when both are on; or the
        // message alone when only it is set. Weather off + no message => nothing to draw.
        String weatherSummary = null;
        WeatherIcon icon = null;
        String leftText = null;
        String rightText = null;
        if (weatherEnabled && weatherState != null) {
            if (weatherState.data != null) {
                leftText = WeatherModels.locationText(
                        weatherState.data.city, weatherState.data.district);
                rightText = weatherState.data.text + " " + weatherState.data.temperature + " ℃";
                icon = WeatherIcon.load(getContext(), weatherState.data.icon);
                weatherSummary = leftText + "  " + rightText;
            } else if (weatherState.message != null && weatherState.message.length() > 0) {
                weatherSummary = weatherState.message;
            }
        }

        if (!weatherEnabled && !hasMessage) return;

        float originalSize = datePaint.getTextSize();
        Typeface originalTypeface = datePaint.getTypeface();

        // Build the main-line carousel items. Rich icon rendering is only possible when the
        // line shows the weather summary alone (no message rotating through it).
        java.util.List<String> mainItems = new java.util.ArrayList<>();
        if (weatherSummary != null) mainItems.add(weatherSummary);
        if (hasMessage) mainItems.add(customMessage);
        if (mainItems.isEmpty()) {
            datePaint.setTextSize(originalSize);
            datePaint.setTypeface(originalTypeface);
            return;
        }
        boolean richWeatherLine = icon != null && !hasMessage;

        String widest = mainItems.get(0);
        for (String item : mainItems) {
            if (measureSupportingText(item) > measureSupportingText(widest)) widest = item;
        }
        applySupportingTypeface(widest);
        float iconSize = dateSize * 0.95f;
        float iconGap = dateSize * 0.25f;
        float measured = richWeatherLine
            ? measureSupportingText(leftText) + measureSupportingText(rightText)
                + iconSize + iconGap * 2f
            : measureSupportingText(widest);
        float available = getWidth() * 0.92f;
        float scale = measured > available ? Math.max(available / measured, 0.65f) : 1f;
        datePaint.setTextSize(dateSize * scale);
        iconSize *= scale;
        iconGap *= scale;
        Paint.FontMetrics weatherMetrics = datePaint.getFontMetrics();
        float baseline = timeBaseline + timeMetrics.descent + gap - weatherMetrics.ascent;

        if (richWeatherLine) {
            float leftWidth = measureSupportingText(leftText);
            float rightWidth = measureSupportingText(rightText);
            float total = leftWidth + rightWidth + iconSize + iconGap * 2f;
            float cursor = centerX - total / 2f;
            drawSupportingText(canvas, leftText, cursor, baseline, Paint.Align.LEFT);
            cursor += leftWidth + iconGap;
            float iconTop = baseline + (weatherMetrics.ascent + weatherMetrics.descent) / 2f - iconSize / 2f;
            icon.draw(canvas, cursor, iconTop, iconSize, datePaint);
            cursor += iconSize + iconGap;
            drawSupportingText(canvas, rightText, cursor, baseline, Paint.Align.LEFT);
        } else if (mainItems.size() == 1) {
            // Single item: center it, marquee-scrolling in place when it overflows the width.
            drawWeatherDetailItem(canvas, mainItems.get(0), null, centerX, baseline,
                weatherMetrics, 0f, mainCarouselElapsed(mainItems.get(0)));
            if (measureSupportingText(mainItems.get(0)) > weatherDetailAvailableWidth()) {
                postInvalidateDelayed(WEATHER_DETAIL_FRAME_DELAY_MILLIS);
            }
        } else {
            // Weather + message rotate through the line with the detail-line animation.
            if (!mainItems.equals(messageCarousel.items)) messageCarousel.setItems(mainItems);
            drawCarousel(canvas, messageCarousel, centerX, baseline, weatherMetrics);
        }

        drawWeatherDetail(canvas, centerX, baseline, weatherMetrics, detailGapScale);
        datePaint.setTextSize(originalSize);
        datePaint.setTypeface(originalTypeface);
    }

    /** Elapsed time within the single-item scroll cycle for the main carousel. */
    private long mainCarouselElapsed(String item) {
        long now = SystemClock.uptimeMillis();
        if (messageCarousel.cycleStartedAt == 0L
                || messageCarousel.items == null
                || messageCarousel.items.size() != 1
                || !item.equals(messageCarousel.items.get(0))) {
            messageCarousel.setItems(java.util.Collections.singletonList(item));
            messageCarousel.cycleStartedAt = now;
        }
        long elapsed = now - messageCarousel.cycleStartedAt;
        long duration = weatherDetailDisplayDuration(item);
        if (elapsed >= duration) {
            messageCarousel.cycleStartedAt = now;
            elapsed = 0L;
        }
        return elapsed;
    }


    private void drawWeatherDetail(Canvas canvas, float centerX, float weatherBaseline,
            Paint.FontMetrics weatherMetrics, float detailGapScale) {
        if (backgroundRepository == null || !backgroundRepository.isWeatherDetailed()) return;
        if (weatherDetailCarousel.isEmpty()) return;
        float detailBaseline = weatherBaseline + (weatherMetrics.descent
            - weatherMetrics.ascent + weatherMetrics.descent) * detailGapScale;
        drawCarousel(canvas, weatherDetailCarousel, centerX, detailBaseline, weatherMetrics);
    }

    /**
     * Drives one {@link Carousel} at {@code baseline}: holds each item, scrolls it
     * horizontally when it overflows the available width, then transitions to the next.
     * Used by both the detailed-weather line and the main weather/message line so their
     * animation and scrolling behaviour stay identical.
     */
    private void drawCarousel(Canvas canvas, Carousel carousel, float centerX,
            float baseline, Paint.FontMetrics metrics) {
        if (carousel.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (carousel.cycleStartedAt == 0L) carousel.cycleStartedAt = now;

        int currentIndex = Math.min(carousel.index, carousel.items.size() - 1);
        String currentItem = carousel.items.get(currentIndex);
        long elapsed = now - carousel.cycleStartedAt;
        long displayDuration = weatherDetailDisplayDuration(currentItem);

        if (carousel.items.size() == 1) {
            if (elapsed >= displayDuration) {
                carousel.cycleStartedAt = now;
                elapsed = 0L;
            }
            drawWeatherDetailItem(canvas, currentItem, null, centerX,
                baseline, metrics, 0f, elapsed);
            if (measureSupportingText(currentItem) > weatherDetailAvailableWidth()) {
                postInvalidateDelayed(WEATHER_DETAIL_FRAME_DELAY_MILLIS);
            }
            return;
        }

        long fadeOutEnd = displayDuration + WEATHER_DETAIL_TRANSITION_MILLIS;
        long fadeInEnd = fadeOutEnd + WEATHER_DETAIL_TRANSITION_MILLIS;

        int nextIndex = (carousel.index + 1) % carousel.items.size();
        String nextItem = carousel.items.get(nextIndex);

        if (elapsed < displayDuration) {
            drawWeatherDetailItem(canvas, currentItem, null, centerX,
                baseline, metrics, 0f, elapsed);
        } else if (elapsed < fadeOutEnd) {
            float progress = (float) (elapsed - displayDuration)
                / WEATHER_DETAIL_TRANSITION_MILLIS;
            drawWeatherDetailItem(canvas, currentItem, nextItem, centerX,
                baseline, metrics, progress * 0.5f, displayDuration);
        } else if (elapsed < fadeInEnd) {
            float progress = (float) (elapsed - fadeOutEnd)
                / WEATHER_DETAIL_TRANSITION_MILLIS;
            drawWeatherDetailItem(canvas, currentItem, nextItem, centerX,
                baseline, metrics, 0.5f + progress * 0.5f, 0L);
        } else {
            carousel.index = nextIndex;
            carousel.cycleStartedAt = now;
            drawWeatherDetailItem(canvas, nextItem, null, centerX,
                baseline, metrics, 0f, 0L);
        }

        postInvalidateDelayed(WEATHER_DETAIL_FRAME_DELAY_MILLIS);
    }

    /**
     * Draws the carousel line. {@code progress} runs across the whole transition where
     * {@code [0, 0.5)} fades out {@code current} and {@code [0.5, 1]} brings in {@code next};
     * outside a transition it is 0 and only {@code current} is drawn at full strength.
     */
    private void drawWeatherDetailItem(Canvas canvas, String current, String next,
            float centerX, float baseline, Paint.FontMetrics metrics, float progress,
            long itemElapsed) {
        boolean transitioning = next != null;
        boolean useTimeTransition =
            com.clockmods.BuildConfig.WEATHER_DETAIL_USES_TIME_TRANSITION;
        if (!transitioning) {
            drawWeatherDetailLine(canvas, current, centerX, baseline, metrics, 255,
                ClockPreferences.TRANSITION_FADE, 0f, itemElapsed);
            return;
        }
        String transition = useTimeTransition ? timeTransition : ClockPreferences.TRANSITION_FADE;
        if (progress < 0.5f) {
            float outProgress = progress / 0.5f;
            drawWeatherDetailLine(canvas, current, centerX, baseline, metrics,
                Math.round(255f * (1f - outProgress)), transition, outProgress, itemElapsed);
        } else {
            float inProgress = (progress - 0.5f) / 0.5f;
            drawWeatherDetailLine(canvas, next, centerX, baseline, metrics,
                Math.round(255f * inProgress), transition, -(1f - inProgress), itemElapsed);
        }
    }

    /**
     * Renders one carousel line honouring the transition style. {@code phase} encodes the
     * spatial offset for motion styles: negative means leaving (old item), positive means
     * entering (new item), 0 means settled.
     */
    private void drawWeatherDetailLine(Canvas canvas, String text, float centerX,
            float baseline, Paint.FontMetrics metrics, int alpha, String transition, float phase,
            long itemElapsed) {
        if (text == null || text.length() == 0) return;
        int originalAlpha = datePaint.getAlpha();
        applySupportingTypeface(text);
        float clampedAlpha = Math.max(0, Math.min(255, alpha));
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(transition)
                || ClockPreferences.TRANSITION_SLIDE_DOWN.equals(transition)) {
            float direction = ClockPreferences.TRANSITION_SLIDE_UP.equals(transition) ? -1f : 1f;
            float distance = datePaint.getTextSize() * 0.6f;
            canvas.save();
            canvas.translate(0f, direction * distance * phase);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawScrollingWeatherText(canvas, text, centerX, baseline, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_SCALE.equals(transition)) {
            float scale = 1f - 0.12f * Math.abs(phase);
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(scale, scale, centerX, pivotY);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawScrollingWeatherText(canvas, text, centerX, baseline, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_FLIP.equals(transition)) {
            float scaleY = Math.max(0.05f, 1f - Math.abs(phase));
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(1f, scaleY, centerX, pivotY);
            datePaint.setAlpha(originalAlpha);
            drawScrollingWeatherText(canvas, text, centerX, baseline, itemElapsed);
            canvas.restore();
        } else {
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
                drawScrollingWeatherText(canvas, text, centerX, baseline, itemElapsed);
        }
        datePaint.setAlpha(originalAlpha);
    }

            private long weatherDetailDisplayDuration(String text) {
            float overflow = measureSupportingText(text) - weatherDetailAvailableWidth();
            if (overflow <= 0f) return WEATHER_DETAIL_HOLD_MILLIS;
            float speed = WEATHER_DETAIL_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
            long scrollMillis = (long) Math.ceil(overflow / speed * 1000f);
            return Math.max(WEATHER_DETAIL_HOLD_MILLIS,
                WEATHER_DETAIL_SCROLL_PAUSE_MILLIS * 2L + scrollMillis);
            }

            private float weatherDetailAvailableWidth() {
            float padding = WEATHER_DETAIL_HORIZONTAL_PADDING_DP
                * getResources().getDisplayMetrics().density;
            return Math.max(1f, getWidth() - padding * 2f);
            }

            private void drawScrollingWeatherText(Canvas canvas, String text, float centerX,
                float baseline, long elapsed) {
            float textWidth = measureSupportingText(text);
            float availableWidth = weatherDetailAvailableWidth();
            if (textWidth <= availableWidth) {
                drawSupportingText(canvas, text, centerX, baseline, Paint.Align.CENTER);
                return;
            }

            float left = centerX - availableWidth / 2f;
            float overflow = textWidth - availableWidth;
            float speed = WEATHER_DETAIL_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
            long scrollMillis = (long) Math.ceil(overflow / speed * 1000f);
            float progress = Math.max(0f, Math.min(1f,
                (elapsed - WEATHER_DETAIL_SCROLL_PAUSE_MILLIS) / (float) scrollMillis));
            canvas.save();
            canvas.clipRect(left, 0f, left + availableWidth, getHeight());
            drawSupportingText(canvas, text, left - overflow * progress, baseline, Paint.Align.LEFT);
            canvas.restore();
            }

    private float measureSupportingText(String text) {
        if (text == null || text.length() == 0) return 0f;
        int characterCount = text.codePointCount(0, text.length());
        Typeface originalTypeface = datePaint.getTypeface();
        float width = 0f;
        for (int start = 0; start < text.length();) {
            int codePoint = text.codePointAt(start);
            int end = start + Character.charCount(codePoint);
            datePaint.setTypeface(supportingTypefaceFor(codePoint));
            width += datePaint.measureText(text, start, end);
            start = end;
        }
        datePaint.setTypeface(originalTypeface);
        return width
            + Math.max(0, characterCount - 1)
                * datePaint.getTextSize() * SUPPORTING_TEXT_LETTER_SPACING;
    }

    /**
     * Rotating-line carousel state: which item is showing and when its cycle began.
     * Two instances drive the detailed-weather line and the main weather/message line,
     * sharing the identical timing, transition and horizontal-scroll behaviour.
     */
    private static final class Carousel {
        java.util.List<String> items;
        int index;
        long cycleStartedAt;

        void setItems(java.util.List<String> newItems) {
            items = newItems;
            index = 0;
            cycleStartedAt = 0L;
        }

        boolean isEmpty() {
            return items == null || items.isEmpty();
        }
    }

    private void drawSupportingText(Canvas canvas, String text, float x, float baseline,
            Paint.Align align) {
        if (text == null || text.length() == 0) return;
        float width = measureSupportingText(text);
        float cursor = align == Paint.Align.CENTER ? x - width / 2f
            : align == Paint.Align.RIGHT ? x - width : x;
        Paint.Align originalAlign = datePaint.getTextAlign();
        Typeface originalTypeface = datePaint.getTypeface();
        datePaint.setTextAlign(Paint.Align.LEFT);
        float spacing = datePaint.getTextSize() * SUPPORTING_TEXT_LETTER_SPACING;
        for (int start = 0; start < text.length();) {
            int codePoint = text.codePointAt(start);
            int end = start + Character.charCount(codePoint);
            datePaint.setTypeface(supportingTypefaceFor(codePoint));
            canvas.drawText(text, start, end, cursor, baseline, datePaint);
            cursor += datePaint.measureText(text, start, end);
            if (end < text.length()) cursor += spacing;
            start = end;
        }
        datePaint.setTypeface(originalTypeface);
        datePaint.setTextAlign(originalAlign);
    }

    private Typeface supportingTypefaceFor(int codePoint) {
        return ClockTypefaceResolver.resolveSupportingForCodePoint(
            getContext(), backgroundRepository.getFontFamily(), boldText, codePoint);
    }

    private void applyTextStyles() {
        if (backgroundRepository == null) {
            return;
        }
        timeFontScale = backgroundRepository.getTimeFontScale();
        dateFontScale = backgroundRepository.getDateFontScale();
        timePaint.setColor(backgroundRepository.getTimeColor());
        secondsPaint.setColor(backgroundRepository.getTimeColor());
        periodPaint.setColor(backgroundRepository.getTimeColor());
        datePaint.setColor(backgroundRepository.getDateColor());
        blinkColon = backgroundRepository.isBlinkColon();
        animateTimeChanges = backgroundRepository.isAnimateTimeChanges();
        timeTransition = backgroundRepository.getTimeTransition();
        boldText = backgroundRepository.isBoldText();
        String fontFamily = backgroundRepository.getFontFamily();
        Typeface timeTypeface = ClockTypefaceResolver.resolveTime(getContext(), fontFamily, boldText);
        timePaint.setTypeface(timeTypeface);
        secondsPaint.setTypeface(timeTypeface);
        periodPaint.setTypeface(timeTypeface);
        datePaint.setTypeface(ClockTypefaceResolver.resolveSupporting(
            getContext(), fontFamily, boldText, true));
        showSeconds = backgroundRepository.isShowSeconds();
        showLunar = backgroundRepository.isShowLunar();
        smallSeconds = backgroundRepository.isSmallSeconds();
        use24Hour = backgroundRepository.isUse24Hour();
        clockUseEnglish = backgroundRepository.isClockUseEnglish();
        dateLang = LocaleManager.dateLang(backgroundRepository.getClockLanguage());
        customMessage = backgroundRepository.getCustomMessage();
        datePatternCn = backgroundRepository.getDatePatternCn();
        datePatternEn = backgroundRepository.getDatePatternEn();
        portraitStacked = backgroundRepository.isPortraitStacked();
        dateLunarDualLine = backgroundRepository.isDateLunarDualLine();
        NetworkTimeProvider timeProvider = networkTimeProvider;
        if (timeProvider != null) {
            timeProvider.setEnabled(backgroundRepository.isUseNetworkTime());
            timeProvider.setSyncIntervalMinutes(backgroundRepository.getSyncIntervalMinutes());
        }
    }

    private void applySupportingTypeface(String text) {
        datePaint.setTypeface(ClockTypefaceResolver.resolveSupporting(
                getContext(), backgroundRepository.getFontFamily(), boldText, containsChinese(text)));
    }

    static boolean containsChinese(String text) {
        if (text == null) {
            return false;
        }
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private java.util.TimeZone resolveTimeZone() {
        if (backgroundRepository == null) {
            return java.util.TimeZone.getDefault();
        }
        String zoneId = backgroundRepository.getTimeZoneId();
        if (zoneId == null || zoneId.length() == 0) {
            return java.util.TimeZone.getDefault();
        }
        return java.util.TimeZone.getTimeZone(zoneId);
    }

    private boolean shouldDimBackground(Calendar now) {
        if (backgroundRepository == null
                || !ClockPreferences.MODE_IMAGE.equals(backgroundRepository.getBackgroundMode())) {
            return false;
        }
        if (backgroundRepository.isDimBackground()) {
            return true;
        }
        if (!backgroundRepository.isScheduleDimBackground()) {
            return false;
        }
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return BackgroundDimSchedule.isActive(currentMinutes,
                backgroundRepository.getDimStartMinutes(),
                backgroundRepository.getDimEndMinutes());
    }

    private static String longerOf(String a, String b) {
        return a.length() >= b.length() ? a : b;
    }

    /**
     * Returns the pixel text size the main clock time is rendered at for the given
     * view size, font scale and {@code displayTime}. This mirrors the measurement
     * performed in {@link #onDraw} so overlays (e.g. the hourly chime) can match the
     * clock's time font size exactly. {@code timeTypeface} must be the same face the
     * clock uses so digit widths measure identically.
     */
    public static float measureTimeTextSize(int width, int height, float timeFontScale,
            ClockTimeFormatter.DisplayTime displayTime, Typeface timeTypeface) {
        Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Paint secondsPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Paint periodPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        mainPaint.setTypeface(timeTypeface);
        secondsPaint.setTypeface(timeTypeface);
        periodPaint.setTypeface(timeTypeface);
        mainPaint.setTextSize(1f);
        secondsPaint.setTextSize(0.6f);
        periodPaint.setTextSize(0.3f);
        float gap = mainPaint.measureText(" ") * SMALL_SECONDS_GAP_SPACE_FRACTION;
        float measuredMainWidth = ClockTextLayout.stableTextWidth(displayTime.mainText, mainPaint);
        float leftAccessoryWidth = displayTime.hasPeriod()
                ? gap + periodPaint.measureText(displayTime.periodText) : 0f;
        float rightAccessoryWidth = displayTime.hasSmallSeconds()
                ? gap + ClockTextLayout.stableTextWidth(displayTime.secondsText, secondsPaint) : 0f;
        float measuredTimeWidth = ClockLayoutCalculator.calculateTimeGroupWidth(
                measuredMainWidth, leftAccessoryWidth, rightAccessoryWidth);
        return ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, measuredTimeWidth, timeFontScale,
                TIME_HEIGHT_FRACTION, TIME_MAX_WIDTH_FRACTION);
    }

    private void drawAnimatedTime(Canvas canvas, ClockTimeFormatter.DisplayTime nextTime,
            float centerX, float mainBaseline) {
        if (!animateTimeChanges) {
            displayedTime = nextTime;
            previousTime = null;
            drawTime(canvas, nextTime, centerX, mainBaseline, 255);
            return;
        }

        if (displayedTime == null) {
            displayedTime = nextTime;
        } else if (!sameDisplayTime(displayedTime, nextTime)) {
            previousTime = displayedTime;
            displayedTime = nextTime;
            timeTransitionStartedAt = SystemClock.uptimeMillis();
        }

        if (previousTime == null) {
            drawTime(canvas, displayedTime, centerX, mainBaseline, 255);
            return;
        }

        float progress = Math.min(1f, (SystemClock.uptimeMillis() - timeTransitionStartedAt)
                / (float) TIME_TRANSITION_DURATION_MILLIS);
        drawConfiguredTimeTransition(canvas, previousTime, displayedTime,
            centerX, mainBaseline, progress);
        if (progress < 1f) {
            postInvalidateDelayed(16L);
        } else {
            previousTime = null;
        }
    }

    private void drawConfiguredTimeTransition(Canvas canvas,
            ClockTimeFormatter.DisplayTime oldTime, ClockTimeFormatter.DisplayTime newTime,
            float centerX, float mainBaseline, float progress) {
        float eased = 1f - (float) Math.pow(1f - progress, 3);
        drawTimeTransition(canvas, oldTime, newTime, centerX, mainBaseline, eased);
    }

    private static boolean sameDisplayTime(ClockTimeFormatter.DisplayTime first,
            ClockTimeFormatter.DisplayTime second) {
        return first.mainText.equals(second.mainText)
            && first.secondsText.equals(second.secondsText)
            && first.periodText.equals(second.periodText)
            && first.colonVisible == second.colonVisible;
    }

    private void drawTimeTransition(Canvas canvas, ClockTimeFormatter.DisplayTime oldTime,
            ClockTimeFormatter.DisplayTime newTime, float centerX, float mainBaseline, float progress) {
        if (oldTime.hasSmallSeconds() != newTime.hasSmallSeconds()
                || oldTime.hasPeriod() != newTime.hasPeriod()
                || oldTime.mainText.length() != newTime.mainText.length()
                || oldTime.secondsText.length() != newTime.secondsText.length()
                || oldTime.periodText.length() != newTime.periodText.length()) {
            drawTime(canvas, newTime, centerX, mainBaseline, 255);
            return;
        }

        float mainWidth = stableTextWidth(newTime.mainText, timePaint);
        float gapWidth = smallSecondsGapWidth();
        drawTextTransition(canvas, oldTime.mainText, newTime.mainText,
            centerX, mainBaseline, timePaint, progress,
            oldTime.colonVisible, newTime.colonVisible);
        if (newTime.hasPeriod()) {
            float periodWidth = periodPaint.measureText(newTime.periodText);
            drawTextTransition(canvas, oldTime.periodText, newTime.periodText,
                    centerX - mainWidth / 2f - gapWidth - periodWidth / 2f,
                bottomAlignedBaseline(mainBaseline, timePaint, periodPaint),
                periodPaint, progress, true, true);
        }
        if (newTime.hasSmallSeconds()) {
            float secondsWidth = stableTextWidth(newTime.secondsText, secondsPaint);
            drawTextTransition(canvas, oldTime.secondsText, newTime.secondsText,
                    centerX + mainWidth / 2f + gapWidth + secondsWidth / 2f,
                bottomAlignedBaseline(mainBaseline, timePaint, secondsPaint),
                secondsPaint, progress, true, true);
        }
    }

    private void drawTextTransition(Canvas canvas, String oldText, String newText,
            float centerX, float baseline, Paint paint, float progress,
            boolean oldColonsVisible, boolean newColonsVisible) {
        int originalAlpha = paint.getAlpha();
        float digitWidth = widestDigitWidth(paint);
        float cursor = centerX - stableTextWidth(newText, paint, digitWidth) / 2f;
        for (int index = 0; index < newText.length(); index++) {
            String newCharacter = newText.substring(index, index + 1);
            String oldCharacter = oldText.substring(index, index + 1);
            float characterWidth = Math.max(
                stableCharacterWidth(newCharacter, paint, digitWidth),
                stableCharacterWidth(oldCharacter, paint, digitWidth));
            float characterCenter = cursor + characterWidth / 2f;
            boolean colonVisibilityChanged = ":".equals(newCharacter)
                    && oldColonsVisible != newColonsVisible;
            if (newCharacter.equals(oldCharacter) && !colonVisibilityChanged) {
                if (":".equals(newCharacter) && !newColonsVisible) {
                    cursor += characterWidth;
                    continue;
                }
                setClockTextAlpha(paint, 255);
                canvas.drawText(newCharacter, characterCenter,
                    alignedCharacterBaseline(newCharacter, baseline, paint), paint);
            } else if (colonVisibilityChanged) {
                int colonAlpha = newColonsVisible
                        ? Math.round(255f * progress)
                        : Math.round(255f * (1f - progress));
                setClockTextAlpha(paint, colonAlpha);
                canvas.drawText(newCharacter, characterCenter,
                        alignedCharacterBaseline(newCharacter, baseline, paint), paint);
            } else {
                drawChangedCharacterTransition(canvas, oldCharacter, newCharacter,
                    characterCenter, baseline, paint, progress);
            }
            cursor += characterWidth;
        }
        setClockTextAlpha(paint, originalAlpha);
    }

            private void drawChangedCharacterTransition(Canvas canvas, String oldCharacter,
                String newCharacter, float centerX, float baseline, Paint paint, float progress) {
            float pivotY = baseline - paint.getTextSize() / 2f;
            if (ClockPreferences.TRANSITION_SLIDE_UP.equals(timeTransition)
                || ClockPreferences.TRANSITION_SLIDE_DOWN.equals(timeTransition)) {
                float direction = ClockPreferences.TRANSITION_SLIDE_UP.equals(timeTransition) ? -1f : 1f;
                float distance = paint.getTextSize() * 0.24f;
                drawTransformedCharacter(canvas, oldCharacter, centerX, baseline, paint,
                    Math.round(255f * (1f - progress)), 1f,
                    direction * distance * progress, pivotY);
                drawTransformedCharacter(canvas, newCharacter, centerX, baseline, paint,
                    Math.round(255f * progress), 1f,
                    -direction * distance * (1f - progress), pivotY);
                return;
            }
            if (ClockPreferences.TRANSITION_SCALE.equals(timeTransition)) {
                drawTransformedCharacter(canvas, oldCharacter, centerX, baseline, paint,
                    Math.round(255f * (1f - progress)), 1f + 0.08f * progress, 0f, pivotY);
                drawTransformedCharacter(canvas, newCharacter, centerX, baseline, paint,
                    Math.round(255f * progress), 0.88f + 0.12f * progress, 0f, pivotY);
                return;
            }
            if (ClockPreferences.TRANSITION_FLIP.equals(timeTransition)) {
                if (progress < 0.5f) {
                drawTransformedCharacter(canvas, oldCharacter, centerX, baseline, paint,
                    255, Math.max(0.05f, 1f - progress * 2f), 0f, pivotY);
                } else {
                drawTransformedCharacter(canvas, newCharacter, centerX, baseline, paint,
                    255, Math.max(0.05f, (progress - 0.5f) * 2f), 0f, pivotY);
                }
                return;
            }
            setClockTextAlpha(paint, Math.round(255f * (1f - progress)));
            canvas.drawText(oldCharacter, centerX,
                alignedCharacterBaseline(oldCharacter, baseline, paint), paint);
            setClockTextAlpha(paint, Math.round(255f * progress));
            canvas.drawText(newCharacter, centerX,
                alignedCharacterBaseline(newCharacter, baseline, paint), paint);
            }

            private void drawTransformedCharacter(Canvas canvas, String character,
                float centerX, float baseline, Paint paint, int alpha, float scaleY,
                float translateY, float pivotY) {
            setClockTextAlpha(paint, alpha);
            canvas.save();
            canvas.translate(0f, translateY);
            canvas.scale(1f, scaleY, centerX, pivotY);
            canvas.drawText(character, centerX,
                alignedCharacterBaseline(character, baseline, paint), paint);
            canvas.restore();
            }

    private void drawTime(Canvas canvas, ClockTimeFormatter.DisplayTime displayTime,
            float centerX, float mainBaseline, int alpha) {
        int oldTimeAlpha = timePaint.getAlpha();
        int oldSecondsAlpha = secondsPaint.getAlpha();
        int oldPeriodAlpha = periodPaint.getAlpha();
        setClockTextAlpha(timePaint, alpha);
        setClockTextAlpha(secondsPaint, alpha);
        setClockTextAlpha(periodPaint, alpha);
        if (!displayTime.hasSmallSeconds() && !displayTime.hasPeriod()) {
            drawMainTimeText(canvas, displayTime.mainText, centerX, mainBaseline,
                    displayTime.colonVisible);
            setClockTextAlpha(timePaint, oldTimeAlpha);
            setClockTextAlpha(secondsPaint, oldSecondsAlpha);
            setClockTextAlpha(periodPaint, oldPeriodAlpha);
            return;
        }

        float mainWidth = stableTextWidth(displayTime.mainText, timePaint);
        float gapWidth = smallSecondsGapWidth();
        drawMainTimeText(canvas, displayTime.mainText, centerX, mainBaseline,
            displayTime.colonVisible);
        if (displayTime.hasPeriod()) {
            float periodWidth = periodPaint.measureText(displayTime.periodText);
            canvas.drawText(displayTime.periodText,
                centerX - mainWidth / 2f - gapWidth - periodWidth / 2f,
                bottomAlignedBaseline(mainBaseline, timePaint, periodPaint), periodPaint);
        }
        if (displayTime.hasSmallSeconds()) {
            float secondsWidth = stableTextWidth(displayTime.secondsText, secondsPaint);
            drawStableText(canvas, displayTime.secondsText,
                centerX + mainWidth / 2f + gapWidth + secondsWidth / 2f,
                bottomAlignedBaseline(mainBaseline, timePaint, secondsPaint), secondsPaint);
        }
        setClockTextAlpha(timePaint, oldTimeAlpha);
        setClockTextAlpha(secondsPaint, oldSecondsAlpha);
        setClockTextAlpha(periodPaint, oldPeriodAlpha);
    }

    private void drawMainTimeText(Canvas canvas, String text, float centerX, float baseline,
            boolean colonsVisible) {
        float digitWidth = widestDigitWidth(timePaint);
        float cursor = centerX - stableTextWidth(text, timePaint, digitWidth) / 2f;
        for (int index = 0; index < text.length(); index++) {
            String character = text.substring(index, index + 1);
            float characterWidth = stableCharacterWidth(character, timePaint, digitWidth);
            if (colonsVisible || !":".equals(character)) {
                canvas.drawText(character, cursor + characterWidth / 2f,
                    alignedCharacterBaseline(character, baseline, timePaint), timePaint);
            }
            cursor += characterWidth;
        }
    }

    private static void drawStableText(Canvas canvas, String text, float centerX,
            float baseline, Paint paint) {
        float digitWidth = widestDigitWidth(paint);
        float cursor = centerX - stableTextWidth(text, paint, digitWidth) / 2f;
        for (int index = 0; index < text.length(); index++) {
            String character = text.substring(index, index + 1);
            float characterWidth = stableCharacterWidth(character, paint, digitWidth);
            canvas.drawText(character, cursor + characterWidth / 2f, baseline, paint);
            cursor += characterWidth;
        }
    }

    private static float stableTextWidth(String text, Paint paint) {
        return ClockTextLayout.stableTextWidth(text, paint);
    }

    private static float stableTextWidth(String text, Paint paint, float digitWidth) {
        return ClockTextLayout.stableTextWidth(text, paint, digitWidth);
    }

    private static float stableCharacterWidth(String character, Paint paint, float digitWidth) {
        return ClockTextLayout.stableCharacterWidth(character, paint, digitWidth);
    }

    private static float widestDigitWidth(Paint paint) {
        return ClockTextLayout.widestDigitWidth(paint);
    }

    private float alignedCharacterBaseline(String character, float baseline, Paint paint) {
        return paint == timePaint
                ? ClockTextLayout.alignedCharacterBaseline(character, baseline, paint) : baseline;
    }

    private void setClockTextAlpha(Paint paint, int alpha) {
        paint.setAlpha(alpha);
        int shadowAlpha = Math.round(CLOCK_SHADOW_ALPHA * alpha / 255f);
        paint.setShadowLayer(clockShadowRadius, 0f, clockShadowDy, shadowAlpha << 24);
    }

    private static float bottomAlignedBaseline(float mainBaseline, Paint mainPaint,
            Paint accessoryPaint) {
        return ClockTextLayout.bottomAlignedBaseline(mainBaseline, mainPaint, accessoryPaint);
    }

    private float smallSecondsGapWidth() {
        return timePaint.measureText(" ") * SMALL_SECONDS_GAP_SPACE_FRACTION;
    }

    private void drawBackgroundImage(Canvas canvas, int width, int height) {
        if (backgroundBitmap == null || backgroundBitmap.isRecycled()) {
            return;
        }
        float scale = ClockLayoutCalculator.centerCropScale(
                backgroundBitmap.getWidth(), backgroundBitmap.getHeight(), width, height);
        float translatedX = (width - backgroundBitmap.getWidth() * scale) / 2f;
        float translatedY = (height - backgroundBitmap.getHeight() * scale) / 2f;
        bitmapMatrix.reset();
        bitmapMatrix.setScale(scale, scale);
        bitmapMatrix.postTranslate(translatedX, translatedY);
        canvas.drawBitmap(backgroundBitmap, bitmapMatrix, bitmapPaint);
    }

    private void replaceBackgroundBitmap(Bitmap bitmap, int width, int height) {
        if (backgroundBitmap != null && backgroundBitmap != bitmap && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        backgroundBitmap = bitmap;
        loadedWidth = width;
        loadedHeight = height;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        synchronized (workerLock) {
            attached = true;
            imageExecutor = Executors.newSingleThreadExecutor();
            networkTimeProvider = new NetworkTimeProvider();
        }
        requestBackgroundReload();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        ExecutorService executor;
        NetworkTimeProvider timeProvider;
        synchronized (workerLock) {
            attached = false;
            executor = imageExecutor;
            imageExecutor = null;
            timeProvider = networkTimeProvider;
            networkTimeProvider = null;
        }
        if (executor != null) executor.shutdownNow();
        if (timeProvider != null) timeProvider.shutdown();
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        super.onDetachedFromWindow();
    }
}