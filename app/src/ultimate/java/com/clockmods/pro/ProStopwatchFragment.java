package com.clockmods.pro;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
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
        @Override public void run() {
            if (running) {
                renderTime();
                handler.postDelayed(this, 32L);
            }
        }
    };
    private SharedPreferences preferences;
    private TextView display;
    private MaterialButton startPause;
    private LinearLayout laps;
    private ScrollView lapScroll;
    private ValueAnimator lapScrollAnimator;
    private long accumulated;
    private long startedElapsed;
    private boolean running;
    private long startedWall;
    private final ArrayList<Long> lapTimes = new ArrayList<>();
    private View records;
    private ToolWorkspaceLayout workspace;
    private MaterialButton lapButton, resetButton;
    private ValueAnimator controlsAnimator;
    private float sideVisibility;
    private Boolean showSideControls;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_stopwatch, container, false);
        preferences = requireContext().getSharedPreferences("pro_stopwatch", Context.MODE_PRIVATE);
        display = root.findViewById(R.id.stopwatch_display);
        startPause = root.findViewById(R.id.stopwatch_start_pause);
        laps = root.findViewById(R.id.stopwatch_laps);
        lapScroll = (ScrollView) laps.getParent();
        workspace = (ToolWorkspaceLayout) root;
        records = root.findViewById(R.id.stopwatch_records);
        lapButton = root.findViewById(R.id.stopwatch_lap);
        resetButton = root.findViewById(R.id.stopwatch_reset);
        ToolButtonMotion.install(startPause); ToolButtonMotion.install(resetButton); ToolButtonMotion.install(lapButton);
        showSideControls = null;
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
        handler.removeCallbacks(ticker);
        if (running) handler.postDelayed(ticker, 32L);
    }

    private void reset() {
        if (lapScrollAnimator != null) lapScrollAnimator.cancel();
        laps.setTranslationY(0);
        running = false;
        accumulated = 0L;
        lapTimes.clear();
        renderLaps();
        persist();
        render();
    }

    private void addLap() {
        if (!running && accumulated == 0L) return;
        boolean firstLap = lapTimes.isEmpty();
        boolean animate = workspace.isLaidOut() && ValueAnimator.areAnimatorsEnabled();
        if (firstLap && animate) {
            androidx.transition.ChangeBounds expand = new androidx.transition.ChangeBounds();
            expand.excludeChildren(lapScroll, true);
            androidx.transition.TransitionManager.beginDelayedTransition(workspace, expand.setDuration(280));
        }
        lapTimes.add(0, elapsed());
        persist();
        records.setVisibility(View.VISIBLE);
        workspace.setExpandedDetails(true);
        if (lapScrollAnimator != null) lapScrollAnimator.cancel();
        View entry = createLapRow(0);
        laps.addView(entry, 0);
        if (animate && !firstLap) {
            OneShotPreDrawListener.add(entry, () -> revealLap(entry));
        } else {
            setLapOffset(0);
        }
    }

    private void revealLap(View entry) {
        if (entry.getParent() != laps) return;
        if (lapScrollAnimator != null) lapScrollAnimator.cancel();
        // Keep the old rows at their current screen positions across the prepend.
        int offset = lapScroll.getScrollY() - Math.round(laps.getTranslationY()) + entry.getHeight();
        setLapOffset(offset);
        lapScrollAnimator = ValueAnimator.ofInt(offset, 0);
        lapScrollAnimator.setDuration(200);
        lapScrollAnimator.setInterpolator(new DecelerateInterpolator());
        lapScrollAnimator.addUpdateListener(value -> setLapOffset((Integer) value.getAnimatedValue()));
        lapScrollAnimator.start();
    }

    private void setLapOffset(int offset) {
        lapScroll.scrollTo(0, offset);
        // A short list has insufficient scroll range. Translate only the remaining distance.
        laps.setTranslationY(lapScroll.getScrollY() - offset);
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

    private void renderTime() {
        display.setText(ClockTimeText.align(format(elapsed())));
    }

    private void render() {
        renderTime();
        ToolButtonMotion.running(startPause, running);
        startPause.setText(running ? R.string.timer_pause : R.string.timer_start);
        startPause.setContentDescription(startPause.getText());
        startPause.setIconResource(running ? R.drawable.tool_ic_pause : R.drawable.tool_ic_play);
        boolean show = running || accumulated > 0;
        if (showSideControls != null && showSideControls == show) return;
        boolean animate = showSideControls != null && startPause.isLaidOut() && ValueAnimator.areAnimatorsEnabled();
        showSideControls = show;
        if (controlsAnimator != null) controlsAnimator.cancel();
        if (animate) {
            controlsAnimator = ValueAnimator.ofFloat(sideVisibility, show ? 1f : 0f);
            controlsAnimator.setDuration(240);
            controlsAnimator.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
            controlsAnimator.addUpdateListener(frame -> layoutSideControls((float) frame.getAnimatedValue()));
            controlsAnimator.start();
        } else layoutSideControls(show ? 1f : 0f);
    }

    private void layoutSideControls(float fraction) {
        sideVisibility = fraction;
        layoutSideControl(resetButton, fraction);
        layoutSideControl(lapButton, fraction);
    }

    private void layoutSideControl(MaterialButton button, float fraction) {
        android.widget.FrameLayout slot = (android.widget.FrameLayout) button.getParent();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) slot.getLayoutParams();
        params.width = Math.round(dp(72) * fraction);
        slot.setLayoutParams(params);
        slot.setVisibility(fraction == 0f ? View.GONE : View.VISIBLE);
        button.setAlpha(fraction);
        button.setEnabled(Boolean.TRUE.equals(showSideControls));
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
    @Override public void onDestroyView() { if (lapScrollAnimator != null) lapScrollAnimator.cancel(); if (controlsAnimator != null) controlsAnimator.cancel(); display = null; super.onDestroyView(); }
}
