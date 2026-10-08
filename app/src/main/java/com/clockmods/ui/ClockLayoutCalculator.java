package com.clockmods.ui;

public final class ClockLayoutCalculator {
    private ClockLayoutCalculator() {
    }

    /** Reserve the primary line first; each side may contain two supporting rows and a gap. */
    public static float capSupportingSize(float desired, float height, float primaryHeight,
            float lineHeightAtOne) {
        float side = Math.max(0f, (height * .88f - primaryHeight) / 2f);
        return Math.max(0f, Math.min(desired, side / (Math.max(.01f, lineHeightAtOne) * 2.5f + .9f)));
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
        float fraction = Float.isNaN(sizeFraction) || Float.isInfinite(sizeFraction)
                ? 1f : Math.max(0f, Math.min(1f, sizeFraction));
        return Math.max(1f, maxFittingSize * fraction);
    }

    public static float calculateTimeGroupWidth(float mainWidth, float leftAccessoryWidth,
            float rightAccessoryWidth) {
        return mainWidth + leftAccessoryWidth + rightAccessoryWidth;
    }

    /**
     * Caps a size so the row it is drawn at still fits the width. The sizes above are built to
     * fit at their default factor, and the size sliders scale them from there; past 100% that
     * would carry a long row off both edges of the frame, so growth stops where the row fills
     * {@code maxWidthFraction} of the width instead. At or below the default factor this never
     * binds, so an untouched face is unchanged.
     *
     * @param size                    size the row would otherwise be drawn at
     * @param measuredWidthAtOnePixel text width measured with a text size of 1px
     * @param width                   current view width in pixels
     * @param maxWidthFraction        maximum fraction of the width the text may span
     */
    public static float capToWidth(float size, float measuredWidthAtOnePixel, int width,
            float maxWidthFraction) {
        if (measuredWidthAtOnePixel <= 0f) {
            return size;
        }
        return Math.min(size, width * maxWidthFraction / measuredWidthAtOnePixel);
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
