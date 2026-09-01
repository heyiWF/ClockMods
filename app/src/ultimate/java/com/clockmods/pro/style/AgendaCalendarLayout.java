package com.clockmods.pro.style;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.pro.CalendarDashboardSizing;
import com.clockmods.pro.CalendarDayNumberView;
import com.clockmods.pro.CalendarTheme;
import com.clockmods.pro.MonthGestureLayout;
import com.clockmods.pro.style.CalendarLayoutCapabilities.Capability;
import com.clockmods.ui.AlmanacLineView;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.ui.QWeatherLogoView;
import com.clockmods.ui.StatusBarView;
import com.clockmods.ui.WeatherIconView;
import com.clockmods.weather.WeatherModels;
import com.clockmods.weather.WeatherTemperatureFormatter;
import com.clockmods.pro.schedule.AgendaScheduleView;
import com.clockmods.pro.schedule.ScheduleItem;
import com.clockmods.pro.schedule.ScheduleStore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * 靛蓝周程: a seven-day strip over one detail card. A paper planner rather than a dashboard — the
 * page shows a week, and the day you pick gets the space a month grid would have spent on the other
 * five weeks.
 *
 * <p>Only the strip pages. The card stays put and re-binds once the swipe commits, which keeps the
 * async weather and almanac binding in one place instead of duplicating it into a preview card the
 * user would see for 200ms.</p>
 */
public final class AgendaCalendarLayout implements CalendarLayout, CalendarPager {
    /** Seven days of lunar labels and badges is cheap; a month of them would not be. */
    private static final Set<CalendarPageState.DayDetail> DAY_DETAILS =
            Collections.unmodifiableSet(EnumSet.of(CalendarPageState.DayDetail.LUNAR,
                    CalendarPageState.DayDetail.FESTIVALS,
                    CalendarPageState.DayDetail.HOLIDAY));
    private static final Set<CalendarPageState.DayDetail> SELECTION_DETAILS =
            Collections.unmodifiableSet(EnumSet.of(CalendarPageState.DayDetail.LUNAR,
                    CalendarPageState.DayDetail.ALMANAC_TABOO));

    private final CalendarLayoutCapabilities capabilities;
    private final List<DayCell> cells = new ArrayList<>();

    private Context context;
    private View root;
    private CalendarLayoutHost host;

    private MonthGestureLayout monthPanel;
    private TextView titleView, weekView, todayButton;
    private StatusBarView statusBar;
    private FrameLayout stripViewport;
    private LinearLayout strip, stripPreview;
    private View card, divider, attribution;
    private TextView dateView, chipView, festivalsView, weatherNow, weatherText;
    private AlmanacLineView suitableView, avoidView;
    private LinearLayout weatherRow;
    private View stretch;
    private WeatherIconView weatherIcon;
    private LoadingIndicator weatherLoading;
    private AgendaScheduleView scheduleView;
    private ScheduleStore scheduleStore;

    private ClockPreferences preferences;
    private CalendarTheme theme = CalendarTheme.resolve(CalendarTheme.ID_AGENDA);
    private CalendarPageState pageState;
    private int previewDirection;
    private WeatherModels.WeatherState lastWeatherState;
    private WeatherModels.DailyForecastState lastForecastState;

    private boolean responsiveSizingPosted;
    private final Runnable responsiveSizingRunnable = () -> {
        responsiveSizingPosted = false;
        if (root != null) applyResponsiveSizing();
    };

    public AgendaCalendarLayout(CalendarLayoutCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities must not be null");
        }
        this.capabilities = capabilities;
    }

    // region lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            CalendarLayoutHost host) {
        context = inflater.getContext();
        this.host = host;
        root = inflater.inflate(R.layout.calendar_layout_agenda, container, false);
        monthPanel = root.findViewById(R.id.calendar_month_panel);
        titleView = root.findViewById(R.id.calendar_agenda_title);
        weekView = root.findViewById(R.id.calendar_agenda_week);
        todayButton = root.findViewById(R.id.calendar_agenda_today);
        statusBar = root.findViewById(R.id.calendar_status_bar);
        stripViewport = root.findViewById(R.id.calendar_agenda_strip_viewport);
        strip = root.findViewById(R.id.calendar_agenda_strip);
        stripPreview = root.findViewById(R.id.calendar_agenda_strip_preview);
        card = root.findViewById(R.id.calendar_agenda_card);
        dateView = root.findViewById(R.id.calendar_agenda_date);
        chipView = root.findViewById(R.id.calendar_agenda_chip);
        festivalsView = root.findViewById(R.id.calendar_agenda_festivals);
        divider = root.findViewById(R.id.calendar_agenda_divider);
        weatherRow = root.findViewById(R.id.calendar_agenda_weather);
        weatherIcon = root.findViewById(R.id.calendar_agenda_weather_icon);
        weatherLoading = root.findViewById(R.id.calendar_agenda_weather_loading);
        weatherLoading.setContentDescription(
                context.getString(R.string.calendar_forecast_loading));
        weatherNow = root.findViewById(R.id.calendar_agenda_weather_now);
        weatherText = root.findViewById(R.id.calendar_agenda_weather_text);
        suitableView = root.findViewById(R.id.calendar_agenda_suitable);
        avoidView = root.findViewById(R.id.calendar_agenda_avoid);
        stretch = root.findViewById(R.id.calendar_agenda_stretch);
        scheduleView = root.findViewById(R.id.calendar_agenda_schedule);
        scheduleStore = new ScheduleStore(context);
        attribution = root.findViewById(R.id.calendar_weather_attribution);
        configureViews();
        return root;
    }

    private void configureViews() {
        titleView.setAccessibilityHeading(true);
        titleView.setOnClickListener(view -> host.onMonthPickerRequested());
        titleView.setTooltipText(context.getString(R.string.calendar_jump_title));
        todayButton.setOnClickListener(view -> host.onTodayRequested());
        attribution.setOnClickListener(view -> host.onAttributionClicked());
        monthPanel.setMonthGestureListener(new MonthGestureLayout.Listener() {
            @Override public void onMonthDrag(float offset) {
                host.onPageDragged(offset);
            }

            @Override public void onMonthDragFinished(int direction) {
                host.onPageDragFinished(direction);
            }
        });
        // The strip is the only part of the panel that turns a drag into a page turn; a drag on the
        // card must reach the outer pager instead, so the gesture is scoped to the viewport.
        monthPanel.setDragRegion(stripViewport);
        scheduleView.setListener(new AgendaScheduleView.Listener() {
            @Override public void onAddRequested() { showScheduleEditor(null); }
            @Override public void onItemSelected(ScheduleItem item) { showScheduleEditor(item); }
        });
        monthPanel.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        strip.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        scheduleResponsiveSizing();
    }

    @Override
    public void onDestroyView() {
        if (monthPanel != null) monthPanel.removeCallbacks(responsiveSizingRunnable);
        responsiveSizingPosted = false;
        cells.clear();
        pageState = null;
        lastWeatherState = null;
        lastForecastState = null;
    }

    @Override
    public void setActive(boolean active) {
        if (statusBar == null) return;
        if (active) statusBar.start(); else statusBar.stop();
        // The 宜/忌 marquee runs only while the page is the one the user is looking at.
        suitableView.setActive(active);
        avoidView.setActive(active);
    }

    // endregion
    // region settings and theme

    @Override
    public void applySettings(CalendarTheme theme, ClockPreferences preferences,
            BackgroundRepository background) {
        this.theme = theme;
        this.preferences = preferences;
        float density = context.getResources().getDisplayMetrics().density;
        root.setBackground(theme.newPageBackground());
        applyPanelBackground(card);
        // The capsule sits at the end of a header row rather than in a side column, so unlike the
        // dashboard it is aligned to the row's end and tinted as support rather than as body type.
        statusBar.setBackgroundRepository(background);
        statusBar.setContentAlignedStart(false);
        statusBar.setTintOverride(theme.followsUserTimeColor ? 0 : theme.secondary);
        statusBar.setVisibility(preferences.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        titleView.setTextColor(theme.text);
        weekView.setTextColor(theme.secondary);
        todayButton.setTextColor(theme.accent);
        dateView.setTextColor(theme.text);
        festivalsView.setTextColor(theme.accent);
        divider.setBackgroundColor(withAlpha(theme.secondary, 0.22f));
        weatherNow.setTextColor(theme.text);
        weatherText.setTextColor(theme.secondary);
        if (weatherLoading != null) {
            // The preset paints its own card, so the indicator takes the preset's accent rather
            // than the Material theme's primary, on a container that draws nothing.
            weatherLoading.setIndicatorColor(theme.accent);
            weatherLoading.setContainerColor(Color.TRANSPARENT);
        }
        suitableView.setColor(theme.suitable);
        avoidView.setColor(theme.avoid);
        scheduleView.applyTheme(theme);
        weatherIcon.setIconColor(preferences.isWeatherIconDynamicColor()
                ? ExperienceBridge.resolveAccentColor(context, theme.weatherIcon)
                : theme.weatherIcon);
        tintAttribution();
        // Re-tinting the palette does not rebuild cells, so already-bound ones repaint themselves.
        for (DayCell cell : cells) cell.applyTheme();
        renderWeather();
        applyResponsiveSizing();
    }

    private void applyPanelBackground(View panel) {
        int left = panel.getPaddingLeft();
        int top = panel.getPaddingTop();
        int right = panel.getPaddingRight();
        int bottom = panel.getPaddingBottom();
        panel.setBackground(theme.newPanelBackground(
                context.getResources().getDisplayMetrics().density));
        panel.setPadding(left, top, right, bottom);
    }

    /** The attribution row has no ids of its own; its two labels and the logo are tinted by type. */
    private void tintAttribution() {
        if (!(attribution instanceof ViewGroup)) return;
        ViewGroup row = (ViewGroup) attribution;
        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(withAlpha(theme.secondary, 0.66f));
            } else if (child instanceof QWeatherLogoView) {
                ((QWeatherLogoView) child).setLogoColor(withAlpha(theme.secondary, 0.8f));
            }
        }
    }

    @Override
    public void applyTypefaces() {
        Typeface regular = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false);
        Typeface display = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), preferences.isBoldText());
        titleView.setTypeface(display);
        weekView.setTypeface(regular);
        todayButton.setTypeface(regular);
        dateView.setTypeface(display);
        chipView.setTypeface(regular);
        festivalsView.setTypeface(regular);
        weatherNow.setTypeface(display);
        weatherText.setTypeface(regular);
        // The 宜/忌 glyph bolds itself on top of the regular face, so both lines take that one.
        suitableView.setTypeface(regular);
        avoidView.setTypeface(regular);
        scheduleView.setTypefaces(display, regular);
        for (DayCell cell : cells) cell.setTypefaces(display, regular);
        applyStripTypefaces(stripPreview, display, regular);
    }

    private void applyStripTypefaces(LinearLayout target, Typeface display, Typeface regular) {
        for (int index = 0; index < target.getChildCount(); index++) {
            View child = target.getChildAt(index);
            if (child instanceof DayCell) ((DayCell) child).setTypefaces(display, regular);
        }
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
    }

    // endregion
    // region sizing

    private void scheduleResponsiveSizing() {
        if (responsiveSizingPosted || monthPanel == null) return;
        responsiveSizingPosted = true;
        if (!monthPanel.post(responsiveSizingRunnable)) responsiveSizingPosted = false;
    }

    @Override
    public void applyResponsiveSizing() {
        if (monthPanel == null || monthPanel.getWidth() <= 0 || monthPanel.getHeight() <= 0) return;
        float density = context.getResources().getDisplayMetrics().density;
        int panelHeight = monthPanel.getHeight() - monthPanel.getPaddingTop()
                - monthPanel.getPaddingBottom();
        float chromeSize = CalendarDashboardSizing.monthTitleSize(
                CalendarDashboardSizing.monthToolbarHeight(panelHeight, density), density);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, chromeSize);
        float supportSize = CalendarDashboardSizing.agendaSubheadingSize(
                CalendarDashboardSizing.monthFooterHeight(panelHeight, density), density);
        weekView.setTextSize(TypedValue.COMPLEX_UNIT_PX, supportSize);
        todayButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, supportSize);
        applyStripSizing(density);
        applyCardSizing(density);
    }

    private void applyStripSizing(float density) {
        if (strip.getWidth() <= 0 || strip.getHeight() <= 0) return;
        // In the rail the seven cells divide the height and each one owns the full width; in the
        // strip it is the other way round. Feeding the sizing functions the per-cell box either way
        // keeps one set of ratios for both orientations.
        boolean rail = strip.getOrientation() == LinearLayout.VERTICAL;
        float cellWidth = Math.max(1f, rail ? strip.getWidth() : strip.getWidth() / 7f);
        float cellHeight = Math.max(1f, rail ? strip.getHeight() / 7f : strip.getHeight());
        float numberSize = CalendarDashboardSizing.agendaStripNumberSize(cellWidth, cellHeight,
                density);
        float labelSize = CalendarDashboardSizing.agendaStripLunarSize(cellWidth, cellHeight,
                density);
        for (DayCell cell : cells) cell.applySizing(numberSize, labelSize);
        for (int index = 0; index < stripPreview.getChildCount(); index++) {
            View child = stripPreview.getChildAt(index);
            if (child instanceof DayCell) ((DayCell) child).applySizing(numberSize, labelSize);
        }
    }

    private void applyCardSizing(float density) {
        if (card.getWidth() <= 0 || card.getHeight() <= 0) return;
        float cardWidth = card.getWidth() - card.getPaddingLeft() - card.getPaddingRight();
        float cardHeight = card.getHeight() - card.getPaddingTop() - card.getPaddingBottom();
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                CalendarDashboardSizing.agendaDetailTitleSize(cardWidth, cardHeight, density));
        float bodySize = CalendarDashboardSizing.agendaDetailBodySize(cardWidth, cardHeight,
                density);
        chipView.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize);
        festivalsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize);
        weatherText.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize);
        suitableView.setTextSizePx(bodySize);
        avoidView.setTextSizePx(bodySize);
        // The pane is the one block on the card the user writes into, so it carries a little more
        // weight than the 宜忌 lines it sits under rather than less.
        scheduleView.applySizing(bodySize * 1.08f, bodySize * 1.08f);
        int iconSize = Math.max(1, Math.round(
                CalendarDashboardSizing.agendaWeatherIconSize(cardWidth, cardHeight, density)));
        ViewGroup.LayoutParams iconParams = weatherIcon.getLayoutParams();
        if (iconParams.width != iconSize || iconParams.height != iconSize) {
            iconParams.width = iconSize;
            iconParams.height = iconSize;
            weatherIcon.setLayoutParams(iconParams);
        }
        // The current reading is the one number on the card that competes with the date, so it is
        // pinned to the icon rather than to the body scale.
        weatherNow.setTextSize(TypedValue.COMPLEX_UNIT_PX, iconSize * 0.46f);
    }

    // endregion
    // region binding

    /** The weekday names live inside the strip cells, so there is no header row to rebuild. */
    @Override
    public void bindWeekdays(CalendarPageState state) { }

    @Override
    public void bind(CalendarPageState state) {
        pageState = state;
        titleView.setText(state.title.full);
        weekView.setText(state.title.sub);
        weekView.setVisibility(state.title.sub.length() == 0 ? View.GONE : View.VISIBLE);
        strip.removeAllViews();
        cells.clear();
        for (int index = 0; index < state.days.size(); index++) {
            DayCell cell = new DayCell(true);
            cell.bind(state.days.get(index), state, index, index == state.selection.index);
            cells.add(cell);
            strip.addView(cell, cellParams());
        }
        renderCard();
        applyResponsiveSizing();
    }

    @Override
    public void updateSelection(CalendarPageState state) {
        pageState = state;
        int count = Math.min(cells.size(), state.days.size());
        for (int index = 0; index < count; index++) {
            cells.get(index).applySelection(index == state.selection.index);
        }
        renderCard();
    }

    /** The whole card describes the selected day, so every part of it is redrawn together. */
    private void renderCard() {
        if (pageState == null) return;
        CalendarPageState.DayInfo day = selectedDay();
        dateView.setText(pageState.selection.dateLine);
        if (day != null && day.holidayBadge.length() > 0) {
            String label = day.holidayName.length() > 0
                    ? day.holidayBadge + " " + day.holidayName : day.holidayBadge;
            chipView.setText(label);
            int chipColor = day.holidayOffDay ? theme.restBadge : theme.workBadge;
            chipView.setTextColor(chipColor);
            chipView.setBackground(newChipBackground(chipColor));
            chipView.setVisibility(View.VISIBLE);
        } else {
            chipView.setVisibility(View.GONE);
        }
        String festivals = day == null || day.festivals.isEmpty() ? ""
                : android.text.TextUtils.join(" · ", day.festivals);
        festivalsView.setText(festivals);
        festivalsView.setVisibility(festivals.length() == 0 ? View.GONE : View.VISIBLE);
        String suitableHint = context.getString(R.string.calendar_suitable_prefix);
        String avoidHint = context.getString(R.string.calendar_avoid_prefix);
        suitableView.setLine(pageState.selection.suitableLine, suitableHint);
        suitableView.setVisibility(
                pageState.selection.suitableLine.length() == 0 ? View.GONE : View.VISIBLE);
        avoidView.setLine(pageState.selection.avoidLine, avoidHint);
        avoidView.setVisibility(
                pageState.selection.avoidLine.length() == 0 ? View.GONE : View.VISIBLE);
        renderSchedule();
        renderWeather();
    }

    private void renderSchedule() {
        if (scheduleView == null || pageState == null) return;
        CalendarPageState.DayInfo day = selectedDay();
        if (day == null) {
            scheduleView.setVisibility(View.GONE);
            return;
        }
        scheduleView.setVisibility(View.VISIBLE);
        scheduleView.setItems(scheduleStore.itemsFor(day.year, day.month, day.dayOfMonth));
    }

    @Nullable
    private CalendarPageState.DayInfo selectedDay() {
        if (pageState == null) return null;
        int index = pageState.selection.index;
        return index < 0 || index >= pageState.days.size() ? null : pageState.days.get(index);
    }

    /** A rounded outline in the badge's own colour, so the chip needs no second palette entry. */
    private GradientDrawable newChipBackground(int color) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(density * 5f);
        shape.setColor(withAlpha(color, 0.12f));
        shape.setStroke(Math.max(1, Math.round(density)), withAlpha(color, 0.45f));
        return shape;
    }

    @Override
    public void bindWeather(WeatherModels.WeatherState state) {
        lastWeatherState = state;
        renderWeather();
    }

    @Override
    public void bindForecast(WeatherModels.DailyForecastState state) {
        lastForecastState = state;
        renderWeather();
    }

    /**
     * The add / edit sheet. Both fields are Material text fields and the time one opens a
     * {@link MaterialTimePicker} rather than a bare {@code TimePicker}: the picker is a
     * {@code DialogFragment}, so it survives a rotation and follows the platform's own clock-vs-
     * keypad affordance instead of this app reinventing one.
     */
    private void showScheduleEditor(@Nullable ScheduleItem existing) {
        CalendarPageState.DayInfo day = selectedDay();
        if (day == null) return;
        float density = context.getResources().getDisplayMetrics().density;

        TextInputLayout titleField = new TextInputLayout(context, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        titleField.setHint(context.getString(R.string.ultimate_schedule_dialog_title_hint));
        TextInputEditText titleInput = new TextInputEditText(titleField.getContext());
        titleInput.setSingleLine(true);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        titleInput.setText(existing == null ? "" : existing.title);
        titleField.addView(titleInput);

        TextInputLayout timeField = new TextInputLayout(context, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        timeField.setHint(context.getString(R.string.ultimate_schedule_dialog_time));
        // The field is a button in a text field's clothing: tapping anywhere in it opens the
        // picker, and it never takes focus, so no keyboard ever competes with the dialog.
        timeField.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        timeField.setEndIconDrawable(R.drawable.ic_nest_clock_farsight_digital);
        TextInputEditText timeInput = new TextInputEditText(timeField.getContext());
        timeInput.setSingleLine(true);
        timeInput.setFocusable(false);
        timeInput.setCursorVisible(false);
        timeInput.setInputType(InputType.TYPE_NULL);
        timeInput.setText(existing != null && existing.hasTime() ? existing.timeLabel() : "");
        timeField.addView(timeInput);
        View.OnClickListener openPicker = view -> showTimePicker(timeInput);
        timeInput.setOnClickListener(openPicker);
        timeField.setEndIconOnClickListener(openPicker);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = Math.round(density * 24f);
        content.setPadding(side, Math.round(density * 12f), side, 0);
        content.addView(titleField, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        timeParams.topMargin = Math.round(density * 12f);
        content.addView(timeField, timeParams);

        MaterialAlertDialogBuilder editor = new MaterialAlertDialogBuilder(context)
                .setTitle(existing == null ? R.string.ultimate_schedule_dialog_add
                        : R.string.ultimate_schedule_dialog_edit)
                .setView(content)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setPositiveButton(R.string.ultimate_schedule_save, (dialog, which) ->
                        saveSchedule(day, existing, titleInput.getText().toString(),
                                timeInput.getText().toString()));
        if (existing != null) {
            editor.setNeutralButton(R.string.ultimate_schedule_dialog_delete, (dialog, which) -> {
                    scheduleStore.remove(day.year, day.month, day.dayOfMonth, existing.id);
                    renderSchedule();
                });
        }
        editor.show();
    }

    /**
     * Seeds the picker from whatever the field already reads, so reopening it lands on the time the
     * user last chose rather than snapping back to a default.
     */
    private void showTimePicker(TextInputEditText timeInput) {
        int hour = 9;
        int minute = 0;
        String current = timeInput.getText() == null ? "" : timeInput.getText().toString();
        String[] parts = current.split(":");
        if (parts.length == 2) {
            try {
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                hour = 9;
                minute = 0;
            }
        }
        boolean is24Hour = preferences == null || preferences.isUse24Hour();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(is24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                // The dial is the affordance Material leads with, and it fits here; the keypad
                // stays one tap away on the picker's own toggle for anyone who prefers typing.
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.ultimate_schedule_dialog_time)
                .build();
        // Only the positive button commits: dismissing or cancelling must leave the field alone.
        picker.addOnPositiveButtonClickListener(view -> timeInput.setText(
                String.format(Locale.US, "%02d:%02d", picker.getHour(), picker.getMinute())));
        picker.show(host.fragmentManager(), "agenda-schedule-time");
    }

    private void saveSchedule(CalendarPageState.DayInfo day, @Nullable ScheduleItem existing,
            String title, String time) {
        title = title.trim();
        // An untitled item would render as a blank row, so an empty title simply saves nothing.
        if (title.length() == 0) return;
        int hour = ScheduleItem.TIME_NONE;
        int minute = ScheduleItem.TIME_NONE;
        if (time.length() > 0) {
            String[] parts = time.split(":");
            if (parts.length == 2) {
                try {
                    hour = Integer.parseInt(parts[0].trim());
                    minute = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    hour = ScheduleItem.TIME_NONE;
                    minute = ScheduleItem.TIME_NONE;
                }
            }
        }
        String id = existing == null ? "" : existing.id;
        scheduleStore.save(day.year, day.month, day.dayOfMonth, id, title, hour, minute);
        renderSchedule();
    }

    /**
     * The card carries one day's weather, which is the daily forecast for that day plus — only when
     * the day is today — the live reading beside it.
     *
     * <p>The forecast covers a handful of days either side of today, and a week page can easily sit
     * outside it. When there is nothing for the selected day the row goes away entirely rather than
     * showing {@code --}: a planner with a blank weather slot reads as broken, whereas a planner
     * with no weather slot reads as a planner. The attribution goes with it, since with nothing from
     * the service on screen there is nothing to attribute.</p>
     */
    private void renderWeather() {
        if (weatherRow == null || preferences == null) return;
        boolean enabled = capabilities.supports(Capability.WEATHER)
                && preferences.isWeatherEnabled();
        CalendarPageState.DayInfo day = selectedDay();
        WeatherModels.DailyForecast forecast = enabled ? forecastFor(day) : null;
        if (forecast == null) {
            // The card used to collapse the moment there was nothing to show, which made a request
            // in flight indistinguishable from a day the forecast does not reach. A day inside the
            // forecast window that is still loading now keeps the row and spins in it.
            boolean loading = enabled && isForecastLoading() && dayCouldHaveForecast(day);
            setWeatherRowVisible(loading);
            setWeatherLoading(loading);
            return;
        }
        setWeatherRowVisible(true);
        setWeatherLoading(false);
        weatherIcon.setIconCode(forecast.iconDay, preferences.isWeatherIconFill());
        StringBuilder summary = new StringBuilder();
        append(summary, forecast.textDay);
        append(summary, temperatureRangeLabel(forecast.tempMin, forecast.tempMax));
        if (!empty(forecast.humidity)) {
            append(summary, context.getString(R.string.weather_humidity_format, forecast.humidity));
        }
        String windScale = empty(forecast.windScaleDay) ? ""
                : context.getString(R.string.weather_wind_scale_format, forecast.windScaleDay);
        append(summary, join(forecast.windDirDay, windScale));
        weatherText.setText(summary.toString());
        // The live reading only means anything on the day it was taken.
        boolean live = day != null && day.today && lastWeatherState != null
                && lastWeatherState.data != null;
        weatherNow.setVisibility(live ? View.VISIBLE : View.GONE);
        if (live) weatherNow.setText(temperatureLabel(lastWeatherState.data.temperature));
    }

    private void setWeatherRowVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        weatherRow.setVisibility(visibility);
        divider.setVisibility(visibility);
        attribution.setVisibility(visibility);
        stretch.setVisibility(visibility);
    }

    /** While loading, the indicator stands in for the whole reading, not just the icon. */
    private void setWeatherLoading(boolean loading) {
        if (weatherLoading != null) {
            weatherLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        weatherIcon.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (loading) {
            weatherNow.setVisibility(View.GONE);
            weatherText.setText("");
        }
    }

    private boolean isForecastLoading() {
        return lastForecastState != null && lastForecastState.data == null
                && lastForecastState.status == WeatherModels.Status.LOADING;
    }

    /**
     * A forecast only ever reaches a few days ahead, so a week in 2031 must not sit there spinning
     * for one that will never arrive. Today and the days after it are the only ones a request can
     * answer — and the comparison uses the app's own time zone, the same one the host dated the
     * page with.
     */
    private boolean dayCouldHaveForecast(@Nullable CalendarPageState.DayInfo day) {
        if (day == null) return false;
        if (day.today) return true;
        Calendar today = Calendar.getInstance(appTimeZone());
        int ordinal = (day.year * 12 + day.month) * 32 + day.dayOfMonth;
        int todayOrdinal = (today.get(Calendar.YEAR) * 12 + today.get(Calendar.MONTH)) * 32
                + today.get(Calendar.DAY_OF_MONTH);
        return ordinal > todayOrdinal;
    }

    private TimeZone appTimeZone() {
        String id = preferences == null ? "" : preferences.getTimeZoneId();
        return id == null || id.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(id);
    }

    @Nullable
    private WeatherModels.DailyForecast forecastFor(@Nullable CalendarPageState.DayInfo day) {
        if (day == null || lastForecastState == null || lastForecastState.data == null) return null;
        // DayInfo already carries the civil date the host resolved, so the key is a format away —
        // no SimpleDateFormat and no second opinion about the time zone.
        return lastForecastState.data.findByDate(String.format(Locale.US, "%04d-%02d-%02d",
                day.year, day.month + 1, day.dayOfMonth));
    }

    private String temperatureLabel(String celsius) {
        String unit = preferences.getWeatherTemperatureUnit();
        return WeatherTemperatureFormatter.replaceUnit(
                context.getString(R.string.weather_temperature_format,
                        WeatherTemperatureFormatter.numeric(celsius, unit)), unit);
    }

    private String temperatureRangeLabel(String minimumCelsius, String maximumCelsius) {
        String unit = preferences.getWeatherTemperatureUnit();
        return WeatherTemperatureFormatter.replaceUnit(
                context.getString(R.string.weather_temperature_range_format,
                        WeatherTemperatureFormatter.numeric(minimumCelsius, unit),
                        WeatherTemperatureFormatter.numeric(maximumCelsius, unit)), unit);
    }

    /** No clock: a week page carrying one as well would just be the dashboard with fewer days. */
    @Override public void bindClock(CalendarClockState clock) { }

    @Override
    public void announcePage() {
        if (titleView == null || pageState == null) return;
        String heading = pageState.title.sub.length() == 0 ? pageState.title.full
                : pageState.title.full + " " + pageState.title.sub;
        ViewCompat.setAccessibilityPaneTitle(titleView, heading);
    }

    @Override
    public Set<CalendarPageState.DayDetail> requiredDayDetails() { return DAY_DETAILS; }

    @Override
    public Set<CalendarPageState.DayDetail> requiredSelectionDetails() { return SELECTION_DETAILS; }

    // endregion
    // region paging

    @Nullable
    @Override
    public CalendarPager getPager() {
        return capabilities.supports(Capability.PAGE_SWIPE) ? this : null;
    }

    @Override public PageUnit unit() { return PageUnit.WEEK; }

    /**
     * The viewport, not the strip: in the rail the strip is a narrow column, and sliding it by its
     * own width would leave the outgoing week still on screen.
     */
    @Override
    public float pageWidth() {
        return stripViewport == null ? 0f : stripViewport.getWidth();
    }

    @Override
    public int preparedDirection() {
        return stripPreview != null && stripPreview.getVisibility() == View.VISIBLE
                ? previewDirection : 0;
    }

    @Override
    public void bindPreview(int direction, CalendarPageState adjacent) {
        if (stripViewport == null || stripPreview == null) return;
        stripPreview.removeAllViews();
        for (int index = 0; index < adjacent.days.size(); index++) {
            DayCell cell = new DayCell(false);
            cell.bind(adjacent.days.get(index), adjacent, index,
                    index == adjacent.selection.index);
            stripPreview.addView(cell, cellParams());
        }
        previewDirection = direction;
        stripPreview.setTranslationX(direction * pageWidth());
        stripPreview.setVisibility(View.VISIBLE);
        applyResponsiveSizing();
    }

    @Override
    public void setPageOffset(float offsetPx) {
        if (strip == null) return;
        strip.setTranslationX(offsetPx);
        if (stripPreview != null && previewDirection != 0) {
            stripPreview.setTranslationX(offsetPx + previewDirection * pageWidth());
        }
    }

    @Override
    public void animateCommit(int direction, Runnable onSettled) {
        if (strip == null || stripPreview == null) return;
        final float width = pageWidth();
        strip.animate().cancel();
        stripPreview.animate().cancel();
        strip.animate().translationX(direction > 0 ? -width : width)
                .setDuration(210L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        stripPreview.animate().translationX(0f)
                .setDuration(210L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onSettled)
                .start();
    }

    @Override
    public void animateSnapBack(Runnable onSettled) {
        if (strip == null || stripPreview == null) return;
        strip.animate().cancel();
        stripPreview.animate().cancel();
        strip.animate().translationX(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        if (previewDirection != 0) {
            stripPreview.animate().translationX(previewDirection * pageWidth())
                    .setDuration(180L)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(onSettled)
                    .start();
        }
    }

    @Override
    public void resetPages() {
        if (strip == null || stripPreview == null) return;
        strip.animate().cancel();
        stripPreview.animate().cancel();
        strip.setTranslationX(0f);
        stripPreview.setVisibility(View.INVISIBLE);
        stripPreview.removeAllViews();
        previewDirection = 0;
    }

    // endregion
    // region cell

    /** Seven equal shares of whichever axis the strip runs along. */
    private LinearLayout.LayoutParams cellParams() {
        boolean rail = strip.getOrientation() == LinearLayout.VERTICAL;
        return rail
                ? new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
    }

    /**
     * One day of the week: its weekday name, its number and its lunar label. Holding its own
     * {@link CalendarPageState.DayInfo} is what lets {@link #updateSelection} walk cells rather than
     * pair {@code strip.getChildAt(i)} with {@code days.get(i)}.
     *
     * <p>The cell reads the strip's orientation once, at construction, and builds itself as a column
     * (portrait strip) or a row (landscape rail). Both shapes carry the same three labels and the
     * same today accent; only the axis differs, so there is one binding path for both.</p>
     */
    private final class DayCell extends LinearLayout {
        private final Rect labelInk = new Rect();
        private final boolean rail;
        private final boolean interactive;
        private final TextView weekdayLabel;
        private final CalendarDayNumberView number;
        private final TextView lunarLabel;
        /** Today's accent: an underline in the strip, a leading bar in the rail. */
        private final View accentMark;

        private CalendarPageState.DayInfo day;
        private CalendarPageState state;
        private String baseDescription = "";

        DayCell(boolean interactive) {
            super(AgendaCalendarLayout.this.context);
            this.interactive = interactive;
            this.rail = strip.getOrientation() == LinearLayout.VERTICAL;
            float density = getResources().getDisplayMetrics().density;
            int accentThickness = Math.max(1, Math.round(density * 2.5f));
            setOrientation(rail ? HORIZONTAL : VERTICAL);
            setGravity(Gravity.CENTER);
            setBaselineAligned(false);
            int pad = Math.round(density * 3f);
            setPadding(pad, pad, pad, pad);

            weekdayLabel = new TextView(getContext());
            weekdayLabel.setGravity(Gravity.CENTER);
            weekdayLabel.setIncludeFontPadding(false);
            weekdayLabel.setSingleLine(true);
            weekdayLabel.setLetterSpacing(0.08f);
            number = new CalendarDayNumberView(getContext());
            // The strip has room for the lunar label as its own line, so the number carries neither
            // badge nor disc — today and the selection are marks, not fills on the glyph.
            number.setBadge("", 0);
            number.setFill(0);
            lunarLabel = new TextView(getContext());
            lunarLabel.setGravity(Gravity.CENTER);
            lunarLabel.setIncludeFontPadding(false);
            lunarLabel.setSingleLine(true);
            accentMark = new View(getContext());
            accentMark.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

            if (rail) {
                // Weighted slots, not wrapped ones. A lunar label is two glyphs on most days and
                // three when it carries a solar term or a festival (处暑, 七夕节) — and a wrapped
                // label hands that extra width back by squeezing the number's box, which slides
                // the number left on exactly those rows and breaks the column. Fixed shares keep
                // every number on the same axis whatever the label says.
                addView(accentMark, new LayoutParams(accentThickness, LayoutParams.MATCH_PARENT));
                weekdayLabel.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                lunarLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                LayoutParams weekdayParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.1f);
                weekdayParams.setMarginStart(Math.round(density * 6f));
                addView(weekdayLabel, weekdayParams);
                addView(number, new LayoutParams(0, LayoutParams.MATCH_PARENT, 2.6f));
                LayoutParams lunarParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.1f);
                lunarParams.setMarginEnd(Math.round(density * 6f));
                addView(lunarLabel, lunarParams);
            } else {
                addView(weekdayLabel, new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                addView(number, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
                addView(lunarLabel, new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                LayoutParams accentParams =
                        new LayoutParams(LayoutParams.MATCH_PARENT, accentThickness);
                accentParams.topMargin = Math.round(density * 4f);
                accentParams.setMarginStart(Math.round(density * 6f));
                accentParams.setMarginEnd(Math.round(density * 6f));
                addView(accentMark, accentParams);
            }
        }

        void bind(CalendarPageState.DayInfo day, CalendarPageState state, int index,
                boolean selected) {
            this.day = day;
            this.state = state;
            // CalendarMonth.createWeek starts the window on the user's first day of week, and the
            // host rotates weekdayNames the same way, so the cell index indexes both.
            weekdayLabel.setText(index < state.weekdayNames.length ? state.weekdayNames[index] : "");
            number.setDay(day.dayNumber, dayColor());
            lunarLabel.setText(lunarText());
            baseDescription = day.contentDescription;
            applyTheme();
            applySelection(selected);
            setClickable(interactive);
            setFocusable(interactive);
            if (interactive) setOnClickListener(view -> host.onDaySelected(day));
        }

        /** A festival or statutory name outranks the lunar date: it is why the day stands out. */
        private String lunarText() {
            if (!day.festivals.isEmpty()) return day.festivals.get(0);
            if (day.holidayName.length() > 0) return day.holidayName;
            return day.lunarShort;
        }

        private int dayColor() {
            if (day.today) return theme.today;
            if (state != null && state.highlightWeekends && day.weekend) return theme.weekend;
            return theme.day;
        }

        void applyTheme() {
            if (day == null) return;
            number.setDay(day.dayNumber, dayColor());
            weekdayLabel.setTextColor(state != null && state.highlightWeekends && day.weekend
                    ? theme.weekend : theme.weekday);
            // A festival name is the accent's job; a plain lunar date is support type.
            boolean highlighted = !day.festivals.isEmpty() || day.holidayName.length() > 0;
            lunarLabel.setTextColor(highlighted ? theme.accent : theme.secondary);
            accentMark.setBackgroundColor(day.today ? theme.today : 0x00000000);
        }

        void applySelection(boolean selected) {
            setBackground(selected
                    ? theme.newSelectionBackground(getResources().getDisplayMetrics().density)
                    : null);
            setContentDescription(selected
                    ? baseDescription + context.getString(R.string.calendar_selected_suffix)
                    : baseDescription);
        }

        void setTypefaces(Typeface display, Typeface regular) {
            number.setTypefaces(display, regular);
            weekdayLabel.setTypeface(regular);
            lunarLabel.setTypeface(regular);
        }

        void applySizing(float numberSize, float labelSize) {
            number.setDayTextSize(numberSize);
            weekdayLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSize);
            lunarLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSize);
        }

        /**
         * Equalises the two gaps a reader actually sees in the stacked cell — weekday ink to
         * number ink, and number ink to lunar ink.
         *
         * <p>It measures the laid-out views rather than the paint. {@code Paint.getFontMetrics()}
         * reports the <em>primary</em> typeface, but every glyph here is CJK and comes from a
         * fallback font with taller metrics, and it is the fallback that sizes the line box — so
         * arithmetic on paint metrics describes a box that is not on screen. {@code getBaseline()}
         * is fallback-aware, which makes it the only honest reference point.</p>
         *
         * <p>The correction is applied as a bottom margin on the weekday label, which pushes the
         * number's box down by the full margin and its ink centre by half — so it lands half in
         * each gap. It accumulates against the current margin and stops once the two are within a
         * pixel, which is what keeps it from oscillating across layout passes.</p>
         */
        private void balanceStackedGaps() {
            CharSequence weekdayText = weekdayLabel.getText();
            CharSequence lunarText = lunarLabel.getText();
            if (weekdayText.length() == 0 || lunarText.length() == 0) return;
            if (weekdayLabel.getHeight() == 0 || lunarLabel.getHeight() == 0) return;
            float numberInk = number.inkHeight();
            if (numberInk <= 0f) return;

            Paint paint = lunarLabel.getPaint();
            String weekday = weekdayText.toString();
            paint.getTextBounds(weekday, 0, weekday.length(), labelInk);
            float weekdayInkBottom =
                    weekdayLabel.getTop() + weekdayLabel.getBaseline() + labelInk.bottom;
            String lunar = lunarText.toString();
            paint.getTextBounds(lunar, 0, lunar.length(), labelInk);
            float lunarInkTop = lunarLabel.getTop() + lunarLabel.getBaseline() + labelInk.top;

            float numberCenter = number.getTop() + number.getHeight() / 2f;
            float gapAbove = numberCenter - numberInk / 2f - weekdayInkBottom;
            float gapBelow = lunarInkTop - (numberCenter + numberInk / 2f);
            float delta = gapBelow - gapAbove;
            if (Math.abs(delta) < 1f) return;

            LayoutParams params = (LayoutParams) weekdayLabel.getLayoutParams();
            int margin = Math.max(0, Math.round(params.bottomMargin + delta));
            if (params.bottomMargin == margin) return;
            params.bottomMargin = margin;
            weekdayLabel.setLayoutParams(params);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            // Only a laid-out cell knows its baselines, so the balance is settled here rather
            // than when the text or the size is set.
            if (!rail) balanceStackedGaps();
        }
    }

    // endregion
    // region text helpers

    private static boolean empty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static void append(StringBuilder value, String part) {
        if (empty(part)) return;
        if (value.length() > 0) value.append(" · ");
        value.append(part);
    }

    private static String join(String first, String second) {
        if (empty(first)) return second;
        if (empty(second)) return first;
        return first + " " + second;
    }

    // endregion
}
