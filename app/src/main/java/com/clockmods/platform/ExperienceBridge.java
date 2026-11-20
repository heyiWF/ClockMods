package com.clockmods.platform;

import android.content.Intent;

public final class ExperienceBridge {
    private ExperienceBridge() {
    }

    public static Intent createImagePickerIntent() {
        Intent intent;
        if (ApiCompat.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("image/*");
        return intent;
    }
}
