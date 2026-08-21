package com.clockmods.sdk.clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordered, thread-safe registry used by settings, previews, and the live clock host. */
public final class ClockStyleRegistry {
    private final Map<String, ClockStyle> styles = new LinkedHashMap<>();
    private String fallbackId;

    public synchronized ClockStyleRegistry register(ClockStyle style) {
        validateStyle(style);
        String id = style.getMetadata().getId();
        if (styles.containsKey(id)) {
            throw new IllegalArgumentException("Clock style id is already registered: " + id);
        }
        styles.put(id, style);
        if (fallbackId == null) {
            fallbackId = id;
        }
        return this;
    }

    public synchronized ClockStyleRegistry registerAll(Iterable<? extends ClockStyle> additions) {
        if (additions == null) throw new IllegalArgumentException("styles must not be null");
        for (ClockStyle style : additions) register(style);
        return this;
    }

    public synchronized ClockStyleRegistry setFallback(String styleId) {
        if (!styles.containsKey(styleId)) {
            throw new IllegalArgumentException("Fallback must be registered first: " + styleId);
        }
        fallbackId = styleId;
        return this;
    }

    public synchronized ClockStyle find(String styleId) {
        return styleId == null ? null : styles.get(styleId);
    }

    public synchronized ClockStyle resolve(String styleId) {
        ensureNotEmpty();
        ClockStyle requested = styleId == null ? null : styles.get(styleId);
        return requested != null ? requested : styles.get(fallbackId);
    }

    public synchronized ClockStyle resolveForApi(String styleId, int apiLevel) {
        ensureNotEmpty();
        ClockStyle requested = styleId == null ? null : styles.get(styleId);
        if (requested != null && requested.getMetadata().supportsApi(apiLevel)) {
            return requested;
        }
        ClockStyle fallback = styles.get(fallbackId);
        if (fallback != null && fallback.getMetadata().supportsApi(apiLevel)) {
            return fallback;
        }
        for (ClockStyle style : styles.values()) {
            if (style.getMetadata().supportsApi(apiLevel)) return style;
        }
        throw new IllegalStateException("No registered clock style supports API " + apiLevel);
    }

    public synchronized String getFallbackId() {
        return fallbackId;
    }

    public synchronized int size() {
        return styles.size();
    }

    public synchronized List<ClockStyle> getStyles() {
        return Collections.unmodifiableList(new ArrayList<>(styles.values()));
    }

    private void ensureNotEmpty() {
        if (styles.isEmpty()) {
            throw new IllegalStateException("No clock styles have been registered");
        }
    }

    private static void validateStyle(ClockStyle style) {
        if (style == null || style.getMetadata() == null || style.getThemeTokens() == null
                || style.getRenderer() == null) {
            throw new IllegalArgumentException(
                    "ClockStyle, metadata, theme tokens, and renderer must not be null");
        }
    }
}
