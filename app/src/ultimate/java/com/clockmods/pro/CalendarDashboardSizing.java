package com.clockmods.pro;

/**
 * Every size the calendar page derives from measured geometry, as pure functions of pixels and
 * density. Layouts in {@link com.clockmods.pro.style} compose these rather than inlining magic
 * numbers, which is what keeps the sizing unit-testable without a device.
 */
public final class CalendarDashboardSizing {
    private CalendarDashboardSizing() {}

    public static float clockTimeSize(float cardWidth, float cardHeight, boolean showSeconds,
            float density) {
        float widthUnits = showSeconds ? 4.0f : 3.2f;
        return capDp(fit(cardWidth / widthUnits, cardHeight * 0.52f), density, 144f);
    }

    public static float weatherIconSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.20f, cardHeight * 0.52f), density, 112f);
    }

    public static float weatherTemperatureSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.18f, cardHeight * 0.38f), density, 80f);
    }

    public static float weatherSummarySize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.045f, cardHeight * 0.13f), density, 22f);
    }

    public static float forecastHeadingSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.12f, cardHeight * 0.12f), density, 22f);
    }

    public static float forecastTextSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.105f, cardHeight * 0.105f), density, 19f);
    }

    public static float forecastDetailSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.08f, cardHeight * 0.08f), density, 14f);
    }

    public static float forecastIconSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.28f, cardHeight * 0.28f), density, 52f);
    }

    public static float monthDaySize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.46f, cellHeight * 0.72f), density, 36f);
    }

    /**
     * The lunar line under a month-grid number. The height bound is what binds in a 6-row grid, so
     * it is the ratio that decides legibility; at 0.20 the label reads at arm's length on a desk
     * clock instead of asking to be leaned into.
     */
    public static float monthLunarSize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.22f, cellHeight * 0.20f), density, 18f);
    }

    public static float monthToolbarHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.105f, panelHeight, density, 48f, 72f, 0.18f);
    }

    public static float monthWeekdayHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.065f, panelHeight, density, 24f, 40f, 0.12f);
    }

    public static float monthFooterHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.075f, panelHeight, density, 28f, 48f, 0.14f);
    }

    public static float monthTitleSize(float toolbarHeight, float density) {
        return capDp(toolbarHeight * 0.43f, density, 28f);
    }

    public static float monthWeekdaySize(float cellWidth, float weekdayHeight, float density) {
        return capDp(fit(cellWidth * 0.30f, weekdayHeight * 0.54f), density, 20f);
    }

    public static float monthFooterSize(float footerHeight, float density) {
        return capDp(footerHeight * 0.42f, density, 18f);
    }

    /** The band under a number that the lunar label lives in; grows with the label it holds. */
    public static float monthLabelHeight(float cellHeight, float lunarSize) {
        return fit(cellHeight * 0.32f, lunarSize * 1.65f);
    }

    /**
     * Upper bound for the poster masthead's month word. It is only a bound: the word itself is
     * anywhere from two glyphs (八月) to nine (SEPTEMBER), so {@code CalendarWordmarkView} measures
     * the real text and shrinks from here. Fitting on proportion alone would overflow the long ones.
     */
    public static float posterWordmarkSize(float blockWidth, float blockHeight, float density) {
        return capDp(fit(blockWidth * 0.46f, blockHeight * 0.54f), density, 160f);
    }

    /** The year beneath the month word, derived from it so the pair scales as one block. */
    public static float posterYearSize(float wordmarkSize, float blockHeight, float density) {
        return capDp(fit(wordmarkSize * 0.34f, blockHeight * 0.22f), density, 56f);
    }

    /**
     * The number in a poster cell. Larger in proportion than {@link #monthDaySize} because a poster
     * cell carries no lunar label and no badge — the number is the whole cell.
     */
    public static float posterCellNumberSize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.40f, cellHeight * 0.44f), density, 34f);
    }

    /** Height of the band under a poster number that holds the today dot or the selection rule. */
    public static float posterMarkHeight(float numberSize, float density) {
        return Math.max(safeDensity(density) * 3f, numberSize * 0.24f);
    }

    public static float posterWeekdaySize(float cellWidth, float rowHeight, float density) {
        return capDp(fit(cellWidth * 0.24f, rowHeight * 0.52f), density, 14f);
    }

    public static float spacing(float proportionalSize, float density, float maximumDp) {
        return Math.max(0f, Math.min(Math.max(0f, proportionalSize),
                safeDensity(density) * maximumDp));
    }

    public static float fit(float widthBound, float heightBound) {
        float fitted = Math.min(widthBound, heightBound);
        if (Float.isNaN(fitted) || Float.isInfinite(fitted)) return 1f;
        return Math.max(1f, fitted);
    }

    /**
     * The number in a 周程 strip cell. Seven cells share the width a month grid gives to seven
     * columns, but each one is six times as tall, so the height bound is what actually binds.
     */
    public static float agendaStripNumberSize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.52f, cellHeight * 0.34f), density, 40f);
    }

    /**
     * The weekday label above a strip number and the lunar label below it, at one shared size.
     * The height bound is the one that binds in both orientations, so it — not the cap — is what
     * decides legibility. At 0.22 both a portrait cell and a landscape rail cell, which is barely
     * three quarters as tall, reach the 17dp cap; the number still keeps the majority of the cell.
     */
    public static float agendaStripLunarSize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.30f, cellHeight * 0.22f), density, 17f);
    }

    /**
     * The 周程 subheading — 「第 N 周」 and the 「今天」 chip share the same size. Deliberately
     * larger than {@link #monthFooterSize} (18dp) even though the two derive from the same
     * {@code monthFooterHeight}: the dashboard's footer is a caption under a big clock, whereas
     * here it is the only secondary line in the header, so it lifts to stay legible next to the
     * 28dp month title.
     */
    public static float agendaSubheadingSize(float footerHeight, float density) {
        return capDp(footerHeight * 0.48f, density, 24f);
    }

    /** The 公历 heading on the 周程 detail card. */
    public static float agendaDetailTitleSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.055f, cardHeight * 0.13f), density, 24f);
    }

    /** Every supporting line on the detail card: festivals, weather, 宜 and 忌. */
    public static float agendaDetailBodySize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.036f, cardHeight * 0.075f), density, 16f);
    }

    public static float agendaWeatherIconSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.13f, cardHeight * 0.22f), density, 52f);
    }

    private static float capDp(float value, float density, float maximumDp) {
        return Math.max(1f, Math.min(value, safeDensity(density) * maximumDp));
    }

    private static float boundedChrome(float target, float panelHeight, float density,
            float minimumDp, float maximumDp, float maximumPanelFraction) {
        float upper = Math.max(1f, Math.min(safeDensity(density) * maximumDp,
                panelHeight * maximumPanelFraction));
        float lower = Math.min(safeDensity(density) * minimumDp, upper);
        return Math.max(lower, Math.min(target, upper));
    }

    private static float safeDensity(float density) {
        return density > 0f && !Float.isNaN(density) && !Float.isInfinite(density)
                ? density : 1f;
    }
}
