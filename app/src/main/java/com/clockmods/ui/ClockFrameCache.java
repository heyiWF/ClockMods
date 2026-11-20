package com.clockmods.ui;

/**
 * Snapshot of everything {@link ClockView}'s draw pass measures before it can draw.
 *
 * <p>The clock redraws once a second, and a digit transition adds ~9 more frames. Every one of
 * those frames used to re-solve the same numbers: the text size that fits the view, the date
 * line size, both baselines, the accessory widths. None of them can change between two ticks,
 * because the digits are all measured at the <em>widest</em> digit width — {@code "08:41"} and
 * {@code "17:59"} are exactly as wide, so the solver only reacts to the character
 * <em>count</em>, the view size and the font settings.</p>
 *
 * <p>This class is the cache key plus the cached values. It holds no Android types so the
 * matching rules can be unit tested on the JVM.</p>
 */
final class ClockFrameCache {
    /** True when the values below describe the key that was stored last. */
    boolean valid;

    // ---- key ---------------------------------------------------------------
    int width;
    int height;
    int generation;
    int flags;
    float timeScale;
    float dateScale;
    int mainLength;
    int secondsLength;
    int periodLength;
    String dateText;
    String lunarText;
    String messageText;

    // ---- values shared by both layouts -------------------------------------
    float timeSize;
    float dateSize;
    /** Width of the primary string (two-digit block in the stacked layout) at its final size. */
    float mainWidth;
    float secondsWidth;
    float periodWidth;
    /** Width of the space that separates the main block from its accessories. */
    float gapWidth;

    // ---- values for the primary layout -------------------------------------
    float timeCenterX;
    float timeBaseline;
    float timeAscent;
    float timeDescent;
    /** Absolute baselines of the two accessories, solved once instead of per frame. */
    float secondsBaseline;
    float periodBaseline;
    /** Supporting (message) line: absolute baseline and the band it can repaint on its own. */
    boolean hasSupportMessage;
    float supportBaseline;
    float supportTop;
    float supportBottom;
    boolean hasLunar;

    // ---- values for the stacked layout -------------------------------------
    int lines;
    float lineAdvance;
    float firstBaseline;
    float stackedDateBaseline;
    float stackedGap;
    /** A custom message occupies the supporting line, which reserves vertical space. */
    boolean hasMessage;

    /**
     * True when the stored values can be reused for the described frame. {@code dateText},
     * {@code lunarText} and {@code messageText} are compared by value: they are re-created
     * once a second but only change once a minute (or when the user edits them), and a stale
     * value would move every baseline.
     */
    boolean matches(int width, int height, int generation, float timeScale, float dateScale,
            int flags, int mainLength, int secondsLength, int periodLength,
            String dateText, String lunarText, String messageText) {
        return valid
                && this.width == width
                && this.height == height
                && this.generation == generation
                && this.flags == flags
                && this.timeScale == timeScale
                && this.dateScale == dateScale
                && this.mainLength == mainLength
                && this.secondsLength == secondsLength
                && this.periodLength == periodLength
                && same(dateText, this.dateText)
                && same(lunarText, this.lunarText)
                && same(messageText, this.messageText);
    }

    /** Records the key of the frame that is about to be measured. */
    void storeKey(int width, int height, int generation, float timeScale, float dateScale,
            int flags, int mainLength, int secondsLength, int periodLength,
            String dateText, String lunarText, String messageText) {
        this.width = width;
        this.height = height;
        this.generation = generation;
        this.flags = flags;
        this.timeScale = timeScale;
        this.dateScale = dateScale;
        this.mainLength = mainLength;
        this.secondsLength = secondsLength;
        this.periodLength = periodLength;
        this.dateText = dateText;
        this.lunarText = lunarText;
        this.messageText = messageText;
        this.valid = true;
    }

    void invalidate() {
        valid = false;
    }

    private static boolean same(String first, String second) {
        return first == second || (first != null && first.equals(second));
    }
}
