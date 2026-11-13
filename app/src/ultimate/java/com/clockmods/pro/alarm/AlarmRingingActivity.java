package com.clockmods.pro.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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
    private TextView clock, title;
    private long activeId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() {
        if (clock != null) clock.setText(ClockTimeText.align(android.text.format.DateFormat.getTimeFormat(AlarmRingingActivity.this).format(new java.util.Date())));
        handler.postDelayed(this, 1000);
    }};
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> { if ("ringing_id".equals(key)) refresh(); };
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
        root.addView(clock, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER);
        MaterialButton snooze = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonTonalStyle);
        snooze.setText(R.string.alarm_snooze); snooze.setIconResource(R.drawable.alarm_ic_snooze);
        snooze.setOnClickListener(v -> command(AlarmDelivery.SNOOZE));
        MaterialButton dismiss = new MaterialButton(this); dismiss.setText(R.string.alarm_dismiss);
        dismiss.setOnClickListener(v -> command(AlarmDelivery.STOP));
        int width = Math.round(Math.min(520, getResources().getConfiguration().screenWidthDp - 48) * getResources().getDisplayMetrics().density);
        for (MaterialButton button : new MaterialButton[]{snooze, dismiss}) {
            button.setTextSize(20); button.setInsetTop(0); button.setInsetBottom(0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, gap * 3, 1); params.setMargins(gap / 6, 0, gap / 6, 0);
            controls.addView(button, params);
        }
        root.addView(controls, new LinearLayout.LayoutParams(width, -2));
        com.clockmods.background.ClockPreferences fonts = new com.clockmods.background.ClockPreferences(this);
        clock.setTypeface(com.clockmods.ui.ClockTypefaceResolver.resolve(this, fonts.getRenderFontFamily(), fonts.isBoldText() ? 700 : 400));
        setContentView(root); root.requestApplyInsets();
        preferences = getSharedPreferences("pro_alarm", MODE_PRIVATE); preferences.registerOnSharedPreferenceChangeListener(listener);
        refresh(); tick.run();
    }
    private void refresh() {
        AlarmStore store = new AlarmStore(this); activeId = store.ringingId();
        if (activeId == 0 && !store.pendingRings().isEmpty()) activeId = store.pendingRings().keySet().iterator().next();
        AlarmSpec alarm = store.find(activeId);
        if (alarm == null) { finish(); return; }
        title.setText(alarm.label.isEmpty() ? getString(R.string.alarm_ringing) : alarm.label);
    }
    private void command(String action) { startService(AlarmDelivery.command(this, activeId, action)); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); refresh(); }
    @Override protected void onDestroy() {
        handler.removeCallbacks(tick); if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy();
    }
}
