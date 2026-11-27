package com.clockmods.pro.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import com.google.android.material.color.MaterialColors;
import android.view.WindowInsets;
import android.view.WindowManager;
import com.google.android.material.button.MaterialButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.ui.ClockTimeText;

/** The visible controls always address the currently ringing occurrence, including queued alarms. */
public final class AlarmRingingActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private TextView clock, title, phase;
    private long activeId;
    private boolean timer, showingSwipeResult;
    private LinearLayout controls;
    private MaterialButton snooze, dismiss, next;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() {
        if (clock != null && !timer) clock.setText(ClockTimeText.align(android.text.format.DateFormat.getTimeFormat(AlarmRingingActivity.this).format(new java.util.Date())));
        handler.postDelayed(this, 1000);
    }};
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> { if ("ringing_id".equals(key) || AlarmPreferences.SWIPE.equals(key)) refresh(); };
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }
    @Override public void onCreate(Bundle state) {
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state); setShowWhenLocked(true); setTurnScreenOn(true);
        AlarmStore store = new AlarmStore(this);
        if (!store.pendingRings().isEmpty()) androidx.core.content.ContextCompat.startForegroundService(this,
                new Intent(this, AlarmRingingService.class).putExtra(AlarmScheduler.EXTRA_ID, store.pendingRings().keySet().iterator().next()));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, "Alarm"));
        int gap = Math.round(24 * getResources().getDisplayMetrics().density);
        root.setOnApplyWindowInsetsListener((view, insets) -> { android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(gap + bars.left, gap + bars.top, gap + bars.right, gap + bars.bottom); return insets; });
        title = new TextView(this); title.setTextSize(24); title.setGravity(Gravity.CENTER); root.addView(title);
        androidx.appcompat.widget.AppCompatTextView time = new androidx.appcompat.widget.AppCompatTextView(this);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(time, 32, 96, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        time.setSingleLine(true); clock = time; clock.setGravity(Gravity.CENTER);
        LinearLayout face = new LinearLayout(this);
        face.setOrientation(LinearLayout.VERTICAL);
        face.setGravity(Gravity.CENTER);
        phase = new TextView(this);
        phase.setTextSize(24);
        phase.setGravity(Gravity.CENTER);
        face.addView(phase);
        face.addView(clock, new LinearLayout.LayoutParams(-1, -2));
        root.addView(face, new LinearLayout.LayoutParams(-1, 0, 1));
        controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER);
        snooze = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonTonalStyle);
        snooze.setText(R.string.alarm_snooze); snooze.setIconResource(R.drawable.alarm_ic_snooze);
        snooze.setOnClickListener(v -> command(AlarmDelivery.SNOOZE));
        dismiss = new MaterialButton(this); dismiss.setText(R.string.alarm_dismiss);
        dismiss.setOnClickListener(v -> command(AlarmDelivery.STOP));
        next = new MaterialButton(this); next.setText(R.string.pomodoro_next);
        next.setOnClickListener(v -> command(AlarmDelivery.NEXT));
        int width = Math.round(Math.min(520, getResources().getConfiguration().screenWidthDp - 48) * getResources().getDisplayMetrics().density);
        for (MaterialButton button : new MaterialButton[]{snooze, dismiss, next}) {
            button.setTextSize(20); button.setInsetTop(0); button.setInsetBottom(0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, gap * 3, 1); params.setMargins(gap / 6, 0, gap / 6, 0);
            controls.addView(button, params);
        }
        root.addView(controls, new LinearLayout.LayoutParams(width, -2));
        com.clockmods.background.ClockPreferences fonts = new com.clockmods.background.ClockPreferences(this);
        clock.setTypeface(com.clockmods.ui.ClockTypefaceResolver.resolve(this, fonts.getRenderFontFamily(), fonts.isBoldText() ? 700 : 400));
        setContentView(root); root.requestApplyInsets();
        preferences = getSharedPreferences("pro_alarm", MODE_PRIVATE); preferences.registerOnSharedPreferenceChangeListener(listener);
        AlarmPreferences.settings(this).registerOnSharedPreferenceChangeListener(listener);
        refresh(); tick.run();
    }
    private void refresh() {
        if (showingSwipeResult) return;
        AlarmStore store = new AlarmStore(this); activeId = store.ringingId();
        if (activeId == 0 && !store.pendingRings().isEmpty()) activeId = store.pendingRings().keySet().iterator().next();
        AlarmSpec alarm = store.find(activeId);
        if (alarm == null) { finish(); return; }
        controls.removeAllViews();
        timer = alarm.isTimer();
        title.setText(timer ? getString(alarm.countdown ? R.string.pro_page_countdown : R.string.pro_page_pomodoro)
                : AlarmDelivery.title(this, alarm));
        if (timer && alarm.countdown && !alarm.timerLabel.isEmpty()) title.setText(alarm.timerLabel);
        phase.setVisibility(timer && !alarm.countdown && alarm.timerDuration > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        if (timer) {
            phase.setText(AlarmDelivery.phaseLabel(alarm));
            clock.setText(alarm.timerDuration > 0 ? ClockTimeText.align(AlarmDelivery.duration(alarm)) : alarm.label);
        }
        dismiss.setText(AlarmDelivery.stopLabel(alarm));
        if (timer) {
            controls.addView(dismiss);
            if (!alarm.countdown && alarm.completedDeadline != 0) controls.addView(next);
            return;
        }
        boolean swapped = AlarmPreferences.swapped(this, activeId);
        if (AlarmPreferences.enabled(this, AlarmPreferences.SWIPE)) {
            long occurrenceId = activeId;
            controls.addView(new AlarmSwipeView(this, swapped,
                    MaterialColors.getColor(controls, androidx.appcompat.R.attr.colorPrimary),
                    MaterialColors.getColor(controls, com.google.android.material.R.attr.colorOnPrimary),
                    MaterialColors.getColor(controls, com.google.android.material.R.attr.colorOnSurface),
                    alarm.snoozeMinutes, action -> {
                        showingSwipeResult = true;
                        startService(AlarmDelivery.command(this, occurrenceId, action));
                    }, () -> { showingSwipeResult = false; refresh(); }),
                    new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        controls.addView(swapped ? dismiss : snooze); controls.addView(swapped ? snooze : dismiss);
    }
    private boolean volumeSnooze(int key) {
        return !showingSwipeResult && !timer && (key == android.view.KeyEvent.KEYCODE_VOLUME_UP || key == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
                || key == android.view.KeyEvent.KEYCODE_VOLUME_MUTE) && AlarmPreferences.enabled(this, AlarmPreferences.VOLUME);
    }
    @Override public boolean onKeyDown(int key, android.view.KeyEvent event) {
        if (volumeSnooze(key)) { if (event.getRepeatCount() == 0) command(AlarmDelivery.SNOOZE); return true; }
        return super.onKeyDown(key, event);
    }
    @Override public boolean onKeyUp(int key, android.view.KeyEvent event) {
        return volumeSnooze(key) || super.onKeyUp(key, event);
    }
    private void command(String action) { startService(AlarmDelivery.command(this, activeId, action)); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); refresh(); }
    @Override protected void onDestroy() {
        AlarmPreferences.settings(this).unregisterOnSharedPreferenceChangeListener(listener);
        handler.removeCallbacks(tick); if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy();
    }
}
