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

import com.clockmods.LocaleManager;
import com.clockmods.R;
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
    private Runnable locationTimeout;

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
            listener.onDailyForecastState(DailyForecastState.of(Status.LOADING, msg(R.string.forecast_fetching)));
            fetchManual(++generation);
        } else if (!hasLocationPermission()) {
            // Permission is a prerequisite owned by the visible screen, not a fetch failure.
            listener.onDailyForecastState(DailyForecastState.of(Status.IDLE, msg(R.string.weather_waiting_permission)));
        } else {
            listener.onDailyForecastState(DailyForecastState.of(Status.LOADING, msg(R.string.forecast_fetching)));
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
        stopLocation();
        final int requestGeneration = ++generation;
        Location best = newest(lastKnownLocation(LocationManager.NETWORK_PROVIDER),
                lastKnownLocation(LocationManager.GPS_PROVIDER));
        if (best != null && System.currentTimeMillis() - best.getTime() >= 0
                && System.currentTimeMillis() - best.getTime() < 10L * 60L * 1000L) {
            fetchAutomatic(best, requestGeneration);
            return;
        }
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (!running || requestGeneration != generation || locationListener != this) return;
                stopLocation();
                fetchAutomatic(location, requestGeneration);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };
        boolean requested = requestProvider(LocationManager.NETWORK_PROVIDER);
        requested |= requestProvider(LocationManager.GPS_PROVIDER);
        if (!requested) {
            stopLocation();
            reportFailure(Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable));
            schedule(60L * 1000L);
            return;
        }
        final Location fallback = best;
        locationTimeout = new Runnable() {
            @Override public void run() {
                if (!running || requestGeneration != generation || locationListener == null) return;
                stopLocation();
                if (fallback != null) fetchAutomatic(fallback, requestGeneration);
                else {
                    reportFailure(Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable));
                    schedule(60L * 1000L);
                }
            }
        };
        handler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS);
    }

    private Location lastKnownLocation(String provider) {
        if (locationManager == null) return null;
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException | IllegalArgumentException unavailable) {
            return null;
        }
    }

    private boolean requestProvider(String provider) {
        if (locationManager == null) return false;
        try {
            if (!locationManager.isProviderEnabled(provider)) return false;
            locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener);
            return true;
        } catch (SecurityException | IllegalArgumentException unavailable) {
            return false;
        }
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
            reportFailure(Status.CONFIG_ERROR, msg(R.string.weather_not_configured));
            return;
        }
        executor.execute(() -> {
            try {
                final DailyForecastData data = request.execute();
                handler.post(() -> {
                    if (!running || requestGeneration != generation) return;
                    repository.save(data, source);
                    listener.onDailyForecastState(new DailyForecastState(Status.SUCCESS, data, null));
                    schedule(nextRefreshDelay(data.updatedAt));
                });
            } catch (final Exception error) {
                handler.post(() -> {
                    if (!running || requestGeneration != generation) return;
                    reportFailure(Status.NETWORK_ERROR,
                            msg(R.string.forecast_fetch_failed, describeError(error)));
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
        if (locationTimeout != null) {
            handler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }
        if (locationListener != null) {
            try { if (locationManager != null) locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
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

    private void reportFailure(Status status, String detail) {
        WeatherFailureFeedback.showOnce(context, detail);
        listener.onDailyForecastState(DailyForecastState.of(status,
                WeatherFailureFeedback.placeholder(context)));
    }

    private String msg(int resId) {
        return LocaleManager.wrap(context).getString(resId);
    }

    private String msg(int resId, Object... args) {
        return LocaleManager.wrap(context).getString(resId, args);
    }

    private interface ForecastRequest { DailyForecastData execute() throws Exception; }
}
