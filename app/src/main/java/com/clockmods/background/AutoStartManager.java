package com.clockmods.background;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.clockmods.platform.ApiCompat;
import android.provider.Settings;

/**
 * Drives the launcher-disguise entry point used for boot auto-start.
 *
 * <p>The app ships a HOME/DEFAULT {@code <activity-alias>} ({@code .HomeLauncherAlias}) that is
 * disabled by default. When the user turns on "开机自启动" the alias is enabled and the user is
 * guided to pick this app as the device's default home; the system then launches the clock
 * automatically at boot, which is exempt from the Android 10+ background-activity-start restriction.
 * Turning the toggle off disables the alias and the device falls back to its normal launcher.
 *
 * <p>Uses framework APIs only (no AppCompat/Material) so it compiles for every flavor, including
 * the API 1 {@code compat} build; the Android 10 role-manager API is reached reflectively so the
 * class is never linked on older runtimes or older compile SDKs.
 */
public final class AutoStartManager {
    /** Fully-qualified (namespace-based) name of the HOME alias declared in the manifest. */
    private static final String HOME_ALIAS = "com.clockmods.HomeLauncherAlias";
    /** Build.VERSION_CODES.Q is not defined by compileSdk 28, so use its numeric value. */
    private static final int ANDROID_Q = 29;

    private AutoStartManager() {
    }

    /** Enables or disables the HOME alias component without killing the running app. */
    public static void setEnabled(Context context, boolean enabled) {
        try {
            ComponentName alias = new ComponentName(context.getPackageName(), HOME_ALIAS);
            int state = enabled
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            context.getPackageManager().setComponentEnabledSetting(
                    alias, state, PackageManager.DONT_KILL_APP);
        } catch (RuntimeException ignored) {
            // A restricted device should never crash the settings screen.
        }
    }

    /**
     * Guides the user to make this app the default home so it launches on boot: the role picker on
     * Android 10+, the home-settings screen on 5.0–9, and the system's own "select a Home app"
     * prompt (already triggered by enabling the alias) on older releases.
     */
    public static void requestHomeRole(Context context) {
        try {
            Intent intent = null;
            if (ApiCompat.SDK_INT >= ANDROID_Q) {
                intent = HomeRoleRequester.createRequestIntent(context);
            }
            if (intent == null && ApiCompat.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            }
            if (intent == null) {
                return;
            }
            Activity activity = findActivity(context);
            if (activity != null) {
                activity.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (RuntimeException ignored) {
            // Some OEMs expose no home-settings screen; the alias is already enabled and the system
            // prompts for a default Home on the next Home press.
        }
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Reflective access to the Android 10 role manager. The API 29 classes are never linked
     * on older runtimes, and the code also compiles against compileSdk 28.
     */
    private static final class HomeRoleRequester {
        @SuppressLint("WrongConstant") // Context.ROLE_SERVICE was added after compileSdk 28.
        static Intent createRequestIntent(Context context) {
            if (ApiCompat.SDK_INT < ANDROID_Q) {
                return null;
            }
            try {
                Object roleManager = context.getSystemService("role");
                if (roleManager == null) {
                    return null;
                }
                Class<?> type = roleManager.getClass();
                String roleHome = (String) type.getField("ROLE_HOME").get(null);
                boolean available = Boolean.TRUE.equals(
                        type.getMethod("isRoleAvailable", String.class)
                                .invoke(roleManager, roleHome));
                boolean held = Boolean.TRUE.equals(
                        type.getMethod("isRoleHeld", String.class)
                                .invoke(roleManager, roleHome));
                if (!available || held) {
                    return null;
                }
                return (Intent) type.getMethod("createRequestRoleIntent", String.class)
                        .invoke(roleManager, roleHome);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
