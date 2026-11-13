package com.clockmods.pro.alarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action) && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                && !AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) return;
        // A reboot must not resurrect stale audio; future alarms and snoozes retain their IDs.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) new AlarmStore(context).clearRings();
        AlarmScheduler.rescheduleAll(context);
    }
}
