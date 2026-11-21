package com.clockmods.pro.timer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

/** Only the resumed host's selected tool may keep its window awake. */
public final class TimingScreenOn implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY = "keep_screen_on_while_timing";
    private final Activity host;
    private final SharedPreferences settings, timers, stopwatch;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable atDeadline = this::refresh;
    private boolean resumed;
    private String page = "";

    public TimingScreenOn(Activity host) {
        this.host = host;
        settings = settings(host);
        timers = host.getSharedPreferences("pro_timers", 0);
        stopwatch = host.getSharedPreferences("pro_stopwatch", 0);
    }

    public static SharedPreferences settings(Context context) {
        return context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE);
    }

    public void select(String destination) {
        if (destination.equals(page)) return;
        page = destination;
        refresh();
    }

    public void resume() {
        resumed = true;
        settings.registerOnSharedPreferenceChangeListener(this);
        timers.registerOnSharedPreferenceChangeListener(this);
        stopwatch.registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    public void pause() {
        resumed = false;
        settings.unregisterOnSharedPreferenceChangeListener(this);
        timers.unregisterOnSharedPreferenceChangeListener(this);
        stopwatch.unregisterOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences source, String key) {
        if (key == null || (source == settings && KEY.equals(key))
                || (source == stopwatch && "stopwatch".equals(page) && "running".equals(key))
                || (source == timers && (key.equals(page + "_running") || key.equals(page + "_deadline")))) refresh();
    }

    private void refresh() {
        handler.removeCallbacks(atDeadline);
        boolean keep = false;
        if (resumed && settings.getBoolean(KEY, false)) {
            if ("stopwatch".equals(page)) {
                keep = stopwatch.getBoolean("running", false);
            } else if ("pomodoro".equals(page) || "countdown".equals(page)) {
                long remaining = timers.getLong(page + "_deadline", 0) - System.currentTimeMillis();
                keep = timers.getBoolean(page + "_running", false) && remaining > 0;
                if (keep) handler.postDelayed(atDeadline, remaining);
            }
        }
        int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (((host.getWindow().getAttributes().flags & flag) != 0) != keep) {
            if (keep) host.getWindow().addFlags(flag); else host.getWindow().clearFlags(flag);
        }
    }
}
