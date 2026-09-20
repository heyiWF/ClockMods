package com.clockmods.ultimate

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.clockmods.widget.update.WidgetUpdateCoordinator

/** Keeps launcher widgets synchronized with app preferences and configuration changes. */
class UltimateApplication : Application() {
    private val widgetHandler = Handler(Looper.getMainLooper())
    private val refreshWidgets = Runnable {
        WidgetUpdateCoordinator.execute { WidgetUpdateCoordinator.updateAll(this) }
    }
    private val widgetPreferences = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && (key.startsWith("weather_") || key == "clock_language")) {
            widgetHandler.removeCallbacks(refreshWidgets)
            widgetHandler.postDelayed(refreshWidgets, 300L)
        }
    }

    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        super.onConfigurationChanged(config)
        widgetHandler.removeCallbacks(refreshWidgets)
        widgetHandler.postDelayed(refreshWidgets, 300L)
    }

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("clock_prefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(widgetPreferences)
        widgetHandler.post(refreshWidgets)
    }
}
