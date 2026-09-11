package com.clockmods.ultimate;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

/** Applies a slow, bounded two-dimensional translation to the shared page host. */
public final class AntiBurnController {
    private static final long FRAME_INTERVAL_MILLIS = 1000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View target;
    private final View dimOverlay;
    private final AntiBurnPreferences preferences;
    private final Runnable frame = this::applyFrame;
    private boolean running;

    public AntiBurnController(View target, View dimOverlay, AntiBurnPreferences preferences) {
        if (target == null || preferences == null) {
            throw new IllegalArgumentException("target and preferences are required");
        }
        this.target = target;
        this.dimOverlay = dimOverlay;
        this.preferences = preferences;
    }

    public void start() {
        if (running) return;
        running = true;
        cancelFrame();
        frame.run();
    }

    public void stop() {
        running = false;
        cancelFrame();
        target.setTranslationX(0f);
        target.setTranslationY(0f);
        target.setScaleX(1f);
        target.setScaleY(1f);
        if (dimOverlay != null) dimOverlay.setAlpha(0f);
    }

    public void refresh() {
        if (!running) return;
        cancelFrame();
        applyFrame();
    }

    private void cancelFrame() {
        handler.removeCallbacks(frame);
        target.removeCallbacks(frame);
    }

    private void applyFrame() {
        if (!running) return;
        if (!preferences.isEnabled()) {
            target.setTranslationX(0f);
            target.setTranslationY(0f);
            target.setScaleX(1f);
            target.setScaleY(1f);
            if (dimOverlay != null) dimOverlay.setAlpha(0f);
            handler.postDelayed(frame, 1000L);
            return;
        }
        long periodMs = preferences.getPeriodMinutes() * 60_000L;
        float progress = (android.os.SystemClock.elapsedRealtime() % periodMs)
                / (float) periodMs;
        double angle = progress * Math.PI * 2d;
        float amplitude = preferences.getAmplitudeDp()
                * target.getResources().getDisplayMetrics().density;
        target.setTranslationX((float) Math.sin(angle) * amplitude);
        target.setTranslationY((float) Math.sin(angle * 2d + Math.PI / 3d)
                * amplitude * .72f);
        if (target.getWidth() > 0 && target.getHeight() > 0 && amplitude > 0f) {
            target.setScaleX(1f + amplitude * 2f / target.getWidth());
            target.setScaleY(1f + amplitude * 2f / target.getHeight());
        }
        if (dimOverlay != null) dimOverlay.setAlpha(preferences.isAutoDim() ? .14f : 0f);
        // The drift is deliberately imperceptible. A one-second update preserves that motion
        // while avoiding a permanent 60 fps render loop on an always-on display.
        handler.postDelayed(frame, FRAME_INTERVAL_MILLIS);
    }

    public void setEnabled(boolean enabled) {
        preferences.setEnabled(enabled);
        refresh();
    }
}
