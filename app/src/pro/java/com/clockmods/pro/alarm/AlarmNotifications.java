package com.clockmods.pro.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import com.clockmods.R;

public final class AlarmNotifications {
    private static final String CHANNEL_ID = "clockmods_alarm";
    private AlarmNotifications() { }

    public static void show(Context context) {
                context.getSystemService(NotificationManager.class).notify(100, build(context));
        }

        public static android.app.Notification build(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.alarm_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI,
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
        PendingIntent ringing = PendingIntent.getActivity(context, 101,
                new Intent(context, AlarmRingingActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.alarm_ringing))
                .setContentText(context.getString(R.string.alarm_open_to_dismiss))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(ringing)
                .setFullScreenIntent(ringing, true)
                .build();
    }

    public static void cancel(Context context) {
        context.getSystemService(NotificationManager.class).cancel(100);
    }
}