package com.clockmods.pro.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;

import com.clockmods.pro.CalendarTheme;

/**
 * The gallery thumbnail for one style, drawn from the preset's own colours and its own composition.
 *
 * <p>Since the catalogue holds one entry per composition rather than per colourway, the thumbnail
 * is the only thing that tells two entries apart before you commit to one — so each {@code Kind}
 * gets its own drawing rather than a shared month mock in a different tint. That makes this class
 * a duplicate of the layouts by construction: <strong>when a composition changes shape, the
 * matching {@code draw*} below has to change with it</strong>, or the gallery starts advertising a
 * page that no longer exists.</p>
 *
 * <p>Deliberately not a render of the real view tree: at 64×78dp the layouts' own dp floors — a
 * 48dp toolbar, a 24dp weekday row — are taller than the whole thumbnail, so a real render would
 * come out as chrome and nothing else, and every card would spin up 42 carousels to do it.</p>
 */
public final class CalendarPreviewPainter {
    private final CalendarStyleMetadata.Kind kind;
    private final CalendarTheme theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF shape = new RectF();
    private final GradientDrawable page;
    private final GradientDrawable card;
    private final GradientDrawable panel;

    public CalendarPreviewPainter(CalendarStyle style, float density) {
        kind = style.getMetadata().getKind();
        theme = style.getTheme();
        page = theme.newPageBackground();
        card = theme.newPanelBackground(density);
        panel = theme.newPanelBackground(density);
    }

    /** Scales the page-sized corner radii down with the thumbnail. */
    public void setBounds(int width, int height) {
        float corner = height * 0.055f;
        page.setCornerRadius(corner);
        card.setCornerRadius(corner * 0.8f);
        panel.setCornerRadius(corner * 0.8f);
    }

    public void draw(Canvas canvas, int width, int height) {
        if (width <= 0 || height <= 0) return;
        page.setBounds(0, 0, width, height);
        page.draw(canvas);
        float inset = width * 0.07f;
        switch (kind) {
            case POSTER:
                drawPoster(canvas, width, height, inset);
                break;
            case AGENDA:
                drawAgenda(canvas, width, height, inset);
                break;
            case DASHBOARD:
                drawDashboard(canvas, width, height, inset);
                break;
            case WALL:
            default:
                drawWall(canvas, width, height, inset);
                break;
        }
    }

    // ------------------------------------------------------------------ dashboard and wall

    /** Clock and weather card over a month panel — the shipped composition. */
    private void drawDashboard(Canvas canvas, int width, int height, float inset) {
        float cardBottom = inset + height * 0.30f;
        card.setBounds(Math.round(inset), Math.round(inset),
                Math.round(width - inset), Math.round(cardBottom));
        card.draw(canvas);
        drawClockCard(canvas, width, height, inset, cardBottom);
        float monthTop = cardBottom + height * 0.035f;
        panel.setBounds(Math.round(inset), Math.round(monthTop),
                Math.round(width - inset), Math.round(height - inset));
        panel.draw(canvas);
        drawMonth(canvas, inset, monthTop, width - inset, height - inset);
    }

    /** The same month panel with the clock column dropped, filling the sheet. */
    private void drawWall(Canvas canvas, int width, int height, float inset) {
        panel.setBounds(Math.round(inset), Math.round(inset),
                Math.round(width - inset), Math.round(height - inset));
        panel.draw(canvas);
        drawMonth(canvas, inset, inset, width - inset, height - inset);
    }

    /** A clock readout over a weather line, standing in for the cards the preset keeps. */
    private void drawClockCard(Canvas canvas, int width, int height, float top, float bottom) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.text);
        float barHeight = height * 0.055f;
        float barWidth = width * 0.40f;
        float barTop = top + (bottom - top) * 0.22f;
        shape.set((width - barWidth) / 2f, barTop, (width + barWidth) / 2f, barTop + barHeight);
        canvas.drawRoundRect(shape, barHeight / 2f, barHeight / 2f, paint);

        float iconRadius = height * 0.020f;
        float lineTop = barTop + barHeight + (bottom - barTop - barHeight) * 0.34f;
        float lineWidth = width * 0.22f;
        float lineLeft = (width - (iconRadius * 2f + width * 0.03f + lineWidth)) / 2f;
        paint.setColor(theme.weatherIcon);
        canvas.drawCircle(lineLeft + iconRadius, lineTop + iconRadius, iconRadius, paint);
        paint.setColor(theme.secondary);
        float lineHeight = iconRadius * 1.1f;
        shape.set(lineLeft + iconRadius * 2f + width * 0.03f, lineTop + iconRadius - lineHeight / 2f,
                lineLeft + iconRadius * 2f + width * 0.03f + lineWidth,
                lineTop + iconRadius + lineHeight / 2f);
        canvas.drawRoundRect(shape, lineHeight / 2f, lineHeight / 2f, paint);
    }

    /** Title bar, weekday header and five week rows, with today and a selected day marked. */
    private void drawMonth(Canvas canvas, float left, float top, float right, float bottom) {
        float pad = (right - left) * 0.09f;
        float innerLeft = left + pad;
        float innerRight = right - pad;
        float titleHeight = (bottom - top) * 0.085f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.text);
        shape.set(innerLeft, top + pad * 0.8f,
                innerLeft + (innerRight - innerLeft) * 0.42f, top + pad * 0.8f + titleHeight);
        canvas.drawRoundRect(shape, titleHeight / 2f, titleHeight / 2f, paint);
        float chevron = Math.max(1f, titleHeight * 0.28f);
        paint.setColor(theme.secondary);
        canvas.drawCircle(innerRight - chevron * 4f, shape.centerY(), chevron, paint);
        paint.setColor(theme.accent);
        canvas.drawCircle(innerRight - chevron, shape.centerY(), chevron, paint);

        float gridTop = shape.bottom + (bottom - top) * 0.07f;
        float gridBottom = bottom - pad * 0.8f;
        float columnWidth = (innerRight - innerLeft) / 7f;
        float rowHeight = (gridBottom - gridTop) / 6f;
        float dot = Math.max(1f, Math.min(columnWidth, rowHeight) * 0.30f);
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 7; column++) {
                float centerX = innerLeft + columnWidth * (column + 0.5f);
                float centerY = gridTop + rowHeight * (row + 0.5f);
                if (row == 0) {
                    paint.setColor(theme.weekday);
                    canvas.drawCircle(centerX, centerY, dot * 0.66f, paint);
                    continue;
                }
                if (row == 4 && column == 5) {
                    shape.set(centerX - columnWidth * 0.42f, centerY - rowHeight * 0.40f,
                            centerX + columnWidth * 0.42f, centerY + rowHeight * 0.40f);
                    paint.setColor(theme.selectionFill);
                    canvas.drawRoundRect(shape, dot, dot, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(1f, dot * 0.4f));
                    paint.setColor(theme.selectionStroke);
                    canvas.drawRoundRect(shape, dot, dot, paint);
                    paint.setStyle(Paint.Style.FILL);
                } else if (row == 2 && column == 3) {
                    if (theme.todayFill != 0) {
                        paint.setColor(theme.todayFill);
                        canvas.drawCircle(centerX, centerY, dot * 1.85f, paint);
                    }
                    paint.setColor(theme.today);
                    canvas.drawCircle(centerX, centerY, dot, paint);
                    continue;
                }
                paint.setColor(row == 1 && column == 1 ? theme.restBadge : theme.day);
                canvas.drawCircle(centerX, centerY, dot, paint);
            }
        }
    }

    // ------------------------------------------------------------------ poster

    /**
     * The masthead is the whole point of this one, so the thumbnail spends a third of its height on
     * it. No panel is drawn at all — the absence of a card is what the eye picks up first.
     */
    private void drawPoster(Canvas canvas, int width, int height, float inset) {
        float left = width * 0.11f;
        float right = width - left;
        paint.setStyle(Paint.Style.FILL);

        // 八月 / AUGUST: one heavy slab, at the weight the real wordmark carries.
        float wordTop = height * 0.11f;
        float wordHeight = height * 0.15f;
        paint.setColor(theme.text);
        shape.set(left, wordTop, left + (right - left) * 0.78f, wordTop + wordHeight);
        canvas.drawRoundRect(shape, wordHeight * 0.16f, wordHeight * 0.16f, paint);

        // 2026 beneath it. Grey, not accent: the poster spends its one saturated colour on today,
        // and a red year would promise a page that does not exist.
        float yearHeight = wordHeight * 0.42f;
        float yearTop = wordTop + wordHeight + height * 0.035f;
        paint.setColor(theme.secondary);
        shape.set(left, yearTop, left + (right - left) * 0.34f, yearTop + yearHeight);
        canvas.drawRoundRect(shape, yearHeight / 2f, yearHeight / 2f, paint);

        // The 今天 action, which really does sit out at the right of this line.
        paint.setColor(theme.accent);
        float todayWidth = (right - left) * 0.16f;
        shape.set(right - todayWidth, yearTop + yearHeight * 0.30f, right,
                yearTop + yearHeight * 0.30f + yearHeight * 0.55f);
        canvas.drawRoundRect(shape, shape.height() / 2f, shape.height() / 2f, paint);

        // The hairline that separates masthead from grid.
        float ruleY = yearTop + yearHeight + height * 0.055f;
        paint.setColor(withAlpha(theme.secondary, 0.45f));
        canvas.drawRect(left, ruleY, right, ruleY + Math.max(1f, height * 0.006f), paint);

        float gridTop = ruleY + height * 0.05f;
        float gridBottom = height - inset * 0.9f;
        float columnWidth = (right - left) / 7f;
        float rowHeight = (gridBottom - gridTop) / 6f;
        float dot = Math.max(1f, Math.min(columnWidth, rowHeight) * 0.30f);
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 7; column++) {
                float centerX = left + columnWidth * (column + 0.5f);
                float centerY = gridTop + rowHeight * (row + 0.5f);
                if (row == 0) {
                    paint.setColor(theme.weekday);
                    canvas.drawCircle(centerX, centerY, dot * 0.58f, paint);
                    continue;
                }
                if (row == 2 && column == 3) {
                    // Today recolours the number and underscores it; it is never a filled disc.
                    paint.setColor(theme.today);
                    canvas.drawCircle(centerX, centerY - dot * 0.35f, dot, paint);
                    canvas.drawRect(centerX - columnWidth * 0.30f, centerY + dot * 1.2f,
                            centerX + columnWidth * 0.30f,
                            centerY + dot * 1.2f + Math.max(1f, dot * 0.45f), paint);
                    continue;
                }
                if (row == 4 && column == 5) {
                    // Selection is a rule under the number, not a filled cell.
                    paint.setColor(theme.day);
                    canvas.drawCircle(centerX, centerY - dot * 0.35f, dot, paint);
                    paint.setColor(theme.selectionStroke);
                    canvas.drawRect(centerX - columnWidth * 0.30f, centerY + dot * 1.2f,
                            centerX + columnWidth * 0.30f,
                            centerY + dot * 1.2f + Math.max(1f, dot * 0.45f), paint);
                    continue;
                }
                // Days outside the month recede much further than on a dashboard.
                paint.setColor(row == 5 && column > 3
                        ? withAlpha(theme.day, 0.22f) : theme.day);
                canvas.drawCircle(centerX, centerY, dot, paint);
            }
        }
    }

    // ------------------------------------------------------------------ agenda

    /**
     * Seven cells across the top and one tall card underneath. The card takes most of the
     * thumbnail because that is the trade this composition makes: a week instead of a month, in
     * exchange for a day you can actually read.
     */
    private void drawAgenda(Canvas canvas, int width, int height, float inset) {
        paint.setStyle(Paint.Style.FILL);
        float left = inset;
        float right = width - inset;

        // 2026年8月 / 第35周 heading.
        float headingHeight = height * 0.045f;
        paint.setColor(theme.text);
        shape.set(left, height * 0.06f, left + (right - left) * 0.46f,
                height * 0.06f + headingHeight);
        canvas.drawRoundRect(shape, headingHeight / 2f, headingHeight / 2f, paint);
        // 今天, at the far right of the same line.
        paint.setColor(theme.accent);
        float todayWidth = (right - left) * 0.15f;
        shape.set(right - todayWidth, shape.top + headingHeight * 0.15f, right,
                shape.bottom - headingHeight * 0.05f);
        canvas.drawRoundRect(shape, shape.height() / 2f, shape.height() / 2f, paint);
        float headingBottom = height * 0.06f + headingHeight;

        // The seven-day strip: weekday tick, number, and an underline under the selected day.
        float stripTop = headingBottom + height * 0.045f;
        float stripBottom = stripTop + height * 0.135f;
        float columnWidth = (right - left) / 7f;
        float dot = Math.max(1f, columnWidth * 0.26f);
        int selected = 4;
        for (int column = 0; column < 7; column++) {
            float centerX = left + columnWidth * (column + 0.5f);
            paint.setColor(theme.weekday);
            canvas.drawCircle(centerX, stripTop + dot * 0.8f, dot * 0.5f, paint);
            paint.setColor(column == selected ? theme.accent : theme.day);
            canvas.drawCircle(centerX, stripTop + (stripBottom - stripTop) * 0.52f, dot, paint);
            if (column == selected) {
                canvas.drawRect(centerX - columnWidth * 0.30f, stripBottom - dot * 0.55f,
                        centerX + columnWidth * 0.30f, stripBottom - dot * 0.1f, paint);
            }
        }

        // The detail card fills everything left, which is what makes this shape recognisable.
        float cardTop = stripBottom + height * 0.045f;
        panel.setBounds(Math.round(left), Math.round(cardTop),
                Math.round(right), Math.round(height - inset));
        panel.draw(canvas);

        float pad = (right - left) * 0.10f;
        float lineLeft = left + pad;
        float lineRight = right - pad;
        float lineHeight = height * 0.032f;
        float y = cardTop + pad * 0.85f;

        // Date heading.
        paint.setColor(theme.text);
        shape.set(lineLeft, y, lineRight, y + lineHeight * 1.25f);
        canvas.drawRoundRect(shape, lineHeight * 0.5f, lineHeight * 0.5f, paint);
        y = shape.bottom + lineHeight * 0.85f;

        // 宜 then 忌, the pair that sits tighter than the blocks around it.
        paint.setColor(theme.suitable);
        shape.set(lineLeft, y, lineRight - (lineRight - lineLeft) * 0.10f, y + lineHeight);
        canvas.drawRoundRect(shape, lineHeight / 2f, lineHeight / 2f, paint);
        y = shape.bottom + lineHeight * 0.55f;
        paint.setColor(theme.avoid);
        shape.set(lineLeft, y, lineRight - (lineRight - lineLeft) * 0.24f, y + lineHeight);
        canvas.drawRoundRect(shape, lineHeight / 2f, lineHeight / 2f, paint);
        y = shape.bottom + lineHeight * 1.25f;

        // Schedule rows: an accent time chip and a longer title, twice.
        float chipWidth = (lineRight - lineLeft) * 0.22f;
        for (int row = 0; row < 2 && y + lineHeight < height - inset - pad * 0.4f; row++) {
            paint.setColor(theme.accent);
            shape.set(lineLeft, y, lineLeft + chipWidth, y + lineHeight);
            canvas.drawRoundRect(shape, lineHeight / 2f, lineHeight / 2f, paint);
            paint.setColor(withAlpha(theme.secondary, 0.75f));
            shape.set(lineLeft + chipWidth + (lineRight - lineLeft) * 0.08f, y,
                    lineRight - (lineRight - lineLeft) * (row == 0 ? 0.06f : 0.30f),
                    y + lineHeight);
            canvas.drawRoundRect(shape, lineHeight / 2f, lineHeight / 2f, paint);
            y = shape.bottom + lineHeight * 1.15f;
        }
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
    }
}
