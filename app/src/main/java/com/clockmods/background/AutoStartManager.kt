package com.clockmods.background

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** Drives the launcher-disguise entry point used for boot auto-start. */
object AutoStartManager {
    private const val HOME_ALIAS = "com.clockmods.HomeLauncherAlias"

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) {
        try {
            val alias = ComponentName(context.packageName, HOME_ALIAS)
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                alias,
                state,
                PackageManager.DONT_KILL_APP,
            )
        } catch (_: RuntimeException) {
            // A restricted device should never crash the settings screen.
        }
    }

    @JvmStatic
    fun requestHomeRole(context: Context) {
        try {
            var intent: Intent? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = HomeRoleRequester.createRequestIntent(context)
            }
            if (intent == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent = Intent(Settings.ACTION_HOME_SETTINGS)
            }
            intent ?: return
            val activity = findActivity(context)
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: RuntimeException) {
            // Some OEMs expose no home-settings screen.
        }
    }

    private fun findActivity(initialContext: Context): Activity? {
        var context = initialContext
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    private object HomeRoleRequester {
        fun createRequestIntent(context: Context): Intent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                ?: return null
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME) ||
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) return null
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
}
