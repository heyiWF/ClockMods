package com.clockmods.pro.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.clockmods.LocaleManager;
import com.clockmods.R;

/** The service owns sound; a silent notification channel cannot play a second, wrong ringtone. */
public final class AlarmDelivery {
    public static final String CHANNEL = "clockmods_alarm_v2";
    public static final String STOP = "com.clockmods.pro.alarm.action.STOP_RINGING";
    public static final String SNOOZE = "com.clockmods.pro.alarm.action.SNOOZE";
    private AlarmDelivery() {}
    public static Notification notification(Context context, AlarmSpec alarm, int queued) {
        Context localized = LocaleManager.wrap(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, localized.getString(R.string.alarm_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null); channel.enableVibration(false); manager.createNotificationChannel(channel);
        long id = alarm == null ? 0 : alarm.id;
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, AlarmRingingActivity.class).setData(AlarmScheduler.uri(id))
                        .putExtra(AlarmScheduler.EXTRA_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_clock)
                .setContentTitle(alarm == null || alarm.label.isEmpty() ? localized.getString(R.string.alarm_ringing) : alarm.label)
                .setContentText(queued > 1 ? localized.getString(R.string.alarm_queue_count, queued) : localized.getString(R.string.alarm_open_to_dismiss))
                .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false).setContentIntent(show)
                .addAction(0, localized.getString(R.string.alarm_snooze), action(context, id, SNOOZE))
                .addAction(0, localized.getString(R.string.alarm_dismiss), action(context, id, STOP));
        if (alarm != null && (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent())) builder.setFullScreenIntent(show, true);
        return builder.build();
    }
    public static Intent command(Context context, long id, String action) {
        return new Intent(context, AlarmRingingService.class).setAction(action)
                .setData(AlarmScheduler.uri(id)).putExtra(AlarmScheduler.EXTRA_ID, id);
    }
    private static PendingIntent action(Context context, long id, String action) {
        return PendingIntent.getService(context, 0, command(context, id, action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
