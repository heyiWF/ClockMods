package com.clockmods.pro.alarm;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import java.util.LinkedHashMap;

/** Simultaneous occurrences queue independently; dismissing one never cancels another's schedule. */
public final class AlarmRingingService extends Service {
    private final LinkedHashMap<Long, AlarmSpec> queue = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long activeId;
    private Ringtone ringtone;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioFocusRequest focus;
    private final Runnable timeout = () -> dismiss(activeId, false);
    @Override public void onCreate() {
        super.onCreate();
        AlarmStore store = new AlarmStore(this);
        for (Long id : store.pendingRings().keySet()) queue.put(id, store.find(id));
        startForeground(100, AlarmDelivery.notification(this, null, 0));
        PowerManager power = getSystemService(PowerManager.class);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClockMods:alarm");
        wakeLock.acquire(11 * 60_000L);
        VibratorManager manager = getSystemService(VibratorManager.class);
        vibrator = manager == null ? null : manager.getDefaultVibrator();
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { playNext(); return START_STICKY; }
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, activeId);
        if (AlarmDelivery.STOP.equals(intent.getAction()) || AlarmDelivery.SNOOZE.equals(intent.getAction())) {
            dismiss(id, AlarmDelivery.SNOOZE.equals(intent.getAction()));
        } else {
            AlarmSpec alarm = new AlarmStore(this).find(id);
            if (alarm != null) { new AlarmStore(this).rememberRing(id); queue.putIfAbsent(id, alarm); }
            if (activeId == 0) playNext();
            else startForeground(100, AlarmDelivery.notification(this, queue.get(activeId), queue.size()));
        }
        return START_STICKY;
    }
    private void dismiss(long id, boolean snooze) {
        AlarmSpec ringing = queue.remove(id);
        new AlarmStore(this).forgetRing(id);
        if (ringing != null && snooze) {
            AlarmStore store = new AlarmStore(this); AlarmSpec saved = store.find(id);
            if (saved != null) {
                saved.snoozeAt = System.currentTimeMillis() + saved.snoozeMinutes * 60_000L;
                store.save(saved); AlarmScheduler.schedule(this, saved);
            }
        }
        if (activeId == id || queue.isEmpty()) { stopAudio(); activeId = 0; playNext(); }
        else startForeground(100, AlarmDelivery.notification(this, queue.get(activeId), queue.size()));
    }
    private void playNext() {
        if (queue.isEmpty()) { new AlarmStore(this).setRingingId(0); stopSelf(); return; }
        AlarmSpec alarm = queue.values().iterator().next(); activeId = alarm.id;
        new AlarmStore(this).setRingingId(activeId);
        startForeground(100, AlarmDelivery.notification(this, alarm, queue.size()));
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(change -> {}).build();
        getSystemService(AudioManager.class).requestAudioFocus(focus);
        if (!"silent".equals(alarm.ringtone)) {
            Uri uri = "default".equals(alarm.ringtone) ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(alarm.ringtone);
            if (!playRingtone(uri, attributes))
                playRingtone(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attributes);
        }
        if (alarm.vibrate && vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0), attributes);
        if (wakeLock != null) { if (wakeLock.isHeld()) wakeLock.release(); wakeLock.acquire(11 * 60_000L); }
        Long until = new AlarmStore(this).pendingRings().get(activeId);
        handler.postDelayed(timeout, until == null ? 0 : Math.max(0, until - System.currentTimeMillis()));
    }
    private boolean playRingtone(Uri uri, AudioAttributes attributes) {
        try {
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone == null) return false;
            ringtone.setAudioAttributes(attributes); ringtone.setLooping(true); ringtone.play();
            return ringtone.isPlaying();
        } catch (RuntimeException unavailable) {
            if (ringtone != null) { try { ringtone.stop(); } catch (RuntimeException ignored) { } }
            ringtone = null; return false;
        }
    }
    private void stopAudio() {
        handler.removeCallbacks(timeout);
        if (ringtone != null) { ringtone.stop(); ringtone = null; }
        if (vibrator != null) vibrator.cancel();
        if (focus != null) { getSystemService(AudioManager.class).abandonAudioFocusRequest(focus); focus = null; }
    }
    @Override public void onDestroy() {
        stopAudio(); new AlarmStore(this).setRingingId(0);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
