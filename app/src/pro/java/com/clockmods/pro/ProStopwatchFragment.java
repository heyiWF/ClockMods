package com.clockmods.pro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public final class ProStopwatchFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this, 32L); }
    };
    private SharedPreferences preferences;
    private TextView display;
    private MaterialButton startPause;
    private LinearLayout laps;
    private long accumulated;
    private long startedElapsed;
    private boolean running;
    private int lapCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_stopwatch, container, false);
        preferences = requireContext().getSharedPreferences("pro_stopwatch", Context.MODE_PRIVATE);
        display = root.findViewById(R.id.stopwatch_display);
        startPause = root.findViewById(R.id.stopwatch_start_pause);
        laps = root.findViewById(R.id.stopwatch_laps);
        accumulated = preferences.getLong("accumulated", 0L);
        running = preferences.getBoolean("running", false);
        startedElapsed = running ? SystemClock.elapsedRealtime() : 0L;
        startPause.setOnClickListener(view -> toggle());
        root.findViewById(R.id.stopwatch_reset).setOnClickListener(view -> reset());
        root.findViewById(R.id.stopwatch_lap).setOnClickListener(view -> addLap());
        render();
        return root;
    }

    private void toggle() {
        if (running) {
            accumulated = elapsed();
            running = false;
        } else {
            startedElapsed = SystemClock.elapsedRealtime();
            running = true;
        }
        persist();
        render();
    }

    private void reset() {
        running = false;
        accumulated = 0L;
        lapCount = 0;
        laps.removeAllViews();
        persist();
        render();
    }

    private void addLap() {
        if (!running && accumulated == 0L) return;
        TextView lap = new TextView(requireContext());
        lap.setText(getString(R.string.stopwatch_lap_value, ++lapCount, format(elapsed())));
        lap.setTextSize(15);
        lap.setPadding(12, 6, 12, 6);
        laps.addView(lap, 0);
    }

    private long elapsed() {
        return accumulated + (running ? SystemClock.elapsedRealtime() - startedElapsed : 0L);
    }

    private void render() {
        if (display == null) return;
        display.setText(format(elapsed()));
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
    }

    private String format(long millis) {
        long centiseconds = millis / 10L;
        return String.format(Locale.US, "%02d:%02d.%02d", centiseconds / 6000L,
                (centiseconds / 100L) % 60L, centiseconds % 100L);
    }

    private void persist() {
        preferences.edit().putLong("accumulated", elapsed())
                .putBoolean("running", running).apply();
        if (running) {
            accumulated = elapsed();
            startedElapsed = SystemClock.elapsedRealtime();
        }
    }

    @Override public void onResume() { super.onResume(); handler.post(ticker); }
    @Override public void onPause() { handler.removeCallbacks(ticker); persist(); super.onPause(); }
}