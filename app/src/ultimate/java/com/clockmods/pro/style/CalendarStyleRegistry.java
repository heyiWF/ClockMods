package com.clockmods.pro.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered, thread-safe registry used by settings, previews, and the live calendar page. Deliberately
 * a copy of {@code ClockStyleRegistry} rather than a shared generic: the two source sets cannot see
 * one another's style types, and the symmetry is worth more here than the saved lines.
 */
public final class CalendarStyleRegistry {
    private final Map<String, CalendarStyle> styles = new LinkedHashMap<>();
    private String fallbackId;

    public synchronized CalendarStyleRegistry register(CalendarStyle style) {
        validateStyle(style);
        String id = style.getMetadata().getId();
        if (styles.containsKey(id)) {
            throw new IllegalArgumentException("Calendar style id is already registered: " + id);
        }
        styles.put(id, style);
        if (fallbackId == null) {
            fallbackId = id;
        }
        return this;
    }

    public synchronized CalendarStyleRegistry registerAll(Iterable<? extends CalendarStyle> additions) {
        if (additions == null) throw new IllegalArgumentException("styles must not be null");
        for (CalendarStyle style : additions) register(style);
        return this;
    }

    public synchronized CalendarStyleRegistry setFallback(String styleId) {
        if (!styles.containsKey(styleId)) {
            throw new IllegalArgumentException("Fallback must be registered first: " + styleId);
        }
        fallbackId = styleId;
        return this;
    }

    public synchronized CalendarStyle find(String styleId) {
        return styleId == null ? null : styles.get(styleId);
    }

    public synchronized CalendarStyle resolve(String styleId) {
        ensureNotEmpty();
        CalendarStyle requested = styleId == null ? null : styles.get(styleId);
        return requested != null ? requested : styles.get(fallbackId);
    }

    public synchronized CalendarStyle resolveForApi(String styleId, int apiLevel) {
        ensureNotEmpty();
        CalendarStyle requested = styleId == null ? null : styles.get(styleId);
        if (requested != null && requested.getMetadata().supportsApi(apiLevel)) {
            return requested;
        }
        CalendarStyle fallback = styles.get(fallbackId);
        if (fallback != null && fallback.getMetadata().supportsApi(apiLevel)) {
            return fallback;
        }
        for (CalendarStyle style : styles.values()) {
            if (style.getMetadata().supportsApi(apiLevel)) return style;
        }
        throw new IllegalStateException("No registered calendar style supports API " + apiLevel);
    }

    public synchronized String getFallbackId() {
        return fallbackId;
    }

    public synchronized int size() {
        return styles.size();
    }

    public synchronized List<CalendarStyle> getStyles() {
        return Collections.unmodifiableList(new ArrayList<>(styles.values()));
    }

    private void ensureNotEmpty() {
        if (styles.isEmpty()) {
            throw new IllegalStateException("No calendar styles have been registered");
        }
    }

    private static void validateStyle(CalendarStyle style) {
        if (style == null || style.getMetadata() == null || style.getTheme() == null) {
            throw new IllegalArgumentException("CalendarStyle, metadata, and theme must not be null");
        }
    }
}
