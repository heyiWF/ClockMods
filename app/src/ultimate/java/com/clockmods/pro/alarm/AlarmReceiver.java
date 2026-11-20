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
        if (SnoozeNotification.STOP.equals(intent.getAction())) {
            if (alarm != null && !alarm.isTimer() && expected > 0) {
                SnoozeNotification.stop(context, store, alarm, expected);
            }
            return;
        }
        // Cancelled, edited and duplicate deliveries must never ring a different alarm.
        if (alarm == null || expected == 0 || expected != alarm.scheduledAt
                || expected <= alarm.lastFiredAt || (!alarm.enabled && alarm.snoozeAt != expected)) return;
        // A snooze broadcast from an older release can race package-replacement cleanup.
        if (alarm.isTimer()) { store.delete(alarm.id); return; }
        alarm.lastFiredAt = expected;
        if (alarm.snoozeAt == expected) alarm.snoozeAt = 0;
        alarm.scheduledAt = 0;
        if (alarm.repeat == AlarmSpec.Repeat.ONCE
                && AlarmScheduler.nextTrigger(alarm, context, System.currentTimeMillis()) == 0) alarm.enabled = false;
        store.save(alarm); AlarmDelivery.ring(context, alarm);
        AlarmScheduler.schedule(context, alarm);
    }
}
