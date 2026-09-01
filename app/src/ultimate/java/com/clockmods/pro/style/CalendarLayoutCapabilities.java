package com.clockmods.pro.style;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a {@link CalendarLayout} actually puts on screen.
 *
 * <p>This replaces the single {@code sideColumn} boolean {@link com.clockmods.pro.CalendarTheme}
 * used to carry, which conflated three unrelated decisions: whether a clock is shown, whether
 * weather is worth polling for, and whether the month pane keeps its gutter. Splitting them lets a
 * wall-calendar style keep its status capsule without also inheriting a clock.</p>
 *
 * <p>Modelled on {@link com.clockmods.sdk.clock.ClockStyleCapabilities}.</p>
 */
public final class CalendarLayoutCapabilities {
    public enum Capability {
        /** Shows the current time. */
        CLOCK,
        /** Shows current conditions; without it the host does not poll for weather at all. */
        WEATHER,
        /** Shows a multi-day forecast. */
        FORECAST,
        /** Hosts a {@link com.clockmods.ui.StatusBarView} capsule. */
        STATUS_ICONS,
        /** Pages between date windows by dragging; see {@link CalendarLayout#getPager()}. */
        PAGE_SWIPE,
        /** Lets the user pick a day inside the visible window. */
        DAY_SELECTION,
        /** Opens the year/month quick-jump picker. */
        MONTH_PICKER,
        /** Renders a row of weekday names. */
        WEEKDAY_HEADER,
        /** Renders the 宜/忌 line for the selected day. */
        FOOTER_ALMANAC
    }

    private static final CalendarLayoutCapabilities NONE =
            new CalendarLayoutCapabilities(EnumSet.noneOf(Capability.class));

    private final EnumSet<Capability> values;

    private CalendarLayoutCapabilities(EnumSet<Capability> values) {
        this.values = values.clone();
    }

    public static CalendarLayoutCapabilities none() {
        return NONE;
    }

    public static CalendarLayoutCapabilities of(Capability... capabilities) {
        if (capabilities == null || capabilities.length == 0) return NONE;
        EnumSet<Capability> values = EnumSet.noneOf(Capability.class);
        for (Capability capability : capabilities) {
            if (capability == null) {
                throw new IllegalArgumentException("Capability must not be null");
            }
            values.add(capability);
        }
        return new CalendarLayoutCapabilities(values);
    }

    public boolean supports(Capability capability) {
        return capability != null && values.contains(capability);
    }

    public Set<Capability> asSet() {
        return Collections.unmodifiableSet(values.clone());
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CalendarLayoutCapabilities)) return false;
        return values.equals(((CalendarLayoutCapabilities) other).values);
    }

    @Override public int hashCode() {
        return values.hashCode();
    }

    @Override public String toString() {
        return values.toString();
    }
}
