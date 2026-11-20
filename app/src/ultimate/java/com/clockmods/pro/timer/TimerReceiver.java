package com.clockmods.pro.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class TimerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        complete(context, intent.getStringExtra("mode"));
    }

    public static void complete(Context context, String mode) {
        TimerCompletion.complete(context, mode);
    }
}
