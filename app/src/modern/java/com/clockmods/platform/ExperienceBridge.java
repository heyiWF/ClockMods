package com.clockmods.platform;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;

import com.google.android.material.color.DynamicColors;

public final class ExperienceBridge {
    private ExperienceBridge() {
    }

    public static void applyThemeFeatures(Activity activity) {
        DynamicColors.applyToActivityIfAvailable(activity);
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