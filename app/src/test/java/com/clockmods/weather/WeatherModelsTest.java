package com.clockmods.weather;

import com.clockmods.background.ClockPreferences;

import org.junit.Assert;
import org.junit.Test;

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
}