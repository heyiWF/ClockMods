package com.clockmods.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QWeatherClientTest {
    @Test
    public void formatLocationUsesLongitudeThenLatitude() {
        assertEquals("113.42,23.19", QWeatherClient.formatLocation(23.186900, 113.419152));
    }
}