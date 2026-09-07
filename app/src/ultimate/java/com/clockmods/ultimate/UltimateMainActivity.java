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
    private static final String STATE_SETUP_WIZARD_PENDING =
            "ultimate_setup_wizard_launch_pending";
    private String appliedLanguage;
    private boolean setupWizardLaunchPending;

    /**
     * Replaces startActivityForResult / onActivityResult, which the platform retired in favour of
     * a launcher registered up front — the registration has to happen before the activity is
     * STARTED, which is why it is a field initialiser rather than a call inside showSettings().
     */
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> onSettingsClosed());

    private final ActivityResultLauncher<Intent> setupWizardLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> onSetupWizardClosed());

    @Override
    protected boolean shouldDeferNotificationPermission() {
        return !SetupWizardActivity.isCompleted(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            setupWizardLaunchPending = savedInstanceState.getBoolean(
                    STATE_SETUP_WIZARD_PENDING, false);
        }
        appliedLanguage = new ClockPreferences(this).getClockLanguage();
        if (!SetupWizardActivity.isCompleted(this) && !setupWizardLaunchPending) {
            // Defer until the first frame so the host is fully attached before presenting onboarding.
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinishing() && !isDestroyed() && !setupWizardLaunchPending
                        && !SetupWizardActivity.isCompleted(this)) {
                    setupWizardLaunchPending = true;
                    setupWizardLauncher.launch(new Intent(this, SetupWizardActivity.class));
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_SETUP_WIZARD_PENDING, setupWizardLaunchPending);
        super.onSaveInstanceState(outState);
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

    private void onSetupWizardClosed() {
        setupWizardLaunchPending = false;
        // Ask only after the user has seen the essentials and left onboarding, so the system
        // permission sheet never covers the first-launch wizard.
        if (SetupWizardActivity.isCompleted(this)) {
            requestNotificationPermissionIfNeeded();
        }
        String currentLanguage = new ClockPreferences(this).getClockLanguage();
        if (appliedLanguage != null && !appliedLanguage.equals(currentLanguage)) {
            appliedLanguage = currentLanguage;
            recreate();
            return;
        }
        refreshSettingsPages();
    }
}
