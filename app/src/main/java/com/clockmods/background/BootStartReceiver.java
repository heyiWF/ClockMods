package com.clockmods.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.clockmods.MainActivity;

/**
 * Starts the clock directly after boot when "开机自启动" is on.
 *
 * <p>The HOME alias is still used on modern Android where a boot-time activity start is
 * restricted, but on older releases (including Android 1.0, where the user cannot be guided
 * through a role picker) the receiver makes the setting work on its own.
 */
public final class BootStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (!new ClockPreferences(context).isAutoStart()) {
            return;
        }
        try {
            Intent start = new Intent(context, MainActivity.class);
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(start);
        } catch (Throwable ignored) {
            // Android 10+ may block a background activity start; the HOME alias covers that case.
        }
    }
}
