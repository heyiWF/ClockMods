package com.clockmods.ui;

public final class ClockLayoutCalculator {
    private ClockLayoutCalculator() {
    }

    /**
     * Computes the text size relative to the largest size that still fits the
     * available space. The maximum size is the point where the text either
     * exactly fills the usable width ({@code maxWidthFraction} of the view
     * width) or reaches its vertical cap ({@code heightFraction} of the view
     * height), whichever is smaller. The user controlled {@code sizeFraction}
     * (0..1) then scales that maximum, so a value of 1 always renders the text
     * as large as it can be without overflowing — it never leaves unused width.
     *
     * @param width                   current view width in pixels
     * @param height                  current view height in pixels
     * @param measuredWidthAtOnePixel text width measured with a text size of 1px
     * @param sizeFraction            desired fraction of the maximum fitting size
     * @param heightFraction          maximum fraction of the height to occupy
     * @param maxWidthFraction        maximum fraction of the width the text may span
     */
    public static float calculateWidthBasedTextSize(int width, int height,
            float measuredWidthAtOnePixel, float sizeFraction, float heightFraction,
            float maxWidthFraction) {
        if (measuredWidthAtOnePixel <= 0f) {
            return 1f;
        }
        float widthLimited = width * maxWidthFraction / measuredWidthAtOnePixel;
        float heightLimited = height * heightFraction;
        float maxFittingSize = Math.min(widthLimited, heightLimited);
        return Math.max(1f, maxFittingSize * sizeFraction);
    }

    public static float calculateTimeGroupWidth(float mainWidth, float leftAccessoryWidth,
            float rightAccessoryWidth) {
        return mainWidth + leftAccessoryWidth + rightAccessoryWidth;
    }

    public static float calculateMainCenterOffset(float leftAccessoryWidth,
            float rightAccessoryWidth) {
        return (leftAccessoryWidth - rightAccessoryWidth) / 2f;
    }

    public static boolean shouldUseSingleDateLine(boolean landscape, float fullTextWidth, int availableWidth) {
        return landscape && fullTextWidth <= availableWidth * 0.9f;
    }

    public static float centerCropScale(int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        return Math.max((float) viewWidth / bitmapWidth, (float) viewHeight / bitmapHeight);
    }
}