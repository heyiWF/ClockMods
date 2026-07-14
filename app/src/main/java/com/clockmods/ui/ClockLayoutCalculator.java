package com.clockmods.ui;

public final class ClockLayoutCalculator {
    private ClockLayoutCalculator() {
    }

    /**
     * Computes the text size so that the rendered text width equals
     * {@code widthFraction} of the available screen width (orientation aware,
     * since {@code width} is the current view width). The result is capped by
     * {@code heightFraction} of the height so the text never overflows
     * vertically.
     *
     * @param width                   current view width in pixels
     * @param height                  current view height in pixels
     * @param measuredWidthAtOnePixel text width measured with a text size of 1px
     * @param widthFraction           desired fraction of the width to occupy
     * @param heightFraction          maximum fraction of the height to occupy
     */
    public static float calculateWidthBasedTextSize(int width, int height,
            float measuredWidthAtOnePixel, float widthFraction, float heightFraction) {
        if (measuredWidthAtOnePixel <= 0f) {
            return 1f;
        }
        float widthLimited = width * widthFraction / measuredWidthAtOnePixel;
        float heightLimited = height * heightFraction;
        return Math.max(1f, Math.min(widthLimited, heightLimited));
    }

    public static boolean shouldUseSingleDateLine(boolean landscape, float fullTextWidth, int availableWidth) {
        return landscape && fullTextWidth <= availableWidth * 0.9f;
    }

    public static float centerCropScale(int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        return Math.max((float) viewWidth / bitmapWidth, (float) viewHeight / bitmapHeight);
    }
}