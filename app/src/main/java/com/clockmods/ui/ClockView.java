package com.clockmods.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.LocaleManager;
import com.clockmods.platform.ApiCompat;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.BackgroundDimSchedule;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.LunarCalendar;
import com.clockmods.time.NetworkTimeProvider;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClockView extends View {
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long TIME_TRANSITION_DURATION_MILLIS = 300L;
    private static final long SUPPORT_LINE_HOLD_MILLIS = 3000L;
    private static final long SUPPORT_LINE_SCROLL_PAUSE_MILLIS = 1000L;
    private static final long SUPPORT_LINE_FRAME_DELAY_MILLIS = 16L;
    private static final float SUPPORT_LINE_SCROLL_DP_PER_SECOND = 40f;
    private static final float SUPPORT_LINE_HORIZONTAL_PADDING_DP = 24f;
    private static final float MESSAGE_MARQUEE_GAP_EM = 4f;
    private static final float MESSAGE_EDGE_FADE_FRACTION = 0.08f;
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
            // Between two ticks only the digits change, and they live inside timeDirty. A full
            // pass is only needed when something else asked for one (settings, resize, a date
            // change queued by the last frame) or before the first frame has measured itself.
            if (timeDirtyValid && !fullRepaintQueued) {
                invalidate(timeDirty);
            } else {
                fullRepaintQueued = false;
                invalidate();
            }
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
    private final Paint marqueeFadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    // Last formatted time, valid while the second (and the settings version) is unchanged.
    private ClockTimeFormatter.DisplayTime formattedTime;
    private int formattedVersion = -1;
    private int formattedHour = -1;
    private int formattedMinute = -1;
    private int formattedSecond = -1;
    private long timeTransitionStartedAt;
    private float clockShadowRadius;
    private float clockShadowDy;
    // Rotating carousel for the main supporting line; it preserves the exact
    // timing/scroll sequence that the message shared with the former detail line.
    private final Carousel messageCarousel = new Carousel();
    private String customMessage = ClockPreferences.DEFAULT_CUSTOM_MESSAGE;

    // ---- Render-path caches -------------------------------------------------
    // Everything below exists purely to keep onDraw allocation-free: the clock
    // redraws once a second (plus a short digit animation), so any object created
    // per frame becomes garbage ~30000 times an hour on the devices this build
    // targets. None of it changes what is drawn.
    //
    // Reused Calendar/TimeZone: Calendar.getInstance() allocates a calendar plus its
    // lazily filled field arrays on every call.
    private Calendar calendar;
    private java.util.TimeZone timeZoneValue;
    // Typefaces are resolved once per (family, bold) change instead of per frame and,
    // for the supporting line, per character.
    private Typeface timeTypeface;
    private Typeface supportingLatinTypeface;
    private Typeface supportingCjkTypeface;
    private String typefaceFamily;
    private boolean typefaceBold;
    private boolean typefaceResolved;
    private int fontGeneration;
    // Scratch char buffers for Canvas.drawText(char[], ...) / Paint.measureText(char[], ...).
    // The String overloads of both copy the whole string into a fresh char[] per call.
    private char[] glyphBuffer = new char[64];
    private char[] previousGlyphBuffer = new char[64];
    // drawPosText draws a whole run in one native call - one glyph mask instead of one per
    // character - so the clock batches every string it draws. These two buffers hold the run
    // being built: the characters actually drawn (the blinking colon is skipped) and their
    // x/y pairs.
    private char[] drawnBuffer = new char[64];
    private float[] positionBuffer = new float[128];
    // Cached widths of the supporting strings (date, lunar, message), including the
    // per-character tracking, so the measure pass runs once per text/size/font.
    private static final int SUPPORT_METRIC_SLOTS = 4;
    private final String[] metricText = new String[SUPPORT_METRIC_SLOTS];
    private final float[] metricSize = new float[SUPPORT_METRIC_SLOTS];
    private final int[] metricFont = new int[SUPPORT_METRIC_SLOTS];
    private final float[] metricWidth = new float[SUPPORT_METRIC_SLOTS];
    private final float[][] metricAdvances = new float[SUPPORT_METRIC_SLOTS][];
    private int metricNext;
    // Font metrics are obtained into these instances instead of allocating new ones.
    private final Paint.FontMetrics timeFontMetrics = new Paint.FontMetrics();
    private final Paint.FontMetrics dateFontMetrics = new Paint.FontMetrics();
    private final Paint.FontMetrics supportFontMetrics = new Paint.FontMetrics();
    private final Paint.FontMetrics stackedFontMetrics = new Paint.FontMetrics();
    private final Rect digitBounds = new Rect();
    // The marquee edge fade and the message item are rebuilt only when they change.
    private LinearGradient fadeShader;
    private float fadeShaderLeft;
    private float fadeShaderRight;
    private final int[] fadeColors = {0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF};
    private final float[] fadePositions = {0f, 0f, 1f, 1f};
    private SupportLineItem cachedMessageItem;
    private float smallSecondsGapSize = -1f;
    private float smallSecondsGapValue;
    private int smallSecondsGapGeneration = -1;

    // The date/lunar rows are the only text that never changes between ticks, but they are also
    // the most expensive to rasterise (CJK glyphs, per-typeface runs, a blurred shadow). The
    // solver records them here and they are rendered once into a small bitmap that the tick then
    // blits. The bitmap keeps the exact sub-pixel phase: its origin is floored to whole pixels and
    // the text is drawn at the same fractional offset inside it.
    private final String[] supportRowText = new String[2];
    private final float[] supportRowBaseline = new float[2];
    private int supportRowCount;
    private Bitmap supportLayer;
    private int supportLayerLeft;
    private int supportLayerTop;
    private boolean supportLayerValid;
    /** Refuse to hold more than this for a text layer; fall back to drawing the rows directly. */
    private static final int SUPPORT_LAYER_MAX_BYTES = 1 << 21;

    // ---- Frame cache, dirty rectangle and window fill ------------------------
    // The geometry of a frame is solved once and then reused until something that can move a
    // baseline changes: the view size, a font setting, the character count of the time strings,
    // the date/lunar text or the message. Digit transitions and the per-second tick then draw
    // from the cache without touching Paint.measureText, which is the expensive half of a frame.
    private final ClockFrameCache frame = new ClockFrameCache();
    private static final int FLAG_SHOW_SECONDS = 1;
    private static final int FLAG_SMALL_SECONDS = 1 << 1;
    private static final int FLAG_PERIOD = 1 << 2;
    private static final int FLAG_LUNAR = 1 << 3;
    private static final int FLAG_DUAL_LINE = 1 << 4;
    private static final int FLAG_STACKED = 1 << 5;
    private static final int FLAG_MESSAGE = 1 << 6;

    // Only the time digits change between two ticks, so the tick can invalidate just that
    // rectangle instead of the whole surface (the background image is then re-sampled for a
    // fraction of the pixels).
    private final Rect timeDirty = new Rect();
    private boolean timeDirtyValid;
    // Anything outside the time rectangle changed, so one full pass is queued.
    private boolean fullRepaintQueued;
    private String lastDateText;
    private String lastLunarText;
    private boolean lastDimState;
    // Colour the window itself is painted with, or MAX_VALUE when the window is not ours to
    // paint (a non-Activity context). When it matches the background colour the full-surface
    // fill in onDraw is redundant and is skipped.
    private int windowBackgroundColor = Integer.MAX_VALUE;

    // ---- Settings snapshot ---------------------------------------------------
    // Styling a frame used to read about twenty preferences (and re-set four paints) every time,
    // which on this hardware costs more than drawing the digits. The values only change when the
    // user applies settings, so they are cached and re-read when ClockPreferences' write counter
    // moves. Everything below is therefore constant for the lifetime of a frame.
    private int settingsVersion = -1;
    private int backgroundColor;
    private boolean imageMode;
    private boolean dimBackgroundAlways;
    private boolean dimBackgroundScheduled;
    private int dimStartMinutes;
    private int dimEndMinutes;
    private String timeZoneId;

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
        marqueeFadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    public void setBackgroundRepository(BackgroundRepository repository) {
        backgroundRepository = repository;
        requestBackgroundReload();
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
        // While paused the surface may have been destroyed and recreated, so the rectangle the
        // last frame measured cannot be trusted: the first tick after a resume repaints fully.
        timeDirtyValid = false;
        fullRepaintQueued = false;
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
        // First, because the window background and the fill colour both come from the snapshot.
        refreshSettings();
        int width = getWidth();
        int height = getHeight();
        drawBackground(canvas, width, height);

        java.util.TimeZone timeZone = resolveTimeZone();
        Calendar now = calendar;
        if (now == null || now.getTimeZone() != timeZone) {
            now = Calendar.getInstance(timeZone);
            calendar = now;
        }
        NetworkTimeProvider timeProvider = networkTimeProvider;
        now.setTimeInMillis(timeProvider == null
            ? System.currentTimeMillis() : timeProvider.currentTimeMillis());
        boolean dimmed = shouldDimBackground(now);
        if (dimmed) {
            canvas.drawColor(DIM_BACKGROUND_OVERLAY_COLOR);
        }

        ClockTimeFormatter.DisplayTime displayTime = formatDisplayTime(now);
        String dateText = DateFormatter.format(
                clockUseEnglish ? datePatternEn : datePatternCn, now, dateLang);
        String lunarText = showLunar ? LunarCalendar.format(now) : "";

        // Portrait stacked layout is a fully separate path; landscape is untouched.
        boolean stacked = height >= width && portraitStacked;
        boolean layoutRecomputed = ensureLayout(displayTime, dateText, lunarText, width, height,
                stacked);
        if (stacked) {
            drawStackedPortrait(canvas, now, width, height);
        } else {
            drawPrimary(canvas, displayTime, width, height);
        }
        finishFrame(layoutRecomputed, dimmed, dateText, lunarText);
    }

    /**
     * Formats the time, reusing the previous result while the same second is on screen. A second
     * boundary produces a new instance (so the transition can still hold the old one), but the
     * ~9 frames of a digit transition and every repeated draw inside one second now allocate
     * nothing and build no strings.
     */
    private ClockTimeFormatter.DisplayTime formatDisplayTime(Calendar now) {
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        ClockTimeFormatter.DisplayTime cached = formattedTime;
        if (cached != null && formattedVersion == settingsVersion
                && formattedHour == hour && formattedMinute == minute
                && formattedSecond == second) {
            return cached;
        }
        ClockTimeFormatter.DisplayTime formatted = ClockTimeFormatter.format(hour, minute, second,
                showSeconds, blinkColon, smallSeconds, use24Hour, clockUseEnglish);
        formattedTime = formatted;
        formattedVersion = settingsVersion;
        formattedHour = hour;
        formattedMinute = minute;
        formattedSecond = second;
        return formatted;
    }

    /** Draws the primary (non stacked) layout entirely from the frame cache. */
    private void drawPrimary(Canvas canvas, ClockTimeFormatter.DisplayTime displayTime,
            int width, int height) {
        float centerX = width / 2f;
        drawAnimatedTime(canvas, displayTime, frame.timeCenterX, frame.timeBaseline);
        drawSupportLine(canvas, centerX, frame.dateSize);
        drawSupportingRows(canvas, width, height);
    }

    /**
     * Draws the date/lunar rows from the cached layer, falling back to direct text drawing when
     * the layer cannot be allocated.
     */
    private void drawSupportingRows(Canvas canvas, int width, int height) {
        if (supportRowCount == 0) {
            return;
        }
        if (ensureSupportLayer(width)) {
            canvas.drawBitmap(supportLayer, (float) supportLayerLeft, (float) supportLayerTop,
                    null);
            return;
        }
        for (int row = 0; row < supportRowCount; row++) {
            applySupportingTypeface(supportRowText[row]);
            drawSupportingText(canvas, supportRowText[row], width / 2f,
                    supportRowBaseline[row], Paint.Align.CENTER);
        }
    }

    /**
     * Renders the date/lunar rows into a bitmap once per layout change. The rows are static
     * between ticks but cost ~2ms of CJK rasterisation per frame, so a tick pays for one blit
     * instead. Text inside the bitmap is drawn at the same fractional offset the direct path
     * would use, so the pixels are identical.
     */
    private boolean ensureSupportLayer(int width) {
        if (supportLayerValid) {
            return supportLayer != null;
        }
        supportLayerValid = true;
        if (supportLayer != null) {
            supportLayer.recycle();
            supportLayer = null;
        }
        float pad = clockShadowRadius + Math.abs(clockShadowDy) + 2f;
        float top = Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        float widest = 0f;
        for (int row = 0; row < supportRowCount; row++) {
            String text = supportRowText[row];
            applySupportingTypeface(text);
            Paint.FontMetrics metrics = supportFontMetrics;
            datePaint.getFontMetrics(metrics);
            top = Math.min(top, supportRowBaseline[row] + metrics.ascent);
            bottom = Math.max(bottom, supportRowBaseline[row] + metrics.descent);
            widest = Math.max(widest, measureSupportingText(text));
        }
        if (top >= bottom || widest <= 0f) {
            return false;
        }
        int left = (int) Math.floor((width - widest) / 2f - pad);
        int topPixels = (int) Math.floor(top - pad);
        int right = (int) Math.ceil((width + widest) / 2f + pad);
        int bottomPixels = (int) Math.ceil(bottom + pad);
        int layerWidth = right - left;
        int layerHeight = bottomPixels - topPixels;
        if (layerWidth <= 0 || layerHeight <= 0
                || (long) layerWidth * layerHeight * 4 > SUPPORT_LAYER_MAX_BYTES) {
            return false;
        }
        Bitmap bitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);
        Canvas layerCanvas = new Canvas(bitmap);
        for (int row = 0; row < supportRowCount; row++) {
            applySupportingTypeface(supportRowText[row]);
            drawSupportingText(layerCanvas, supportRowText[row], width / 2f - left,
                    supportRowBaseline[row] - topPixels, Paint.Align.CENTER);
        }
        supportLayer = bitmap;
        supportLayerLeft = left;
        supportLayerTop = topPixels;
        return true;
    }

    /**
     * Fills the surface. A solid background is painted by the window itself, so the identical
     * full-surface fill here is skipped; the framework draws the window background (clipped to
     * the dirty rectangle on a partial pass) before {@code onDraw} runs.
     */
    private void drawBackground(Canvas canvas, int width, int height) {
        boolean paintedByWindow = false;
        if (!imageMode) {
            syncWindowBackground(backgroundColor);
            paintedByWindow = windowBackgroundColor == backgroundColor;
        }
        if (!paintedByWindow) {
            canvas.drawColor(backgroundColor);
        }
        drawBackgroundImage(canvas, width, height);
    }

    private void syncWindowBackground(int color) {
        if (windowBackgroundColor == color) {
            return;
        }
        android.content.Context context = getContext();
        if (!(context instanceof android.app.Activity)) {
            return;
        }
        android.view.Window window = ((android.app.Activity) context).getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(color));
        windowBackgroundColor = color;
    }

    /**
     * Re-measures the frame when its key changed. Returns {@code true} when the layout was
     * recomputed, which also means the time rectangle may have moved or resized.
     */
    private boolean ensureLayout(ClockTimeFormatter.DisplayTime displayTime, String dateText,
            String lunarText, int width, int height, boolean stacked) {
        String message = backgroundRepository == null
                ? "" : backgroundRepository.getCustomMessage();
        if (message == null) {
            message = "";
        }
        int flags = (showSeconds ? FLAG_SHOW_SECONDS : 0)
                | (smallSeconds ? FLAG_SMALL_SECONDS : 0)
                | (displayTime.hasPeriod() ? FLAG_PERIOD : 0)
                | (lunarText.length() > 0 ? FLAG_LUNAR : 0)
                | (dateLunarDualLine ? FLAG_DUAL_LINE : 0)
                | (stacked ? FLAG_STACKED : 0)
                | (message.length() > 0 ? FLAG_MESSAGE : 0);
        if (frame.matches(width, height, fontGeneration, timeFontScale, dateFontScale, flags,
                displayTime.mainText.length(), displayTime.secondsText.length(),
                displayTime.periodText.length(), dateText, lunarText, message)) {
            applyFrameTextSizes();
            return false;
        }
        frame.storeKey(width, height, fontGeneration, timeFontScale, dateFontScale, flags,
                displayTime.mainText.length(), displayTime.secondsText.length(),
                displayTime.periodText.length(), dateText, lunarText, message);
        supportLayerValid = false;
        if (stacked) {
            computeStackedLayout(dateText, lunarText, width, height, message.length() > 0);
        } else {
            computePrimaryLayout(displayTime, dateText, lunarText, width, height);
        }
        applyFrameTextSizes();
        return true;
    }

    /** Applies the cached text sizes to the paints; setters only, no measuring. */
    private void applyFrameTextSizes() {
        timePaint.setTextSize(frame.timeSize);
        secondsPaint.setTextSize(frame.timeSize * 0.6f);
        periodPaint.setTextSize(frame.timeSize * 0.3f);
        datePaint.setTextSize(frame.dateSize);
    }

    /**
     * Solves the custom-message line: its baseline and the vertical band it occupies. Doing this
     * in the layout pass is what lets a scrolling message repaint only its own strip instead of
     * the whole clock (which would re-rasterise the digits 60 times a second).
     */
    private void computeSupportGeometry(String message, float baselineAnchor, float timeDescent,
            float gap) {
        frame.hasSupportMessage = message != null && message.length() > 0;
        frame.supportBaseline = 0f;
        frame.supportTop = 0f;
        frame.supportBottom = 0f;
        if (!frame.hasSupportMessage) {
            return;
        }
        applySupportingTypeface(message);
        Paint.FontMetrics metrics = supportFontMetrics;
        datePaint.getFontMetrics(metrics);
        float baseline = baselineAnchor + timeDescent + gap - metrics.ascent;
        float pad = clockShadowRadius + Math.abs(clockShadowDy)
                + datePaint.getTextSize() * 0.12f;
        frame.supportBaseline = baseline;
        frame.supportTop = baseline + metrics.ascent - pad;
        frame.supportBottom = baseline + metrics.descent + pad;
    }

    /** Stores the rectangle the next tick may repaint, clamped to the view. */
    private void setTimeDirty(float left, float top, float right, float bottom) {
        int l = Math.max(0, (int) Math.floor(left));
        int t = Math.max(0, (int) Math.floor(top));
        int r = Math.min(getWidth(), (int) Math.ceil(right));
        int b = Math.min(getHeight(), (int) Math.ceil(bottom));
        if (r <= l || b <= t) {
            timeDirtyValid = false;
            return;
        }
        timeDirty.set(l, t, r, b);
        timeDirtyValid = true;
    }

    /**
     * Ends a frame. When something outside the time rectangle changed - the date rolled over, the
     * lunar date changed, the dim overlay turned on, or the layout was re-solved (which can move
     * and resize the time rectangle) - one full pass is queued so the next tick can go back to
     * repainting the digits only.
     */
    private void finishFrame(boolean layoutRecomputed, boolean dimmed, String dateText,
            String lunarText) {
        boolean outsideTimeChanged = layoutRecomputed
                || dimmed != lastDimState
                || !ApiCompat.equals(dateText, lastDateText)
                || !ApiCompat.equals(lunarText, lastLunarText);
        lastDimState = dimmed;
        lastDateText = dateText;
        lastLunarText = lunarText;
        if (outsideTimeChanged) {
            fullRepaintQueued = true;
            postInvalidate();
        }
    }

    /**
     * Solves the primary layout. This is the measurement stage lifted verbatim out of
     * {@code onDraw}: the arithmetic and the order of the calls are unchanged, they simply run
     * once per key instead of once per frame.
     */
    private void computePrimaryLayout(ClockTimeFormatter.DisplayTime displayTime, String dateText,
            String lunarText, int width, int height) {
        String fullDate = lunarText.length() == 0 ? dateText : dateText + " " + lunarText;

        // Portrait always stacks the date and lunar lines; landscape shares a single line
        // unless the user opts into two rows.
        boolean singleDateLine = lunarText.length() == 0 || (width > height && !dateLunarDualLine);
        // Font size: landscape sizes against the full single-line string whether or not the
        // two-row layout is enabled, so toggling it never changes the font size. Portrait
        // always stacks, so it sizes against the longer of the two rows to fill its narrow width.
        boolean portraitStack = height >= width && lunarText.length() > 0;
        String widestDateText = portraitStack ? longerOf(dateText, lunarText) : fullDate;
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
        datePaint.setTextSize(1f);
        if (dateFontScale <= 1f) {
            dateSize = ClockLayoutCalculator.capSupportingSize(dateSize, height,
                    timePaint.descent() - timePaint.ascent(), datePaint.descent() - datePaint.ascent());
        }
        datePaint.setTextSize(dateSize);

        float centerX = width / 2f;
        float timeCenterX = centerX + ClockLayoutCalculator.calculateMainCenterOffset(
            leftAccessoryWidth * timeSize, rightAccessoryWidth * timeSize);
        float centerY = height / 2f;
        Paint.FontMetrics timeMetrics = timeFontMetrics;
        timePaint.getFontMetrics(timeMetrics);
        Paint.FontMetrics dateMetrics = dateFontMetrics;
        datePaint.getFontMetrics(dateMetrics);
        float timeBaseline = centerY - (timeMetrics.ascent + timeMetrics.descent) / 2f;
        boolean portrait = height >= width;
        float gapFactor = portrait ? 0.9f : 0.35f;
        float gap = Math.min(height * .025f, dateSize * gapFactor);
        float dateBaseline = timeBaseline + timeMetrics.ascent - gap - dateMetrics.descent;
        // Portrait gives the supporting (message) line more breathing room; landscape
        // keeps the tight default. The date/lunar gap uses the very same scale/formula.
        float supportLineScale = portrait ? 1.35f : 1f;
        float lunarRowGap = (dateMetrics.descent - dateMetrics.ascent + dateMetrics.descent)
                * supportLineScale;

        frame.dateText = dateText;
        frame.lunarText = lunarText;
        frame.hasLunar = lunarText.length() > 0;
        frame.timeSize = timeSize;
        frame.dateSize = dateSize;
        frame.timeCenterX = timeCenterX;
        frame.timeBaseline = timeBaseline;
        frame.timeAscent = timeMetrics.ascent;
        frame.timeDescent = timeMetrics.descent;
        // Widths at the final size, which is what the draw calls were measuring per frame.
        frame.mainWidth = stableTextWidth(displayTime.mainText, timePaint);
        frame.secondsWidth = displayTime.hasSmallSeconds()
                ? stableTextWidth(displayTime.secondsText, secondsPaint) : 0f;
        frame.periodWidth = displayTime.hasPeriod()
                ? periodPaint.measureText(displayTime.periodText) : 0f;
        frame.gapWidth = smallSecondsGapWidth();
        if (!frame.hasLunar || singleDateLine) {
            supportRowCount = 1;
            supportRowText[0] = fullDate;
            supportRowBaseline[0] = dateBaseline;
        } else {
            // Portrait, two date lines: the lunar row (nearest the time) keeps the full gap above
            // the time — matching the message gap below it — and the Gregorian date sits one
            // (wider) row-gap above the lunar row.
            supportRowCount = 2;
            supportRowText[0] = lunarText;
            supportRowBaseline[0] = dateBaseline;
            supportRowText[1] = dateText;
            supportRowBaseline[1] = dateBaseline - lunarRowGap;
        }
        frame.secondsBaseline = bottomAlignedBaseline(timeBaseline, timePaint, secondsPaint);
        frame.periodBaseline = bottomAlignedBaseline(timeBaseline, timePaint, periodPaint);
        computeSupportGeometry(frame.messageText, timeBaseline, frame.timeDescent, gap);

        // Rectangle the digit transition and the per-second tick can touch. The extra margin
        // covers the shadow and the slide transition (0.24 em).
        float extra = Math.max(clockShadowRadius + clockShadowDy, timeSize * 0.45f);
        float halfWidth = frame.mainWidth / 2f + frame.gapWidth + extra;
        setTimeDirty(timeCenterX - halfWidth - frame.periodWidth,
                timeBaseline + frame.timeAscent - extra,
                timeCenterX + halfWidth + frame.secondsWidth,
                timeBaseline + frame.timeDescent + extra);
    }

    /**
     * Solves the portrait "stacked" layout: the clock is drawn as large digits on separate
     * lines - hours, minutes and (optionally) seconds, top to bottom, with no colon. Supporting
     * text sizes and block heights are computed first, so the digits can be capped to always
     * leave room for them. Runs only when the frame key changes.
     */
    private void computeStackedLayout(String dateText, String lunarText, int width, int height,
            boolean hasMessage) {
        int lines = showSeconds ? 3 : 2;

        datePaint.setTextSize(1f);
        String widestDate = lunarText.length() == 0 ? dateText : longerOf(dateText, lunarText);
        applySupportingTypeface(widestDate);
        float dateSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, measureSupportingText(widestDate), dateFontScale, 0.08f, 0.92f);
        datePaint.setTextSize(dateSize);
        Paint.FontMetrics dateMetrics = stackedFontMetrics;
        datePaint.getFontMetrics(dateMetrics);
        float dateLineHeight = dateMetrics.descent - dateMetrics.ascent;
        float lunarGap = dateLineHeight * 1.5f;       // wider date <-> lunar row spacing
        boolean hasLunar = lunarText.length() > 0;
        float dateBlockHeight = hasLunar ? (lunarGap + dateLineHeight) : dateLineHeight;
        float messageBlockHeight = hasMessage ? dateLineHeight : 0f;

        float topSafe = height * 0.06f;
        float bottomSafe = height * 0.94f;
        float avail = bottomSafe - topSafe;
        // Date/message stay a small fixed distance from the digits; they only spread
        // toward the edges when the time font grows, and never past the safe margins.
        float desiredGap = dateLineHeight * 1.6f;
        float minGap = dateLineHeight * 0.25f;

        // A two-digit group fills the requested width fraction, capped by a per-line
        // height fraction, then further capped so date + gaps + digits + message fit.
        timePaint.setTextSize(1f);
        float digitPairWidth = stableTextWidth("00", timePaint);
        float heightFraction = lines == 3 ? 0.18f : 0.28f;
        float lineSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
                width, height, digitPairWidth, timeFontScale, heightFraction, 0.66f);
        // blockHeight ~ blockFactor * lineSize; cap lineSize so the whole column fits.
        float blockFactor = (lines - 1) * 1.06f + 0.72f;
        float messageReserve = hasMessage ? (messageBlockHeight + minGap) : 0f;
        float maxBlockHeight = avail - dateBlockHeight - minGap - messageReserve;
        if (timeFontScale <= 1f) {
            lineSize = Math.min(lineSize, maxBlockHeight / blockFactor);
        }
        timePaint.setTextSize(lineSize);
        Rect digitBounds = this.digitBounds;
        timePaint.getTextBounds("0", 0, 1, digitBounds);
        float digitHeight = digitBounds.bottom - digitBounds.top;
        float lineAdvance = lineSize * 1.06f;
        float blockHeight = (lines - 1) * lineAdvance + digitHeight;

        // Equal gap above and below the digits (symmetric). It equals desiredGap when
        // there is spare room, and shrinks toward minGap as the digits grow; the whole
        // content column is then centered vertically within the safe area.
        float fitGap = ((hasMessage ? avail : (avail - messageBlockHeight))
                - dateBlockHeight - blockHeight - messageBlockHeight) / 2f;
        float gap = Math.max(minGap, Math.min(desiredGap, fitGap));
        float contentHeight = dateBlockHeight + gap + blockHeight
                + (hasMessage ? gap + messageBlockHeight : 0f);
        float contentTop = Math.max(topSafe, topSafe + (avail - contentHeight) / 2f);

        float blockTop = contentTop + dateBlockHeight + gap;
        float firstBaseline = blockTop - digitBounds.top;
        float blockBottom = blockTop + blockHeight;

        frame.dateText = dateText;
        frame.lunarText = lunarText;
        frame.hasLunar = hasLunar;
        frame.hasMessage = hasMessage;
        frame.lines = lines;
        frame.timeSize = lineSize;
        frame.dateSize = dateSize;
        frame.lineAdvance = lineAdvance;
        frame.firstBaseline = firstBaseline;
        // Date block bottom sits 'gap' above the hours; the lunar row one row-gap below it.
        frame.stackedDateBaseline = (blockTop - gap - dateBlockHeight) - dateMetrics.ascent;
        frame.stackedGap = gap;
        computeSupportGeometry(frame.messageText, blockBottom + frame.stackedGap, 0f, 0f);
        supportRowCount = hasLunar ? 2 : 1;
        supportRowText[0] = dateText;
        supportRowBaseline[0] = frame.stackedDateBaseline;
        if (hasLunar) {
            supportRowText[1] = lunarText;
            supportRowBaseline[1] = frame.stackedDateBaseline + lunarGap;
        }
        frame.mainWidth = stableTextWidth("00", timePaint);

        float extra = Math.max(clockShadowRadius + clockShadowDy, lineSize * 0.45f);
        float halfWidth = frame.mainWidth / 2f + extra;
        setTimeDirty(width / 2f - halfWidth, firstBaseline + digitBounds.top - extra,
                width / 2f + halfWidth,
                firstBaseline + (lines - 1) * lineAdvance + digitBounds.bottom + extra);
    }

    /** Draws the stacked layout from the frame cache. */
    private void drawStackedPortrait(Canvas canvas, Calendar now, int width, int height) {
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int displayHour = use24Hour ? hour : hour % 12;
        if (!use24Hour && displayHour == 0) {
            displayHour = 12;
        }
        String hoursText = ClockTimeFormatter.twoDigits(displayHour);
        String minutesText = ClockTimeFormatter.twoDigits(now.get(Calendar.MINUTE));
        String secondsText = ClockTimeFormatter.twoDigits(now.get(Calendar.SECOND));
        float centerX = width / 2f;

        long uptime = SystemClock.uptimeMillis();
        boolean animating = false;
        animating |= drawStackedLine(canvas, 0, hoursText, centerX, frame.firstBaseline, uptime);
        animating |= drawStackedLine(canvas, 1, minutesText, centerX,
                frame.firstBaseline + frame.lineAdvance, uptime);
        if (frame.lines == 3) {
            animating |= drawStackedLine(canvas, 2, secondsText, centerX,
                    frame.firstBaseline + 2f * frame.lineAdvance, uptime);
        } else {
            // Drop stale seconds state so re-enabling seconds starts clean.
            stackedText[2] = null;
            stackedPrev[2] = null;
        }
        if (animating) {
            postInvalidateDelayed(16L);
        }

        drawSupportingRows(canvas, width, height);

        // Custom message: its top sits 'gap' below the seconds (symmetric with date).
        if (frame.hasMessage) {
            drawSupportLine(canvas, centerX, frame.dateSize);
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

    /**
     * Draws the supporting line, which now hosts the custom message only. The marquee
     * timing, fade and carousel sequencing are unchanged from the previous combined
     * message line so rendering behaves exactly as before.
     */
    private void drawSupportLine(Canvas canvas, float centerX, float dateSize) {
        if (backgroundRepository == null) return;
        if (!frame.hasSupportMessage || ApiCompat.isEmpty(customMessage)) return;

        float originalSize = datePaint.getTextSize();
        Typeface originalTypeface = datePaint.getTypeface();

        SupportLineItem messageItem = cachedMessageItem;
        if (messageItem == null || !ApiCompat.equals(messageItem.text, customMessage)) {
            messageItem = SupportLineItem.message(customMessage, true);
            cachedMessageItem = messageItem;
        }
        datePaint.setTextSize(dateSize);
        applySupportingTypeface(messageItem.text);
        Paint.FontMetrics supportMetrics = supportFontMetrics;
        datePaint.getFontMetrics(supportMetrics);

        drawSupportLineItem(canvas, messageItem, null, centerX, frame.supportBaseline,
                supportMetrics, 0f, mainCarouselElapsed(messageItem));
        if (messageItem.continuousMarquee
                || itemWidth(messageItem) > supportLineAvailableWidth()) {
            // Only the message band scrolls, so only the message band is invalidated.
            postInvalidateDelayed(SUPPORT_LINE_FRAME_DELAY_MILLIS, 0,
                    (int) Math.floor(frame.supportTop), getWidth(),
                    (int) Math.ceil(frame.supportBottom));
        }
        datePaint.setTextSize(originalSize);
        datePaint.setTypeface(originalTypeface);
    }

    /** Horizontal width an item occupies. */
    private float itemWidth(SupportLineItem item) {
        return measureSupportingText(item.text);
    }

    /** Dispatches an item to the continuous marquee or one-shot scrolling renderer. */
    private void drawSupportLineContent(Canvas canvas, SupportLineItem item,
            float centerX, float baseline, Paint.FontMetrics metrics, long elapsed) {
        if (item.continuousMarquee) {
            drawLoopingMessage(canvas, item.text, centerX, baseline, metrics, elapsed);
        } else {
            drawScrollingMessage(canvas, item.text, centerX, baseline, metrics, elapsed);
        }
    }

    /** Elapsed time within the single-item scroll cycle for the supporting line. */
    private long mainCarouselElapsed(SupportLineItem item) {
        long now = SystemClock.uptimeMillis();
        if (messageCarousel.cycleStartedAt == 0L || !item.equals(messageCarousel.item)) {
            messageCarousel.item = item;
            messageCarousel.cycleStartedAt = now;
        }
        long elapsed = now - messageCarousel.cycleStartedAt;
        if (item.continuousMarquee) return elapsed;
        long duration = supportLineDisplayDuration(item);
        if (elapsed >= duration) {
            messageCarousel.cycleStartedAt = now;
            elapsed = 0L;
        }
        return elapsed;
    }

    /**
     * Draws the carousel line. {@code progress} runs across the whole transition where
     * {@code [0, 0.5)} fades out {@code current} and {@code [0.5, 1]} brings in {@code next}.
     */
    private void drawSupportLineItem(Canvas canvas, SupportLineItem current,
            SupportLineItem next, float centerX, float baseline, Paint.FontMetrics metrics,
            float progress, long itemElapsed) {
        boolean transitioning = next != null;
        if (!transitioning) {
            drawSupportLineItemLine(canvas, current, centerX, baseline, metrics, 255,
                ClockPreferences.TRANSITION_FADE, 0f, itemElapsed);
            return;
        }
        String transition = ClockPreferences.TRANSITION_FADE;
        if (progress < 0.5f) {
            float outProgress = progress / 0.5f;
            drawSupportLineItemLine(canvas, current, centerX, baseline, metrics,
                Math.round(255f * (1f - outProgress)), transition, outProgress, itemElapsed);
        } else {
            float inProgress = (progress - 0.5f) / 0.5f;
            drawSupportLineItemLine(canvas, next, centerX, baseline, metrics,
                Math.round(255f * inProgress), transition, -(1f - inProgress), itemElapsed);
        }
    }

    /**
     * Renders one carousel line honouring the transition style. {@code phase} encodes the
     * spatial offset for motion styles: negative means leaving, positive entering, 0 settled.
     */
    private void drawSupportLineItemLine(Canvas canvas, SupportLineItem item, float centerX,
            float baseline, Paint.FontMetrics metrics, int alpha, String transition, float phase,
            long itemElapsed) {
        if (item == null || item.text.length() == 0) return;
        int originalAlpha = datePaint.getAlpha();
        applySupportingTypeface(item.text);
        float clampedAlpha = Math.max(0, Math.min(255, alpha));
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(transition)
                || ClockPreferences.TRANSITION_SLIDE_DOWN.equals(transition)) {
            float direction = ClockPreferences.TRANSITION_SLIDE_UP.equals(transition) ? -1f : 1f;
            float distance = datePaint.getTextSize() * 0.6f;
            canvas.save();
            canvas.translate(0f, direction * distance * phase);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawSupportLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_SCALE.equals(transition)) {
            float scale = 1f - 0.12f * Math.abs(phase);
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(scale, scale, centerX, pivotY);
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawSupportLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else if (ClockPreferences.TRANSITION_FLIP.equals(transition)) {
            float scaleY = Math.max(0.05f, 1f - Math.abs(phase));
            float pivotY = baseline + (metrics.ascent + metrics.descent) / 2f;
            canvas.save();
            canvas.scale(1f, scaleY, centerX, pivotY);
            datePaint.setAlpha(originalAlpha);
            drawSupportLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
            canvas.restore();
        } else {
            datePaint.setAlpha(Math.round(clampedAlpha / 255f * originalAlpha));
            drawSupportLineContent(canvas, item, centerX, baseline, metrics, itemElapsed);
        }
        datePaint.setAlpha(originalAlpha);
    }

    private long supportLineDisplayDuration(SupportLineItem item) {
        if (item.continuousMarquee) {
            float speed = SUPPORT_LINE_SCROLL_DP_PER_SECOND
                    * getResources().getDisplayMetrics().density;
            float cycleDistance = itemWidth(item) + datePaint.getTextSize()
                    * MESSAGE_MARQUEE_GAP_EM;
            long cycleMillis = (long) Math.ceil(cycleDistance / speed * 1000f);
            return Math.max(SUPPORT_LINE_HOLD_MILLIS, cycleMillis);
        }
        float available = supportLineAvailableWidth();
        float distance = oneShotMarqueeDistance(itemWidth(item), available,
                messageEdgeFadeWidth(available));
        if (distance <= 0f) return SUPPORT_LINE_HOLD_MILLIS;
        float speed = SUPPORT_LINE_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
        long scrollMillis = (long) Math.ceil(distance / speed * 1000f);
        return Math.max(SUPPORT_LINE_HOLD_MILLIS,
                SUPPORT_LINE_SCROLL_PAUSE_MILLIS * 2L + scrollMillis);
    }

    private float supportLineAvailableWidth() {
        float padding = SUPPORT_LINE_HORIZONTAL_PADDING_DP
                * getResources().getDisplayMetrics().density;
        return Math.max(1f, getWidth() - padding * 2f);
    }

    private void drawLoopingMessage(Canvas canvas, String text, float centerX, float baseline,
            Paint.FontMetrics metrics, long elapsed) {
        float textWidth = measureSupportingText(text);
        if (textWidth <= 0f) return;
        float available = supportLineAvailableWidth();
        float left = centerX - available / 2f;
        float right = left + available;
        float gap = datePaint.getTextSize() * MESSAGE_MARQUEE_GAP_EM;
        float cycleDistance = textWidth + gap;
        float speed = SUPPORT_LINE_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
        float offset = loopingMarqueeOffset(elapsed, cycleDistance, speed);
        float top = Math.max(0f, baseline + metrics.ascent - datePaint.getTextSize() * 0.12f);
        float bottom = Math.min(getHeight(), baseline + metrics.descent
                + datePaint.getTextSize() * 0.12f);
        int layer = canvas.saveLayer(left, top, right, bottom, null, Canvas.ALL_SAVE_FLAG);
        canvas.clipRect(left, top, right, bottom);
        float drawX = left - offset;
        while (drawX + textWidth < left) drawX += cycleDistance;
        while (drawX < right) {
            drawSupportingText(canvas, text, drawX, baseline, Paint.Align.LEFT);
            drawX += cycleDistance;
        }
        drawMessageEdgeFade(canvas, left, right, top, bottom);
        canvas.restoreToCount(layer);
    }

    private void drawScrollingMessage(Canvas canvas, String text, float centerX, float baseline,
            Paint.FontMetrics metrics, long elapsed) {
        float textWidth = measureSupportingText(text);
        if (textWidth <= 0f) return;
        float available = supportLineAvailableWidth();
        float fadeWidth = messageEdgeFadeWidth(available);
        float distance = oneShotMarqueeDistance(textWidth, available, fadeWidth);
        if (distance <= 0f) {
            drawSupportingText(canvas, text, centerX, baseline, Paint.Align.CENTER);
            return;
        }

        float left = centerX - available / 2f;
        float right = left + available;
        float speed = SUPPORT_LINE_SCROLL_DP_PER_SECOND
                * getResources().getDisplayMetrics().density;
        float offset = oneShotMarqueeOffset(elapsed, SUPPORT_LINE_SCROLL_PAUSE_MILLIS,
                distance, speed);
        float top = Math.max(0f, baseline + metrics.ascent - datePaint.getTextSize() * 0.12f);
        float bottom = Math.min(getHeight(), baseline + metrics.descent
                + datePaint.getTextSize() * 0.12f);
        int layer = canvas.saveLayer(left, top, right, bottom, null, Canvas.ALL_SAVE_FLAG);
        canvas.clipRect(left, top, right, bottom);
        drawSupportingText(canvas, text, left + fadeWidth - offset, baseline, Paint.Align.LEFT);
        drawMessageEdgeFade(canvas, left, right, top, bottom);
        canvas.restoreToCount(layer);
    }

    private float messageEdgeFadeWidth(float availableWidth) {
        float edgeFraction = Math.min(0.25f, Math.max(0.01f,
                MESSAGE_EDGE_FADE_FRACTION));
        return availableWidth * edgeFraction;
    }

    private void drawMessageEdgeFade(Canvas canvas, float left, float right,
            float top, float bottom) {
        if (fadeShader == null || fadeShaderLeft != left || fadeShaderRight != right) {
            float edgeFraction = Math.min(0.25f, Math.max(0.01f,
                    MESSAGE_EDGE_FADE_FRACTION));
            fadePositions[1] = edgeFraction;
            fadePositions[2] = 1f - edgeFraction;
            fadeShader = new LinearGradient(left, 0f, right, 0f, fadeColors, fadePositions,
                    Shader.TileMode.CLAMP);
            fadeShaderLeft = left;
            fadeShaderRight = right;
        }
        marqueeFadePaint.setShader(fadeShader);
        canvas.drawRect(left, top, right, bottom, marqueeFadePaint);
        marqueeFadePaint.setShader(null);
    }

    static float loopingMarqueeOffset(long elapsedMillis, float cycleDistance,
            float pixelsPerSecond) {
        if (cycleDistance <= 0f || pixelsPerSecond <= 0f) return 0f;
        float travelled = Math.max(0L, elapsedMillis) * pixelsPerSecond / 1000f;
        return travelled % cycleDistance;
    }

    static float oneShotMarqueeDistance(float textWidth, float availableWidth,
            float edgeFadeWidth) {
        if (textWidth <= 0f || availableWidth <= 0f) return 0f;
        float safeFade = Math.max(0f, Math.min(availableWidth / 2f, edgeFadeWidth));
        return Math.max(0f, textWidth - availableWidth + safeFade * 2f);
    }

    static float oneShotMarqueeOffset(long elapsedMillis, long pauseMillis,
            float distance, float pixelsPerSecond) {
        if (distance <= 0f || pixelsPerSecond <= 0f) return 0f;
        long scrollingMillis = Math.max(0L, elapsedMillis - Math.max(0L, pauseMillis));
        float travelled = scrollingMillis * pixelsPerSecond / 1000f;
        return Math.min(distance, travelled);
    }

    private float measureSupportingText(String text) {
        if (text == null || text.length() == 0) return 0f;
        return metricWidth[supportMetrics(text)];
    }

    /**
     * Measures a supporting string (date, lunar date or custom message) once per
     * (text, size, font) and caches both the total width and the per-character advances.
     * A single frame measures the same string three or four times — to size the font, to
     * centre it and again while drawing — and each pass would otherwise re-enter
     * {@code measureText} for every character.
     */
    private int supportMetrics(String text) {
        float size = datePaint.getTextSize();
        for (int slot = 0; slot < SUPPORT_METRIC_SLOTS; slot++) {
            if (metricAdvances[slot] != null
                    && metricFont[slot] == fontGeneration
                    && metricSize[slot] == size
                    && ApiCompat.equals(text, metricText[slot])) {
                return slot;
            }
        }
        int length = text.length();
        char[] glyphs = glyphs(text);
        float[] advances = new float[length];
        float spacing = size * SUPPORTING_TEXT_LETTER_SPACING;
        float width = 0f;
        Typeface originalTypeface = datePaint.getTypeface();
        for (int start = 0; start < length;) {
            int codePoint = text.codePointAt(start);
            int end = start + Character.charCount(codePoint);
            datePaint.setTypeface(supportingTypefaceFor(codePoint));
            float advance = datePaint.measureText(glyphs, start, end - start);
            if (hasSupportingTrackingAt(text, end)) advance += spacing;
            advances[start] = advance;
            width += advance;
            start = end;
        }
        datePaint.setTypeface(originalTypeface);
        int slot = metricNext;
        metricNext = (metricNext + 1) % SUPPORT_METRIC_SLOTS;
        metricText[slot] = text;
        metricSize[slot] = size;
        metricFont[slot] = fontGeneration;
        metricWidth[slot] = width;
        metricAdvances[slot] = advances;
        return slot;
    }

    /** Keeps a pangu U+0020 space at its font-defined width instead of widening it further. */
    static boolean hasSupportingTrackingAt(String text, int boundary) {
        if (boundary <= 0 || boundary >= text.length()) return false;
        if (text.charAt(boundary - 1) == ' '
                && isPanguSpaceAt(text, boundary - 1)) return false;
        return text.charAt(boundary) != ' ' || !isPanguSpaceAt(text, boundary);
    }

    private static boolean isPanguSpaceAt(String text, int spaceOffset) {
        return spaceOffset > 0 && spaceOffset + 1 < text.length()
                && text.charAt(spaceOffset) == ' '
                && TextSpacing.needsPanguSpace(text.codePointBefore(spaceOffset),
                        text.codePointAt(spaceOffset + 1));
    }

    /**
     * Carousel state for the supporting line: the item currently shown and when its
     * cycle began. Only one line is ever displayed, so no list is kept.
     */
    private static final class Carousel {
        SupportLineItem item;
        long cycleStartedAt;
    }

    /**
     * One item on the rotating supporting line. Only the custom message is represented
     * now; {@link #equals}/{@link #hashCode} let a text change restart the cycle.
     */
    private static final class SupportLineItem {
        final String text;
        final boolean message;
        final boolean continuousMarquee;

        private SupportLineItem(String text, boolean message, boolean continuousMarquee) {
            this.text = text;
            this.message = message;
            this.continuousMarquee = continuousMarquee;
        }

        static SupportLineItem message(String text, boolean continuousMarquee) {
            return new SupportLineItem(text, true, continuousMarquee);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SupportLineItem)) return false;
            SupportLineItem other = (SupportLineItem) o;
            return ApiCompat.equals(text, other.text)
                    && message == other.message
                    && continuousMarquee == other.continuousMarquee;
        }

        @Override public int hashCode() {
            int hash = text == null ? 0 : text.hashCode() * 31;
            hash = hash * 31 + (message ? 1 : 0);
            return hash * 31 + (continuousMarquee ? 1 : 0);
        }
    }

    /**
     * Draws supporting text (date, lunar date, custom message). Characters are positioned from the
     * advances cached in {@link #supportMetrics}, and consecutive characters that share a typeface
     * are emitted as one {@code drawPosText} run - so a mixed CJK/Latin date costs a handful of
     * native calls instead of one per character.
     */
    private void drawSupportingText(Canvas canvas, String text, float x, float baseline,
            Paint.Align align) {
        if (text == null || text.length() == 0) return;
        int slot = supportMetrics(text);
        char[] glyphs = glyphs(text);
        float width = metricWidth[slot];
        float[] advances = metricAdvances[slot];
        float[] positions = positions(text.length());
        float cursor = align == Paint.Align.CENTER ? x - width / 2f
            : align == Paint.Align.RIGHT ? x - width : x;
        Paint.Align originalAlign = datePaint.getTextAlign();
        Typeface originalTypeface = datePaint.getTypeface();
        datePaint.setTextAlign(Paint.Align.LEFT);
        int runStart = 0;
        int runLength = 0;
        Typeface runTypeface = null;
        for (int start = 0; start < text.length();) {
            int codePoint = text.codePointAt(start);
            int end = start + Character.charCount(codePoint);
            Typeface typeface = supportingTypefaceFor(codePoint);
            if (end - start > 1) {
                // A surrogate pair cannot go through the char-indexed batch without being split,
                // so it is flushed and drawn on its own.
                if (runLength > 0) {
                    datePaint.setTypeface(runTypeface);
                    canvas.drawPosText(glyphs, runStart, runLength, positions, datePaint);
                    runLength = 0;
                }
                datePaint.setTypeface(typeface);
                canvas.drawText(glyphs, start, end - start, cursor, baseline, datePaint);
                cursor += advances[start];
                start = end;
                continue;
            }
            if (runLength > 0 && typeface != runTypeface) {
                datePaint.setTypeface(runTypeface);
                canvas.drawPosText(glyphs, runStart, runLength, positions, datePaint);
                runLength = 0;
            }
            if (runLength == 0) {
                runStart = start;
                runTypeface = typeface;
            }
            positions[runLength * 2] = cursor;
            positions[runLength * 2 + 1] = baseline;
            runLength++;
            cursor += advances[start];
            start = end;
        }
        if (runLength > 0) {
            datePaint.setTypeface(runTypeface);
            canvas.drawPosText(glyphs, runStart, runLength, positions, datePaint);
        }
        datePaint.setTypeface(originalTypeface);
        datePaint.setTextAlign(originalAlign);
    }

    private Typeface supportingTypefaceFor(int codePoint) {
        boolean chinese = ClockTypefaceResolver.isChinese(codePoint);
        Typeface resolved = chinese ? supportingCjkTypeface : supportingLatinTypeface;
        // Only reachable before the first refreshSettings (no repository yet).
        return resolved != null ? resolved : Typeface.DEFAULT;
    }

    /**
     * Reads the style preferences into the snapshot, but only when one of them was written since
     * the last frame. Re-styling four paints and re-reading twenty values per second was the single
     * most expensive part of a frame; both now happen once per settings change.
     */
    private void refreshSettings() {
        if (backgroundRepository == null) {
            return;
        }
        int version = ClockPreferences.writeVersion();
        if (version == settingsVersion) {
            return;
        }
        boolean firstRead = settingsVersion == -1;
        settingsVersion = version;

        backgroundColor = backgroundRepository.getCurrentColor();
        imageMode = ClockPreferences.MODE_IMAGE.equals(backgroundRepository.getBackgroundMode());
        dimBackgroundAlways = backgroundRepository.isDimBackground();
        dimBackgroundScheduled = backgroundRepository.isScheduleDimBackground();
        dimStartMinutes = backgroundRepository.getDimStartMinutes();
        dimEndMinutes = backgroundRepository.getDimEndMinutes();
        timeZoneId = backgroundRepository.getTimeZoneId();
        // The zone object is resolved lazily by resolveTimeZone() on the next draw.
        timeZoneValue = null;

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
        // Resolve the faces once per (family, bold) change rather than on every frame and,
        // for the supporting line, for every single character.
        if (!typefaceResolved || boldText != typefaceBold
                || !ApiCompat.equals(fontFamily, typefaceFamily)) {
            typefaceFamily = fontFamily;
            typefaceBold = boldText;
            timeTypeface = ClockTypefaceResolver.resolveTime(getContext(), fontFamily, boldText);
            supportingLatinTypeface =
                ClockTypefaceResolver.resolveSupporting(getContext(), fontFamily, boldText, false);
            supportingCjkTypeface =
                ClockTypefaceResolver.resolveSupporting(getContext(), fontFamily, boldText, true);
            typefaceResolved = true;
            fontGeneration++;
        }
        timePaint.setTypeface(timeTypeface);
        secondsPaint.setTypeface(timeTypeface);
        periodPaint.setTypeface(timeTypeface);
        datePaint.setTypeface(supportingCjkTypeface);
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
        // A colour, language or size change can alter what is drawn without moving any cached
        // geometry, so the next pass repaints the whole surface instead of the digit rectangle.
        supportLayerValid = false;
        if (!firstRead) {
            fullRepaintQueued = true;
            postInvalidate();
        }
    }

    private void applySupportingTypeface(String text) {
        datePaint.setTypeface(containsChinese(text) ? supportingCjkTypeface : supportingLatinTypeface);
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
        if (timeZoneValue == null) {
            String zoneId = timeZoneId == null ? ClockPreferences.TIME_ZONE_FOLLOW_SYSTEM : timeZoneId;
            timeZoneValue = zoneId.length() == 0
                    ? java.util.TimeZone.getDefault()
                    : java.util.TimeZone.getTimeZone(zoneId);
        }
        return timeZoneValue;
    }

    /**
     * Drops the cached time zone so the next frame re-reads the device zone. Called when
     * the system reports a time-zone change; while the clock follows the system zone the
     * cached value would otherwise stay stale.
     */
    public void invalidateTimeZone() {
        timeZoneValue = null;
        invalidate();
    }

    private boolean shouldDimBackground(Calendar now) {
        if (backgroundRepository == null || !imageMode) {
            return false;
        }
        if (dimBackgroundAlways) {
            return true;
        }
        if (!dimBackgroundScheduled) {
            return false;
        }
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return BackgroundDimSchedule.isActive(currentMinutes, dimStartMinutes, dimEndMinutes);
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
            // The digit transition is a short fade/slide; ~30 fps is visually smooth here and
            // halves the number of onDraw passes on the slow devices this compat build targets.
            // It only moves the digits, so the same rectangle the ticker uses is enough.
            if (timeDirtyValid) {
                postInvalidateDelayed(33L, timeDirty.left, timeDirty.top, timeDirty.right,
                        timeDirty.bottom);
            } else {
                postInvalidateDelayed(33L);
            }
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

        float mainWidth = frame.mainWidth;
        float gapWidth = frame.gapWidth;
        drawTextTransition(canvas, oldTime.mainText, newTime.mainText,
            centerX, mainBaseline, timePaint, progress,
            oldTime.colonVisible, newTime.colonVisible);
        if (newTime.hasPeriod()) {
            float periodWidth = frame.periodWidth;
            drawTextTransition(canvas, oldTime.periodText, newTime.periodText,
                    centerX - mainWidth / 2f - gapWidth - periodWidth / 2f,
                frame.periodBaseline, periodPaint, progress, true, true);
        }
        if (newTime.hasSmallSeconds()) {
            float secondsWidth = frame.secondsWidth;
            drawTextTransition(canvas, oldTime.secondsText, newTime.secondsText,
                    centerX + mainWidth / 2f + gapWidth + secondsWidth / 2f,
                frame.secondsBaseline, secondsPaint, progress, true, true);
        }
    }

    private void drawTextTransition(Canvas canvas, String oldText, String newText,
            float centerX, float baseline, Paint paint, float progress,
            boolean oldColonsVisible, boolean newColonsVisible) {
        int originalAlpha = paint.getAlpha();
        float digitWidth = widestDigitWidth(paint);
        // Pull both strings into the reusable char buffers: Canvas.drawText(String, int, int,
        // ...) copies the whole string into a fresh char[] per call, and this loop runs per
        // character on every animation frame.
        char[] current = glyphs(newText);
        char[] previous = previousGlyphs(oldText);
        float cursor = centerX - stableTimeWidth(newText, paint, digitWidth) / 2f;
        for (int index = 0; index < newText.length(); index++) {
            char newCharacter = current[index];
            char oldCharacter = previous[index];
            float characterWidth = Math.max(
                stableCharacterWidth(current, index, paint, digitWidth),
                stableCharacterWidth(previous, index, paint, digitWidth));
            float characterCenter = cursor + characterWidth / 2f;
            boolean colonVisibilityChanged = newCharacter == ':'
                    && oldColonsVisible != newColonsVisible;
            if (newCharacter == oldCharacter && !colonVisibilityChanged) {
                if (newCharacter == ':' && !newColonsVisible) {
                    cursor += characterWidth;
                    continue;
                }
                setClockTextAlpha(paint, 255);
                canvas.drawText(current, index, 1, characterCenter,
                    alignedCharacterBaseline(newCharacter, baseline, paint), paint);
            } else if (colonVisibilityChanged) {
                int colonAlpha = newColonsVisible
                        ? Math.round(255f * progress)
                        : Math.round(255f * (1f - progress));
                setClockTextAlpha(paint, colonAlpha);
                canvas.drawText(current, index, 1, characterCenter,
                        alignedCharacterBaseline(newCharacter, baseline, paint), paint);
            } else {
                drawChangedCharacterTransition(canvas, previous, current, index,
                    characterCenter, baseline, paint, progress);
            }
            cursor += characterWidth;
        }
        setClockTextAlpha(paint, originalAlpha);
    }

    private void drawChangedCharacterTransition(Canvas canvas, char[] previous, char[] current,
            int index, float centerX, float baseline, Paint paint, float progress) {
        float pivotY = baseline - paint.getTextSize() / 2f;
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(timeTransition)
                || ClockPreferences.TRANSITION_SLIDE_DOWN.equals(timeTransition)) {
            float direction = ClockPreferences.TRANSITION_SLIDE_UP.equals(timeTransition) ? -1f : 1f;
            float distance = paint.getTextSize() * 0.24f;
            drawTransformedCharacter(canvas, previous, index, centerX, baseline, paint,
                Math.round(255f * (1f - progress)), 1f,
                direction * distance * progress, pivotY);
            drawTransformedCharacter(canvas, current, index, centerX, baseline, paint,
                Math.round(255f * progress), 1f,
                -direction * distance * (1f - progress), pivotY);
            return;
        }
        if (ClockPreferences.TRANSITION_SCALE.equals(timeTransition)) {
            drawTransformedCharacter(canvas, previous, index, centerX, baseline, paint,
                Math.round(255f * (1f - progress)), 1f + 0.08f * progress, 0f, pivotY);
            drawTransformedCharacter(canvas, current, index, centerX, baseline, paint,
                Math.round(255f * progress), 0.88f + 0.12f * progress, 0f, pivotY);
            return;
        }
        if (ClockPreferences.TRANSITION_FLIP.equals(timeTransition)) {
            if (progress < 0.5f) {
                drawTransformedCharacter(canvas, previous, index, centerX, baseline, paint,
                    255, Math.max(0.05f, 1f - progress * 2f), 0f, pivotY);
            } else {
                drawTransformedCharacter(canvas, current, index, centerX, baseline, paint,
                    255, Math.max(0.05f, (progress - 0.5f) * 2f), 0f, pivotY);
            }
            return;
        }
        setClockTextAlpha(paint, Math.round(255f * (1f - progress)));
        canvas.drawText(previous, index, 1, centerX,
            alignedCharacterBaseline(previous[index], baseline, paint), paint);
        setClockTextAlpha(paint, Math.round(255f * progress));
        canvas.drawText(current, index, 1, centerX,
            alignedCharacterBaseline(current[index], baseline, paint), paint);
    }

    private void drawTransformedCharacter(Canvas canvas, char[] text, int index,
            float centerX, float baseline, Paint paint, int alpha, float scaleY,
            float translateY, float pivotY) {
        setClockTextAlpha(paint, alpha);
        canvas.save();
        canvas.translate(0f, translateY);
        canvas.scale(1f, scaleY, centerX, pivotY);
        canvas.drawText(text, index, 1, centerX,
            alignedCharacterBaseline(text[index], baseline, paint), paint);
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

        float mainWidth = frame.mainWidth;
        float gapWidth = frame.gapWidth;
        drawMainTimeText(canvas, displayTime.mainText, centerX, mainBaseline,
            displayTime.colonVisible);
        if (displayTime.hasPeriod()) {
            float periodWidth = frame.periodWidth;
            canvas.drawText(displayTime.periodText,
                centerX - mainWidth / 2f - gapWidth - periodWidth / 2f,
                frame.periodBaseline, periodPaint);
        }
        if (displayTime.hasSmallSeconds()) {
            float secondsWidth = frame.secondsWidth;
            drawStableText(canvas, displayTime.secondsText,
                centerX + mainWidth / 2f + gapWidth + secondsWidth / 2f,
                frame.secondsBaseline, secondsPaint);
        }
        setClockTextAlpha(timePaint, oldTimeAlpha);
        setClockTextAlpha(secondsPaint, oldSecondsAlpha);
        setClockTextAlpha(periodPaint, oldPeriodAlpha);
    }

    private void drawMainTimeText(Canvas canvas, String text, float centerX, float baseline,
            boolean colonsVisible) {
        float digitWidth = widestDigitWidth(timePaint);
        char[] source = glyphs(text);
        char[] drawn = drawnChars(text.length());
        float[] positions = positions(text.length());
        float cursor = centerX - stableTimeWidth(text, timePaint, digitWidth) / 2f;
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = source[index];
            if (colonsVisible || character != ':') {
                drawn[count] = character;
                positions[count * 2] = cursor;
                positions[count * 2 + 1] = alignedCharacterBaseline(character, baseline, timePaint);
                count++;
            }
            cursor += stableCharacterWidth(source, index, timePaint, digitWidth);
        }
        if (count == 0) {
            return;
        }
        // drawPosText treats each position as the glyph origin, which is the left edge of the
        // advance - exactly where the old per-character centred draw started.
        Paint.Align originalAlign = timePaint.getTextAlign();
        timePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawPosText(drawn, 0, count, positions, timePaint);
        timePaint.setTextAlign(originalAlign);
    }

    private void drawStableText(Canvas canvas, String text, float centerX,
            float baseline, Paint paint) {
        float digitWidth = widestDigitWidth(paint);
        char[] glyphs = glyphs(text);
        float[] positions = positions(text.length());
        float cursor = centerX - stableTimeWidth(text, paint, digitWidth) / 2f;
        for (int index = 0; index < text.length(); index++) {
            positions[index * 2] = cursor;
            positions[index * 2 + 1] = baseline;
            cursor += stableCharacterWidth(glyphs, index, paint, digitWidth);
        }
        Paint.Align originalAlign = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawPosText(glyphs, 0, text.length(), positions, paint);
        paint.setTextAlign(originalAlign);
    }

    /**
     * Copies {@code text} into the shared glyph buffer so the native
     * {@code drawText(char[], int, int, ...)} / {@code measureText(char[], int, int)}
     * overloads can be used — the String overloads of both allocate a char[] per call.
     */
    private char[] drawnChars(int length) {
        if (drawnBuffer.length < length) {
            drawnBuffer = new char[Math.max(length, drawnBuffer.length * 2)];
        }
        return drawnBuffer;
    }

    private float[] positions(int characters) {
        int needed = characters * 2;
        if (positionBuffer.length < needed) {
            positionBuffer = new float[Math.max(needed, positionBuffer.length * 2)];
        }
        return positionBuffer;
    }

    private char[] glyphs(String text) {
        return copyInto(glyphBuffer, text, true);
    }

    private char[] previousGlyphs(String text) {
        return copyInto(previousGlyphBuffer, text, false);
    }

    private char[] copyInto(char[] buffer, String text, boolean primary) {
        int length = text.length();
        if (buffer.length < length) {
            buffer = new char[Math.max(length, buffer.length * 2)];
            if (primary) {
                glyphBuffer = buffer;
            } else {
                previousGlyphBuffer = buffer;
            }
        }
        text.getChars(0, length, buffer, 0);
        return buffer;
    }

    private float stableTextWidth(String text, Paint paint) {
        return stableTimeWidth(text, paint, widestDigitWidth(paint));
    }

    /** Width of a time string in which every digit occupies the widest digit width. */
    private float stableTimeWidth(String text, Paint paint, float digitWidth) {
        char[] glyphs = glyphs(text);
        float width = 0f;
        for (int index = 0; index < text.length(); index++) {
            width += stableCharacterWidth(glyphs, index, paint, digitWidth);
        }
        return width;
    }

    private static float stableCharacterWidth(char[] text, int index, Paint paint,
            float digitWidth) {
        char character = text[index];
        if (character >= '0' && character <= '9') {
            return digitWidth;
        }
        if (character == ':') {
            return ClockTextLayout.colonWidth(paint);
        }
        return paint.measureText(text, index, 1);
    }

    private static float widestDigitWidth(Paint paint) {
        return ClockTextLayout.widestDigitWidth(paint);
    }

    private float alignedCharacterBaseline(char character, float baseline, Paint paint) {
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
        float size = timePaint.getTextSize();
        if (size != smallSecondsGapSize || smallSecondsGapGeneration != fontGeneration) {
            smallSecondsGapSize = size;
            smallSecondsGapGeneration = fontGeneration;
            smallSecondsGapValue = timePaint.measureText(" ") * SMALL_SECONDS_GAP_SPACE_FRACTION;
        }
        return smallSecondsGapValue;
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
        if (supportLayer != null && !supportLayer.isRecycled()) {
            supportLayer.recycle();
        }
        supportLayer = null;
        supportLayerValid = false;
        super.onDetachedFromWindow();
    }
}
