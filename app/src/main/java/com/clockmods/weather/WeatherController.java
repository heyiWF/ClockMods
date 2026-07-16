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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.clockmods.weather.WeatherModels.Status;
import static com.clockmods.weather.WeatherModels.WeatherDisplayData;
import static com.clockmods.weather.WeatherModels.WeatherState;

public final class WeatherController {
    public interface Listener { void onWeatherState(WeatherState state); }

    private static final long LOCATION_TIMEOUT_MS = 15000L;
    private final Context context;
    private final Listener listener;
    private final LocationManager locationManager;
    private final WeatherRepository repository;
    private final ClockPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean running;
    private int generation;
    private int intervalMinutes = 30;
    private LocationListener locationListener;

    public WeatherController(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        repository = new WeatherRepository(context);
        preferences = new ClockPreferences(context);
    }

    public void start(int intervalMinutes) {
        handler.removeCallbacksAndMessages(null);
        removeLocationListener();
        generation++;
        running = true;
        this.intervalMinutes = intervalMinutes;
        WeatherDisplayData cached = getCached();
        if (cached != null) listener.onWeatherState(new WeatherState(Status.SUCCESS, cached, null));
        refreshIfNeeded(cached, false);
    }

    public void refreshNow() { refreshIfNeeded(getCached(), true); }

    public void stop() {
        running = false;
        generation++;
        handler.removeCallbacksAndMessages(null);
        removeLocationListener();
    }

    public void shutdown() { stop(); executor.shutdownNow(); }

    private void refreshIfNeeded(WeatherDisplayData cached, boolean force) {
        if (!running) return;
        long maxAge = intervalMinutes * 60L * 1000L;
        if (!force && cached != null && System.currentTimeMillis() - cached.updatedAt < maxAge) {
            schedule(maxAge - (System.currentTimeMillis() - cached.updatedAt));
            return;
        }
        if (isManualLocation()) {
            listener.onWeatherState(WeatherState.of(Status.LOADING, "正在获取天气…"));
            fetchManual(++generation);
            return;
        }
        if (!hasLocationPermission()) {
            listener.onWeatherState(WeatherState.of(Status.PERMISSION_DENIED, "未授予定位权限"));
            return;
        }
        listener.onWeatherState(WeatherState.of(Status.LOADING, "正在获取天气…"));
        requestLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocation() {
        final int requestGeneration = ++generation;
        Location best = newest(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        if (best != null && System.currentTimeMillis() - best.getTime() < 10L * 60L * 1000L) {
            fetch(best, requestGeneration);
            return;
        }
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                removeLocationListener(); fetch(location, requestGeneration);
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
            listener.onWeatherState(WeatherState.of(Status.LOCATION_UNAVAILABLE, "无法获取当前位置"));
            schedule(intervalMinutes * 60L * 1000L);
            return;
        }
        final Location fallback = best;
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!running || requestGeneration != generation) return;
                removeLocationListener();
                if (fallback != null) fetch(fallback, requestGeneration);
                else {
                    listener.onWeatherState(WeatherState.of(Status.LOCATION_UNAVAILABLE, "无法获取当前位置"));
                    schedule(intervalMinutes * 60L * 1000L);
                }
            }
        }, LOCATION_TIMEOUT_MS);
    }

    private void fetch(final Location location, final int requestGeneration) {
        if (!QWeatherConfig.isConfigured()) {
            listener.onWeatherState(WeatherState.of(Status.CONFIG_ERROR, "天气服务未配置"));
            return;
        }
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    final WeatherDisplayData data = new QWeatherClient(context, QWeatherConfig.apiHost())
                            .fetch(location.getLatitude(), location.getLongitude());
                        repository.save(data, ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (!running || requestGeneration != generation) return;
                            listener.onWeatherState(new WeatherState(Status.SUCCESS, data, null));
                            schedule(intervalMinutes * 60L * 1000L);
                        }
                    });
                } catch (final Exception error) {
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (!running || requestGeneration != generation) return;
                            listener.onWeatherState(WeatherState.of(Status.NETWORK_ERROR,
                                    "天气获取失败：" + describeError(error)));
                            schedule(intervalMinutes * 60L * 1000L);
                        }
                    });
                }
            }
        });
    }

    private void fetchManual(final int requestGeneration) {
        if (!QWeatherConfig.isConfigured()) {
            listener.onWeatherState(WeatherState.of(Status.CONFIG_ERROR, "天气服务未配置"));
            return;
        }
        final String locationId = preferences.getWeatherLocationId();
        final String city = preferences.getWeatherCity();
        final String district = preferences.getWeatherDistrict();
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    final WeatherDisplayData data = new QWeatherClient(context, QWeatherConfig.apiHost())
                            .fetchLocation(locationId, city, district);
                    repository.save(data, ClockPreferences.WEATHER_LOCATION_MANUAL);
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (!running || requestGeneration != generation) return;
                            listener.onWeatherState(new WeatherState(Status.SUCCESS, data, null));
                            schedule(intervalMinutes * 60L * 1000L);
                        }
                    });
                } catch (final Exception error) {
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (!running || requestGeneration != generation) return;
                            listener.onWeatherState(WeatherState.of(Status.NETWORK_ERROR,
                                    "天气获取失败：" + describeError(error)));
                            schedule(intervalMinutes * 60L * 1000L);
                        }
                    });
                }
            }
        });
    }

    private boolean isManualLocation() {
        return ClockPreferences.WEATHER_LOCATION_MANUAL.equals(preferences.getWeatherLocationMode());
    }

    private WeatherDisplayData getCached() {
        String source = isManualLocation() ? ClockPreferences.WEATHER_LOCATION_MANUAL
                : ClockPreferences.WEATHER_LOCATION_AUTOMATIC;
        return repository.getCached(source, preferences.getWeatherLocationId());
    }

    private void schedule(long delay) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() { @Override public void run() { refreshNow(); } }, Math.max(1000L, delay));
    }

    private boolean hasLocationPermission() {
        return android.os.Build.VERSION.SDK_INT < 23
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void removeLocationListener() {
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
        if (message != null && message.trim().length() > 0) return message.trim();
        return error.getClass().getSimpleName();
    }
}