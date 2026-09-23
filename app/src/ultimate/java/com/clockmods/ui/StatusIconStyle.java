package com.clockmods.ui;

import android.content.Context;
import android.content.SharedPreferences;

/** Material Symbols axes used only by the clock's network and battery status icons. */
public final class StatusIconStyle {
    public static final String OUTLINED = "outlined";
    public static final String ROUNDED = "rounded";
    public static final String SHARP = "sharp";
    public static final String FILL_AUTO = "auto";
    public static final String FILL_OUTLINE = "outline";
    public static final String FILL_SOLID = "solid";

    private static final String PREFERENCES = "clock_prefs";
    private static final String KEY_FAMILY = "status_symbol_family";
    private static final String KEY_FILL = "status_symbol_fill";
    private static final String KEY_WEIGHT = "status_symbol_weight";
    private static final String KEY_GRADE = "status_symbol_grade";
    private static final String KEY_OPTICAL_SIZE = "status_symbol_optical_size";

    public final String family;
    public final String fill;
    public final int weight;
    public final int grade;
    public final int opticalSize;

    public StatusIconStyle(String family, String fill, int weight, int grade, int opticalSize) {
        this.family = ROUNDED.equals(family) || SHARP.equals(family) ? family : OUTLINED;
        this.fill = FILL_OUTLINE.equals(fill) || FILL_SOLID.equals(fill) ? fill : FILL_AUTO;
        this.weight = Math.max(100, Math.min(700, weight));
        this.grade = Math.max(-50, Math.min(200, grade));
        this.opticalSize = Math.max(20, Math.min(48, opticalSize));
    }

    public static StatusIconStyle defaults() {
        return new StatusIconStyle(OUTLINED, FILL_AUTO, 400, 0, 24);
    }

    public static StatusIconStyle read(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return new StatusIconStyle(
                p.getString(KEY_FAMILY, OUTLINED),
                p.getString(KEY_FILL, FILL_AUTO),
                p.getInt(KEY_WEIGHT, 400),
                p.getInt(KEY_GRADE, 0),
                p.getInt(KEY_OPTICAL_SIZE, 24));
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(KEY_FAMILY, family)
                .putString(KEY_FILL, fill)
                .putInt(KEY_WEIGHT, weight)
                .putInt(KEY_GRADE, grade)
                .putInt(KEY_OPTICAL_SIZE, opticalSize)
                .apply();
    }

    public static void reset(SharedPreferences.Editor editor) {
        editor.remove(KEY_FAMILY).remove(KEY_FILL).remove(KEY_WEIGHT)
                .remove(KEY_GRADE).remove(KEY_OPTICAL_SIZE);
    }

    public StatusIconStyle withFamily(String value) {
        return new StatusIconStyle(value, fill, weight, grade, opticalSize);
    }

    public StatusIconStyle withFill(String value) {
        return new StatusIconStyle(family, value, weight, grade, opticalSize);
    }

    public StatusIconStyle withWeight(int value) {
        return new StatusIconStyle(family, fill, value, grade, opticalSize);
    }

    public StatusIconStyle withGrade(int value) {
        return new StatusIconStyle(family, fill, weight, value, opticalSize);
    }

    public StatusIconStyle withOpticalSize(int value) {
        return new StatusIconStyle(family, fill, weight, grade, value);
    }

    public int fillFor(int codePoint) {
        if (FILL_SOLID.equals(fill)) return 1;
        if (FILL_OUTLINE.equals(fill)) return 0;
        return codePoint == StatusSymbolRenderer.BATTERY_BOLT ? 1 : 0;
    }

    /** Keep existing vector paths for users who have not selected a custom style. */
    public boolean isDefault() {
        return OUTLINED.equals(family) && FILL_AUTO.equals(fill)
                && weight == 400 && grade == 0 && opticalSize == 24;
    }
}
