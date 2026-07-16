package com.clockmods.pro.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AlarmStore store = new AlarmStore(context);
        if (store.enabled()) AlarmScheduler.schedule(context, store.hour(), store.minute());
    }
}