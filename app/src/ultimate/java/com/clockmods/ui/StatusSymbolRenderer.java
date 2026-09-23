package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.LruCache;

/** Draws the small, bundled Material Symbols subset with real variable-font axes. */
public final class StatusSymbolRenderer {
    public static final int BATTERY_0 = 0xF30D;
    public static final int BATTERY_1 = 0xF30C;
    public static final int BATTERY_2 = 0xF30B;
    public static final int BATTERY_3 = 0xF30A;
    public static final int BATTERY_4 = 0xF309;
    public static final int BATTERY_5 = 0xF308;
    public static final int BATTERY_6 = 0xF307;
    public static final int BATTERY_FULL = 0xF304;
    public static final int BATTERY_BOLT = 0xF305;
    public static final int WIFI_0 = 0xF0B0;
    public static final int WIFI_1 = 0xEBE4;
    public static final int WIFI_2 = 0xEBD6;
    public static final int WIFI_3 = 0xEBE1;
    public static final int WIFI_4 = 0xE1BA;
    public static final int WIFI_FULL = 0xF065;
    public static final int CELLULAR_0 = 0xF0A8;
    public static final int CELLULAR_1 = 0xF0A9;
    public static final int CELLULAR_2 = 0xF0AA;
    public static final int CELLULAR_3 = 0xF0AB;
    public static final int CELLULAR_4 = 0xE1C8;
    public static final int CELL_TOWER = 0xEBBA;
    public static final int ETHERNET = 0xE8BE;
    public static final int GLOBE_CANCEL = 0xFFFB7;
    public static final int PUBLIC = 0xE80B;

    private static final LruCache<String, Typeface> TYPEFACES = new LruCache<>(48);

    private StatusSymbolRenderer() {}

    private static synchronized Typeface typeface(Context context, StatusIconStyle style, int fill) {
        String key = style.family + ':' + fill + ':' + style.weight + ':' + style.grade
                + ':' + style.opticalSize;
        Typeface cached = TYPEFACES.get(key);
        if (cached != null) return cached;
        String settings = "'FILL' " + fill + ", 'wght' " + style.weight
                + ", 'GRAD' " + style.grade + ", 'opsz' " + style.opticalSize;
        try {
            Typeface loaded = new Typeface.Builder(context.getAssets(),
                    "status_symbols/" + style.family + ".ttf")
                    .setFontVariationSettings(settings)
                    .build();
            if (loaded != null) TYPEFACES.put(key, loaded);
            return loaded;
        } catch (RuntimeException error) {
            return null;
        }
    }

    public static boolean isAvailable(Context context, StatusIconStyle style, int codePoint) {
        return typeface(context, style, style.fillFor(codePoint)) != null;
    }

    /** Returns false when the font cannot load so callers can retain their vector fallback. */
    public static boolean draw(Canvas canvas, Context context, int codePoint,
            StatusIconStyle style, float left, float top, float size, Paint source) {
        Typeface font = typeface(context, style, style.fillFor(codePoint));
        if (font == null) return false;
        String glyph = new String(Character.toChars(codePoint));
        Paint paint = new Paint(source);
        paint.setTypeface(font);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);
        Rect bounds = new Rect();
        paint.getTextBounds(glyph, 0, glyph.length(), bounds);
        if (bounds.isEmpty()) return false;
        canvas.drawText(glyph, left + size / 2f - bounds.exactCenterX(),
                top + size / 2f - bounds.exactCenterY(), paint);
        return true;
    }
}
