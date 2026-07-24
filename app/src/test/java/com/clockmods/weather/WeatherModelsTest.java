package com.clockmods.weather;

import com.clockmods.background.ClockPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class WeatherModelsTest {
    @Test
    public void combinesCityAndDistrictWithoutDuplicates() {
        Assert.assertEquals("深圳宝安", WeatherModels.locationText("深圳市", "宝安"));
        Assert.assertEquals("北京", WeatherModels.locationText("北京市", "北京市"));
        Assert.assertEquals("东城区", WeatherModels.locationText(null, "东城区"));
        Assert.assertEquals("阿拉善盟左旗", WeatherModels.locationText("阿拉善盟", "左旗"));
    }

    @Test
    public void acceptsOnlySupportedWeatherIntervals() {
        Assert.assertTrue(ClockPreferences.isValidWeatherInterval(10));
        Assert.assertTrue(ClockPreferences.isValidWeatherInterval(720));
        Assert.assertFalse(ClockPreferences.isValidWeatherInterval(5));
        Assert.assertFalse(ClockPreferences.isValidWeatherInterval(1440));
    }

    @Test
    public void exposesMultipleWarningsAsSeparateCarouselItems() {
        WeatherModels.WeatherDetail detail = new WeatherModels.WeatherDetail(null, null,
                null, null, null, "台风红色预警\n\n暴雨橙色预警", null, null);

        List<String> items = detail.carouselItems();

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("台风红色预警", items.get(0));
        Assert.assertEquals("暴雨橙色预警", items.get(1));
    }
}