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
import com.clockmods.ui.ButtonTextSizer;
import com.clockmods.ui.ClockTimeText;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.ArrayList;
import android.view.Gravity;

public final class ProStopwatchFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this, 32L); }
    };
    private SharedPreferences preferences;
    private TextView display;
    private MaterialButton startPause;
    private LinearLayout laps;
    private android.widget.ScrollView lapScroll;
    private android.animation.ValueAnimator lapScrollAnimator;
    private long accumulated;
    private long startedElapsed;
    private boolean running;
    private long startedWall;
    private final ArrayList<Long> lapTimes = new ArrayList<>();
    private View records;
    private ToolWorkspaceLayout workspace;
    private MaterialButton lapButton, resetButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_stopwatch, container, false);
        preferences = requireContext().getSharedPreferences("pro_stopwatch", Context.MODE_PRIVATE);
        display = root.findViewById(R.id.stopwatch_display);
        startPause = root.findViewById(R.id.stopwatch_start_pause);
        laps = root.findViewById(R.id.stopwatch_laps);
        lapScroll = (android.widget.ScrollView) laps.getParent();
        workspace = (ToolWorkspaceLayout) root;
        records = root.findViewById(R.id.stopwatch_records);
        lapButton = root.findViewById(R.id.stopwatch_lap);
        resetButton = root.findViewById(R.id.stopwatch_reset);
        ToolButtonMotion.install(startPause); ToolButtonMotion.install(resetButton); ToolButtonMotion.install(lapButton);
        accumulated = preferences.getLong("accumulated", 0L);
        running = preferences.getBoolean("running", false);
        startedElapsed = preferences.getLong("started_at_realtime", SystemClock.elapsedRealtime());
        startedWall = preferences.getLong("started_at_wall", System.currentTimeMillis());
        lapTimes.clear();
        for (String value : preferences.getString("laps", "").split(",")) {
            try { lapTimes.add(Long.parseLong(value)); } catch (NumberFormatException ignored) { }
        }
        startPause.setOnClickListener(view -> toggle());
        root.findViewById(R.id.stopwatch_reset).setOnClickListener(view -> reset());
        root.findViewById(R.id.stopwatch_lap).setOnClickListener(view -> addLap());
        ProFontApplier.apply(root);
        ButtonTextSizer.applyToTree(root);
        renderLaps();
        render();
        return root;
    }

    private void toggle() {
        if (running) {
            accumulated = elapsed();
            running = false;
        } else {
            startedElapsed = SystemClock.elapsedRealtime();
            startedWall = System.currentTimeMillis();
            running = true;
        }
        persist();
        render();
    }

    private void reset() {
        if (lapScrollAnimator != null) lapScrollAnimator.cancel();
        running = false;
        accumulated = 0L;
        lapTimes.clear();
        renderLaps();
        persist();
        render();
    }

    private void addLap() {
        if (!running && accumulated == 0L) return;
        if (workspace.isLaidOut() && android.animation.ValueAnimator.areAnimatorsEnabled())
            androidx.transition.TransitionManager.beginDelayedTransition(workspace,
                    new androidx.transition.ChangeBounds().setDuration(280));
        lapTimes.add(0, elapsed());
        persist();
        records.setVisibility(View.VISIBLE);
        workspace.setExpandedDetails(true);
        laps.addView(createLapRow(0), 0);
        View entry = laps.getChildAt(0);
        if (entry != null && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            entry.setTranslationY(-dp(24)); entry.setAlpha(0);
            entry.animate().translationY(0).alpha(1).setDuration(260).start();
        }
        if (lapScrollAnimator != null) lapScrollAnimator.cancel();
        // Start from the current position even if a previous insertion is still scrolling.
        if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
            lapScrollAnimator = android.animation.ValueAnimator.ofInt(lapScroll.getScrollY(), 0);
            lapScrollAnimator.setDuration(280);
            lapScrollAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            lapScrollAnimator.addUpdateListener(value -> lapScroll.scrollTo(0, (Integer) value.getAnimatedValue()));
            lapScrollAnimator.start();
        } else lapScroll.scrollTo(0, 0);
    }

    private void renderLaps() {
        laps.removeAllViews();
        records.setVisibility(lapTimes.isEmpty() ? View.GONE : View.VISIBLE);
        workspace.setExpandedDetails(!lapTimes.isEmpty());
        for (int index = 0; index < lapTimes.size(); index++) {
            laps.addView(createLapRow(index));
        }
    }

    private View createLapRow(int index) {
        long total = lapTimes.get(index);
        long previous = index + 1 < lapTimes.size() ? lapTimes.get(index + 1) : 0;
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.addView(lapCell(Integer.toString(lapTimes.size() - index), false),
                new LinearLayout.LayoutParams(dp(48), -2));
        row.addView(lapCell(format(Math.max(0, total - previous)), true),
                new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(lapCell(format(total), true), new LinearLayout.LayoutParams(0, -2, 1));
        ProFontApplier.apply(row);
        return row;
    }

    private TextView lapCell(String value, boolean time) {
        androidx.appcompat.widget.AppCompatTextView cell = new androidx.appcompat.widget.AppCompatTextView(requireContext());
        cell.setText(ClockTimeText.align(value));
        cell.setTextSize(16);
        cell.setSingleLine(true);
        cell.setGravity(time ? Gravity.END : Gravity.START);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(cell, 10, 16, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        return cell;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private long elapsed() {
        return StopwatchTimebase.elapsed(accumulated, running, startedElapsed, startedWall,
                SystemClock.elapsedRealtime(), System.currentTimeMillis());
    }

    private void render() {
        if (display == null) return;
        display.setText(ClockTimeText.align(format(elapsed())));
        ToolButtonMotion.running(startPause, running);
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
        startPause.setContentDescription(startPause.getText());
        startPause.setIconResource(running ? R.drawable.tool_ic_pause : R.drawable.tool_ic_play);
        int visibility = running || accumulated > 0 ? View.VISIBLE : View.GONE;
        if (lapButton.getVisibility() != visibility && startPause.isLaidOut())
            androidx.transition.TransitionManager.beginDelayedTransition((ViewGroup) startPause.getParent(), new androidx.transition.AutoTransition().setDuration(300));
        lapButton.setVisibility(visibility);
        resetButton.setVisibility(running || accumulated > 0 ? View.VISIBLE : View.GONE);
    }

    private String format(long millis) {
        long centiseconds = millis / 10L;
        return String.format(Locale.US, "%02d:%02d.%02d", centiseconds / 6000L,
                (centiseconds / 100L) % 60L, centiseconds % 100L);
    }

    private void persist() {
        if (preferences == null) return;
        StringBuilder savedLaps = new StringBuilder();
        for (long lap : lapTimes) { if (savedLaps.length() > 0) savedLaps.append(','); savedLaps.append(lap); }
        preferences.edit().putLong("accumulated", accumulated)
                .putLong("started_at_realtime", running ? startedElapsed : 0)
                .putLong("started_at_wall", running ? startedWall : 0)
                .putString("laps", savedLaps.toString()).putBoolean("running", running).apply();
    }

    @Override public void onResume() { super.onResume(); handler.post(ticker); }
    @Override public void onPause() { handler.removeCallbacks(ticker); persist(); super.onPause(); }
    @Override public void onDestroyView() { if (lapScrollAnimator != null) lapScrollAnimator.cancel(); display = null; super.onDestroyView(); }
}
