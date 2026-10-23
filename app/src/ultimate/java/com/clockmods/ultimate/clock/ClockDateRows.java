package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import com.clockmods.sdk.clock.ClockRenderContext;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Measured date blocks for clock compositions with a fixed reading order. */
final class ClockDateRows {
    private ClockDateRows() {}

    static String[] orbit(String[] dateAndLunar, Calendar calendar, Locale locale) {
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String weekday = symbols.getWeekdays()[day];
        String shortDay = symbols.getShortWeekdays()[day];
        String solar = dateAndLunar[0];
        // Remove only weekday tokens, never a bare Japanese 日 inside a date.
        for (String token : new String[]{weekday, shortDay}) {
            if (token.isEmpty()) continue;
            String quoted = Pattern.quote(token);
            solar = solar.replaceAll("[（(]\\s*" + quoted + "\\s*[）)]", "");
            if (token.length() > 1) solar = solar.replace(token, "");
            else solar = solar.replaceAll("(^|\\s)" + quoted + "(?=\\s|$)", "$1");
        }
        solar = solar.replaceAll("^[\\s,，·|]+|[\\s,，·|]+$", "").trim();
        return new String[]{solar, weekday, dateAndLunar[1]};
    }

    /** Returns the actual bottom, so following time/supporting text can keep a real gap. */
    static float draw(Canvas canvas, ClockRenderContext context, String[] source,
            float x, float anchorY, float maxWidth, float requested, int color,
            Typeface face, Paint paint, boolean bottomAnchored, boolean allowOverflow) {
        List<String> rows = new ArrayList<>();
        for (String row : source) if (row != null && !row.trim().isEmpty()) rows.add(row.trim());
        if (rows.isEmpty()) return anchorY;
        float width = Math.max(1f, Math.min(maxWidth,
                context.getRight() - context.getDensity() * 12f - x));
        paint.setTypeface(face); paint.setTextSize(requested); paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        if (!allowOverflow) {
            float longest = 1f;
            for (String row : rows) longest = Math.max(longest, paint.measureText(row));
            paint.setTextSize(Math.max(context.getScaledDensity() * 12f,
                    requested * Math.min(1f, width / longest)));
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float rowHeight = metrics.descent - metrics.ascent;
        float gap = Math.max(context.getDensity() * 6f, paint.getTextSize() * .35f);
        float height = rowHeight * rows.size() + gap * (rows.size() - 1);
        float top = bottomAnchored ? anchorY - height : anchorY;
        com.clockmods.sdk.clock.ClockOverlayBounds overlay = context.getStatusOverlay();
        float textWidth = 0f;
        for (String row : rows) textWidth = Math.max(textWidth, Math.min(width, paint.measureText(row)));
        if (!allowOverflow && overlay != null && overlay.spansHorizontally(x, x + textWidth)
                && overlay.spansVertically(top, top + height)) {
            top = bottomAnchored ? overlay.getTop() - gap - height : overlay.getBottom() + gap;
        }
        float baseline = top - metrics.ascent;
        for (String row : rows) {
            String text = row;
            if (!allowOverflow && paint.measureText(text) > width) {
                int end = text.length();
                while (end > 0 && paint.measureText(text.substring(0, end) + "…") > width)
                    end = text.offsetByCodePoints(end, -1);
                text = text.substring(0, end) + "…";
            }
            canvas.drawText(text, x, baseline, paint);
            baseline += rowHeight + gap;
        }
        return top + height;
    }
}
