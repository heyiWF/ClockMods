package com.clockmods.pro.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        AlarmStore store = new AlarmStore(context);
        AlarmSpec alarm = store.find(intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0));
        long expected = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER, 0);
        // Cancelled, edited and duplicate deliveries must never ring a different alarm.
        if (alarm == null || expected == 0 || expected != alarm.scheduledAt
                || expected <= alarm.lastFiredAt || (!alarm.enabled && alarm.snoozeAt != expected)) return;
        alarm.lastFiredAt = expected;
        if (alarm.snoozeAt == expected) alarm.snoozeAt = 0;
        alarm.scheduledAt = 0;
        if (alarm.repeat == AlarmSpec.Repeat.ONCE
                && AlarmScheduler.nextTrigger(alarm, context, System.currentTimeMillis()) == 0) alarm.enabled = false;
        store.save(alarm); store.rememberRing(alarm.id);
        try {
            ContextCompat.startForegroundService(context, new Intent(context, AlarmRingingService.class)
                    .putExtra(AlarmScheduler.EXTRA_ID, alarm.id));
        } catch (android.app.ForegroundServiceStartNotAllowedException restricted) {
            // Inexact fallback alarms may lack the exact-alarm FGS exemption. A user tap can resume.
            store.setRingingId(alarm.id);
            context.getSystemService(android.app.NotificationManager.class).notify(100,
                    AlarmDelivery.notification(context, alarm, store.pendingRings().size()));
        }
        AlarmScheduler.schedule(context, alarm);
    }
}
