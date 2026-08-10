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

        public boolean satisfies(boolean detailedRequired) {
            return !detailedRequired || detail != null;
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
         * Language-dependent labels/units for the detail carousel. Supplied by the UI layer
         * (from string resources) so this model stays free of Android dependencies and unit
         * testable. Each format string takes a single {@code %s} value except unit-only ones.
         */
        public static final class DetailLabels {
            public final String feelsFormat;   // e.g. "体感 %s ℃" / "Feels %s ℃"
            public final String humidityFormat; // "湿度 %s%%" / "Humidity %s%%"
            public final String windScaleFormat; // "%s 级" / "Force %s"
            public final String precipFormat;   // "降水 %s mm" / "Precip %s mm"
            public final String airFormat;      // "空气 %s" / "AQI %s"
            public final String warningSuffix;  // "预警" / " Warning"

            public DetailLabels(String feelsFormat, String humidityFormat, String windScaleFormat,
                    String precipFormat, String airFormat, String warningSuffix) {
                this.feelsFormat = feelsFormat;
                this.humidityFormat = humidityFormat;
                this.windScaleFormat = windScaleFormat;
                this.precipFormat = precipFormat;
                this.airFormat = airFormat;
                this.warningSuffix = warningSuffix;
            }
        }

        /** Chinese labels, matching the app's historical default wording. */
        public static final DetailLabels CHINESE_LABELS = new DetailLabels(
                "体感 %s ℃", "湿度 %s%%", "%s 级", "降水 %s mm", "空气 %s", "预警");

        /**
         * Builds the ordered list of detail strings that should be shown in the rotating
         * detailed weather line. Optional items (precipitation, warnings, air quality) are
         * only included when meaningful data is available.
         */
        public List<String> carouselItems() {
            return carouselItems(CHINESE_LABELS);
        }

        public List<String> carouselItems(DetailLabels labels) {
            List<String> items = new ArrayList<>();
            if (isPresent(feelsLike)) items.add(String.format(labels.feelsFormat, feelsLike));
            if (isPresent(humidity)) items.add(String.format(labels.humidityFormat, humidity));
            if (isPresent(windDir) || isPresent(windScale)) {
                String wind = isPresent(windDir) ? windDir : "";
                if (isPresent(windScale)) {
                    String scale = String.format(labels.windScaleFormat, windScale);
                    wind = (wind.length() > 0 ? wind + " " : "") + scale;
                }
                items.add(wind);
            }
            if (hasPrecipitation()) items.add(String.format(labels.precipFormat, precip));
            if (isPresent(warning)) {
                String[] warnings = warning.split("\\n");
                for (int index = 0; index < warnings.length && index < 20; index++) {
                    String item = warnings[index].trim();
                    if (item.length() > 0) {
                        items.add(item.contains(labels.warningSuffix.trim())
                                ? item : item + labels.warningSuffix);
                    }
                }
            }
            if (isPresent(aqiValue)) {
                String aqi = String.format(labels.airFormat, aqiValue);
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

    public static final class DailyForecast {
        public final String fxDate, tempMin, tempMax, iconDay, textDay;
        public final String windDirDay, windScaleDay, humidity;

        public DailyForecast(String fxDate, String tempMin, String tempMax, String iconDay,
                String textDay, String windDirDay, String windScaleDay, String humidity) {
            this.fxDate = fxDate; this.tempMin = tempMin; this.tempMax = tempMax;
            this.iconDay = iconDay; this.textDay = textDay; this.windDirDay = windDirDay;
            this.windScaleDay = windScaleDay; this.humidity = humidity;
        }
    }

    public static final class DailyForecastData {
        public final String locationId, city, district;
        public final long updatedAt;
        public final List<DailyForecast> entries;

        public DailyForecastData(String locationId, String city, String district, long updatedAt,
                List<DailyForecast> entries) {
            this.locationId = locationId; this.city = city; this.district = district;
            this.updatedAt = updatedAt;
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }

        public DailyForecast findByDate(String date) {
            for (DailyForecast entry : entries) {
                if (entry.fxDate.equals(date)) return entry;
            }
            return null;
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

    public static final class DailyForecastState {
        public final Status status;
        public final DailyForecastData data;
        public final String message;

        public DailyForecastState(Status status, DailyForecastData data, String message) {
            this.status = status; this.data = data; this.message = message;
        }

        public static DailyForecastState of(Status status, String message) {
            return new DailyForecastState(status, null, message);
        }
    }

    public static String locationText(String city, String district) {
        if (city == null) return district == null ? "" : district;
        // The trailing "市" suffix only appears on Chinese city names; English names pass through.
        String displayCity = city.endsWith("市") ? city.substring(0, city.length() - 1) : city;
        if (district == null || district.length() == 0
                || city.equals(district) || displayCity.equals(district)) return displayCity;
        // Chinese names read naturally when joined directly; Latin names need a separating space.
        String separator = containsHan(displayCity) || containsHan(district) ? "" : " ";
        return displayCity + separator + district;
    }

    private static boolean containsHan(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    public static List<Integer> intervals() {
        return Collections.unmodifiableList(java.util.Arrays.asList(10, 30, 60, 180, 360, 720));
    }
}