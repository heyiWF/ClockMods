package com.clockmods.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ExperienceBridge {
    private ExperienceBridge() {
    }

    public static void applyThemeFeatures(Activity activity) {
        // Compatibility flavor keeps the platform theme on API 14+.
    }

    /**
     * The compat flavour has no Material dynamic-colour palette, so weather icons that opt into
     * accent tinting simply fall back to the caller-supplied colour.
     */
    public static int resolveAccentColor(Context context, int fallback) {
        return fallback;
    }

    public static Intent createImagePickerIntent() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("image/*");
        return intent;
    }
}