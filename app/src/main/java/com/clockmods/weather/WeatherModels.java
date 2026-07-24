package com.clockmods.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeatherModels {
    private WeatherModels() { }

    public static final class WeatherDisplayData {
        public final String locationId, city, district, text, icon, temperature;
        public final long updatedAt;
        public final WeatherDetail detail;
        public WeatherDisplayData(String locationId, String city, String district,
                String text, String icon, String temperature, long updatedAt) {
            this(locationId, city, district, text, icon, temperature, updatedAt, null);
        }
        public WeatherDisplayData(String locationId, String city, String district,
                String text, String icon, String temperature, long updatedAt, WeatherDetail detail) {
            this.locationId = locationId; this.city = city; this.district = district;
            this.text = text; this.icon = icon; this.temperature = temperature;
            this.updatedAt = updatedAt; this.detail = detail;
        }
    }

    /** Extra weather metrics rotated through the detailed weather line. */
    public static final class WeatherDetail {
        public final String feelsLike;   // 体感温度，摄氏度
        public final String humidity;    // 相对湿度，百分比
        public final String windDir;     // 风向
        public final String windScale;   // 风力等级
        public final String precip;      // 降水量，毫米
        public final String warning;     // 预警事件名称，可能为空
        public final String aqiValue;    // 空气质量指数值，可能为空
        public final String aqiCategory; // 空气质量类别，可能为空

        public WeatherDetail(String feelsLike, String humidity, String windDir, String windScale,
                String precip, String warning, String aqiValue, String aqiCategory) {
            this.feelsLike = feelsLike; this.humidity = humidity; this.windDir = windDir;
            this.windScale = windScale; this.precip = precip; this.warning = warning;
            this.aqiValue = aqiValue; this.aqiCategory = aqiCategory;
        }

        /**
         * Builds the ordered list of detail strings that should be shown in the rotating
         * detailed weather line. Optional items (precipitation, warnings, air quality) are
         * only included when meaningful data is available.
         */
        public List<String> carouselItems() {
            List<String> items = new ArrayList<>();
            if (isPresent(feelsLike)) items.add("体感 " + feelsLike + "℃");
            if (isPresent(humidity)) items.add("湿度 " + humidity + "%");
            if (isPresent(windDir) || isPresent(windScale)) {
                String wind = isPresent(windDir) ? windDir : "";
                if (isPresent(windScale)) wind = (wind.length() > 0 ? wind + " " : "") + windScale + "级";
                items.add(wind);
            }
            if (hasPrecipitation()) items.add("降水 " + precip + "mm");
            if (isPresent(warning)) {
                String[] warnings = warning.split("\\n");
                for (int index = 0; index < warnings.length && index < 20; index++) {
                    String item = warnings[index].trim();
                    if (item.length() > 0) items.add(item.contains("预警") ? item : item + "预警");
                }
            }
            if (isPresent(aqiValue)) {
                String aqi = "空气 " + aqiValue;
                if (isPresent(aqiCategory)) aqi += " " + aqiCategory;
                items.add(aqi);
            }
            return items;
        }

        private boolean hasPrecipitation() {
            if (!isPresent(precip)) return false;
            try { return Double.parseDouble(precip) > 0d; } catch (NumberFormatException e) { return false; }
        }

        private static boolean isPresent(String value) {
            return value != null && value.trim().length() > 0;
        }
    }

    public enum Status { IDLE, LOADING, SUCCESS, PERMISSION_DENIED, LOCATION_UNAVAILABLE,
        NETWORK_ERROR, API_ERROR, CONFIG_ERROR }

    public static final class WeatherState {
        public final Status status;
        public final WeatherDisplayData data;
        public final String message;
        public WeatherState(Status status, WeatherDisplayData data, String message) {
            this.status = status; this.data = data; this.message = message;
        }
        public static WeatherState of(Status status, String message) {
            return new WeatherState(status, null, message);
        }
    }

    public static String locationText(String city, String district) {
        if (city == null) return district == null ? "" : district;
        String displayCity = city.endsWith("市") ? city.substring(0, city.length() - 1) : city;
        if (district == null || district.length() == 0
                || city.equals(district) || displayCity.equals(district)) return displayCity;
        return displayCity + district;
    }

    public static List<Integer> intervals() {
        return Collections.unmodifiableList(java.util.Arrays.asList(10, 30, 60, 180, 360, 720));
    }
}