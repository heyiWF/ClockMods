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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.clockmods.ui.ButtonTextSizer;
import com.clockmods.ui.ClockTimeText;
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
    private RollingTimeText rollingTime;
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
        rollingTime = new RollingTimeText(display);
        phase = root.findViewById(R.id.timer_phase);
        phase.setVisibility(pomodoro ? View.VISIBLE : View.GONE);
        startPause = root.findViewById(R.id.timer_start_pause);
        ToolButtonMotion.install(startPause);
        ToolButtonMotion.install(root.findViewById(R.id.timer_reset));
        ToolButtonMotion.install(root.findViewById(R.id.timer_skip));
        ((TextView) root.findViewById(R.id.timer_title)).setText(
                pomodoro ? R.string.pro_page_pomodoro : R.string.pro_page_countdown);
        root.findViewById(R.id.timer_reset).setOnClickListener(view -> reset());
        startPause.setOnClickListener(view -> toggle());
        root.findViewById(R.id.timer_skip).setVisibility(pomodoro ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.timer_skip).setOnClickListener(view -> advancePomodoro());
        setupPresets(root.findViewById(R.id.timer_presets));
        ProFontApplier.apply(root);
        ButtonTextSizer.applyToTree(root);
        restore();
        refresh();
        return root;
    }

    private void setupPresets(ChipGroup container) {
        int[] minutes = pomodoro ? new int[] {5, 15, 25} : new int[] {5, 10, 30, 60};
        for (int value : minutes) {
            Chip chip = new Chip(requireContext());
            chip.setText(getString(R.string.timer_minutes, value));
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnClickListener(view -> setDuration(value * 60_000L));
            container.addView(chip);
        }
        if (!pomodoro) {
            Chip custom = new Chip(requireContext());
            custom.setText(R.string.timer_custom);
            custom.setEnsureMinTouchTargetSize(true);
            custom.setOnClickListener(view -> showCustomDurationDialog());
            container.addView(custom);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

        private void showCustomDurationDialog() {
        long totalSeconds = Math.min(durationMillis / 1000L, 99L * 3600L + 59L * 60L + 59L);
        NumberPicker hours = createPicker(0, 99, (int) (totalSeconds / 3600L),
            R.string.timer_hours);
        NumberPicker minutes = createPicker(0, 59, (int) ((totalSeconds / 60L) % 60L),
            R.string.timer_minutes_unit);
        NumberPicker seconds = createPicker(0, 59, (int) (totalSeconds % 60L),
            R.string.timer_seconds);

        LinearLayout pickers = new LinearLayout(requireContext());
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setPadding(dp(16), 0, dp(16), 0);
        pickers.addView(hours, pickerParams());
        pickers.addView(minutes, pickerParams());
        pickers.addView(seconds, pickerParams());

        AlertDialog dialog = new com.clockmods.ultimate.navigation.PredictiveAlertDialogBuilder(requireContext())
            .setTitle(R.string.timer_custom_title)
            .setView(pickers)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.apply, null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(view -> {
                long duration = customDurationMillis(hours.getValue(), minutes.getValue(),
                    seconds.getValue());
                if (duration == 0L) {
                Toast.makeText(requireContext(), R.string.timer_custom_zero,
                    Toast.LENGTH_SHORT).show();
                return;
                }
                setDuration(duration);
                dialog.dismiss();
            }));
        dialog.show();
        ProFontApplier.apply(dialog.getWindow().getDecorView());
        ButtonTextSizer.applyToTree(dialog.getWindow().getDecorView());
        }

        static long customDurationMillis(int hours, int minutes, int seconds) {
        if (hours < 0 || hours > 99 || minutes < 0 || minutes > 59
            || seconds < 0 || seconds > 59) return 0L;
        return (hours * 3600L + minutes * 60L + seconds) * 1000L;
        }

        private NumberPicker createPicker(int min, int max, int value, int descriptionRes) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setWrapSelectorWheel(false);
        picker.setContentDescription(getString(descriptionRes));
        picker.setFormatter(number -> String.format(Locale.US, "%02d", number));
        return picker;
        }

        private LinearLayout.LayoutParams pickerParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        }

    private void setDuration(long duration) {
        TimerScheduler.cancel(requireContext(), prefixMode());
        deadlineMillis = 0L;
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
        if (pomodoro) {
            rollingTime.set(String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60), running);
        } else {
            rollingTime.set(String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds / 60) % 60, totalSeconds % 60), running);
        }
        if (pomodoro) phase.setText(phaseLabel());
        ToolButtonMotion.running(startPause, running);
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
        startPause.setIconResource(running ? R.drawable.tool_ic_pause : R.drawable.tool_ic_play);
        startPause.setContentDescription(startPause.getText());
        startPause.setEnabled(remainingMillis > 0);
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

    @Override public void onDestroyView() { rollingTime.dispose(); display = null; super.onDestroyView(); }

    @Override public void onResume() { super.onResume(); handler.post(ticker); }
    @Override public void onPause() { handler.removeCallbacks(ticker); persist(); super.onPause(); }
}
