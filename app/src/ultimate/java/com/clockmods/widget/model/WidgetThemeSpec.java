package com.clockmods.widget.model;
public final class WidgetThemeSpec {
    public enum Font { SANS, SERIF, MONOSPACE }
    public final String id;
    public final int displayNameRes, backgroundDrawableRes, primaryTextColor, secondaryTextColor, accentColor, iconColor, defaultBackgroundAlpha;
    public final Font fontLayoutVariant;
    public final boolean supportsAnalog = true, supportsDigital = true, supportsWeather = true, supportsCalendar = true;
    public WidgetThemeSpec(String id, int name, int background, int primary, int secondary, int accent, Font font, int alpha) {
        this.id=id; displayNameRes=name; backgroundDrawableRes=background; primaryTextColor=primary;
        secondaryTextColor=secondary; accentColor=accent; iconColor=accent; fontLayoutVariant=font; defaultBackgroundAlpha=alpha;
    }
}
