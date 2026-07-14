package com.clockmods.ui;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Holds a single Google Material Symbols glyph (defined on a 960x960 viewport)
 * as a parsed {@link Path} and renders it scaled into an arbitrary destination
 * rectangle. The glyph is drawn filled with the supplied paint so it can be
 * tinted to match the clock color.
 */
final class MaterialIcon {

    /** Material Symbols glyphs use a 960x960 viewport. */
    private static final float VIEWPORT = 960f;

    private final Path basePath;
    private final Path scaledPath = new Path();
    private final Matrix matrix = new Matrix();

    MaterialIcon(String pathData) {
        basePath = SvgPath.parse(pathData);
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
        float scale = size / VIEWPORT;
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(left, top);
        scaledPath.reset();
        basePath.transform(matrix, scaledPath);
        canvas.drawPath(scaledPath, paint);
    }

    // --- Battery Android series (fill proportional to level 0..6). ---

    static final MaterialIcon BATTERY_0 = new MaterialIcon(
            "M160,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L160,290Q160,290 160,290Q160,290 160,290L160,670Q160,670 160,670Q160,670 "
                    + "160,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 103.5,233.5Q127,210 "
                    + "160,210L720,210Q753,210 776.5,233.5Q800,257 800,290L800,380L820,380Q845,380 "
                    + "862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 820,580L800,580L800,670Q800,703 "
                    + "776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_1 = new MaterialIcon(
            "M240,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L240,290L240,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_2 = new MaterialIcon(
            "M320,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L320,290L320,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_3 = new MaterialIcon(
            "M400,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L400,290L400,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_4 = new MaterialIcon(
            "M480,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L480,290L480,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_5 = new MaterialIcon(
            "M560,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L560,290L560,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_6 = new MaterialIcon(
            "M640,670L720,670Q720,670 720,670Q720,670 720,670L720,290Q720,290 720,290Q720,290 "
                    + "720,290L640,290L640,670ZM160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 "
                    + "103.5,233.5Q127,210 160,210L720,210Q753,210 776.5,233.5Q800,257 "
                    + "800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 "
                    + "820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_FULL = new MaterialIcon(
            "M160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 103.5,233.5Q127,210 "
                    + "160,210L720,210Q753,210 776.5,233.5Q800,257 800,290L800,380L820,380Q845,380 "
                    + "862.5,397.5Q880,415 880,440L880,520Q880,546 862.5,563Q845,580 820,580L800,580L800,670Q800,703 "
                    + "776.5,726.5Q753,750 720,750L160,750Z");

    static final MaterialIcon BATTERY_BOLT = new MaterialIcon(
            "M390,630L410,630L590,450L463,450L490,330L470,330L290,510L417,510L390,630ZM160,750Q127,750 "
                    + "103.5,726.5Q80,703 80,670L80,290Q80,257 103.5,233.5Q127,210 160,210L720,210Q753,210 "
                    + "776.5,233.5Q800,257 800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 "
                    + "862.5,563Q845,580 820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750ZM160,670L720,670L720,290L160,290L160,670Z");

    static final MaterialIcon BATTERY_ALERT = new MaterialIcon(
            "M440,630Q457,630 468.5,618.5Q480,607 480,590Q480,573 468.5,561.5Q457,550 440,550Q423,550 "
                    + "411.5,561.5Q400,573 400,590Q400,607 411.5,618.5Q423,630 440,630ZM400,490L480,490L480,330L400,330L400,490Z"
                    + "M160,750Q127,750 103.5,726.5Q80,703 80,670L80,290Q80,257 103.5,233.5Q127,210 160,210L720,210Q753,210 "
                    + "776.5,233.5Q800,257 800,290L800,380L820,380Q845,380 862.5,397.5Q880,415 880,440L880,520Q880,546 "
                    + "862.5,563Q845,580 820,580L800,580L800,670Q800,703 776.5,726.5Q753,750 720,750L160,750ZM160,670L720,670L720,290L160,290L160,670Z");

    static final MaterialIcon[] BATTERY_LEVELS = {
            BATTERY_0, BATTERY_1, BATTERY_2, BATTERY_3, BATTERY_4, BATTERY_5, BATTERY_6
    };

    // --- Network Wifi series (bars 0..3 plus full). ---

    static final MaterialIcon WIFI_1_BAR = new MaterialIcon(
            "M480,840L0,360Q96,262 220,211Q344,160 480,160Q617,160 741,211Q865,262 960,360L480,840Z"
                    + "M361,607Q386,589 416.5,579Q447,569 480,569Q513,569 543.5,579Q574,589 599,607L844,362Q766,303 "
                    + "673.5,271.5Q581,240 480,240Q379,240 286.5,271.5Q194,303 116,362L361,607Z");

    static final MaterialIcon WIFI_2_BAR = new MaterialIcon(
            "M480,840L0,360Q96,262 220,211Q344,160 480,160Q617,160 741,211Q865,262 960,360L480,840Z"
                    + "M299,545Q337,517 383,501.5Q429,486 480,486Q531,486 577,501.5Q623,517 661,545L844,362Q766,303 "
                    + "673.5,271.5Q581,240 480,240Q379,240 286.5,271.5Q194,303 116,362L299,545Z");

    static final MaterialIcon WIFI_3_BAR = new MaterialIcon(
            "M480,840L0,360Q96,262 220,211Q344,160 480,160Q617,160 741,211Q865,262 960,360L480,840Z"
                    + "M232,478Q285,440 348,418.5Q411,397 480,397Q549,397 612,418.5Q675,440 728,478L844,362Q766,303 "
                    + "673.5,271.5Q581,240 480,240Q379,240 286.5,271.5Q194,303 116,362L232,478Z");

    static final MaterialIcon WIFI_FULL = new MaterialIcon(
            "M480,840L0,360Q95,263 219.5,211.5Q344,160 480,160Q617,160 741,211Q865,262 960,360L480,840Z"
                    + "M174,420Q241,372 319,346Q397,320 480,320Q563,320 641,346Q719,372 786,420L844,362Q765,302 "
                    + "672,271Q579,240 480,240Q381,240 288,271Q195,302 116,362L174,420Z");

    /** Indexed by bar count 0..4. Bars are drawn as a dimmed full glyph. */
    static final MaterialIcon[] WIFI_BARS = {
            WIFI_1_BAR, WIFI_1_BAR, WIFI_2_BAR, WIFI_3_BAR, WIFI_FULL
    };

    // --- Signal Cellular series (bars 0..4). ---

    static final MaterialIcon CELLULAR_1_BAR = new MaterialIcon(
            "M80,880L880,80L880,880L80,880ZM400,800L800,800L800,274L400,674L400,800Z");

    static final MaterialIcon CELLULAR_2_BAR = new MaterialIcon(
            "M80,880L880,80L880,880L80,880ZM520,800L800,800L800,274L520,554L520,800Z");

    static final MaterialIcon CELLULAR_3_BAR = new MaterialIcon(
            "M80,880L880,80L880,880L80,880ZM600,800L800,800L800,274L600,474L600,800Z");

    static final MaterialIcon CELLULAR_4_BAR = new MaterialIcon(
            "M80,880L880,80L880,880L80,880Z");

    /** Indexed by bar count 0..4. */
    static final MaterialIcon[] CELLULAR_BARS = {
            CELLULAR_1_BAR, CELLULAR_1_BAR, CELLULAR_2_BAR, CELLULAR_3_BAR, CELLULAR_4_BAR
    };
}
