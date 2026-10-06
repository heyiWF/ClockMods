package com.clockmods.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/** The Compose calendar's softly filled circular 宜/忌 label, with centered glyph ink. */
public final class AlmanacBadge {
    public static final float BACKGROUND_ALPHA = .18f;
    private AlmanacBadge() {}

    public static void draw(Canvas canvas, String prefix, float centerX, float centerY,
            float diameter, int color, Paint glyph, Paint background, Rect bounds) {
        background.setColor((color & 0x00FFFFFF) | (Math.round(Color.alpha(color)
                * BACKGROUND_ALPHA) << 24));
        canvas.drawCircle(centerX, centerY, diameter / 2f, background);
        String text = prefix.trim();
        glyph.setColor(color);
        glyph.setTextAlign(Paint.Align.LEFT);
        glyph.getTextBounds(text, 0, text.length(), bounds);
        float inkWidth = Math.max(1f, bounds.width());
        float inkHeight = Math.max(1f, bounds.height());
        float fit = Math.min(1f, diameter * .72f / Math.max(inkWidth, inkHeight));
        if (fit < 1f) {
            glyph.setTextSize(glyph.getTextSize() * fit);
            glyph.getTextBounds(text, 0, text.length(), bounds);
        }
        canvas.drawText(text, centerX - bounds.exactCenterX(),
                centerY - bounds.exactCenterY(), glyph);
    }
}
