package com.clockmods.pro;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.clockmods.R;
import com.clockmods.pro.alarm.*;
import com.clockmods.ui.ClockTimeText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class ProAlarmFragment extends Fragment {
    private LinearLayout body;
    private android.content.SharedPreferences observed;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
        if ("alarms_v2".equals(key) || "ringing_id".equals(key)) render();
    };
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        ScrollView scroll = new ScrollView(requireContext()); body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL); int pad = dp(16); body.setPadding(pad, pad, pad, dp(104)); scroll.addView(body);
        android.widget.FrameLayout root = new android.widget.FrameLayout(requireContext());
        root.addView(scroll, new android.widget.FrameLayout.LayoutParams(dp(Math.min(680, getResources().getConfiguration().screenWidthDp)), -1, android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL));
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton add = new com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton(requireContext());
        add.setText(R.string.alarm_add); add.setIconResource(R.drawable.import_ic_add); add.setOnClickListener(v -> startActivity(new Intent(requireContext(), AlarmEditorActivity.class)));
        android.widget.FrameLayout.LayoutParams action = new android.widget.FrameLayout.LayoutParams(-2, dp(64), android.view.Gravity.BOTTOM | android.view.Gravity.END); action.setMargins(dp(24), dp(24), dp(24), dp(24)); root.addView(add, action); return root;
    }
    @Override public void onResume() { super.onResume(); AlarmScheduler.rescheduleAll(requireContext());
        observed = requireContext().getSharedPreferences("pro_alarm", 0); observed.registerOnSharedPreferenceChangeListener(listener); render(); }
    @Override public void onPause() { if (observed != null) observed.unregisterOnSharedPreferenceChangeListener(listener); super.onPause(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void render() {
        if (body == null) return; body.removeAllViews();
        TextView heading = new TextView(requireContext()); heading.setText(R.string.pro_page_alarm);
        heading.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        heading.setPadding(dp(8), dp(8), dp(8), dp(12)); body.addView(heading);
        if (new AlarmStore(requireContext()).isRinging()) {
            MaterialButton ringing = new MaterialButton(requireContext()); ringing.setText(R.string.alarm_open_to_dismiss);
            ringing.setOnClickListener(v -> startActivity(new Intent(requireContext(), AlarmRingingActivity.class))); body.addView(ringing);
        }
        if (!AlarmScheduler.canScheduleExact(requireContext())) access(R.string.alarm_exact_access_required, 0);
        if (!AlarmAccess.fullScreen(requireContext())) access(R.string.alarm_full_screen_access_required, 1);
        if (!AlarmAccess.notifications(requireContext())) access(R.string.alarm_notification_access_required, 2);
        java.util.List<AlarmSpec> alarms = new AlarmStore(requireContext()).all();
        if (alarms.isEmpty()) { TextView empty = new TextView(requireContext()); empty.setText(R.string.alarm_empty); empty.setPadding(0, dp(24), 0, dp(24)); body.addView(empty); }
        for (AlarmSpec alarm : alarms) {
            MaterialCardView card = new MaterialCardView(requireContext()); card.setRadius(dp(28)); card.setStrokeWidth(0); card.setCardElevation(0); card.setClickable(true); card.setOnClickListener(v -> edit(alarm.id));
            LinearLayout content = new LinearLayout(requireContext()); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(12), dp(20), dp(16));
            LinearLayout row = new LinearLayout(requireContext()); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView time = new TextView(requireContext()); time.setTextSize(56);
            time.setText(ClockTimeText.align(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)));
            time.setOnClickListener(v -> edit(alarm.id)); row.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
            MaterialSwitch enabled = new MaterialSwitch(requireContext()); enabled.setChecked(alarm.enabled || alarm.snoozeAt > System.currentTimeMillis());
            enabled.setContentDescription(getString(R.string.alarm_enabled) + " " + time.getText());
            enabled.setOnCheckedChangeListener((view, value) -> { AlarmAccess.enabled(requireContext(), alarm, value); render(); }); row.addView(enabled); content.addView(row);
            TextView label = new TextView(requireContext()); label.setText(alarm.label.isEmpty() ? AlarmLabels.repeat(requireContext(), alarm) : alarm.label + " · " + AlarmLabels.repeat(requireContext(), alarm)); content.addView(label, 0);
            long next = AlarmScheduler.nextTrigger(alarm, requireContext(), System.currentTimeMillis());
            TextView summary = new TextView(requireContext()); summary.setText(ClockTimeText.align(next > 0 ? getString(R.string.alarm_next_trigger,
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(next)))
                    : getString(alarm.enabled ? R.string.alarm_no_next : R.string.alarm_disabled))); content.addView(summary);
            card.addView(content); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(12); body.addView(card, params);
        }
        ProFontApplier.apply(body);
    }
    private void edit(long id) { startActivity(new Intent(requireContext(), AlarmEditorActivity.class).putExtra(AlarmScheduler.EXTRA_ID, id)); }
    private void access(int message, int kind) {
        MaterialButton button = new MaterialButton(requireContext()); button.setText(message); button.setOnClickListener(v -> AlarmAccess.settings(requireContext(), kind)); body.addView(button);
    }
    @Override public void onDestroyView() { body = null; super.onDestroyView(); }
}
