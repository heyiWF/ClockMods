package com.clockmods.pro.timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.clockmods.R;
import com.clockmods.pro.ProMainActivity;

public final class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "clockmods_timer";

    @Override public void onReceive(Context context, Intent intent) {
        String mode = intent.getStringExtra("mode");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.timer_channel), NotificationManager.IMPORTANCE_HIGH));
        PendingIntent open = PendingIntent.getActivity(context, 203,
                new Intent(context, ProMainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean pomodoro = "pomodoro".equals(mode);
        manager.notify(pomodoro ? 201 : 202, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(pomodoro
                        ? R.string.pomodoro_complete : R.string.countdown_complete))
                .setContentText(context.getString(R.string.timer_complete_open))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build());
    }
}