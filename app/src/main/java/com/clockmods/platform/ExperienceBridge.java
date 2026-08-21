package com.clockmods.platform;

import android.app.Activity;
import android.content.Context;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

public final class ExperienceBridge {
    private ExperienceBridge() {
    }

    public static void applyThemeFeatures(Activity activity) {
        DynamicColors.applyToActivityIfAvailable(activity);
    }

    /**
     * Resolves the Material 3 accent (primary) colour of the current theme, which reflects the
     * device's dynamic colour palette on Android 12+ once {@link #applyThemeFeatures} has run.
     */
    public static int resolveAccentColor(Context context, int fallback) {
        return MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, fallback);
    }

}
