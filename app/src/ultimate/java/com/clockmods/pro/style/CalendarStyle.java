package com.clockmods.pro.style;

import com.clockmods.pro.CalendarTheme;

/**
 * A gallery entry: one composition bound to one palette.
 *
 * <p>The same rule the clock SDK states at {@code docs/clock-style-sdk.md:194} applies here —
 * <strong>swapping colours alone is not a new style</strong>. A new entry has to change the
 * composition; a recolour belongs in {@link CalendarTheme} as a variant of an existing one.</p>
 */
public interface CalendarStyle {
    CalendarStyleMetadata getMetadata();

    CalendarTheme getTheme();

    /**
     * A fresh layout per host. Unlike the stateless {@code ClockRenderer}, a
     * {@link CalendarLayout} holds view references and cannot be shared.
     */
    CalendarLayout newLayout();
}
