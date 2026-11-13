package com.clockmods.pro.alarm;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.clockmods.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AlarmShiftActivity extends AlarmUi {
    private int editingDay;
    @Override public void onCreate(Bundle state) { super.onCreate(state); editingDay = state == null ? 0 : state.getInt("editingDay"); render(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("editingDay", editingDay); super.onSaveInstanceState(state); }
    @Override protected void timeSelected(int minutes) { draft.cycleMinutes[editingDay] = minutes; render(); }
    @Override protected void dateSelected(LocalDate date) { draft.cycleStart = date; render(); }
    private void render() {
        screen(R.string.alarm_shift, () -> {
            boolean active = false; for (int i = 0; i < draft.cycleLength; i++) active |= draft.cycleMinutes[i] >= 0;
            if (!active) { Toast.makeText(this, R.string.alarm_shift_empty, Toast.LENGTH_LONG).show(); return; } returnDraft();
        });
        row(R.string.alarm_cycle_length, getString(R.string.alarm_days, draft.cycleLength), () -> {
            NumberPicker picker = new NumberPicker(this); picker.setMinValue(2); picker.setMaxValue(62); picker.setValue(draft.cycleLength);
            show(new MaterialAlertDialogBuilder(this).setTitle(R.string.alarm_cycle_length).setView(picker)
                .setPositiveButton(R.string.apply, (dialog, which) -> { draft.cycleLength = picker.getValue(); render(); }).setNegativeButton(R.string.cancel, null).create());
        });
        row(R.string.alarm_cycle_start, date(draft.cycleStart), () -> datePicker(draft.cycleStart));
        space(12); TextView help = text(getString(R.string.alarm_shift_help), 14); help.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); body.addView(help); space(12);
        long elapsed = ChronoUnit.DAYS.between(draft.cycleStart, LocalDate.now()); int today = elapsed < 0 ? -1 : (int) (elapsed % draft.cycleLength);
        for (int index = 0; index < draft.cycleLength; index++) {
            final int day = index; MaterialCardView card = card(); LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(20), dp(12), dp(16), dp(12)); row.setMinimumHeight(dp(80));
            LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(getString(R.string.alarm_cycle_day, day + 1) + (day == today ? " · " + getString(R.string.alarm_today) : ""), 16); title.setPadding(0, 0, 0, dp(4)); labels.addView(title);
            TextView time = text(draft.cycleMinutes[day] < 0 ? getString(R.string.alarm_rest_day) : time(draft.cycleMinutes[day]), 22); time.setPadding(0, 0, 0, 0); time.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); labels.addView(time);
            labels.setOnClickListener(v -> { editingDay = day; timePicker(draft.cycleMinutes[day] < 0 ? draft.hour * 60 + draft.minute : draft.cycleMinutes[day]); });
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1)); MaterialSwitch enabled = new MaterialSwitch(this); enabled.setChecked(draft.cycleMinutes[day] >= 0); enabled.setContentDescription(getString(R.string.alarm_cycle_day, day + 1));
            enabled.setOnCheckedChangeListener((button, checked) -> { draft.cycleMinutes[day] = checked ? draft.hour * 60 + draft.minute : -1; render(); }); row.addView(enabled); card.addView(row);
        }
    }
}
