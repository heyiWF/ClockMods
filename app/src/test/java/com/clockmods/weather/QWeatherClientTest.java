package com.clockmods.weather;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QWeatherClientTest {
    @Test
    public void formatLocationUsesLongitudeThenLatitude() {
        assertEquals("113.42,23.19", QWeatherClient.formatLocation(23.186900, 113.419152));
    }

    @Test
    public void warningUsesDetailedHeadlineWhenAvailable() throws Exception {
        JSONObject alert = new JSONObject("{\"headline\":\"深圳市台风红色预警信号\","
                + "\"eventType\":{\"name\":\"台风\"},\"color\":{\"code\":\"red\"}}");

        assertEquals("深圳市台风红色预警信号", QWeatherClient.formatWarning(alert));
    }

    @Test
    public void warningCombinesEventAndOfficialColorWithoutHeadline() throws Exception {
        JSONObject alert = new JSONObject("{\"eventType\":{\"name\":\"台风\"},"
                + "\"color\":{\"code\":\"red\"}}");

        assertEquals("台风红色预警", QWeatherClient.formatWarning(alert));
    }

    @Test
    public void warningPreservesUnknownFutureColorCode() throws Exception {
        JSONObject alert = new JSONObject("{\"eventType\":{\"name\":\"暴雨\"},"
                + "\"color\":{\"code\":\"crimson\"}}");

        assertEquals("暴雨crimson预警", QWeatherClient.formatWarning(alert));
    }
}