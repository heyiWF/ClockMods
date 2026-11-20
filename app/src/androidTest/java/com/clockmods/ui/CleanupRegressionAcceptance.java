package com.clockmods.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;

import com.clockmods.background.ClockPreferences;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Runs without network requests and restores all touched preferences. */
@android.annotation.TargetApi(29)
public final class CleanupRegressionAcceptance extends Instrumentation {
    private int checks;

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        final Throwable[] failure = {null};
        runOnMainSync(() -> {
            try {
                checkReset();
                checkStatusMeasurement();
            } catch (Throwable error) {
                failure[0] = error;
            }
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

    private void checkReset() {
        SharedPreferences prefs = getTargetContext().getSharedPreferences("clock_prefs", 0);
        Map<String, ?> before = new HashMap<>(prefs.getAll());
        try {
            ClockPreferences settings = new ClockPreferences(getTargetContext());
            settings.setUseNetworkTime(true);
            settings.setSyncIntervalMinutes(10);
            settings.setTimeZoneId("America/New_York");
            settings.restoreDefaults();
            check(!settings.isUseNetworkTime(), "reset disables network time");
            check(settings.getSyncIntervalMinutes() == ClockPreferences.DEFAULT_SYNC_INTERVAL_MINUTES,
                    "reset restores sync interval");
            check(settings.getTimeZoneId().equals(ClockPreferences.DEFAULT_TIME_ZONE_ID),
                    "reset restores time zone");
        } finally {
            restore(prefs, before);
        }
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
        return new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_LEVEL, level)
                .putExtra(BatteryManager.EXTRA_SCALE, 100)
                .putExtra(BatteryManager.EXTRA_PLUGGED, charging ? 1 : 0);
    }

    private static void restore(SharedPreferences prefs, Map<String, ?> values) {
        SharedPreferences.Editor edit = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
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
