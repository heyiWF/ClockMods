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
import com.clockmods.background.BackgroundDimSchedule;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.LunarCalendar;
import com.clockmods.time.NetworkTimeProvider;
import com.clockmods.weather.WeatherModels;
import com.clockmods.weather.WeatherModels.WeatherState;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClockView extends View {
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long TIME_TRANSITION_DURATION_MILLIS = 300L;
    private static final float SMALL_SECONDS_GAP_SPACE_FRACTION = 0.35f;
    private static final float SUPPORTING_TEXT_LETTER_SPACING = 0.025f;
    private static final int CLOCK_SHADOW_ALPHA = 0x66;
    private static final int DIM_BACKGROUND_OVERLAY_COLOR = 0x80000000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            long now = System.currentTimeMillis();
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
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final NetworkTimeProvider networkTimeProvider = new NetworkTimeProvider();
    private final SimpleDateFormat chineseDateFormat = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
    private final SimpleDateFormat englishDateFormat = new SimpleDateFormat("yyyy/M/d", Locale.US);
    private final String[] chineseWeekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    private final String[] englishWeekDays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
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
    private ClockTimeFormatter.DisplayTime displayedTime;
    private ClockTimeFormatter.DisplayTime previousTime;
    private long timeTransitionStartedAt;
    private float clockShadowRadius;
    private float clockShadowDy;
    private WeatherState weatherState;

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
        invalidate();
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
                        replaceBackgroundBitmap(result, requestedWidth, requestedHeight);
                    }
                });
            }
        });
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
        now.setTimeInMillis(networkTimeProvider.currentTimeMillis());
        if (shouldDimBackground(now)) {
            canvas.drawColor(DIM_BACKGROUND_OVERLAY_COLOR);
        }

        applyTextStyles();

        chineseDateFormat.setTimeZone(timeZone);
        englishDateFormat.setTimeZone(timeZone);
        ClockTimeFormatter.DisplayTime displayTime = ClockTimeFormatter.format(
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND),
            showSeconds, blinkColon, smallSeconds, use24Hour, clockUseEnglish);
        int weekDayIndex = now.get(Calendar.DAY_OF_WEEK) - 1;
        String dateText = clockUseEnglish
                ? englishDateFormat.format(now.getTime()) + " " + englishWeekDays[weekDayIndex]
                : chineseDateFormat.format(now.getTime()) + chineseWeekDays[weekDayIndex];
        String lunarText = showLunar ? LunarCalendar.format(now) : "";

        String fullDate = lunarText.length() == 0 ? dateText : dateText + " " + lunarText;

        // In landscape the date and lunar text share a single line; in portrait
        // they stack. The width-based size is computed against whichever string
        // actually spans the full line so it fits the requested screen-width %.
        boolean singleDateLine = lunarText.length() == 0 || width > height;
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
            width, height, measuredTimeWidth, timeFontScale, 1f);
        float dateSize = ClockLayoutCalculator.calculateWidthBasedTextSize(
            width, height, measureSupportingText(widestDateText), dateFontScale, 0.14f);
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

        drawAnimatedTime(canvas, displayTime, timeCenterX, timeBaseline);
        drawWeather(canvas, centerX, timeBaseline, dateSize, timeMetrics, gap);
        if (lunarText.length() == 0) {
            applySupportingTypeface(dateText);
            drawSupportingText(canvas, dateText, centerX, dateBaseline, Paint.Align.CENTER);
            return;
        }

        if (singleDateLine) {
            applySupportingTypeface(fullDate);
            drawSupportingText(canvas, fullDate, centerX, dateBaseline, Paint.Align.CENTER);
        } else {
            applySupportingTypeface(dateText);
            drawSupportingText(canvas, dateText, centerX, dateBaseline, Paint.Align.CENTER);
            applySupportingTypeface(lunarText);
            drawSupportingText(canvas, lunarText, centerX,
                dateBaseline - dateMetrics.ascent + gap * 0.5f, Paint.Align.CENTER);
        }
    }

        private void drawWeather(Canvas canvas, float centerX, float timeBaseline,
            float dateSize, Paint.FontMetrics timeMetrics, float gap) {
        if (weatherState == null || backgroundRepository == null || !backgroundRepository.isWeatherEnabled()) return;
        String text = weatherState.message;
        String leftText = null;
        String rightText = null;
        WeatherIcon icon = null;
        if (weatherState.data != null) {
            leftText = WeatherModels.locationText(weatherState.data.city, weatherState.data.district);
            rightText = weatherState.data.text + " " + weatherState.data.temperature + "℃";
            icon = WeatherIcon.load(getContext(), weatherState.data.icon);
            text = leftText + "  " + rightText;
        }
        if (text == null || text.length() == 0) return;
        float originalSize = datePaint.getTextSize();
        Typeface originalTypeface = datePaint.getTypeface();
        applySupportingTypeface(text);
        float iconSize = dateSize * 0.95f;
        float iconGap = dateSize * 0.25f;
        float measured = icon == null ? measureSupportingText(text)
            : measureSupportingText(leftText) + measureSupportingText(rightText)
                + iconSize + iconGap * 2f;
        float available = getWidth() * 0.92f;
        float scale = measured > available ? Math.max(available / measured, 0.65f) : 1f;
        datePaint.setTextSize(dateSize * scale);
        iconSize *= scale;
        iconGap *= scale;
        Paint.FontMetrics weatherMetrics = datePaint.getFontMetrics();
        float baseline = timeBaseline + timeMetrics.descent + gap - weatherMetrics.ascent;
        if (icon == null) {
            drawSupportingText(canvas, text, centerX, baseline, Paint.Align.CENTER);
        } else {
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
        }
        datePaint.setTextSize(originalSize);
        datePaint.setTypeface(originalTypeface);
    }

    private float measureSupportingText(String text) {
        if (text == null || text.length() == 0) return 0f;
        int characterCount = text.codePointCount(0, text.length());
        return datePaint.measureText(text)
            + Math.max(0, characterCount - 1)
                * datePaint.getTextSize() * SUPPORTING_TEXT_LETTER_SPACING;
    }

    private void drawSupportingText(Canvas canvas, String text, float x, float baseline,
            Paint.Align align) {
        if (text == null || text.length() == 0) return;
        float width = measureSupportingText(text);
        float cursor = align == Paint.Align.CENTER ? x - width / 2f
            : align == Paint.Align.RIGHT ? x - width : x;
        Paint.Align originalAlign = datePaint.getTextAlign();
        datePaint.setTextAlign(Paint.Align.LEFT);
        float spacing = datePaint.getTextSize() * SUPPORTING_TEXT_LETTER_SPACING;
        for (int start = 0; start < text.length();) {
            int end = start + Character.charCount(text.codePointAt(start));
            canvas.drawText(text, start, end, cursor, baseline, datePaint);
            cursor += datePaint.measureText(text, start, end);
            if (end < text.length()) cursor += spacing;
            start = end;
        }
        datePaint.setTextAlign(originalAlign);
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
        networkTimeProvider.setEnabled(backgroundRepository.isUseNetworkTime());
        networkTimeProvider.setSyncIntervalMinutes(backgroundRepository.getSyncIntervalMinutes());
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
        return stableTextWidth(text, paint, widestDigitWidth(paint));
    }

    private static float stableTextWidth(String text, Paint paint, float digitWidth) {
        float width = 0f;
        for (int index = 0; index < text.length(); index++) {
            width += stableCharacterWidth(text.substring(index, index + 1), paint, digitWidth);
        }
        return width;
    }

    private static float stableCharacterWidth(String character, Paint paint, float digitWidth) {
        if (character.length() != 1 || character.charAt(0) < '0' || character.charAt(0) > '9') {
            return paint.measureText(character);
        }
        return digitWidth;
    }

    private static float widestDigitWidth(Paint paint) {
        float widestDigit = 0f;
        for (char digit = '0'; digit <= '9'; digit++) {
            widestDigit = Math.max(widestDigit, paint.measureText(String.valueOf(digit)));
        }
        return widestDigit;
    }

    private float alignedCharacterBaseline(String character, float baseline, Paint paint) {
        if (paint != timePaint || !":".equals(character)) {
            return baseline;
        }
        Rect digitBounds = new Rect();
        Rect colonBounds = new Rect();
        paint.getTextBounds("0", 0, 1, digitBounds);
        paint.getTextBounds(character, 0, 1, colonBounds);
        float digitCenter = (digitBounds.top + digitBounds.bottom) / 2f;
        float colonCenter = (colonBounds.top + colonBounds.bottom) / 2f;
        return baseline + digitCenter - colonCenter;
    }

    private void setClockTextAlpha(Paint paint, int alpha) {
        paint.setAlpha(alpha);
        int shadowAlpha = Math.round(CLOCK_SHADOW_ALPHA * alpha / 255f);
        paint.setShadowLayer(clockShadowRadius, 0f, clockShadowDy, shadowAlpha << 24);
    }

    private static float bottomAlignedBaseline(float mainBaseline, Paint mainPaint,
            Paint accessoryPaint) {
        Rect mainDigitBounds = new Rect();
        Rect accessoryDigitBounds = new Rect();
        mainPaint.getTextBounds("0", 0, 1, mainDigitBounds);
        accessoryPaint.getTextBounds("0", 0, 1, accessoryDigitBounds);
        return mainBaseline + mainDigitBounds.bottom - accessoryDigitBounds.bottom;
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
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        imageExecutor.shutdownNow();
        networkTimeProvider.shutdown();
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        super.onDetachedFromWindow();
    }
}