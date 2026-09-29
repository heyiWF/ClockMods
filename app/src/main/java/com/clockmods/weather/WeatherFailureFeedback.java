package com.clockmods.weather;

import android.content.Context;
import android.widget.Toast;

import com.clockmods.LocaleManager;
import com.clockmods.R;

/** Keeps detailed fetch failures out of weather views and avoids repeated toasts. */
public final class WeatherFailureFeedback {
    private static final WeatherFailureDeduplicator DEDUPLICATOR =
            new WeatherFailureDeduplicator();

    private WeatherFailureFeedback() { }

    public static String placeholder(Context context) {
        return LocaleManager.wrap(context).getString(R.string.weather_fetch_failed_placeholder);
    }

    /** Shows one detailed failure per app process, even across weather and forecast. */
    public static void showOnce(Context context, String detail) {
        if (!DEDUPLICATOR.shouldShow()) return;
        Toast.makeText(context.getApplicationContext(), detail, Toast.LENGTH_LONG).show();
    }
}
