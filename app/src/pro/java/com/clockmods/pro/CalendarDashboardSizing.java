package com.clockmods.pro;

final class CalendarDashboardSizing {
    private CalendarDashboardSizing() {}

    static float clockTimeSize(float cardWidth, float cardHeight, boolean showSeconds,
            float density) {
        float widthUnits = showSeconds ? 4.0f : 3.2f;
        return capDp(fit(cardWidth / widthUnits, cardHeight * 0.52f), density, 144f);
    }

    static float weatherIconSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.20f, cardHeight * 0.52f), density, 112f);
    }

    static float weatherTemperatureSize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.18f, cardHeight * 0.38f), density, 80f);
    }

    static float weatherSummarySize(float cardWidth, float cardHeight, float density) {
        return capDp(fit(cardWidth * 0.045f, cardHeight * 0.13f), density, 22f);
    }

    static float forecastHeadingSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.12f, cardHeight * 0.12f), density, 22f);
    }

    static float forecastTextSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.105f, cardHeight * 0.105f), density, 19f);
    }

    static float forecastDetailSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.08f, cardHeight * 0.08f), density, 14f);
    }

    static float forecastIconSize(float columnWidth, float cardHeight, float density) {
        return capDp(fit(columnWidth * 0.28f, cardHeight * 0.28f), density, 52f);
    }

    static float monthDaySize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.46f, cellHeight * 0.72f), density, 36f);
    }

    static float monthLunarSize(float cellWidth, float cellHeight, float density) {
        return capDp(fit(cellWidth * 0.20f, cellHeight * 0.18f), density, 18f);
    }

    static float monthToolbarHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.105f, panelHeight, density, 48f, 72f, 0.18f);
    }

    static float monthWeekdayHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.065f, panelHeight, density, 24f, 40f, 0.12f);
    }

    static float monthFooterHeight(float panelHeight, float density) {
        return boundedChrome(panelHeight * 0.075f, panelHeight, density, 28f, 48f, 0.14f);
    }

    static float monthTitleSize(float toolbarHeight, float density) {
        return capDp(toolbarHeight * 0.43f, density, 28f);
    }

    static float monthWeekdaySize(float cellWidth, float weekdayHeight, float density) {
        return capDp(fit(cellWidth * 0.30f, weekdayHeight * 0.54f), density, 20f);
    }

    static float monthFooterSize(float footerHeight, float density) {
        return capDp(footerHeight * 0.42f, density, 18f);
    }

    static float monthLabelHeight(float cellHeight, float lunarSize) {
        return fit(cellHeight * 0.30f, lunarSize * 1.65f);
    }

    static float spacing(float proportionalSize, float density, float maximumDp) {
        return Math.max(0f, Math.min(Math.max(0f, proportionalSize),
                safeDensity(density) * maximumDp));
    }

    static float fit(float widthBound, float heightBound) {
        float fitted = Math.min(widthBound, heightBound);
        if (Float.isNaN(fitted) || Float.isInfinite(fitted)) return 1f;
        return Math.max(1f, fitted);
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
