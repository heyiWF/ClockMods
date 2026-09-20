package com.clockmods.pro.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmRingingService : Service() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(100, AlarmNotifications.build(this))
        AlarmStore(this).setRinging(true)
        ringtone = RingtoneManager.getRingtone(
            this,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        )?.also {
            it.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            it.isLooping = true
            it.play()
        }
        vibrator = getSystemService(VibratorManager::class.java)?.defaultVibrator
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0L, 500L, 500L), 0),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The ringing notification exposes a "stop" action so the alarm can always be dismissed,
        // even when the full-screen ringing activity could not be launched.
        if (intent?.action == AlarmNotifications.ACTION_STOP_RINGING) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringtone?.stop()
        vibrator?.cancel()
        AlarmStore(this).setRinging(false)
        AlarmNotifications.cancel(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
