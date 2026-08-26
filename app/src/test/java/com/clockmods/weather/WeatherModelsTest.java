package com.clockmods.weather;

import com.clockmods.background.ClockPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class WeatherModelsTest {
    /** Mirrors the Chinese string resources the app feeds into the carousel. */
    private static final WeatherModels.WeatherDetail.DetailLabels CHINESE_LABELS =
            new WeatherModels.WeatherDetail.DetailLabels(
                    "体感 %s℃", "湿度 %s%%", "%s 级", "降水 %s mm", "空气 %s", "预警");

    @Test
    public void combinesCityAndDistrictWithoutDuplicates() {
        Assert.assertEquals("深圳宝安", WeatherModels.locationText("深圳市", "宝安"));
        Assert.assertEquals("北京", WeatherModels.locationText("北京市", "北京市"));
        Assert.assertEquals("东城区", WeatherModels.locationText(null, "东城区"));
        Assert.assertEquals("阿拉善盟左旗", WeatherModels.locationText("阿拉善盟", "左旗"));
    }

    @Test
    public void ordersEnglishNamesDistrictFirstWithComma() {
        // English addresses read district-first: "Bao'an, Shenzhen", not "Shenzhen Bao'an".
        Assert.assertEquals("Bao'an, Shenzhen", WeatherModels.locationText("Shenzhen", "Bao'an"));
        Assert.assertEquals("Pudong, Shanghai", WeatherModels.locationText("Shanghai", "Pudong"));
        // Single-name and equal-name fallbacks stay unchanged for English too.
        Assert.assertEquals("Beijing", WeatherModels.locationText("Beijing", "Beijing"));
        Assert.assertEquals("Bao'an", WeatherModels.locationText(null, "Bao'an"));
    }

    @Test
    public void acceptsOnlySupportedWeatherIntervals() {
        Assert.assertTrue(ClockPreferences.isValidWeatherInterval(10));
        Assert.assertTrue(ClockPreferences.isValidWeatherInterval(720));
        Assert.assertFalse(ClockPreferences.isValidWeatherInterval(5));
        Assert.assertFalse(ClockPreferences.isValidWeatherInterval(1440));
    }

    @Test
    public void cacheCapabilityRequiresDetailOnlyWhenEnabled() {
        WeatherModels.WeatherDisplayData basic = new WeatherModels.WeatherDisplayData(
                "101010100", "北京市", "北京市", "晴", "100", "26", 1L);
        WeatherModels.WeatherDisplayData detailed = new WeatherModels.WeatherDisplayData(
                "101010100", "北京市", "北京市", "晴", "100", "26", 1L,
                new WeatherModels.WeatherDetail("27", "40", "东北风", "2", "0",
                        null, null, null));

        Assert.assertTrue(basic.satisfies(false));
        Assert.assertFalse(basic.satisfies(true));
        Assert.assertTrue(detailed.satisfies(false));
        Assert.assertTrue(detailed.satisfies(true));
    }

    @Test
    public void exposesMultipleWarningsAsSeparateCarouselItems() {
        WeatherModels.WeatherDetail detail = new WeatherModels.WeatherDetail(null, null,
                null, null, null, "台风红色预警\n\n暴雨橙色预警", null, null);

        List<String> items = detail.carouselItems(CHINESE_LABELS,
                WeatherTemperatureFormatter.UNIT_CELSIUS);

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("台风红色预警", items.get(0));
        Assert.assertEquals("暴雨橙色预警", items.get(1));
    }

    @Test
    public void keepsTemperatureUnitAttachedLikePercent() {
        WeatherModels.WeatherDetail detail = new WeatherModels.WeatherDetail(
                "27", "40", null, null, null, null, null, null);

        List<String> items = detail.carouselItems(CHINESE_LABELS,
                WeatherTemperatureFormatter.UNIT_CELSIUS);

        Assert.assertEquals("体感 27℃", items.get(0));
        Assert.assertEquals("湿度 40%", items.get(1));
    }

    @Test
    public void convertsFeelsLikeForSelectedDisplayUnit() {
        WeatherModels.WeatherDetail detail = new WeatherModels.WeatherDetail(
                "25", null, null, null, null, null, null, null);
        WeatherModels.WeatherDetail.DetailLabels labels =
                new WeatherModels.WeatherDetail.DetailLabels(
                        "Feels %s℃", "Humidity %s%%", "Force %s", "Precip %s mm",
                        "AQI %s", " Warning");

        List<String> items = detail.carouselItems(labels,
                WeatherTemperatureFormatter.UNIT_FAHRENHEIT);

        Assert.assertEquals(1, items.size());
        Assert.assertEquals("Feels 77℉", items.get(0));
    }
}
