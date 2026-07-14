package com.clockmods.weather;

import java.util.Collections;
import java.util.List;

public final class WeatherModels {
    private WeatherModels() { }

    public static final class WeatherDisplayData {
        public final String locationId, city, district, text, icon, temperature;
        public final long updatedAt;
        public WeatherDisplayData(String locationId, String city, String district,
                String text, String icon, String temperature, long updatedAt) {
            this.locationId = locationId; this.city = city; this.district = district;
            this.text = text; this.icon = icon; this.temperature = temperature; this.updatedAt = updatedAt;
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
        if (district == null || district.length() == 0 || city.equals(district)) return displayCity;
        return displayCity + district;
    }

    public static List<Integer> intervals() {
        return Collections.unmodifiableList(java.util.Arrays.asList(10, 30, 60, 180, 360, 720));
    }
}