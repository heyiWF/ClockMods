package com.clockmods.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.clockmods.background.ClockPreferences;

import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.clockmods.weather.WeatherModels.DailyForecastData;
import static com.clockmods.weather.WeatherModels.DailyForecastState;
import static com.clockmods.weather.WeatherModels.Status;

public final class DailyForecastController {
    public interface Listener { void onDailyForecastState(DailyForecastState state); }

    private static final long LOCATION_TIMEOUT_MS = 15000L;
    private final Context context;
    private final Listener listener;
    private final LocationManager locationManager;
    private final DailyForecastRepository repository;
    private final ClockPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean running;
    private int generation;
    private LocationListener locationListener;

    public DailyForecastController(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        repository = new DailyForecastRepository(context);
        preferences = new ClockPreferences(context);
    }

    public void start() {
        stopLocation();
        handler.removeCallbacksAndMessages(null);
        running = true;
        generation++;
        DailyForecastData cached = getCached();
        if (cached != null) {
            listener.onDailyForecastState(new DailyForecastState(Status.SUCCESS, cached, null));
            schedule(nextRefreshDelay(cached.updatedAt));
            return;
        }
        refreshNow();
    }

    public void refreshNow() {
        if (!running) return;
        if (isManualLocation()) {
            listener.onDailyForecastState(DailyForecastState.of(Status.LOADING, "正在获取天气预报…"));
            fetchManual(++generation);
        } else if (!hasLocationPermission()) {
            listener.onDailyForecastState(DailyForecastState.of(Status.PERMISSION_DENIED, "未授予定位权限"));
        } else {
            listener.onDailyForecastState(DailyForecastState.of(Status.LOADING, "正在获取天气预报…"));
            requestLocation();
        }
    }

    public void stop() {
        running = false;
        generation++;
        handler.removeCallbacksAndMessages(null);
        stopLocation();
    }

    public void shutdown() { stop(); executor.shutdownNow(); }

    @SuppressWarnings("MissingPermission")
    private void requestLocation() {
        final int requestGeneration = ++generation;
        Location best = newest(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        if (best != null && System.currentTimeMillis() - best.getTime() < 10L * 60L * 1000L) {
            fetchAutomatic(best, requestGeneration);
            return;
        }
        final Location fallback = best;
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                stopLocation(); fetchAutomatic(location, requestGeneration);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };
        boolean requested = false;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
            requested = true;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener);
            requested = true;
        }
        if (!requested) {
            listener.onDailyForecastState(DailyForecastState.of(Status.LOCATION_UNAVAILABLE, "无法获取当前位置"));
            return;
        }
        handler.postDelayed(() -> {
            if (!running || requestGeneration != generation) return;
            stopLocation();
            if (fallback != null) fetchAutomatic(fallback, requestGeneration);
            else listener.onDailyForecastState(DailyForecastState.of(
                    Status.LOCATION_UNAVAILABLE, "无法获取当前位置"));
        }, LOCATION_TIMEOUT_MS);
    }

    private void fetchAutomatic(final Location location, final int requestGeneration) {
        fetch(requestGeneration, () -> new QWeatherClient(context, QWeatherConfig.apiHost())
                .fetchDaily(location.getLatitude(), location.getLongitude()),
                ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
    }

    private void fetchManual(final int requestGeneration) {
        final String locationId = preferences.getWeatherLocationId();
        final String city = preferences.getWeatherCity();
        final String district = preferences.getWeatherDistrict();
        fetch(requestGeneration, () -> new QWeatherClient(context, QWeatherConfig.apiHost())
                .fetchDailyLocation(locationId, city, district),
                ClockPreferences.WEATHER_LOCATION_MANUAL);
    }

    private void fetch(final int requestGeneration, final ForecastRequest request, final String source) {
        if (!QWeatherConfig.isConfigured()) {
            listener.onDailyForecastState(DailyForecastState.of(Status.CONFIG_ERROR, "天气服务未配置"));
            return;
        }
        executor.execute(() -> {
            try {
                final DailyForecastData data = request.execute();
                repository.save(data, source);
                handler.post(() -> {
                    if (!running || requestGeneration != generation) return;
                    listener.onDailyForecastState(new DailyForecastState(Status.SUCCESS, data, null));
                    schedule(nextRefreshDelay(data.updatedAt));
                });
            } catch (final Exception error) {
                handler.post(() -> {
                    if (!running || requestGeneration != generation) return;
                    listener.onDailyForecastState(DailyForecastState.of(Status.NETWORK_ERROR,
                            "天气预报获取失败：" + describeError(error)));
                });
            }
        });
    }

    private DailyForecastData getCached() {
        String source = isManualLocation() ? ClockPreferences.WEATHER_LOCATION_MANUAL
                : ClockPreferences.WEATHER_LOCATION_AUTOMATIC;
        return repository.getCached(source, preferences.getWeatherLocationId(),
            System.currentTimeMillis(), appTimeZone());
    }

    private long nextRefreshDelay(long updatedAt) {
        long now = System.currentTimeMillis();
        long ageDelay = Math.max(1000L, DailyForecastRepository.MAX_AGE_MS - (now - updatedAt));
        java.util.Calendar midnight = java.util.Calendar.getInstance(
            appTimeZone());
        midnight.setTimeInMillis(now);
        midnight.add(java.util.Calendar.DAY_OF_MONTH, 1);
        midnight.set(java.util.Calendar.HOUR_OF_DAY, 0);
        midnight.set(java.util.Calendar.MINUTE, 0);
        midnight.set(java.util.Calendar.SECOND, 0);
        midnight.set(java.util.Calendar.MILLISECOND, 0);
        return Math.min(ageDelay, Math.max(1000L, midnight.getTimeInMillis() - now));
    }

    private void schedule(long delay) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::refreshNow, delay);
    }

    private boolean isManualLocation() {
        return ClockPreferences.WEATHER_LOCATION_MANUAL.equals(preferences.getWeatherLocationMode());
    }

    private TimeZone appTimeZone() {
        String id = preferences.getTimeZoneId();
        return id == null || id.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(id);
    }

    private boolean hasLocationPermission() {
        return android.os.Build.VERSION.SDK_INT < 23
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void stopLocation() {
        if (locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
            locationListener = null;
        }
    }

    private static Location newest(Location first, Location second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.getTime() >= second.getTime() ? first : second;
    }

    private static String describeError(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().length() == 0
                ? error.getClass().getSimpleName() : message.trim();
    }

    private interface ForecastRequest { DailyForecastData execute() throws Exception; }
}