package com.clockmods.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;

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
     * Lives here so shared {@code main} code can obtain it without linking against Material,
     * which the compat flavour does not ship.
     */
    public static int resolveAccentColor(Context context, int fallback) {
        return MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, fallback);
    }

    public static Intent createImagePickerIntent() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*");
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        return intent;
    }
}