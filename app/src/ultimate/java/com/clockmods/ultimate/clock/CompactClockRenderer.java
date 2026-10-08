package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockThemeTokens;
import com.clockmods.sdk.style.ResponsiveTextPolicy;
import com.clockmods.ui.ClockTimeFormatter;
import java.util.Calendar;

/** Compact composition for extreme windows, retaining the selected theme's palette and fonts. */
final class CompactClockRenderer extends UltimateClockStyles.RendererBase {

    @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
            ClockThemeTokens theme) {
        background(canvas, context, theme);
        Paint metrics = fill(theme.getPrimaryTextColor());
        float width = context.getWidth(), height = context.getHeight();
        if (width <= 0f || height <= 0f) return;
        float radius = Math.min(width, height) * .06f;
        canvas.drawRoundRect(context.getLeft() + width * .02f, context.getTop() + height * .02f,
                context.getRight() - width * .02f, context.getBottom() - height * .02f,
                radius, radius, fill(theme.getSurfaceColor()));
        float top = context.getTop() + height * .04f;
        if (context.getStatusOverlay() != null) top = Math.max(top,
                Math.min(context.getBottom(), context.getStatusOverlay().getBottom() + height * .02f));
        float usableWidth = width * .92f;
        float usableHeight = Math.max(0f, context.getBottom() - context.getBottomInset() - height * .04f - top);
        if (usableHeight <= 0f) return;
        Typeface display = displayTypeface(theme, Typeface.NORMAL);
        Typeface supporting = supportingTypeface(theme, Typeface.NORMAL);
        Calendar calendar = state.newCalendar();
        ClockTimeFormatter.DisplayTime time = ClockTimeFormatter.format(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), state.isShowSeconds(),
                false, false, state.isUse24Hour(), state.getLocale().getLanguage().equals("en"));
        String[] digits = width < height * .45f ? time.mainText.split(":") : new String[] {time.mainText};
        String support = contextText(state);
        if (support.isEmpty()) support = state.getTimeZoneText();
        boolean dateVisible = !state.getDateText().isEmpty();
        boolean supportVisible = !support.isEmpty();
        float secondaryHeight = usableHeight * .12f;
        float periodHeight = time.periodText.isEmpty() ? 0f : secondaryHeight;
        float primaryHeight = usableHeight - periodHeight
                - (dateVisible ? secondaryHeight : 0f) - (supportVisible ? secondaryHeight : 0f);
        float primarySize = Float.MAX_VALUE;
        metrics.setTypeface(display); metrics.setTextSize(1f);
        for (String digit : digits) primarySize = Math.min(primarySize,
                ResponsiveTextPolicy.fitLine(primaryHeight * state.getTimeScale(), metrics.measureText(digit),
                        usableWidth, primaryHeight / digits.length, metrics.descent() - metrics.ascent()));
        metrics.setTextSize(primarySize);
        for (int i = 0; i < digits.length; i++) {
            String text = time.colonVisible ? digits[i] : digits[i].replace(':', ' ');
            float center = top + primaryHeight * (i + .5f) / digits.length;
            drawTimeWithPaint(canvas, text, context.getCenterX(),
                    center - (metrics.ascent() + metrics.descent()) / 2f, centeredPaint(theme, display, primarySize));
        }
        float y = top + primaryHeight;
        float baseSize = Math.min(usableWidth * .065f, secondaryHeight * .65f);
        if (periodHeight > 0f) { line(canvas, time.periodText, context.getCenterX(), y, usableWidth,
                periodHeight, baseSize, theme.getPrimaryTextColor(), supporting); y += periodHeight; }
        if (dateVisible) { line(canvas, state.getDateText(), context.getCenterX(), y, usableWidth,
                secondaryHeight, baseSize * state.getDateScale(), theme.getPrimaryTextColor(), supporting); y += secondaryHeight; }
        if (supportVisible) {
            float size = Math.min(baseSize * state.getSupportingScale(), secondaryHeight * .65f);
            metrics.setTypeface(supporting); metrics.setTextSize(size);
            float baseline = y + secondaryHeight / 2f - (metrics.ascent() + metrics.descent()) / 2f;
            if (state.isMessageActive()) {
                textMessageMarquee(canvas, context, support, context.getCenterX(), baseline,
                        usableWidth, size, theme.getPrimaryTextColor(), Paint.Align.CENTER, supporting,
                        state.getMessageScrollElapsedMillis(), state.isMessageContinuous());
            } else {
                line(canvas, support, context.getCenterX(), y, usableWidth,
                        secondaryHeight, size, theme.getPrimaryTextColor(), supporting);
            }
        }
    }

    private Paint centeredPaint(ClockThemeTokens theme, Typeface face, float size) {
        Paint paint = fill(theme.getPrimaryTextColor());
        paint.setTypeface(face); paint.setTextSize(size); paint.setColor(theme.getPrimaryTextColor());
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    private void line(Canvas canvas, String value, float x, float top, float width, float height,
            float desired, int color, Typeface face) {
        Paint metrics = fill(color);
        metrics.setTypeface(face); metrics.setTextSize(1f);
        float size = ResponsiveTextPolicy.fitLine(desired, metrics.measureText(value), width, height,
                metrics.descent() - metrics.ascent());
        metrics.setTextSize(size);
        text(canvas, value, x, top + height / 2f - (metrics.ascent() + metrics.descent()) / 2f,
                size, color, Paint.Align.CENTER, face);
    }
}
