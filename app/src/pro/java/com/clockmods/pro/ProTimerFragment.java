package com.clockmods.pro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.clockmods.pro.timer.TimerScheduler;

import java.util.Locale;

public final class ProTimerFragment extends Fragment {
    private static final String ARG_MODE = "mode";
    private static final String MODE_POMODORO = "pomodoro";
    private static final String MODE_COUNTDOWN = "countdown";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 250L); }
    };

    private SharedPreferences preferences;
    private TextView display;
    private TextView phase;
    private MaterialButton startPause;
    private boolean pomodoro;
    private long durationMillis;
    private long remainingMillis;
    private long deadlineMillis;
    private boolean running;
    private int pomodoroPhase;

    static ProTimerFragment newPomodoroInstance() { return newInstance(MODE_POMODORO); }
    static ProTimerFragment newCountdownInstance() { return newInstance(MODE_COUNTDOWN); }

    private static ProTimerFragment newInstance(String mode) {
        ProTimerFragment fragment = new ProTimerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_timer, container, false);
        pomodoro = MODE_POMODORO.equals(requireArguments().getString(ARG_MODE));
        preferences = requireContext().getSharedPreferences("pro_timers", Context.MODE_PRIVATE);
        display = root.findViewById(R.id.timer_display);
        phase = root.findViewById(R.id.timer_phase);
        startPause = root.findViewById(R.id.timer_start_pause);
        ((TextView) root.findViewById(R.id.timer_title)).setText(
                pomodoro ? R.string.pro_page_pomodoro : R.string.pro_page_countdown);
        root.findViewById(R.id.timer_reset).setOnClickListener(view -> reset());
        startPause.setOnClickListener(view -> toggle());
        root.findViewById(R.id.timer_skip).setVisibility(pomodoro ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.timer_skip).setOnClickListener(view -> advancePomodoro());
        setupPresets(root.findViewById(R.id.timer_presets));
        restore();
        refresh();
        return root;
    }

    private void setupPresets(LinearLayout container) {
        if (pomodoro) {
            addPresetRow(container, new int[] {5, 15, 25}, 12);
            return;
        }
        addPresetGroup(container, new int[] {5, 10}, false);
        addPresetGroup(container, new int[] {30, 60}, true);
    }

    private void addPresetRow(LinearLayout container, int[] minutes, int spacingDp) {
        int spacing = dp(spacingDp);
        for (int index = 0; index < minutes.length; index++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (index > 0) params.setMarginStart(spacing);
            container.addView(createPresetButton(minutes[index]), params);
        }
    }

    private void addPresetGroup(LinearLayout container, int[] minutes, boolean addStartMargin) {
        LinearLayout group = new LinearLayout(requireContext());
        group.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (addStartMargin) groupParams.setMarginStart(dp(12));
        container.addView(group, groupParams);
        addPresetRow(group, minutes, 8);
    }

    private MaterialButton createPresetButton(int minutes) {
        MaterialButton button = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(getString(R.string.timer_minutes, minutes));
        button.setOnClickListener(view -> setDuration(minutes * 60_000L));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setDuration(long duration) {
        running = false;
        durationMillis = duration;
        remainingMillis = duration;
        persist();
        refresh();
    }

    private void toggle() {
        if (running) {
            remainingMillis = Math.max(0L, deadlineMillis - System.currentTimeMillis());
            running = false;
            TimerScheduler.cancel(requireContext(), prefixMode());
        } else if (remainingMillis > 0L) {
            deadlineMillis = System.currentTimeMillis() + remainingMillis;
            running = true;
            TimerScheduler.schedule(requireContext(), prefixMode(), deadlineMillis);
        }
        persist();
        refresh();
    }

    private void reset() {
        running = false;
        TimerScheduler.cancel(requireContext(), prefixMode());
        if (pomodoro) {
            pomodoroPhase = 0;
            durationMillis = 25 * 60_000L;
        }
        remainingMillis = durationMillis;
        persist();
        refresh();
    }

    private void advancePomodoro() {
        pomodoroPhase = (pomodoroPhase + 1) % 3;
        durationMillis = pomodoroPhase == 0 ? 25 * 60_000L
                : pomodoroPhase == 1 ? 5 * 60_000L : 15 * 60_000L;
        remainingMillis = durationMillis;
        running = false;
        TimerScheduler.cancel(requireContext(), prefixMode());
        persist();
        refresh();
    }

    private void refresh() {
        if (display == null) return;
        if (running) {
            remainingMillis = Math.max(0L, deadlineMillis - System.currentTimeMillis());
            if (remainingMillis == 0L) {
                running = false;
                if (pomodoro) advancePomodoro();
                else persist();
            }
        }
        long totalSeconds = (remainingMillis + 999L) / 1000L;
        display.setText(String.format(Locale.US, "%02d:%02d", totalSeconds / 60,
                totalSeconds % 60));
        phase.setText(pomodoro ? phaseLabel() : getString(R.string.timer_ready));
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
    }

    private String phaseLabel() {
        return getString(pomodoroPhase == 0 ? R.string.pomodoro_focus
                : pomodoroPhase == 1 ? R.string.pomodoro_short_break
                : R.string.pomodoro_long_break);
    }

    private String prefix() { return pomodoro ? "pomodoro_" : "countdown_"; }
    private String prefixMode() { return pomodoro ? "pomodoro" : "countdown"; }

    private void restore() {
        durationMillis = preferences.getLong(prefix() + "duration",
                pomodoro ? 25 * 60_000L : 10 * 60_000L);
        remainingMillis = preferences.getLong(prefix() + "remaining", durationMillis);
        deadlineMillis = preferences.getLong(prefix() + "deadline", 0L);
        running = preferences.getBoolean(prefix() + "running", false);
        pomodoroPhase = preferences.getInt(prefix() + "phase", 0);
    }

    private void persist() {
        preferences.edit().putLong(prefix() + "duration", durationMillis)
                .putLong(prefix() + "remaining", remainingMillis)
                .putLong(prefix() + "deadline", deadlineMillis)
                .putBoolean(prefix() + "running", running)
                .putInt(prefix() + "phase", pomodoroPhase).apply();
    }

    @Override public void onResume() { super.onResume(); handler.post(ticker); }
    @Override public void onPause() { handler.removeCallbacks(ticker); persist(); super.onPause(); }
}