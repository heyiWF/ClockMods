package com.clockmods.ultimate;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;


import com.clockmods.pro.ProMainActivity;
import com.clockmods.background.ClockPreferences;
import com.clockmods.ultimate.settings.UltimateSettingsActivity;

/** Ultimate shell for the clock, tool destinations, and full-screen settings. */
public final class UltimateMainActivity extends ProMainActivity {
    private String appliedLanguage;

    /**
     * Replaces startActivityForResult / onActivityResult, which the platform retired in favour of
     * a launcher registered up front — the registration has to happen before the activity is
     * STARTED, which is why it is a field initialiser rather than a call inside showSettings().
     */
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> onSettingsClosed());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedLanguage = new ClockPreferences(this).getClockLanguage();
        if (!SetupWizardActivity.isCompleted(this)) {
            // Defer until the first frame so the host is fully attached before presenting onboarding.
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinishing() && !isDestroyed() && !SetupWizardActivity.isCompleted(this)) {
                    startActivity(new Intent(this, SetupWizardActivity.class));
                }
            });
        }
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
        settingsLauncher.launch(UltimateSettingsActivity.createIntent(this));
    }

    private void onSettingsClosed() {
        String currentLanguage = new ClockPreferences(this).getClockLanguage();
        if (appliedLanguage != null && !appliedLanguage.equals(currentLanguage)) {
            appliedLanguage = currentLanguage;
            recreate();
            return;
        }
        refreshSettingsPages();
    }
}
