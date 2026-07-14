package com.clockmods.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import static com.clockmods.weather.WeatherModels.WeatherDisplayData;

public final class QWeatherClient {
    private final String host;
    private final int timeoutMs;

    public QWeatherClient(String host) { this(host, 15000); }
    public QWeatherClient(String host, int timeoutMs) {
        this.host = host; this.timeoutMs = timeoutMs;
    }

    public WeatherDisplayData fetch(double latitude, double longitude) throws Exception {
        JSONObject location = getLocation(latitude, longitude);
        return fetchLocation(location.getString("id"), location.optString("adm2", ""),
                location.optString("name", ""));
    }

    public WeatherDisplayData fetchLocation(String locationId, String city, String district) throws Exception {
        String path = "/v7/weather/now?location=" + urlEncode(locationId) + "&lang=zh";
        JSONObject body = request(path);
        JSONObject now = body.getJSONObject("now");
        return new WeatherDisplayData(locationId, city, district,
                now.optString("text", ""), now.optString("icon", ""),
                now.optString("temp", "--"), System.currentTimeMillis());
    }

    private JSONObject getLocation(double latitude, double longitude) throws Exception {
        JSONObject body = request("/geo/v2/city/lookup?location=" + formatLocation(latitude, longitude)
                + "&range=cn&number=1&lang=zh");
        JSONArray locations = body.getJSONArray("location");
        if (locations.length() == 0) throw new IOException("No QWeather location");
        return locations.getJSONObject(0);
    }

    static String formatLocation(double latitude, double longitude) {
        return String.format(Locale.US, "%.2f,%.2f", longitude, latitude);
    }

    private JSONObject request(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + host + path).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("Authorization", "Bearer " + QWeatherSigner.token(
                    QWeatherConfig.credentialId(), QWeatherConfig.projectId(),
                    QWeatherConfig.privateKeyBase64(), System.currentTimeMillis() / 1000L));
            int status = connection.getResponseCode();
            String response = read(status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream());
            JSONObject json = new JSONObject(response);
            if (status < 200 || status >= 300 || !"200".equals(json.optString("code"))) {
                throw new IOException("QWeather error: " + json.optString("code", String.valueOf(status)));
            }
            return json;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Empty QWeather response");
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            String line; while ((line = reader.readLine()) != null) output.append(line);
        }
        return output.toString();
    }

    private static String urlEncode(String value) throws Exception {
        return java.net.URLEncoder.encode(value, "UTF-8");
    }
}