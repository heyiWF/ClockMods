package com.clockmods.weather;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

public class DailyForecastRepositoryTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    @Test
    public void cacheRequiresThreeDatesAndExpiresAtMidnightOrSixHours() {
        long updatedAt = millis(2026, Calendar.JULY, 28, 20, 0);
        WeatherModels.DailyForecastData complete = data(updatedAt,
                "2026-07-28", "2026-07-29", "2026-07-30");

        Assert.assertTrue(DailyForecastRepository.isReusable(complete,
                millis(2026, Calendar.JULY, 28, 23, 59), ZONE));
        Assert.assertFalse(DailyForecastRepository.isReusable(complete,
                millis(2026, Calendar.JULY, 29, 0, 1), ZONE));
        Assert.assertFalse(DailyForecastRepository.isReusable(data(updatedAt,
                "2026-07-28", "2026-07-30"), millis(2026, Calendar.JULY, 28, 21, 0), ZONE));
        Assert.assertFalse(DailyForecastRepository.isReusable(complete,
                millis(2026, Calendar.JULY, 29, 2, 1), ZONE));
    }

    private static WeatherModels.DailyForecastData data(long updatedAt, String... dates) {
        WeatherModels.DailyForecast[] entries = new WeatherModels.DailyForecast[dates.length];
        for (int index = 0; index < dates.length; index++) {
            entries[index] = new WeatherModels.DailyForecast(dates[index], "20", "30", "100",
                    "晴", "", "", "");
        }
        return new WeatherModels.DailyForecastData("101280601", "深圳市", "南山区",
                updatedAt, Arrays.asList(entries));
    }

    private static long millis(int year, int month, int day, int hour, int minute) {
        Calendar value = Calendar.getInstance(ZONE);
        value.clear();
        value.set(year, month, day, hour, minute);
        return value.getTimeInMillis();
    }
}