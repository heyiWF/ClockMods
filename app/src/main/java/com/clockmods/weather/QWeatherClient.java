package com.clockmods.weather;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static com.clockmods.weather.WeatherModels.WeatherDetail;
import static com.clockmods.weather.WeatherModels.WeatherDisplayData;

public final class QWeatherClient {
    private final String host;
    private final int timeoutMs;
    private final TlsSocketFactory socketFactory;

    public QWeatherClient(Context context, String host) { this(context, host, 15000); }
    public QWeatherClient(Context context, String host, int timeoutMs) {
        this.host = host; this.timeoutMs = timeoutMs;
        this.socketFactory = TlsSocketFactory.create(context.getApplicationContext());
    }

    public WeatherDisplayData fetch(double latitude, double longitude) throws Exception {
        return fetch(latitude, longitude, false);
    }

    public WeatherDisplayData fetch(double latitude, double longitude, boolean detailed) throws Exception {
        JSONObject location = getLocation(latitude, longitude);
        double lat = optDouble(location, "lat", latitude);
        double lon = optDouble(location, "lon", longitude);
        return fetchNow(location.getString("id"), location.optString("adm2", ""),
                location.optString("name", ""), lat, lon, detailed);
    }

    public WeatherDisplayData fetchLocation(String locationId, String city, String district) throws Exception {
        return fetchNow(locationId, city, district, Double.NaN, Double.NaN, false);
    }

    public WeatherDisplayData fetchLocation(String locationId, String city, String district,
            double latitude, double longitude, boolean detailed) throws Exception {
        return fetchNow(locationId, city, district, latitude, longitude, detailed);
    }

    private WeatherDisplayData fetchNow(String locationId, String city, String district,
            double latitude, double longitude, boolean detailed) throws Exception {
        String path = "/v7/weather/now?location=" + urlEncode(locationId) + "&lang=zh";
        JSONObject body = request(path);
        JSONObject now = body.getJSONObject("now");
        WeatherDetail detail = detailed
                ? buildDetail(now, latitude, longitude) : null;
        return new WeatherDisplayData(locationId, city, district,
                now.optString("text", ""), now.optString("icon", ""),
                now.optString("temp", "--"), System.currentTimeMillis(), detail);
    }

    private WeatherDetail buildDetail(JSONObject now, double latitude, double longitude) {
        String warning = null;
        String aqiValue = null;
        String aqiCategory = null;
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            warning = fetchWarning(latitude, longitude);
            String[] aqi = fetchAirQuality(latitude, longitude);
            aqiValue = aqi[0];
            aqiCategory = aqi[1];
        }
        return new WeatherDetail(now.optString("feelsLike", ""), now.optString("humidity", ""),
                now.optString("windDir", ""), now.optString("windScale", ""),
                now.optString("precip", ""), warning, aqiValue, aqiCategory);
    }

    private String fetchWarning(double latitude, double longitude) {
        try {
            JSONObject body = requestRaw("/weatheralert/v1/current/"
                    + formatCoordinate(latitude) + "/" + formatCoordinate(longitude) + "?lang=zh");
            JSONArray alerts = body.optJSONArray("alerts");
            if (alerts == null || alerts.length() == 0) return null;
            return formatWarning(alerts.getJSONObject(0));
        } catch (Exception ignored) { return null; }
    }

    static String formatWarning(JSONObject alert) {
        String headline = alert.optString("headline", "").trim();
        if (headline.length() > 0) return headline;

        JSONObject eventType = alert.optJSONObject("eventType");
        String event = eventType == null ? "" : eventType.optString("name", "").trim();
        if (event.length() == 0) return null;

        JSONObject color = alert.optJSONObject("color");
        String colorCode = color == null ? "" : color.optString("code", "").trim();
        String colorName = warningColorName(colorCode);
        return event + colorName + "预警";
    }

    private static String warningColorName(String code) {
        if (code.length() == 0) return "";
        switch (code.toLowerCase(Locale.US)) {
            case "white": return "白色";
            case "gray": return "灰色";
            case "green": return "绿色";
            case "blue": return "蓝色";
            case "yellow": return "黄色";
            case "amber": return "琥珀色";
            case "orange": return "橙色";
            case "red": return "红色";
            case "purple": return "紫色";
            case "black": return "黑色";
            default: return code;
        }
    }

    private String[] fetchAirQuality(double latitude, double longitude) {
        try {
            JSONObject body = requestRaw("/airquality/v1/current/"
                    + formatCoordinate(latitude) + "/" + formatCoordinate(longitude) + "?lang=zh");
            JSONArray indexes = body.optJSONArray("indexes");
            if (indexes == null || indexes.length() == 0) return new String[] {null, null};
            JSONObject index = indexes.getJSONObject(0);
            for (int i = 0; i < indexes.length(); i++) {
                if ("qaqi".equals(indexes.getJSONObject(i).optString("code"))) {
                    index = indexes.getJSONObject(i);
                    break;
                }
            }
            String value = index.optString("aqiDisplay", "");
            String category = index.optString("category", "");
            return new String[] {value.length() > 0 ? value : null,
                    category.length() > 0 ? category : null};
        } catch (Exception ignored) { return new String[] {null, null}; }
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

    static String formatCoordinate(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static double optDouble(JSONObject object, String key, double fallback) {
        String value = object.optString(key, "");
        try { return value.length() > 0 ? Double.parseDouble(value) : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    private JSONObject request(String path) throws Exception {
        JSONObject json = requestRaw(path);
        if (!"200".equals(json.optString("code"))) {
            throw new IOException("QWeather error: " + json.optString("code"));
        }
        return json;
    }

    private JSONObject requestRaw(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + host + path).openConnection();
        try {
            if (connection instanceof HttpsURLConnection && socketFactory != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
            }
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
            if (status < 200 || status >= 300) {
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