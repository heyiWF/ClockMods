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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.google.android.material.button.MaterialButton;
import com.clockmods.ui.ButtonTextSizer;
import com.clockmods.pro.timer.TimerScheduler;
import com.clockmods.pro.timer.PomodoroSequence;
import com.clockmods.pro.timer.SequenceEditorView;

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
    private CountdownPage countdown;
    private long durationMillis;
    private long remainingMillis;
    private long deadlineMillis;
    private boolean running;
    private int pomodoroPhase;
    private java.util.List<PomodoroSequence.Step> sequence;
    private AlertDialog sequenceDialog;
    private SequenceEditorView sequenceEditor;

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
        if (MODE_COUNTDOWN.equals(requireArguments().getString(ARG_MODE))) {
            countdown = new CountdownPage(this);
            return countdown;
        }
        View root = inflater.inflate(R.layout.fragment_pro_timer, container, false);
        preferences = requireContext().getSharedPreferences("pro_timers", Context.MODE_PRIVATE);
        display = root.findViewById(R.id.timer_display);
        rollingTime = new RollingTimeText(display);
        phase = root.findViewById(R.id.timer_phase);
        startPause = root.findViewById(R.id.timer_start_pause);
        ToolButtonMotion.install(startPause);
        ToolButtonMotion.install(root.findViewById(R.id.timer_reset));
        ToolButtonMotion.install(root.findViewById(R.id.timer_skip));
        ((TextView) root.findViewById(R.id.timer_title)).setText(
                R.string.pro_page_pomodoro);
        root.findViewById(R.id.timer_reset).setOnClickListener(view -> reset());
        startPause.setOnClickListener(view -> toggle());
        root.findViewById(R.id.timer_skip).setOnClickListener(view -> advancePomodoro());
        setupPresets(root.findViewById(R.id.timer_presets));
        ProFontApplier.apply(root);
        ButtonTextSizer.applyToTree(root);
        restore();
        refresh();
        if (savedInstanceState != null && savedInstanceState.containsKey("sequence_draft"))
            root.post(() -> { if (isAdded() && display != null) editSequence(savedInstanceState.getString("sequence_draft")); });
        return root;
    }

    private void setupPresets(ChipGroup container) {
        Chip edit = new Chip(requireContext());
        edit.setText(R.string.pomodoro_edit_sequence);
        edit.setOnClickListener(v -> editSequence(PomodoroSequence.encode(sequence)));
        container.addView(edit);
    }

    private int[] materialColors(int... attributes) {
        int[] colors = new int[attributes.length];
        for (int i = 0; i < attributes.length; i++)
            colors[i] = com.google.android.material.color.MaterialColors.getColor(requireView(), attributes[i]);
        return colors;
    }

    private void editSequence(String draft) {
        int[] fills = materialColors(com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorSecondaryContainer, com.google.android.material.R.attr.colorTertiaryContainer);
        int[] inks = materialColors(com.google.android.material.R.attr.colorOnPrimaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer, com.google.android.material.R.attr.colorOnTertiaryContainer);
        sequenceEditor = new SequenceEditorView(requireContext(), draft, fills, inks, ignored -> {},
                () -> sequenceDialog.cancel(), steps -> {
                    PomodoroSequence.apply(requireContext(), steps);
                    restore();
                    refresh();
                    sequenceDialog.dismiss();
                });
        android.widget.FrameLayout content = new android.widget.FrameLayout(requireContext());
        content.setPadding(dp(20), dp(12), dp(20), dp(20));
        content.addView(sequenceEditor, new android.widget.FrameLayout.LayoutParams(-1, -2));
        sequenceDialog = new com.clockmods.ultimate.navigation.PredictiveAlertDialogBuilder(requireContext())
                .setTitle(R.string.pomodoro_edit_sequence).setView(content).create();
        sequenceDialog.setOnDismissListener(dialog -> { sequenceDialog = null; sequenceEditor = null; });
        sequenceDialog.show();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (sequenceEditor != null) state.putString("sequence_draft", PomodoroSequence.encode(sequenceEditor.steps()));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toggle() {
        if (running) {
            remainingMillis = Math.max(0L, deadlineMillis - System.currentTimeMillis());
            running = false;
            TimerScheduler.cancel(requireContext(), MODE_POMODORO);
        } else if (remainingMillis > 0L) {
            deadlineMillis = System.currentTimeMillis() + remainingMillis;
            running = true;
            TimerScheduler.schedule(requireContext(), MODE_POMODORO, deadlineMillis);
        }
        persist();
        refresh();
    }

    private void reset() {
        running = false;
        TimerScheduler.cancel(requireContext(), MODE_POMODORO);
        pomodoroPhase = 0;
        durationMillis = sequence.get(0).duration();
        remainingMillis = durationMillis;
        persist();
        refresh();
    }

    private void advancePomodoro() {
        pomodoroPhase = (pomodoroPhase + 1) % sequence.size();
        durationMillis = sequence.get(pomodoroPhase).duration();
        remainingMillis = durationMillis;
        running = false;
        TimerScheduler.cancel(requireContext(), MODE_POMODORO);
        persist();
        refresh();
    }

    private void refresh() {
        if (display == null) return;
        if (running != preferences.getBoolean("pomodoro_running", false)
                || deadlineMillis != preferences.getLong("pomodoro_deadline", 0L)) restore();
        if (running) {
            remainingMillis = Math.max(0L, deadlineMillis - System.currentTimeMillis());
            if (remainingMillis == 0L) {
                com.clockmods.pro.timer.TimerReceiver.complete(requireContext(), MODE_POMODORO);
                restore();
            }
        }
        long totalSeconds = (remainingMillis + 999L) / 1000L;
        rollingTime.set(String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60), running);
        phase.setText(phaseLabel());
        ToolButtonMotion.running(startPause, running);
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
        startPause.setIconResource(running ? R.drawable.tool_ic_pause : R.drawable.tool_ic_play);
        startPause.setContentDescription(startPause.getText());
        startPause.setEnabled(remainingMillis > 0);
    }

    private String phaseLabel() {
        return getString(com.clockmods.pro.timer.TimerCompletion.phaseLabel(sequence.get(pomodoroPhase).label));
    }

    private void restore() {
        sequence = PomodoroSequence.load(preferences);
        pomodoroPhase = PomodoroSequence.index(preferences, sequence);
        durationMillis = preferences.getLong("pomodoro_duration",
                sequence.get(pomodoroPhase).duration());
        remainingMillis = preferences.getLong("pomodoro_remaining", durationMillis);
        deadlineMillis = preferences.getLong("pomodoro_deadline", 0L);
        running = preferences.getBoolean("pomodoro_running", false);
    }

    private void persist() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(com.clockmods.pro.timer.TimerCompletion.PENDING_PHASE);
        editor.putLong("pomodoro_duration", durationMillis)
                .putLong("pomodoro_remaining", remainingMillis)
                .putLong("pomodoro_deadline", deadlineMillis)
                .putBoolean("pomodoro_running", running)
                .putInt("pomodoro_phase", pomodoroPhase).apply();
    }

    @Override public void onDestroyView() {
        if (sequenceDialog != null) sequenceDialog.dismiss();
        if (rollingTime != null) rollingTime.dispose();
        countdown = null;
        display = null;
        super.onDestroyView();
    }
    @Override public void onResume() {
        super.onResume();
        if (countdown != null) countdown.resume();
        else { restore(); handler.post(ticker); }
    }
    @Override public void onPause() {
        if (countdown != null) countdown.pause();
        handler.removeCallbacks(ticker);
        super.onPause();
    }
}
