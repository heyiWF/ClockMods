package com.clockmods.sdk.clock;

import android.graphics.Bitmap;

/**
 * Optional host-provided background for a clock frame.
 *
 * <p>The host owns the bitmap lifecycle. Renderers may use this information as a base layer and
 * still add their own panels, bezels, and typography above it. A {@code null} background means
 * the style should use its own designed surface.</p>
 */
public final class ClockBackground {
    public enum Mode { THEME, COLOR, IMAGE }

    private final Bitmap bitmap;
    private final int color;
    private final boolean dimmed;
    private final Mode mode;

    private ClockBackground(Bitmap bitmap, int color, boolean dimmed, Mode mode) {
        this.bitmap = bitmap;
        this.color = color;
        this.dimmed = dimmed;
        this.mode = mode;
    }

    public static ClockBackground theme(boolean dimmed) {
        return new ClockBackground(null, 0, dimmed, Mode.THEME);
    }

    public static ClockBackground color(int color, boolean dimmed) {
        return new ClockBackground(null, color, dimmed, Mode.COLOR);
    }

    public static ClockBackground image(Bitmap bitmap, int fallbackColor, boolean dimmed) {
        if (bitmap == null || bitmap.isRecycled()) {
            return color(fallbackColor, dimmed);
        }
        return new ClockBackground(bitmap, fallbackColor, dimmed, Mode.IMAGE);
    }

    public boolean hasImage() {
        return mode == Mode.IMAGE && bitmap != null && !bitmap.isRecycled();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getColor() {
        return color;
    }

    public boolean isDimmed() {
        return dimmed;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean usesThemeSurface() {
        return mode == Mode.THEME;
    }
}
