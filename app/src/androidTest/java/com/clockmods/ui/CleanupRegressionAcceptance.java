package com.clockmods.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.clockmods.background.ClockPreferences;
import com.clockmods.weather.WeatherController;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Runs without network requests and restores all touched preferences. */
@android.annotation.TargetApi(29)
public final class CleanupRegressionAcceptance extends Instrumentation {
    private int checks;
    private boolean lite;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        lite = args != null && "true".equals(args.getString("lite"));
        start();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        final Throwable[] failure = {null};
        runOnMainSync(() -> {
            try {
                checkIconAnimation();
                checkReset();
                checkLocationCleanup();
                checkWeatherPermissionPrerequisite();
                if (lite) checkStatusMeasurement();
            } catch (Throwable error) { failure[0] = error; }
        });
        if (failure[0] != null) {
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(failure[0]));
            finish(Activity.RESULT_CANCELED, result);
        } else {
            result.putString("stream", "PASS: " + checks + " cleanup regression checks\n");
            finish(Activity.RESULT_OK, result);
        }
    }
    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private void checkIconAnimation() throws Exception {
        Class<?> type = Class.forName("com.clockmods.ui.WeatherIcon");
        Object icon;
        try {
            Method load = type.getDeclaredMethod("load", Context.class, String.class, boolean.class);
            load.setAccessible(true); icon = load.invoke(null, getTargetContext(), "100", true);
        } catch (NoSuchMethodException liteSignature) {
            Method load = type.getDeclaredMethod("load", Context.class, String.class);
            load.setAccessible(true); icon = load.invoke(null, getTargetContext(), "100");
        }
        check(icon != null, "bundled weather glyph loads");
        Method draw = type.getDeclaredMethod("draw", Canvas.class, float.class, float.class, float.class, Paint.class);
        draw.setAccessible(true);
        Bitmap target = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        try {
            Paint paint = new Paint(); paint.setColor(Color.WHITE); paint.setAlpha(64);
            draw.invoke(icon, new Canvas(target), 0f, 0f, 64f, paint);
            Object first = field(icon, "bitmap").get(icon);
            target.eraseColor(Color.TRANSPARENT); paint.setAlpha(128);
            draw.invoke(icon, new Canvas(target), 0f, 0f, 64f, paint);
            check(first == field(icon, "bitmap").get(icon), "alpha animation reuses the rasterized glyph");
            int maxAlpha = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
                maxAlpha = Math.max(maxAlpha, Color.alpha(target.getPixel(x, y)));
            check(maxAlpha == 128, "cached glyph applies requested alpha exactly once");
        } finally { target.recycle(); }
    }
    private void checkReset() {
        SharedPreferences prefs = getTargetContext().getSharedPreferences("clock_prefs", 0);
        Map<String, ?> before = new HashMap<>(prefs.getAll());
        try {
            ClockPreferences settings = new ClockPreferences(getTargetContext());
            settings.setUseNetworkTime(true); settings.setSyncIntervalMinutes(10);
            settings.setTimeZoneId("America/New_York"); settings.restoreDefaults();
            check(!settings.isUseNetworkTime(), "reset disables network time");
            check(settings.getSyncIntervalMinutes() == ClockPreferences.DEFAULT_SYNC_INTERVAL_MINUTES,
                    "reset restores sync interval");
            check(settings.getTimeZoneId().equals(ClockPreferences.DEFAULT_TIME_ZONE_ID), "reset restores time zone");
        } finally { restore(prefs, before); }
    }
    private void checkWeatherPermissionPrerequisite() throws Exception {
        // Separate stores keep this test independent of real cache freshness / selected cities.
        Map<String, SharedPreferences> stores = new HashMap<>();
        final boolean[] granted = {false};
        Context guarded = new ContextWrapper(getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public int checkSelfPermission(String permission) {
                return granted[0] ? android.content.pm.PackageManager.PERMISSION_GRANTED
                        : android.content.pm.PackageManager.PERMISSION_DENIED;
            }
            @Override public Object getSystemService(String name) {
                return Context.LOCATION_SERVICE.equals(name) ? null : super.getSystemService(name);
            }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                if (!stores.containsKey(name)) {
                    SharedPreferences prefs = super.getSharedPreferences("permission-test-" + name, mode);
                    prefs.edit().clear().commit();
                    stores.put(name, prefs);
                }
                return stores.get(name);
            }
        };
        java.util.List<com.clockmods.weather.WeatherModels.Status> states = new java.util.ArrayList<>();
        WeatherController controller = new WeatherController(guarded, state -> states.add(state.status));
        Field deduplicatorField = com.clockmods.weather.WeatherFailureFeedback.class.getDeclaredField("DEDUPLICATOR");
        deduplicatorField.setAccessible(true);
        Object deduplicator = deduplicatorField.get(null);
        Field shown = field(deduplicator, "shown");
        boolean previousShown = shown.getBoolean(deduplicator);
        shown.setBoolean(deduplicator, false);
        try {
            controller.start(30);
            controller.refreshNow();
            check(states.size() == 2 && states.get(0) == com.clockmods.weather.WeatherModels.Status.IDLE
                    && states.get(1) == com.clockmods.weather.WeatherModels.Status.IDLE,
                    "automatic weather waits instead of loading or failing before permission");
            check(!shown.getBoolean(deduplicator), "waiting for permission does not consume the failure toast");
            try {
                Class<?> daily = Class.forName("com.clockmods.weather.DailyForecastController");
                Class<?> callback = Class.forName("com.clockmods.weather.DailyForecastController$Listener");
                java.util.List<Object> forecastStates = new java.util.ArrayList<>();
                Object listener = java.lang.reflect.Proxy.newProxyInstance(callback.getClassLoader(),
                        new Class<?>[]{callback}, (proxy, method, args) -> {
                            if (args != null && args.length == 1) forecastStates.add(args[0]);
                            return null;
                        });
                Object forecast = daily.getConstructor(Context.class, callback).newInstance(guarded, listener);
                try {
                    daily.getMethod("start").invoke(forecast);
                    check(forecastStates.size() == 1 && forecastStates.get(0).getClass().getField("status")
                            .get(forecastStates.get(0)) == com.clockmods.weather.WeatherModels.Status.IDLE,
                            "forecast also waits before permission");
                    check(!shown.getBoolean(deduplicator), "forecast permission wait has no failure toast");
                } finally { daily.getMethod("shutdown").invoke(forecast); }
            } catch (ClassNotFoundException removedInLite) { check(lite, "only Lite omits daily forecasts"); }
            granted[0] = true;
            controller.refreshNow();
            check(states.contains(com.clockmods.weather.WeatherModels.Status.LOADING),
                    "refresh after granting permission begins location acquisition");
        } finally {
            controller.shutdown();
            shown.setBoolean(deduplicator, previousShown);
            for (SharedPreferences prefs : stores.values()) prefs.edit().clear().commit();
        }
    }
    private void checkLocationCleanup() throws Exception {
        Context noLocation = new ContextWrapper(getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Object getSystemService(String name) {
                return Context.LOCATION_SERVICE.equals(name) ? null : super.getSystemService(name);
            }
        };
        WeatherController controller = new WeatherController(noLocation, state -> { });
        try {
            Method lastKnown = controller.getClass().getDeclaredMethod("lastKnownLocation", String.class);
            lastKnown.setAccessible(true);
            check(lastKnown.invoke(controller, "gps") == null, "missing location service has no cached location");
            Method request = controller.getClass().getDeclaredMethod("requestProvider", String.class);
            request.setAccessible(true);
            check(Boolean.FALSE.equals(request.invoke(controller, "gps")), "missing provider does not crash");
            checkTimeoutRemoved(controller, "removeLocationListener");
        } finally { controller.shutdown(); }
        try {
            Class<?> daily = Class.forName("com.clockmods.weather.DailyForecastController");
            Class<?> callback = Class.forName("com.clockmods.weather.DailyForecastController$Listener");
            Object listener = java.lang.reflect.Proxy.newProxyInstance(callback.getClassLoader(),
                    new Class<?>[]{callback}, (proxy, method, args) -> null);
            Object forecast = daily.getConstructor(Context.class, callback).newInstance(noLocation, listener);
            try { checkTimeoutRemoved(forecast, "stopLocation"); }
            finally { daily.getMethod("shutdown").invoke(forecast); }
        } catch (ClassNotFoundException removedInLite) {
            check(lite, "only Lite removes the forecast controller");
        }
    }
    private void checkTimeoutRemoved(Object controller, String cleanup) throws Exception {
        Handler handler = (Handler) field(controller, "handler").get(controller);
        Runnable timeout = () -> { throw new AssertionError("cancelled location timeout fired"); };
        field(controller, "locationTimeout").set(controller, timeout);
        handler.postDelayed(timeout, 60000L);
        check(handler.hasCallbacks(timeout), "location timeout is pending before completion");
        Method method = controller.getClass().getDeclaredMethod(cleanup);
        method.setAccessible(true); method.invoke(controller);
        check(!handler.hasCallbacks(timeout), "location completion removes pending timeout");
        check(field(controller, "locationTimeout").get(controller) == null, "location timeout reference released");
    }
    private void checkStatusMeasurement() throws Exception {
        StatusBarView status = new StatusBarView(getTargetContext());
        Method update = StatusBarView.class.getDeclaredMethod("updateBatteryFromIntent", Intent.class);
        update.setAccessible(true);
        update.invoke(status, battery(9, false));
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        status.measure(unspecified, unspecified);
        int oldWidth = status.getMeasuredWidth();
        status.layout(0, 0, oldWidth, status.getMeasuredHeight());
        check(!status.isLayoutRequested(), "status layout begins clean");
        update.invoke(status, battery(100, true));
        check(status.isLayoutRequested(), "longer battery label requests measurement");
        status.measure(unspecified, unspecified);
        check(status.getMeasuredWidth() > oldWidth, "charging label receives the required width");
    }
    private Intent battery(int level, boolean charging) {
        return new Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL, level)
                .putExtra(BatteryManager.EXTRA_SCALE, 100).putExtra(BatteryManager.EXTRA_PLUGGED, charging ? 1 : 0);
    }
    private static void restore(SharedPreferences prefs, Map<String, ?> values) {
        SharedPreferences.Editor edit = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            if (value instanceof String) edit.putString(key, (String) value);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else if (value instanceof Long) edit.putLong(key, (Long) value);
            else if (value instanceof Float) edit.putFloat(key, (Float) value);
            else if (value instanceof java.util.Set) edit.putStringSet(key, (java.util.Set<String>) value);
        }
        edit.commit();
    }
}
