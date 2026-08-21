package com.clockmods.ultimate;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.clockmods.pro.ProMainActivity;
import com.clockmods.background.ClockPreferences;
import com.clockmods.ultimate.settings.UltimateSettingsActivity;

/** Ultimate shell for the clock, tool destinations, and full-screen settings. */
public final class UltimateMainActivity extends ProMainActivity {
    private static final int REQUEST_ULTIMATE_SETTINGS = 4817;
    private String appliedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedLanguage = new ClockPreferences(this).getClockLanguage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguage = new ClockPreferences(this).getClockLanguage();
        if (appliedLanguage != null && !appliedLanguage.equals(currentLanguage)) {
            appliedLanguage = currentLanguage;
            getWindow().getDecorView().post(this::recreate);
        }
    }

    @Override
    public void showSettings() {
        Intent intent = UltimateSettingsActivity.createIntent(this);
        startActivityForResult(intent, REQUEST_ULTIMATE_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ULTIMATE_SETTINGS) {
            String currentLanguage = new ClockPreferences(this).getClockLanguage();
            if (appliedLanguage != null && !appliedLanguage.equals(currentLanguage)) {
                appliedLanguage = currentLanguage;
                recreate();
                return;
            }
            refreshSettingsPages();
        }
    }
}
