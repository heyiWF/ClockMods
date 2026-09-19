package com.clockmods.ultimate;

import android.app.Application;

import com.clockmods.ultimate.settings.UltimateEmbeddingRules;

/** Registers adaptive settings behavior before the first Activity is created. */
public final class UltimateApplication extends Application {
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener widgetPreferences;
    private final android.os.Handler widgetHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshWidgets = () -> com.clockmods.widget.update.WidgetUpdateCoordinator.execute(
            () -> com.clockmods.widget.update.WidgetUpdateCoordinator.updateAll(this));
    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        widgetHandler.removeCallbacks(refreshWidgets);
        widgetHandler.postDelayed(refreshWidgets, 300);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        UltimateEmbeddingRules.install(this);
        widgetPreferences = (prefs, key) -> {
            if (key != null && (key.startsWith("weather_") || key.equals("clock_language"))) {
                widgetHandler.removeCallbacks(refreshWidgets);
                widgetHandler.postDelayed(refreshWidgets, 300);
            }
        };
        getSharedPreferences("clock_prefs", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(widgetPreferences);
        widgetHandler.post(refreshWidgets);
    }
}
