package com.clockmods.weather

import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QWeatherClientTest {
    @Test fun formatLocationUsesLongitudeThenLatitude() {
        assertEquals("113.42,23.19", QWeatherClient.formatLocation(23.186900, 113.419152))
    }

    @Test fun warningUsesDetailedHeadlineWhenAvailable() {
        val alert = JSONObject("{\"headline\":\"深圳市台风红色预警信号\",\"eventType\":{\"name\":\"台风\"},\"color\":{\"code\":\"red\"}}")
        assertEquals("深圳市台风红色预警信号", QWeatherClient.formatWarning(alert))
    }

    @Test fun warningCombinesEventAndOfficialColorWithoutHeadline() {
        val alert = JSONObject("{\"eventType\":{\"name\":\"台风\"},\"color\":{\"code\":\"red\"}}")
        assertEquals("台风红色预警", QWeatherClient.formatWarning(alert))
    }

    @Test fun warningPreservesUnknownFutureColorCode() {
        val alert = JSONObject("{\"eventType\":{\"name\":\"暴雨\"},\"color\":{\"code\":\"crimson\"}}")
        assertEquals("暴雨crimson预警", QWeatherClient.formatWarning(alert))
    }

    @Test fun formatsMultipleWarningsAndSkipsInvalidItems() {
        val alerts = JSONArray("[{\"headline\":\"台风红色预警\"},null,{\"eventType\":{\"name\":\"暴雨\"},\"color\":{\"code\":\"orange\"}}]")
        assertEquals("台风红色预警\n暴雨橙色预警", QWeatherClient.formatWarnings(alerts))
    }

    @Test fun parsesDailyForecastByFxDateAndKeepsOptionalFields() {
        val body = JSONObject(
            "{\"code\":\"200\",\"daily\":[" +
                "{\"fxDate\":\"2026-07-30\",\"tempMin\":\"25\",\"tempMax\":\"33\",\"iconDay\":\"305\",\"textDay\":\"小雨\"}," +
                "{\"fxDate\":\"2026-07-28\",\"tempMin\":\"27\",\"tempMax\":\"35\",\"iconDay\":\"100\",\"textDay\":\"晴\",\"windDirDay\":\"南风\",\"windScaleDay\":\"2\",\"humidity\":\"63\"}]}",
        )
        val data = QWeatherClient.parseDailyForecast(body, "101280601", "深圳市", "南山区", 123L)
        assertEquals(2, data.entries.size)
        val today = data.findByDate("2026-07-28")!!
        assertEquals("27", today.tempMin)
        assertEquals("35", today.tempMax)
        assertEquals("100", today.iconDay)
        assertEquals("晴", today.textDay)
        assertEquals("南风", today.windDirDay)
        assertNull(data.findByDate("2026-07-29"))
    }

    @Test(expected = IOException::class)
    fun rejectsEmptyDailyForecast() {
        QWeatherClient.parseDailyForecast(
            JSONObject("{\"code\":\"200\",\"daily\":[]}"),
            "101280601",
            "深圳市",
            "南山区",
            123L,
        )
    }
}
