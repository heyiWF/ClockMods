package com.clockmods.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Holds a single Google Material Symbols glyph and renders it scaled into an
 * arbitrary destination rectangle. The glyph is drawn filled with the supplied
 * paint so it can be tinted to match the clock color.
 *
 * <p>The path data is taken verbatim from the Material Symbols SVG exports,
 * which use a {@code viewBox="0 -960 960 960"} coordinate system (Y ranges from
 * -960 at the top to 0 at the bottom). The parsed path is translated by +960 in
 * Y so it fits the positive {@code 0..960} viewport used for rendering.</p>
 */
final class MaterialIcon {

    /** Material Symbols glyphs use a 960x960 viewport. */
    private static final float VIEWPORT = 960f;

    private final Path basePath;
        private final float verticalCenter;
    private final Path scaledPath = new Path();
    private final Matrix matrix = new Matrix();
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap;
    private int bitmapSize;
    private int bitmapColor;
    private int bitmapAlpha;

    MaterialIcon(String pathData) {
        Path parsed = SvgPath.parse(pathData);
        // Material Symbols exports use viewBox "0 -960 960 960"; shift the glyph
        // into the positive 0..960 space expected by the renderer.
        Matrix shift = new Matrix();
        shift.setTranslate(0f, VIEWPORT);
        Path shifted = new Path();
        parsed.transform(shift, shifted);
        basePath = shifted;
        RectF bounds = new RectF();
        basePath.computeBounds(bounds, true);
        verticalCenter = bounds.centerY();
    }

    /**
     * Draws the glyph centered inside the given square-ish bounds while
     * preserving aspect ratio (icons are square by definition).
     *
     * @param canvas target canvas
     * @param left   left edge of the destination box
     * @param top    top edge of the destination box
     * @param size   width/height of the destination box in pixels
     * @param paint  fill paint (color/alpha/shadow already configured)
     */
    void draw(Canvas canvas, float left, float top, float size, Paint paint) {
        int targetSize = Math.max(1, (int) Math.ceil(size));
        int color = paint.getColor();
        int alpha = paint.getAlpha();
        if (bitmap == null || bitmapSize != targetSize || bitmapColor != color || bitmapAlpha != alpha) {
            bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(bitmap);
            matrix.reset();
            matrix.setScale(targetSize / VIEWPORT, targetSize / VIEWPORT);
            matrix.postTranslate(0f, targetSize / 2f - verticalCenter * targetSize / VIEWPORT);
            scaledPath.reset();
            basePath.transform(matrix, scaledPath);
            iconPaint.setColor(color);
            iconPaint.setAlpha(alpha);
            iconPaint.setStyle(Paint.Style.FILL);
            bitmapCanvas.drawPath(scaledPath, iconPaint);
            bitmapSize = targetSize;
            bitmapColor = color;
            bitmapAlpha = alpha;
        }
        canvas.drawBitmap(bitmap, left, top, null);
    }

    // --- Battery Android series (fill proportional to level 0..6 plus Full). ---

    static final MaterialIcon BATTERY_0 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm0-80h540q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H160q-17 "
                    + "0-28.5 11.5T120-600v240q0 17 11.5 28.5T160-320Zm700-60v-200h20q17 0 28.5 11.5T920-540v120q0 "
                    + "17-11.5 28.5T880-380h-20Zm-740 60v-320 320Z");

    static final MaterialIcon BATTERY_1 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm80-80h460q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H240v320Zm620-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_2 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm160-80h380q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H320v320Zm540-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_3 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm240-80h300q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H400v320Zm460-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_4 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm320-80h220q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H480v320Zm380-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_5 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm400-80h140q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640H560v320Zm300-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_6 = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm480-80h60q17 0 28.5-11.5T740-360v-240q0-17-11.5-28.5T700-640h-60v320Zm220-60v-200h20q17 "
                    + "0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_FULL = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h540q50 0 85 35t35 85v240q0 50-35 "
                    + "85t-85 35H160Zm700-140v-200h20q17 0 28.5 11.5T920-540v120q0 17-11.5 28.5T880-380h-20Z");

    static final MaterialIcon BATTERY_BOLT = new MaterialIcon(
            "M160-240q-50 0-85-35t-35-85v-240q0-50 35-85t85-35h562l-64 80H160q-17 0-28.5 "
                    + "11.5T120-600v240q0 17 11.5 28.5T160-320h473l-15 80H160Zm-40-80v-320 320Zm587 "
                    + "40 28-160H600l192-240h21l-28 160h135L728-280h-21Z");

    /** Indexed 0..7 -> Battery Android 0..6 then Full. */
    static final MaterialIcon[] BATTERY_LEVELS = {
            BATTERY_0, BATTERY_1, BATTERY_2, BATTERY_3, BATTERY_4, BATTERY_5, BATTERY_6, BATTERY_FULL
    };

    // --- Wi-Fi series: Signal Wifi 0 Bar, Network Wifi 1/2/3 Bar, Signal Wifi 4 Bar. ---

    static final MaterialIcon WIFI_0_BAR = new MaterialIcon(
            "M480-120 0-600q95-97 219.5-148.5T480-800q136 0 260.5 51.5T960-600L480-120Zm0-114 "
                    + "364-364q-79-60-172-91t-192-31q-99 0-192 31t-172 91l364 364Z");

    static final MaterialIcon WIFI_1_BAR = new MaterialIcon(
            "M480-120 0-600q96-98 220-149t260-51q137 0 261 51t219 149L480-120ZM361-353q25-18 "
                    + "55.5-28t63.5-10q33 0 63.5 10t55.5 28l245-245q-78-59-170.5-90.5T480-720q-101 "
                    + "0-193.5 31.5T116-598l245 245Z");

    static final MaterialIcon WIFI_2_BAR = new MaterialIcon(
            "M480-120 0-600q96-98 220-149t260-51q137 0 261 51t219 149L480-120ZM299-415q38-28 "
                    + "84-43.5t97-15.5q51 0 97 15.5t84 43.5l183-183q-78-59-170.5-90.5T480-720q-101 "
                    + "0-193.5 31.5T116-598l183 183Z");

    static final MaterialIcon WIFI_3_BAR = new MaterialIcon(
            "M480-120 0-600q96-98 220-149t260-51q137 0 261 51t219 149L480-120ZM232-482q53-38 "
                    + "116-59.5T480-563q69 0 132 21.5T728-482l116-116q-78-59-170.5-90.5T480-720q-101 "
                    + "0-193.5 31.5T116-598l116 116Z");

    static final MaterialIcon WIFI_4_BAR = new MaterialIcon(
            "M480-120 0-600q95-97 219.5-148.5T480-800q136 0 260.5 51.5T960-600L480-120Z");

    /** Indexed by bar count 0..4. */
    static final MaterialIcon[] WIFI_BARS = {
            WIFI_0_BAR, WIFI_1_BAR, WIFI_2_BAR, WIFI_3_BAR, WIFI_4_BAR
    };

    // --- Signal Cellular series (bars 0..4). ---

    static final MaterialIcon CELLULAR_0_BAR = new MaterialIcon(
            "m80-80 800-800v800H80Zm193-80h527v-526L273-160Z");

    static final MaterialIcon CELLULAR_1_BAR = new MaterialIcon(
            "m80-80 800-800v800H80Zm320-80h400v-526L400-286v126Z");

    static final MaterialIcon CELLULAR_2_BAR = new MaterialIcon(
            "m80-80 800-800v800H80Zm440-80h280v-526L520-406v246Z");

    static final MaterialIcon CELLULAR_3_BAR = new MaterialIcon(
            "m80-80 800-800v800H80Zm520-80h200v-526L600-486v326Z");

    static final MaterialIcon CELLULAR_4_BAR = new MaterialIcon(
            "m80-80 800-800v800H80Z");

    /** Indexed by bar count 0..4. */
    static final MaterialIcon[] CELLULAR_BARS = {
            CELLULAR_0_BAR, CELLULAR_1_BAR, CELLULAR_2_BAR, CELLULAR_3_BAR, CELLULAR_4_BAR
    };

    // --- Ethernet (no signal strength). ---

    static final MaterialIcon ETHERNET = new MaterialIcon(
            "m680-240-56-56 182-184-182-184 56-56 240 240-240 240Zm-400 0L40-480l240-240 56 "
                    + "56-182 184 182 184-56 56Zm11.5-211.5Q280-463 280-480t11.5-28.5Q303-520 320-520t28.5 "
                    + "11.5Q360-497 360-480t-11.5 28.5Q337-440 320-440t-28.5-11.5Zm160 0Q440-463 440-480t11.5-28.5Q463-520 "
                    + "480-520t28.5 11.5Q520-497 520-480t-11.5 28.5Q497-440 480-440t-28.5-11.5Zm160 0Q600-463 600-480t11.5-28.5Q623-520 "
                    + "640-520t28.5 11.5Q680-497 680-480t-11.5 28.5Q657-440 640-440t-28.5-11.5Z");

    // --- No network (Globe 2 Cancel). ---

    static final MaterialIcon GLOBE_CANCEL = new MaterialIcon(
            "m696-80-56-56 84-84-84-84 56-56 84 84 84-84 56 56-83 84 83 84-56 56-84-83-84 "
                    + "83Zm-216 0q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 "
                    + "127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 10-.5 20t-1.5 20h-81q2-10 "
                    + "2.5-20t.5-20q0-20-2.5-40t-7.5-40H654q3 20 4.5 40t1.5 40v20q0 10-1 20h-80q1-10 1-20v-20q0-20-1.5-40t-4.5-40H386q-3 "
                    + "20-4.5 40t-1.5 40q0 20 1.5 40t4.5 40h174v80H404q12 43 31 82.5t45 75.5q18 0 35.5-2t35.5-4l18 "
                    + "78q-23 5-44.5 7.5T480-80ZM170-400h136q-3-20-4.5-40t-1.5-40q0-20 1.5-40t4.5-40H170q-5 20-7.5 "
                    + "40t-2.5 40q0 20 2.5 40t7.5 40Zm34-240h118q9-37 22.5-72.5T376-782q-55 18-99 54.5T204-640Zm172 "
                    + "462q-18-34-31.5-69.5T322-320H204q29 51 73 87.5t99 54.5Zm28-462h152q-12-43-31-82.5T480-798q-26 "
                    + "36-45 75.5T404-640Zm234 0h118q-29-51-73-87.5T584-782q18 34 31.5 69.5T638-640Z");
}
