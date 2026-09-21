package com.clockmods.ultimate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.clockmods.LocaleManager
import com.clockmods.background.ClockPreferences
import com.clockmods.pro.alarm.AlarmScheduler
import com.clockmods.pro.alarm.AlarmStore
import com.clockmods.pro.timer.TimerScheduler
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.compose.UltimateApp
import com.clockmods.ultimate.settings.ComposeSettingsActivity

/**
 * Compose-native launcher shell.
 *
 * Its activity boundary remains deliberately thin: page state lives in Compose, while Android
 * lifecycle, locale, orientation, and permission hand-offs stay here where the platform owns them.
 */
open class ComposeMainActivity : ComponentActivity() {
    private val refreshGeneration = mutableIntStateOf(0)
    private val navigationRequestGeneration = mutableIntStateOf(0)
    private val requestedDestination = mutableStateOf<String?>(null)
    private var appliedLanguage: String? = null
    private var setupWizardLaunchPending = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshAfterPreferencesChange()
    }

    private val setupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        setupWizardLaunchPending = false
        if (SetupWizardActivity.isCompleted(this)) {
            requestNotificationPermission()
        }
        refreshAfterPreferencesChange()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase) ?: newBase)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setLocale(LocaleManager.resolveLocale(this))
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWizardLaunchPending = savedInstanceState?.getBoolean(
            STATE_SETUP_WIZARD_PENDING,
            false,
        ) == true
        if (savedInstanceState == null) applyNavigationIntent(intent)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyOrientation()
        appliedLanguage = ClockPreferences(this).getClockLanguage()

        setContent {
            ClockModsTheme {
                UltimateApp(
                    refreshGeneration = refreshGeneration.intValue,
                    requestedDestination = requestedDestination.value,
                    navigationRequestGeneration = navigationRequestGeneration.intValue,
                    onOpenSettings = {
                        settingsLauncher.launch(ComposeSettingsActivity.createIntent(this))
                    },
                    onOpenCalendarSettings = {
                        settingsLauncher.launch(
                            ComposeSettingsActivity.createSubpageIntent(this, "calendar"),
                        )
                    },
                )
            }
        }

        if (!SetupWizardActivity.isCompleted(this) && !setupWizardLaunchPending) {
            window.decorView.post {
                if (!isFinishing && !isDestroyed && !setupWizardLaunchPending &&
                    !SetupWizardActivity.isCompleted(this)
                ) {
                    setupWizardLaunchPending = true
                    setupLauncher.launch(Intent(this, SetupWizardActivity::class.java))
                }
            }
        } else if (SetupWizardActivity.isCompleted(this)) {
            requestNotificationPermission()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyNavigationIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SETUP_WIZARD_PENDING, setupWizardLaunchPending)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        val alarmStore = AlarmStore(this)
        if (alarmStore.enabled()) {
            AlarmScheduler.schedule(this, alarmStore.hour(), alarmStore.minute())
        }
        TimerScheduler.restoreRunningTimers(this)
        val language = ClockPreferences(this).getClockLanguage()
        if (appliedLanguage != null && language != appliedLanguage) {
            appliedLanguage = language
            recreate()
        } else {
            refreshGeneration.intValue++
        }
    }

    private fun applyOrientation() {
        requestedOrientation = ClockPreferences.toActivityInfoOrientation(
            ClockPreferences(this).getScreenOrientation(),
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun refreshAfterPreferencesChange() {
        val language = ClockPreferences(this).getClockLanguage()
        if (language != appliedLanguage) {
            appliedLanguage = language
            recreate()
        } else {
            refreshGeneration.intValue++
            applyOrientation()
        }
    }

    private fun applyNavigationIntent(intent: Intent?) {
        val destination = resolveDestination(
            intent?.getStringExtra(EXTRA_DESTINATION),
            intent?.data?.lastPathSegment,
        ) ?: return
        requestedDestination.value = destination
        navigationRequestGeneration.intValue++
    }

    companion object {
        const val EXTRA_DESTINATION = "com.clockmods.ultimate.extra.DESTINATION"
        const val DESTINATION_CLOCK = "clock"
        const val DESTINATION_CALENDAR = "calendar"
        private const val STATE_SETUP_WIZARD_PENDING =
            "ultimate_setup_wizard_launch_pending"

        /** Accepts both Compose-era extras and URIs held by pre-migration widget PendingIntents. */
        @JvmStatic
        fun resolveDestination(requested: String?, legacyWidgetAction: String?): String? = when {
            requested == DESTINATION_CLOCK || requested == DESTINATION_CALENDAR -> requested
            legacyWidgetAction.equals("CLOCK", ignoreCase = true) -> DESTINATION_CLOCK
            legacyWidgetAction.equals("CALENDAR", ignoreCase = true) -> DESTINATION_CALENDAR
            else -> null
        }
    }
}

/** Preserves the launcher component identity used by installed Ultimate builds. */
class UltimateMainActivity : ComposeMainActivity()
