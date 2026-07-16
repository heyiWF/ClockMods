package com.clockmods.pro.alarm;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;

public final class AlarmRingingService extends Service {
    private Ringtone ringtone;
    private Vibrator vibrator;

    @Override public void onCreate() {
        super.onCreate();
        startForeground(100, AlarmNotifications.build(this));
        ringtone = RingtoneManager.getRingtone(this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        if (ringtone != null) {
            ringtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM).build());
            ringtone.setLooping(true);
            ringtone.play();
        }
        vibrator = getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[] {0L, 500L, 500L}, 0));
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (ringtone != null) ringtone.stop();
        if (vibrator != null) vibrator.cancel();
        AlarmNotifications.cancel(this);
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}