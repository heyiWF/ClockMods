package com.clockmods.weather;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeatherFailureDeduplicatorTest {
    @Test
    public void weatherAndForecastFailuresShowOnlyOneToast() {
        WeatherFailureDeduplicator deduplicator = new WeatherFailureDeduplicator();

        assertTrue(deduplicator.shouldShow());
        assertFalse(deduplicator.shouldShow());
        assertFalse(deduplicator.shouldShow());
    }
}
