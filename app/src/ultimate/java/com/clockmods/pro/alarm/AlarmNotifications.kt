package com.clockmods.pro.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.clockmods.LocaleManager
import com.clockmods.R

class AlarmNotifications private constructor() {
    companion object {
        private const val CHANNEL_ID = "clockmods_alarm_v2"
        private const val NOTIFICATION_ID = 100
        private const val RINGING_REQUEST_CODE = 101
        private const val STOP_REQUEST_CODE = 102

        /**
         * Android 14 requires USE_FULL_SCREEN_INTENT to be granted by the user for any app whose
         * Play Store listing does not declare itself an alarm or calling app.  While the access is
         * missing the platform only shows a heads-up notification, so the ringing UI has to be
         * opened from an allowed path instead.
         */
        @JvmStatic
        fun requiresFullScreenIntentPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= 34 &&
                !ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.USE_FULL_SCREEN_INTENT,
                ).equals(PackageManager.PERMISSION_GRANTED)

        /**
         * Notification permission is optional on Android 13+.  Alarm delivery must still leave
         * the user with a way to stop the ringing when that permission is denied.
         */
        @JvmStatic
        fun canPostNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            // Also account for the user disabling this app's notifications (or the alarm channel)
            // from system settings.  In either case the ongoing notification cannot be used as a
            // dismissal surface.
            return runCatching {
                val manager = notificationManager(context)
                manager.areNotificationsEnabled() &&
                    manager.getNotificationChannel(CHANNEL_ID)?.importance !=
                    NotificationManager.IMPORTANCE_NONE
            }.getOrDefault(false)
        }

        /**
         * Android 14 can revoke the full-screen intent special access independently of the
         * notification permission.  Keep the API reference behind the version check because this
         * method does not exist on older releases.
         */
        @JvmStatic
        fun canUseFullScreenIntent(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 34) return true
            return runCatching { notificationManager(context).canUseFullScreenIntent() }
                .getOrDefault(false)
        }

        /** The activity intent used by both the notification and the direct fallback path. */
        @JvmStatic
        fun ringingActivityIntent(context: Context): Intent =
            Intent(context, AlarmRingingActivity::class.java)

        /**
         * A background alarm broadcast cannot rely on a hidden notification for dismissal when
         * POST_NOTIFICATIONS is denied.  Opening the ringing activity directly only works when the
         * full-screen intent route is available, because that path is exempt from the Android 11+
         * background activity launch restriction.
         */
        @JvmStatic
        fun shouldOpenRingingActivityFromReceiver(context: Context): Boolean =
            shouldOpenRingingActivityFromReceiver(
                notificationsVisible = canPostNotifications(context),
                fullScreenIntentAvailable = canUseFullScreenIntent(context),
            )

        internal fun shouldOpenRingingActivityFromReceiver(
            notificationsVisible: Boolean,
            fullScreenIntentAvailable: Boolean,
        ): Boolean = !notificationsVisible && fullScreenIntentAvailable

        internal fun shouldAttachFullScreenIntent(
            fullScreenIntentAccess: Boolean,
        ): Boolean = fullScreenIntentAccess

        /** Settings deep link for Android 14's full-screen intent special access. */
        @JvmStatic
        fun fullScreenIntentSettingsIntent(context: Context): Intent? {
            if (Build.VERSION.SDK_INT < 34) return null
            return Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:${context.packageName}"),
            )
        }

        @JvmStatic
        fun show(context: Context) {
            notificationManager(context).notify(NOTIFICATION_ID, build(context))
        }

        @JvmStatic
        fun build(context: Context): Notification = AlarmDelivery.notification(
            context, AlarmStore(context).find(AlarmStore(context).ringingId()), 1,
        )

        @JvmStatic
        fun cancel(context: Context) {
            notificationManager(context).cancel(NOTIFICATION_ID)
        }

        /**
         * Direct dismissal surface that does not depend on the ringing activity being visible.
         * The action is delivered to [AlarmRingingService] through the intent filter declared in
         * the manifest, so it survives the activity being killed or never launched.
         */
        private fun stopRingingIntent(context: Context): PendingIntent = PendingIntent.getService(
            context,
            STOP_REQUEST_CODE,
            Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_STOP_RINGING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        internal const val ACTION_STOP_RINGING = "com.clockmods.pro.alarm.action.STOP_RINGING"

        private fun notificationManager(context: Context): NotificationManager =
            requireNotNull(context.getSystemService(NotificationManager::class.java))
    }
}
