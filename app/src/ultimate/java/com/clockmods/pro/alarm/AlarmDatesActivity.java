package com.clockmods.pro.alarm;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.clockmods.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public final class AlarmDatesActivity extends AlarmUi {
    private YearMonth month;
    private boolean exclusions;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); exclusions = "excluded".equals(getIntent().getStringExtra(MODE));
        month = state == null ? YearMonth.now() : YearMonth.parse(state.getString("month", YearMonth.now().toString())); render();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("month", month.toString()); super.onSaveInstanceState(state); }
    private Set<LocalDate> dates() { return exclusions ? draft.excludedDates : draft.extraDates; }
    private void render() {
        screen(exclusions ? R.string.alarm_excluded_dates : R.string.alarm_extra_dates, this::returnDraft);
        TextView count = text(getString(R.string.alarm_date_count, dates().size()), 28); body.addView(count); space(16);
        MaterialCardView card = card(); LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(12), dp(8), dp(12), dp(16)); card.addView(panel);
        Locale locale = com.clockmods.LocaleManager.resolveLocale(this);
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(month.format(DateTimeFormatter.ofPattern(locale.getLanguage().equals("zh") ? "yyyy年M月" : "LLLL yyyy", locale)), 18);
        heading.setPadding(dp(12), 0, 0, 0); nav.addView(heading, new LinearLayout.LayoutParams(0, dp(56), 1)); heading.setGravity(Gravity.CENTER_VERTICAL);
        nav.addView(navigate(false), new LinearLayout.LayoutParams(dp(48), dp(48))); nav.addView(navigate(true), new LinearLayout.LayoutParams(dp(48), dp(48))); panel.addView(nav);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(7);
        for (int day = 1; day <= 7; day++) { TextView label = text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW_STANDALONE, locale), 14); label.setGravity(Gravity.CENTER); cell(grid, label); }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) cell(grid, new TextView(this));
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day); boolean selected = dates().contains(date);
            MaterialButton button = dayButton(String.valueOf(day), selected);
            if (date.equals(LocalDate.now()) && !selected) { button.setStrokeWidth(dp(1)); button.setStrokeColor(android.content.res.ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary))); }
            button.setContentDescription(date(date) + ", " + getString(selected ? R.string.alarm_selected : R.string.alarm_unselected));
            button.setOnClickListener(v -> { if (!dates().remove(date)) dates().add(date); render(); }); cell(grid, button);
        }
        panel.addView(grid, new LinearLayout.LayoutParams(-1, -2)); space(12);
        TextView help = text(getString(R.string.alarm_dates_help), 14); help.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); body.addView(help);
        ChipGroup chips = new ChipGroup(this); chips.setChipSpacing(dp(8));
        for (LocalDate date : dates()) {
            Chip chip = new Chip(this); chip.setText(date(date)); chip.setCloseIconVisible(true); chip.setCloseIconContentDescription(getString(R.string.alarm_remove_date, date(date)));
            chip.setOnCloseIconClickListener(v -> { dates().remove(date); render(); }); chips.addView(chip);
        }
        body.addView(chips);
        if (!dates().isEmpty()) { MaterialButton clear = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle); clear.setText(R.string.alarm_clear_dates); clear.setOnClickListener(v -> { dates().clear(); render(); }); body.addView(clear); }
    }
    private MaterialButton navigate(boolean forward) {
        MaterialButton button = dayButton("", false); button.setIconResource(forward ? R.drawable.alarm_ic_next : R.drawable.alarm_ic_previous);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START); button.setIconPadding(0); button.setContentDescription(getString(forward ? R.string.alarm_next_month : R.string.alarm_previous_month));
        button.setOnClickListener(v -> { month = month.plusMonths(forward ? 1 : -1); render(); }); return button;
    }
    private void cell(GridLayout grid, android.view.View view) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(); params.width = 0; params.height = dp(48); params.setMargins(dp(1), dp(2), dp(1), dp(2));
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        if (view instanceof MaterialButton) {
            FrameLayout cell = new FrameLayout(this);
            int size = dp(Math.min(40, (Math.min(640, getResources().getConfiguration().screenWidthDp) - 86) / 7f));
            cell.addView(view, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            grid.addView(cell, params);
        } else grid.addView(view, params);
    }
}
