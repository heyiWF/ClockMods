package com.clockmods.pro.alarm;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.clockmods.R;
import java.time.ZoneId;
import java.util.Arrays;

public final class AlarmEditorActivity extends AlarmUi {
    private String childMode;
    private final ActivityResultLauncher<Intent> child = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
        try {
            AlarmSpec changed = AlarmSpec.fromJson(new org.json.JSONObject(result.getData().getStringExtra(DRAFT)));
            if ("extra".equals(childMode)) { draft.extraDates.clear(); draft.extraDates.addAll(changed.extraDates); }
            else if ("excluded".equals(childMode)) { draft.excludedDates.clear(); draft.excludedDates.addAll(changed.excludedDates); }
            else { draft.cycleLength = changed.cycleLength; draft.cycleStart = changed.cycleStart;
                System.arraycopy(changed.cycleMinutes, 0, draft.cycleMinutes, 0, 62); }
            render();
        } catch (Exception ignored) { }
    });
    private final ActivityResultLauncher<Intent> ringtone = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
        Uri picked = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        draft.ringtone = picked == null ? "silent" : RingtoneManager.isDefault(picked) ? "default" : picked.toString();
        render();
    });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); childMode = state == null ? null : state.getString("childMode");
        if (state == null && getIntent().hasExtra(AlarmScheduler.EXTRA_ID)) {
            AlarmSpec saved = new AlarmStore(this).find(getIntent().getLongExtra(AlarmScheduler.EXTRA_ID, 0));
            if (saved == null) { finish(); return; } draft = saved;
        } else if (state == null && draft.id == 0) draft.onceDate = AlarmPlan.nextDate(draft.hour, draft.minute, System.currentTimeMillis(), ZoneId.systemDefault());
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("childMode", childMode); super.onSaveInstanceState(state);
    }
    @Override protected void timeSelected(int value) {
        int old = draft.hour * 60 + draft.minute; draft.hour = value / 60; draft.minute = value % 60;
        for (int i = 0; i < 62; i++) if (draft.cycleMinutes[i] == old) draft.cycleMinutes[i] = value;
        if (draft.repeat == AlarmSpec.Repeat.ONCE) draft.onceDate = AlarmPlan.nextDate(draft.hour, draft.minute, System.currentTimeMillis(), ZoneId.systemDefault());
        render();
    }
    @Override protected void dateSelected(java.time.LocalDate date) { draft.onceDate = date; render(); }
    private void render() {
        screen(draft.id == 0 ? R.string.alarm_add : R.string.alarm_edit_title, this::save);
        timeHeader(draft.hour * 60 + draft.minute, () -> timePicker(draft.hour * 60 + draft.minute));
        row(R.string.alarm_repeat, AlarmLabels.repeat(this, draft), this::repeatPicker);
        if (draft.repeat == AlarmSpec.Repeat.ONCE)
            row(R.string.alarm_once_date, date(draft.onceDate), () -> datePicker(draft.onceDate));
        if (draft.repeat == AlarmSpec.Repeat.WEEKLY) weekdays();
        if (draft.repeat == AlarmSpec.Repeat.SHIFT) row(R.string.alarm_shift, getString(R.string.alarm_days, draft.cycleLength), () -> openChild("shift"));
        if (draft.repeat == AlarmSpec.Repeat.HOLIDAYS || draft.repeat == AlarmSpec.Repeat.WORKDAYS)
            body.addView(text(getString(R.string.alarm_holiday_coverage), 14));
        space(12);
        row(R.string.alarm_extra_dates, getString(R.string.alarm_date_count, draft.extraDates.size()), () -> openChild("extra"));
        row(R.string.alarm_excluded_dates, getString(R.string.alarm_date_count, draft.excludedDates.size()), () -> openChild("excluded"));
        space(12);
        row(R.string.alarm_label, draft.label, () -> {
            com.google.android.material.textfield.TextInputLayout field = new com.google.android.material.textfield.TextInputLayout(this);
            field.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            field.setHint(getString(R.string.alarm_label)); field.setPadding(dp(24), dp(8), dp(24), 0);
            com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(field.getContext());
            input.setSingleLine(true); input.setText(draft.label); field.addView(input);
            show(new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.alarm_label).setView(field)
                .setPositiveButton(R.string.apply, (dialog, which) -> { draft.label = input.getText().toString().trim(); render(); })
                .setNegativeButton(R.string.cancel, null).create());
        });
        row(R.string.alarm_ringtone, AlarmLabels.ringtone(this, draft), this::chooseRingtone);
        toggle(R.string.alarm_vibrate, draft.vibrate, checked -> draft.vibrate = checked);
        row(R.string.alarm_snooze, getString(R.string.timer_minutes, draft.snoozeMinutes), () -> {
            android.widget.NumberPicker picker = new android.widget.NumberPicker(this); picker.setMinValue(1); picker.setMaxValue(60); picker.setValue(draft.snoozeMinutes);
            show(new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.alarm_snooze).setView(picker)
                    .setPositiveButton(R.string.apply, (dialog, which) -> { draft.snoozeMinutes = picker.getValue(); render(); })
                    .setNegativeButton(R.string.cancel, null).create());
        });
        if (draft.id != 0) row(R.string.alarm_delete, "", () -> show(new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setMessage(R.string.alarm_delete_confirm).setPositiveButton(R.string.alarm_delete, (dialog, which) -> {
                    AlarmScheduler.cancel(this, draft.id); new AlarmStore(this).delete(draft.id);
                    if (new AlarmStore(this).isRinging()) startService(AlarmDelivery.command(this, draft.id, AlarmDelivery.STOP));
                    finish();
                }).setNegativeButton(R.string.cancel, null).create()));
    }
    private void save() {
        if (draft.repeat == AlarmSpec.Repeat.WEEKLY && draft.weekdays == 0 && draft.extraDates.isEmpty()) {
            Toast.makeText(this, R.string.alarm_choose_days, Toast.LENGTH_LONG).show(); return;
        }
        draft.enabled = true; draft.snoozeAt = 0; draft.lastFiredAt = 0;
        if (AlarmScheduler.nextTrigger(draft, this, System.currentTimeMillis()) == 0) {
            Toast.makeText(this, R.string.alarm_no_next, Toast.LENGTH_LONG).show(); return;
        }
        new AlarmStore(this).save(draft); AlarmScheduler.schedule(this, draft);
        setResult(RESULT_OK); finish();
    }
    private void repeatPicker() {
        int[] labels = AlarmLabels.REPEATS; String[] items = new String[labels.length];
        for (int i = 0; i < items.length; i++) items[i] = getString(labels[i]);
        show(new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.alarm_repeat)
                .setSingleChoiceItems(items, draft.repeat.ordinal(), (dialog, which) -> {
                    draft.repeat = AlarmSpec.Repeat.values()[which]; dialog.dismiss(); render();
                }).setNegativeButton(R.string.cancel, null).create());
    }
    private void weekdays() {
        android.widget.LinearLayout days = new android.widget.LinearLayout(this);
        String[] names = AlarmLabels.dayNames(this);
        for (int index = 0; index < 7; index++) {
            final int day = index;
            String shortName = java.time.DayOfWeek.of(day + 1).getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, com.clockmods.LocaleManager.resolveLocale(this));
            com.google.android.material.button.MaterialButton button = dayButton(shortName, (draft.weekdays & (1 << day)) != 0);
            button.setContentDescription(names[day]); button.setOnClickListener(v -> { draft.weekdays ^= 1 << day; render(); });
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(0, dp(48), 1); params.setMargins(dp(2), dp(8), dp(2), dp(12)); days.addView(button, params);
        }
        body.addView(days);
    }
    private void weekdayPicker() {
        boolean[] selected = new boolean[7]; for (int i = 0; i < 7; i++) selected[i] = (draft.weekdays & (1 << i)) != 0;
        String[] days = AlarmLabels.dayNames(this);
        show(new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.alarm_weekdays)
                .setMultiChoiceItems(days, selected, (dialog, which, checked) -> selected[which] = checked)
                .setPositiveButton(R.string.apply, (dialog, which) -> { draft.weekdays = 0;
                    for (int i = 0; i < 7; i++) if (selected[i]) draft.weekdays |= 1 << i; render(); })
                .setNegativeButton(R.string.cancel, null).create());
    }
    private void openChild(String mode) {
        childMode = mode;
        child.launch(new Intent(this, "shift".equals(mode) ? AlarmShiftActivity.class : AlarmDatesActivity.class)
                .putExtra(DRAFT, json()).putExtra(MODE, mode));
    }
    private void chooseRingtone() {

        Uri current = "silent".equals(draft.ringtone) ? null : "default".equals(draft.ringtone)
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(draft.ringtone);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);
        try { ringtone.launch(intent); } catch (android.content.ActivityNotFoundException unavailable) {
            Toast.makeText(this, R.string.alarm_ringtone_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
