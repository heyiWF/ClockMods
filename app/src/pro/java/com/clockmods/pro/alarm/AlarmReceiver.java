package com.clockmods.pro.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AlarmStore store = new AlarmStore(context);
        if (!store.enabled()) return;
        ContextCompat.startForegroundService(context,
            new Intent(context, AlarmRingingService.class));
        AlarmScheduler.schedule(context, store.hour(), store.minute());
    }
}