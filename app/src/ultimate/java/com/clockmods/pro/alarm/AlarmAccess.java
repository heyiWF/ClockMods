package com.clockmods.pro.alarm;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;

public final class AlarmAccess {
    private AlarmAccess() {}
    public static boolean notifications(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        android.app.NotificationChannel channel = manager.getNotificationChannel(AlarmDelivery.CHANNEL);
        return manager.areNotificationsEnabled() && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }
    public static boolean fullScreen(Context context) {
        return Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager.class).canUseFullScreenIntent();
    }
    public static void settings(Context context, int kind) {
        Intent intent = kind == 0 ? new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.getPackageName()))
                : kind == 1 && Build.VERSION.SDK_INT >= 34 ? new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + context.getPackageName()))
                : new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        try { context.startActivity(intent); } catch (android.content.ActivityNotFoundException ignored) { }
    }
    public static void enabled(Context context, AlarmSpec alarm, boolean value) {
        alarm.enabled = value; alarm.snoozeAt = 0;
        if (value && alarm.repeat == AlarmSpec.Repeat.ONCE
                && AlarmScheduler.nextTrigger(alarm, context, System.currentTimeMillis()) == 0) {
            alarm.onceDate = AlarmPlan.nextDate(alarm.hour, alarm.minute, System.currentTimeMillis(), java.time.ZoneId.systemDefault());
        }
        new AlarmStore(context).save(alarm); AlarmScheduler.schedule(context, alarm);
        if (!value && new AlarmStore(context).isRinging())
            context.startService(AlarmDelivery.command(context, alarm.id, AlarmDelivery.STOP));
    }
}
