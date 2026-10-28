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

import com.clockmods.background.BackgroundRepository;
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
    private static final float TIME_HEIGHT_FRACTION = 0.55f;
    private static final float TIME_MAX_WIDTH_FRACTION = 0.98f;
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
    // Cycle timing for the single weather summary line (shared with the detail line).
    private final Carousel mainLineCarousel = new Carousel();
    // Weather-icon styling (Pro exposes these; other flavours keep fill + date colour).
    private int weatherIconColor = ClockPreferences.TEXT_COLOR;
    private final Paint weatherIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        weatherDetailCarousel.setItems(wrapPlain(
                (state != null && state.data != null && state.data.detail != null)
                        ? state.data.detail.carouselItems(detailLabels()) : null));
        invalidate();
    }

    /** Wraps a list of plain strings as icon-less carousel items. */
    private static java.util.List<WeatherLineItem> wrapPlain(java.util.List<String> src) {
        if (src == null) return null;
        java.util.List<WeatherLineItem> out = new java.util.ArrayList<>(src.size());
        for (String s : src) out.add(WeatherLineItem.plain(s));
        return out;
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
        canvas.drawColor(ClockPreferences.BACKGROUND_COLOR);
        drawBackgroundImage(canvas, width, height);

        java.util.TimeZone timeZone = resolveTimeZone();
        Calendar now = Calendar.getInstance(timeZone);
        NetworkTimeProvider timeProvider = networkTimeProvider;
        now.setTimeInMillis(timeProvider == null
            ? System.currentTimeMillis() : timeProvider.currentTimeMillis());
        if (shouldDimBackground()) {
            canvas.drawColor(DIM_BACKGROUND_OVERLAY_COLOR);
        }

        applyTextStyles();

        ClockTimeFormatter.DisplayTime displayTime = ClockTimeFormatter.format(
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND),
            showSeconds, blinkColon, smallSeconds, use24Hour);
        String dateText = DateFormatter.format(now);
        String lunarText = showLunar ? LunarCalendar.format(now) : "";

        // Portrait stacked layout is a fully separate path; landscape is untouched.
        if (height >= width && portraitStacked) {
            drawStackedPortrait(canvas, now, dateText, lunarText, width, height);
            return;
        }

        String fullDate = lunarText.length() == 0 ? dateText : dateText + " " + lunarText;

        // Portrait always stacks the date and lunar lines; landscape shares a single line
        // unless the user opts into two rows.
        boolean singleDateLine = lunarText.length() == 0 || (width > height && !dateLunarDualLine);
        // Font size: landscape sizes against the full single-line string whether or not the
        // two-row layout is enabled, so toggling it never changes the font size. Portrait
        // always stacks, so it sizes against the longer of the two rows to fill its narrow width.
        boolean portraitStack = height >= width && lunarText.length() > 0;
        String widestDateText = portraitStack ? longerOf(dateText, lunarText) : fullDate;
        applySupportingTypeface();

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
        datePaint.setTextSize(1f);
        // TEMP (compat verification build): layout protection is disabled, so the date line
        // is never capped against the time block — oversized text may overlap or leave the screen.
        datePaint.setTextSize(dateSize);

        float centerX = width / 2f;
        float timeCenterX = centerX + ClockLayoutCalculator.calculateMainCenterOffset(
            leftAccessoryWidth * timeSize, rightAccessoryWidth * timeSize);
        float centerY = height / 2f;
        Paint.FontMetrics timeMetrics = timePaint.getFontMetrics();
        Paint.FontMetrics dateMetrics = datePaint.getFontMetrics();
        float timeBaseline = centerY - (timeMetrics.ascent + timeMetrics.descent) / 2f;
        boolean portrait = height >= width;
        float gapFactor = portrait ? 0.40f : 0.10f;
        float gap = Math.min(height * .014f, dateSize * gapFactor);
        // Portrait stacks date/lunar and weather/detail as two rows each, so give those
        // rows more breathing room than a plain line (landscape keeps the tight default).
        float weatherDetailScale = portrait ? 1.35f : 1f;                        // 天气↔详细 行距倍数（竖屏更宽）
        // 日期↔农历 的基线间距与 天气↔详细 完全一致：同字号、同公式、同倍数。
        float lunarRowGap = (dateMetrics.descent - dateMetrics.ascent + dateMetrics.descent)
                * weatherDetailScale;

        drawAnimatedTime(canvas, displayTime, timeCenterX, timeBaseline);
        drawWeather(canvas, centerX, timeBaseline, dateSize, timeMetrics, gap, weatherDetailScale);
        drawDateBlock(canvas, dateText, lunarText, singleDateLine, lunarRowGap);
    }

    /** Date and lunar rows share a fixed top-left origin in every clock layout. */
    private void drawDateBlock(Canvas canvas, String dateText, String lunarText,
            boolean singleLine, float rowGap) {
        float density = getResources().getDisplayMetrics().density;
        float left = getPaddingLeft() + 20f * density;
        float top = getPaddingTop() + 16f * density;
        String firstLine = singleLine && lunarText.length() > 0
                ? dateText + " " + lunarText : dateText;
        applySupportingTypeface();
        float baseline = top - datePaint.getFontMetrics().top;
        drawSupportingText(canvas, firstLine, left, baseline, Paint.Align.LEFT);
        if (!singleLine && lunarText.length() > 0) {
            drawSupportingText(canvas, lunarText, left, baseline + rowGap, Paint.Align.LEFT);
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
        applySupportingTypeface();
        float dateSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, measureSupportingText(widestDate), dateFontScale, 0.08f, 0.92f);
        datePaint.setTextSize(dateSize);
        Paint.FontMetrics dateMetrics = datePaint.getFontMetrics();
        float dateLineHeight = dateMetrics.descent - dateMetrics.ascent;
        float lunarGap = dateLineHeight * 1.5f;       // wider date <-> lunar row spacing
        float detailGapScale = 1.35f;                 // wider weather <-> detail row spacing
        boolean hasLunar = lunarText.length() > 0;
        // The bottom supporting line is shown only when weather is enabled.
        boolean weatherShown = backgroundRepository != null
                && backgroundRepository.isWeatherEnabled();
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
        // TEMP compat verification build: the digit column is no longer capped against the
        // surrounding date/weather rows, so oversized digits may cover them or leave the screen.
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

        drawDateBlock(canvas, dateText, lunarText, false, lunarGap);

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
        if (backgroundRepository == null || !backgroundRepository.isWeatherEnabled()) return;

        // The supporting line hosts the weather summary (location + icon + condition); it is
        // always drawn at the user's chosen size — overflow scrolls instead of shrinking.
        WeatherLineItem weatherItem = null;
        if (weatherState != null) {
            if (weatherState.data != null) {
                String leftText = WeatherModels.locationText(
                        weatherState.data.city, weatherState.data.district);
                String rightText = weatherState.data.text + " " + weatherState.data.temperature + "℃";
                WeatherIcon icon = WeatherIcon.load(getContext(), weatherState.data.icon);
                weatherItem = icon != null
                        ? WeatherLineItem.weather(leftText, rightText, icon)
                        : WeatherLineItem.plain(leftText + "  " + rightText);
            } else if (weatherState.message != null && weatherState.message.length() > 0) {
                weatherItem = WeatherLineItem.plain(weatherState.message);
            }
        }
        if (weatherItem == null) return;

        float originalSize = datePaint.getTextSize();
        Typeface originalTypeface = datePaint.getTypeface();

        // Fixed size (never shrinks); overflow is handled by marquee scrolling.
        datePaint.setTextSize(dateSize);
        applySupportingTypeface();
        Paint.FontMetrics weatherMetrics = datePaint.getFontMetrics();
        float baseline = timeBaseline + timeMetrics.descent + gap - weatherMetrics.ascent;

        drawWeatherDetailItem(canvas, weatherItem, null, centerX, baseline,
            weatherMetrics, 0f, mainCarouselElapsed(weatherItem));
        if (itemWidth(weatherItem) > weatherDetailAvailableWidth()) {
            postInvalidateDelayed(WEATHER_DETAIL_FRAME_DELAY_MILLIS);
        }

        drawWeatherDetail(canvas, centerX, baseline, weatherMetrics, detailGapScale);
        datePaint.setTextSize(originalSize);
        datePaint.setTypeface(originalTypeface);
    }

    /** Horizontal width an item occupies, reserving icon + gaps for weather-summary items. */
    private float itemWidth(WeatherLineItem item) {
        if (!item.hasIcon()) return measureSupportingText(item.text);
        float ts = datePaint.getTextSize();
        float iconSize = ts * 0.95f;
        float iconGap = ts * 0.25f;
        return measureSupportingText(item.left) + measureSupportingText(item.right)
            + iconSize + iconGap * 2f;
    }

    /**
     * Draws a weather-summary item as one horizontal run [location][icon][condition],
     * centered, marquee-scrolling as a whole when it overflows the available width. Mirrors
     * {@link #drawScrollingWeatherText} so both overflow behaviours stay identical.
     */
    private void drawScrollingWeatherSummary(Canvas canvas, WeatherLineItem item,
            float centerX, float baseline, Paint.FontMetrics metrics, long elapsed) {
        float ts = datePaint.getTextSize();
        float iconSize = ts * 0.95f;
        float iconGap = ts * 0.25f;
        float leftWidth = measureSupportingText(item.left);
        float rightWidth = measureSupportingText(item.right);
        float total = leftWidth + rightWidth + iconSize + iconGap * 2f;
        float available = weatherDetailAvailableWidth();
        boolean clip = total > available;
        float startX;
        if (clip) {
            float overflow = total - available;
            float speed = WEATHER_DETAIL_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
            long scrollMillis = (long) Math.ceil(overflow / speed * 1000f);
            float progress = Math.max(0f, Math.min(1f,
                (elapsed - WEATHER_DETAIL_SCROLL_PAUSE_MILLIS) / (float) scrollMillis));
            float clipLeft = centerX - available / 2f;
            canvas.save();
            canvas.clipRect(clipLeft, 0f, clipLeft + available, getHeight());
            startX = clipLeft - overflow * progress;
        } else {
            startX = centerX - total / 2f;
        }
        float cursor = startX;
        drawSupportingText(canvas, item.left, cursor, baseline, Paint.Align.LEFT);
        cursor += leftWidth + iconGap;
        float iconTop = baseline + (metrics.ascent + metrics.descent) / 2f - iconSize / 2f;
        weatherIconPaint.setColor(weatherIconColor);
        weatherIconPaint.setAlpha(datePaint.getAlpha());
        item.icon.draw(canvas, cursor, iconTop, iconSize, weatherIconPaint);
        cursor += iconSize + iconGap;
        drawSupportingText(canvas, item.right, cursor, baseline, Paint.Align.LEFT);
        if (clip) canvas.restore();
    }

    /** Dispatches an item to the icon-aware summary renderer or the plain-text renderer. */
    private void drawWeatherLineContent(Canvas canvas, WeatherLineItem item,
            float centerX, float baseline, Paint.FontMetrics metrics, long elapsed) {
        if (item.hasIcon()) {
            drawScrollingWeatherSummary(canvas, item, centerX, baseline, metrics, elapsed);
        } else {
            drawScrollingWeatherText(canvas, item.text, centerX, baseline, elapsed);
        }
    }

    /** Elapsed time within the single-item scroll cycle for the main carousel. */
    private long mainCarouselElapsed(WeatherLineItem item) {
        long now = SystemClock.uptimeMillis();
        if (mainLineCarousel.cycleStartedAt == 0L
                || mainLineCarousel.items == null
                || mainLineCarousel.items.size() != 1
                || !item.equals(mainLineCarousel.items.get(0))) {
            mainLineCarousel.setItems(java.util.Collections.singletonList(item));
            mainLineCarousel.cycleStartedAt = now;
        }
        long elapsed = now - mainLineCarousel.cycleStartedAt;
        long duration = weatherDetailDisplayDuration(item);
        if (elapsed >= duration) {
            mainLineCarousel.cycleStartedAt = now;
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
     * Used by both the weather summary line and the detailed-weather line so their animation
     * and scrolling behaviour stay identical.
     */
    private void drawCarousel(Canvas canvas, Carousel carousel, float centerX,
            float baseline, Paint.FontMetrics metrics) {
        if (carousel.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (carousel.cycleStartedAt == 0L) carousel.cycleStartedAt = now;

        int currentIndex = Math.min(carousel.index, carousel.items.size() - 1);
        WeatherLineItem currentItem = carousel.items.get(currentIndex);
        long elapsed = now - carousel.cycleStartedAt;
        long displayDuration = weatherDetailDisplayDuration(currentItem);

        if (carousel.items.size() == 1) {
            if (elapsed >= displayDuration) {
                carousel.cycleStartedAt = now;
                elapsed = 0L;
            }
            drawWeatherDetailItem(canvas, currentItem, null, centerX,
                baseline, metrics, 0f, elapsed);
            if (itemWidth(currentItem) > weatherDetailAvailableWidth()) {
                postInvalidateDelayed(WEATHER_DETAIL_FRAME_DELAY_MILLIS);
            }
            return;
        }

        long fadeOutEnd = displayDuration + WEATHER_DETAIL_TRANSITION_MILLIS;
        long fadeInEnd = fadeOutEnd + WEATHER_DETAIL_TRANSITION_MILLIS;

        int nextIndex = (carousel.index + 1) % carousel.items.size();
        WeatherLineItem nextItem = carousel.items.get(nextIndex);

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
    private void drawWeatherDetailItem(Canvas canvas, WeatherLineItem current, WeatherLineItem next,
            float centerX, float baseline, Paint.FontMetrics metrics, float progress,
            long itemElapsed) {
        boolean transitioning = next != null;
        if (!transitioning) {
            drawWeatherDetailLine(canvas, current, centerX, baseline, metrics, 255,
                ClockPreferences.TRANSITION_FADE, 0f, itemElapsed);
            return;
        }
        String transition = ClockPreferences.TRANSITION_FADE;
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
    private void drawWeatherDetailLine(Canvas canvas, WeatherLineItem item, float centerX,
            float baseline, Paint.FontMetrics metrics, int alpha, String transition, float phase,
            long itemElapsed) {
        if (item == null || item.text.length() == 0) return;
        int originalAlpha = datePaint.getAlpha();
        applySupportingTypeface();
        float clampedAlpha = Math.max(0, Math.min(255, alpha));
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(transition)
                || ClockPreferences.TRANSITION_SLIDE_DOWN.equals(transition)) {
            float direction = ClockPreferences.TRANSITION_SLIDE_UP.equals(transition) ? -1f : 1f;
            float distance = datePaint.getTextSize() * 0.6f;
            canvas.save();
            canvas.translate(0f, direction * distance * phase);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawWeatherLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_SCALE.equals(transition)) {
            float scale = 1f - 0.12f * Math.abs(phase);
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(scale, scale, centerX, pivotY);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawWeatherLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_FLIP.equals(transition)) {
            float scaleY = Math.max(0.05f, 1f - Math.abs(phase));
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(1f, scaleY, centerX, pivotY);
            datePaint.setAlpha(originalAlpha);
            drawWeatherLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else {
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawWeatherLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
        }
        datePaint.setAlpha(originalAlpha);
    }

    private long weatherDetailDisplayDuration(WeatherLineItem item) {
        float overflow = itemWidth(item) - weatherDetailAvailableWidth();
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
        Typeface originalTypeface = datePaint.getTypeface();
        applySupportingTypeface();
        float width = datePaint.measureText(text);
        datePaint.setTypeface(originalTypeface);
        return width;
    }

    /**
     * Rotating-line carousel state: which item is showing and when its cycle began.
     * Two instances drive the weather summary line and the detailed-weather line,
     * sharing the identical timing, transition and horizontal-scroll behaviour.
     */
    private static final class Carousel {
        java.util.List<WeatherLineItem> items;
        int index;
        long cycleStartedAt;

        void setItems(java.util.List<WeatherLineItem> newItems) {
            if (java.util.Objects.equals(items, newItems)) return;
            // Keep the current position when the refreshed list still covers it, so a data
            // update does not jump the rotating line back to its first entry.
            if (items == null || newItems == null || index >= newItems.size()) {
                index = 0;
                cycleStartedAt = 0L;
            }
            items = newItems;
        }

        boolean isEmpty() {
            return items == null || items.isEmpty();
        }
    }

    /**
     * One item on a rotating supporting line. A plain item carries only {@link #text}; a
     * weather-summary item additionally splits into {@link #left} (location) and {@link #right}
     * (condition + temperature) around a {@link #icon}. {@link #equals}/{@link #hashCode} let
     * {@code List.equals} detect when the carousel contents actually change.
     */
    private static final class WeatherLineItem {
        final String text;
        final String left;
        final String right;
        final WeatherIcon icon;

        private WeatherLineItem(String text, String left, String right, WeatherIcon icon) {
            this.text = text;
            this.left = left;
            this.right = right;
            this.icon = icon;
        }

        static WeatherLineItem plain(String text) {
            return new WeatherLineItem(text, null, null, null);
        }

        static WeatherLineItem weather(String left, String right, WeatherIcon icon) {
            return new WeatherLineItem(left + "  " + right, left, right, icon);
        }

        boolean hasIcon() {
            return icon != null;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WeatherLineItem)) return false;
            WeatherLineItem other = (WeatherLineItem) o;
            // WeatherIcon is cached per (code, style) so equal items share the same instance.
            return text.equals(other.text) && icon == other.icon;
        }

        @Override public int hashCode() {
            return text.hashCode() * 31 + (icon == null ? 0 : System.identityHashCode(icon));
        }
    }

    private void drawSupportingText(Canvas canvas, String text, float x, float baseline,
            Paint.Align align) {
        if (text == null || text.length() == 0) return;
        Paint.Align originalAlign = datePaint.getTextAlign();
        Typeface originalTypeface = datePaint.getTypeface();
        applySupportingTypeface();
        datePaint.setTextAlign(align);
        canvas.drawText(text, x, baseline, datePaint);
        datePaint.setTypeface(originalTypeface);
        datePaint.setTextAlign(originalAlign);
    }

    private void applyTextStyles() {
        if (backgroundRepository == null) {
            return;
        }
        timeFontScale = backgroundRepository.getTimeFontScale();
        dateFontScale = backgroundRepository.getDateFontScale();
        timePaint.setColor(ClockPreferences.TEXT_COLOR);
        secondsPaint.setColor(ClockPreferences.TEXT_COLOR);
        periodPaint.setColor(ClockPreferences.TEXT_COLOR);
        datePaint.setColor(ClockPreferences.TEXT_COLOR);
        blinkColon = backgroundRepository.isBlinkColon();
        animateTimeChanges = backgroundRepository.isAnimateTimeChanges();
        timeTransition = backgroundRepository.getTimeTransition();
        boldText = backgroundRepository.isBoldText();
        Typeface timeTypeface = ClockTypefaceResolver.resolve(getContext(), boldText);
        timePaint.setTypeface(timeTypeface);
        secondsPaint.setTypeface(timeTypeface);
        periodPaint.setTypeface(timeTypeface);
        datePaint.setTypeface(ClockTypefaceResolver.resolve(getContext(), boldText));
        showSeconds = backgroundRepository.isShowSeconds();
        showLunar = backgroundRepository.isShowLunar();
        smallSeconds = backgroundRepository.isSmallSeconds();
        use24Hour = backgroundRepository.isUse24Hour();
        weatherIconColor = ClockPreferences.TEXT_COLOR;
        portraitStacked = backgroundRepository.isPortraitStacked();
        dateLunarDualLine = backgroundRepository.isDateLunarDualLine();
        NetworkTimeProvider timeProvider = networkTimeProvider;
        if (timeProvider != null) {
            timeProvider.setEnabled(backgroundRepository.isUseNetworkTime());
            timeProvider.setSyncIntervalMinutes(backgroundRepository.getSyncIntervalMinutes());
        }
    }

    private void applySupportingTypeface() {
        datePaint.setTypeface(ClockTypefaceResolver.resolve(getContext(), boldText));
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

    private boolean shouldDimBackground() {
        return backgroundRepository != null
                && ClockPreferences.MODE_IMAGE.equals(backgroundRepository.getBackgroundMode())
                && backgroundRepository.isDimBackground();
    }

    private static String longerOf(String a, String b) {
        return a.length() >= b.length() ? a : b;
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
