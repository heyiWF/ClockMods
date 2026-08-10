package com.clockmods.pro;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.TypedValue;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.widget.TextViewCompat;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.CalendarMonth;
import com.clockmods.calendar.HolidayRepository;
import com.clockmods.ui.CalendarFooterCarouselView;
import com.clockmods.ui.CalendarLabelCarouselView;
import com.clockmods.ui.ClockTimeFormatter;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.ui.DateFormatter;
import com.clockmods.ui.StatusBarView;
import com.clockmods.ui.WeatherIconView;
import com.clockmods.weather.DailyForecastController;
import com.clockmods.weather.WeatherController;
import com.clockmods.weather.WeatherModels;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ProCalendarFragment extends Fragment {
    private static final String STATE_MONTH = "visible_month";
    private static final String STATE_SELECTED = "selected_date";
    // These only keep the quick-jump picker compact. Month paging is intentionally not clamped;
    // the algorithmic lunar engine and full month rendering are regression-tested outside them.
    private static final int MIN_YEAR = 1901;
    private static final int MAX_YEAR = 2099;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Calendar visibleMonth = Calendar.getInstance();
    private final Calendar selectedDate = Calendar.getInstance();
    private final Runnable timeTicker = new Runnable() {
        @Override public void run() {
            updateTime();
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L);
        }
    };
    private TextView timeView, secondsView, periodView, title, currentTemperature, feelsLike, weatherSummary;
    private CalendarFooterCarouselView footer;
    private GridLayout weekdayGrid, grid, previewGrid;
    private CalendarMonth visibleCalendar;
    private StatusBarView statusBar;
    private WeatherIconView currentWeatherIcon;
    private View currentWeatherCard, forecastCard, attribution;
    private MonthGestureLayout monthPanel;
    private LinearLayout[] forecastColumns;
    private ClockPreferences preferences;
    private WeatherController weatherController;
    private DailyForecastController forecastController;
    private HolidayRepository holidayRepository;
    private final List<CalendarLabelCarouselView> labelCarousels = new ArrayList<>();
    private boolean resumed;
    private boolean monthAnimating;
    private int previewDirection;
    // Bumped whenever a page animation starts or is aborted, so a deferred end action can tell
    // whether it was superseded by a direct jump and must skip its month offset.
    private int monthAnimationEpoch;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_calendar, container, false);
        preferences = new ClockPreferences(requireContext());
        holidayRepository = new HolidayRepository(requireContext());
        bindViews(root);
        restoreState(savedInstanceState);
        configureViews(root);
        weatherController = new WeatherController(requireContext(), this::bindCurrentWeather, true);
        forecastController = new DailyForecastController(requireContext(), this::bindForecast);
        refreshSettings(root);
        return root;
    }

    private void bindViews(View root) {
        timeView = root.findViewById(R.id.calendar_time);
        secondsView = root.findViewById(R.id.calendar_seconds);
        periodView = root.findViewById(R.id.calendar_period);
        title = root.findViewById(R.id.calendar_month_title);
        footer = root.findViewById(R.id.calendar_selected_footer);
        weekdayGrid = root.findViewById(R.id.calendar_weekdays);
        grid = root.findViewById(R.id.calendar_grid);
        previewGrid = root.findViewById(R.id.calendar_grid_preview);
        statusBar = root.findViewById(R.id.calendar_status_bar);
        currentWeatherIcon = root.findViewById(R.id.calendar_current_weather_icon);
        currentTemperature = root.findViewById(R.id.calendar_current_temperature);
        feelsLike = root.findViewById(R.id.calendar_feels_like);
        weatherSummary = root.findViewById(R.id.calendar_weather_summary);
        currentWeatherCard = root.findViewById(R.id.calendar_current_weather_card);
        forecastCard = root.findViewById(R.id.calendar_forecast_card);
        attribution = root.findViewById(R.id.calendar_weather_attribution);
        monthPanel = root.findViewById(R.id.calendar_month_panel);
        forecastColumns = new LinearLayout[] {root.findViewById(R.id.calendar_forecast_today),
                root.findViewById(R.id.calendar_forecast_tomorrow),
                root.findViewById(R.id.calendar_forecast_after_tomorrow)};
    }

    private void configureViews(View root) {
        title.setAccessibilityHeading(true);
        root.findViewById(R.id.calendar_previous).setOnClickListener(view -> animateMonthChange(-1));
        root.findViewById(R.id.calendar_next).setOnClickListener(view -> animateMonthChange(1));
        root.findViewById(R.id.calendar_today).setOnClickListener(view -> resetToToday());
        title.setOnClickListener(view -> showMonthPicker());
        title.setTooltipText(getString(R.string.calendar_jump_title));
        attribution.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.qweather.com"))));
        monthPanel.setMonthGestureListener(new MonthGestureLayout.Listener() {
            @Override public void onMonthDrag(float offset) {
                if (monthAnimating) return;
                int direction = offset < 0f ? 1 : -1;
                prepareMonthPreview(direction);
                grid.setTranslationX(offset);
                previewGrid.setTranslationX(offset + direction * grid.getWidth());
            }
            @Override public void onMonthDragFinished(int direction) {
                if (direction == 0) animateMonthSnapBack();
                else animateMonthChange(direction);
            }
        });
    }

    private void restoreState(Bundle state) {
        long now = System.currentTimeMillis();
        visibleMonth.setTimeInMillis(state == null ? now : state.getLong(STATE_MONTH, now));
        selectedDate.setTimeInMillis(state == null ? now : state.getLong(STATE_SELECTED, now));
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_MONTH, visibleMonth.getTimeInMillis());
        outState.putLong(STATE_SELECTED, selectedDate.getTimeInMillis());
    }

    @Override public void onResume() {
        super.onResume();
        resumed = true;
        statusBar.start();
        setCarouselsActive(true);
        handler.post(timeTicker);
        startWeatherIfEnabled();
    }

    @Override public void onPause() {
        resumed = false;
        handler.removeCallbacks(timeTicker);
        setCarouselsActive(false);
        statusBar.stop();
        if (weatherController != null) weatherController.stop();
        if (forecastController != null) forecastController.stop();
        super.onPause();
    }

    @Override public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (weatherController != null) weatherController.shutdown();
        if (forecastController != null) forecastController.shutdown();
        weatherController = null;
        forecastController = null;
        super.onDestroyView();
    }

    void refreshSettings() {
        View root = getView();
        if (root == null) return;
        refreshSettings(root);
    }

    private void refreshSettings(View root) {
        preferences = new ClockPreferences(requireContext());
        BackgroundRepository background = new BackgroundRepository(requireContext());
        statusBar.setBackgroundRepository(background);
        statusBar.setContentAlignedStart(true);
        statusBar.setVisibility(preferences.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        Typeface typeface = ClockTypefaceResolver.resolveTime(requireContext(),
                preferences.getFontFamily(), preferences.isBoldText());
        timeView.setTextColor(preferences.getTimeColor());
        currentWeatherIcon.setIconColor(getColor(R.color.calendar_dashboard_blue));
        boolean weatherEnabled = preferences.isWeatherEnabled();
        currentWeatherCard.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
        forecastCard.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
        attribution.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
        populateWeekdays();
        updateTime();
        renderMonth();
        ProFontApplier.apply(root);
        // Re-assert the bold-aware time typefaces after the global font pass: ProFontApplier
        // caches each view's first-seen weight and would otherwise revert the bold-text toggle.
        timeView.setTypeface(typeface);
        secondsView.setTypeface(typeface);
        periodView.setTypeface(typeface);
        if (resumed) startWeatherIfEnabled();
    }

    void onCalendarDestinationEntered(boolean fromAnotherDestination) {
        if (fromAnotherDestination) resetToToday();
    }

    void onCalendarDestinationExited() {
        if (weatherController != null) weatherController.stop();
        if (forecastController != null) forecastController.stop();
    }

    private void startWeatherIfEnabled() {
        if (!preferences.isWeatherEnabled() || weatherController == null || forecastController == null) return;
        weatherController.start(preferences.getWeatherIntervalMinutes());
        forecastController.start();
    }

    private void updateTime() {
        if (timeView == null) return;
        TimeZone zone = appTimeZone();
        Calendar now = Calendar.getInstance(zone);
        boolean use24Hour = preferences.isUse24Hour();
        String pattern = use24Hour ? "HH:mm" : "hh:mm";
        // The formatters must honour the app time zone as well; otherwise an absolute Date is
        // rendered in the device default zone while the AM/PM period below uses the app zone.
        SimpleDateFormat timeFormat = new SimpleDateFormat(pattern, Locale.CHINA);
        timeFormat.setTimeZone(zone);
        timeView.setText(timeFormat.format(now.getTime()));
        secondsView.setVisibility(preferences.isShowSeconds() ? View.VISIBLE : View.GONE);
        SimpleDateFormat secondsFormat = new SimpleDateFormat(":ss", Locale.CHINA);
        secondsFormat.setTimeZone(zone);
        secondsView.setText(secondsFormat.format(now.getTime()));
        if (periodView != null) {
            periodView.setVisibility(use24Hour ? View.GONE : View.VISIBLE);
            if (!use24Hour) {
                periodView.setText(ClockTimeFormatter.periodText(
                        now.get(Calendar.HOUR_OF_DAY), preferences.isClockUseEnglish()));
            }
        }
    }

    private void bindCurrentWeather(WeatherModels.WeatherState state) {
        if (currentTemperature == null) return;
        if (state.data == null) {
            weatherSummary.setText(state.message == null ? getString(R.string.calendar_forecast_loading) : state.message);
            return;
        }
        WeatherModels.WeatherDisplayData data = state.data;
        currentWeatherIcon.setIconCode(data.icon);
        currentTemperature.setText(data.temperature + " ℃");
        String feel = data.detail == null || empty(data.detail.feelsLike) ? "--" : data.detail.feelsLike;
        feelsLike.setText(feel + " ℃");
        StringBuilder summary = new StringBuilder(WeatherModels.locationText(data.city, data.district));
        append(summary, data.text);
        if (data.detail != null) {
            String scale = empty(data.detail.windScale) ? ""
                    : getString(R.string.weather_wind_scale_format, data.detail.windScale);
            append(summary, join(data.detail.windDir, scale));
            if (!empty(data.detail.humidity)) {
                append(summary, getString(R.string.weather_humidity_format, data.detail.humidity));
            }
        }
        weatherSummary.setText(summary.toString());
    }

    private void bindForecast(WeatherModels.DailyForecastState state) {
        if (forecastColumns == null) return;
        if (state.data == null) {
            for (LinearLayout column : forecastColumns) {
                column.removeAllViews();
                addForecastText(column, state.message == null
                    ? getString(R.string.calendar_forecast_loading) : state.message,
                    R.dimen.calendar_forecast_text_size, false);
            }
            return;
        }
        Calendar date = Calendar.getInstance(appTimeZone());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(appTimeZone());
        String[] forecastLabels = getResources().getStringArray(R.array.forecast_day_labels);
        for (int index = 0; index < forecastColumns.length; index++) {
            LinearLayout column = forecastColumns[index];
            column.removeAllViews();
            WeatherModels.DailyForecast forecast = state.data.findByDate(format.format(date.getTime()));
                addForecastText(column, forecastLabels[index],
                    R.dimen.calendar_forecast_label_size, index == 0);
            if (forecast != null) {
                WeatherIconView icon = new WeatherIconView(requireContext());
                icon.setIconColor(index == 0 ? getColor(R.color.calendar_dashboard_blue) : Color.WHITE);
                icon.setIconCode(forecast.iconDay);
                int iconSize = getResources().getDimensionPixelSize(R.dimen.calendar_forecast_icon_size);
                int iconGap = getResources().getDimensionPixelSize(R.dimen.calendar_forecast_icon_gap);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.gravity = Gravity.CENTER_HORIZONTAL;
                iconParams.setMargins(0, iconGap, 0, iconGap);
                column.addView(icon, iconParams);
                addForecastText(column, forecast.textDay, R.dimen.calendar_forecast_text_size, false);
                addForecastText(column, forecast.tempMin + " - " + forecast.tempMax + " ℃",
                        R.dimen.calendar_forecast_text_size, false);
            } else addForecastText(column, getString(R.string.calendar_forecast_unavailable),
                    R.dimen.calendar_forecast_text_size, false);
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addForecastText(LinearLayout parent, String text, int sizeRes, boolean accent) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(sizeRes));
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setSingleLine(true);
        view.setTextColor(accent ? getColor(R.color.calendar_dashboard_blue) : Color.WHITE);
        view.setTypeface(ClockTypefaceResolver.resolveTime(requireContext(),
            preferences.getFontFamily(), true));
        int maximumTextSize = getResources().getDimensionPixelSize(sizeRes);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(view,
                getResources().getDimensionPixelSize(R.dimen.calendar_forecast_min_text_size),
                maximumTextSize, 1, TypedValue.COMPLEX_UNIT_PX);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        parent.addView(view, params);
    }

    private void populateWeekdays() {
        weekdayGrid.removeAllViews();
        String[] weekdayNames = getResources().getStringArray(R.array.calendar_weekday_names);
        int firstDayOfWeek = preferences.getCalendarWeekStart();
        boolean highlightWeekends = preferences.isCalendarHighlightWeekends();
        for (int offset = 0; offset < 7; offset++) {
            int dayOfWeek = (firstDayOfWeek - Calendar.SUNDAY + offset) % 7
                    + Calendar.SUNDAY;
            TextView label = new TextView(requireContext());
            label.setText(weekdayNames[dayOfWeek - Calendar.SUNDAY]);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.calendar_weekday_text_size));
            label.setTextColor(getColor(highlightWeekends && isWeekend(dayOfWeek)
                    ? R.color.calendar_dashboard_weekend : R.color.calendar_dashboard_text));
            label.setTypeface(ClockTypefaceResolver.resolveTime(requireContext(),
                    preferences.getFontFamily(), false));
            weekdayGrid.addView(label, cellParams());
        }
    }

    private void applyMonthOffset(int offset) {
        int targetDay = selectedDate.get(Calendar.DAY_OF_MONTH);
        visibleMonth.add(Calendar.MONTH, offset);
        selectedDate.set(Calendar.YEAR, visibleMonth.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, visibleMonth.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH,
                Math.min(targetDay, visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)));
        renderMonth();
        title.announceForAccessibility(title.getText());
    }

    private void animateMonthChange(final int direction) {
        if (monthAnimating || direction == 0 || grid.getWidth() == 0) return;
        prepareMonthPreview(direction);
        if (!animationsEnabled()) {
            applyMonthOffset(direction);
            resetMonthLayers();
            return;
        }
        monthAnimating = true;
        final int epoch = ++monthAnimationEpoch;
        final float width = grid.getWidth();
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.animate().translationX(direction > 0 ? -width : width)
                .setDuration(210L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        previewGrid.animate().translationX(0f)
                .setDuration(210L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    // A direct jump (today / picker / adjacent-month day) during the animation
                    // bumps the epoch and already re-rendered; skip the offset to avoid advancing
                    // the month twice.
                    if (epoch != monthAnimationEpoch) return;
                    applyMonthOffset(direction);
                    resetMonthLayers();
                    monthAnimating = false;
                }).start();
    }

    private void animateMonthSnapBack() {
        if (monthAnimating) return;
        if (!animationsEnabled()) {
            resetMonthLayers();
            return;
        }
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.animate().translationX(0f).setDuration(180L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        if (previewDirection != 0) {
            previewGrid.animate().translationX(previewDirection * grid.getWidth())
                    .setDuration(180L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(this::resetMonthLayers).start();
        }
    }

    private void prepareMonthPreview(int direction) {
        if (previewDirection == direction && previewGrid.getVisibility() == View.VISIBLE) return;
        Calendar adjacent = (Calendar) visibleMonth.clone();
        adjacent.add(Calendar.MONTH, direction);
        CalendarMonth month = CalendarMonth.create(adjacent.get(Calendar.YEAR),
                adjacent.get(Calendar.MONTH), appTimeZone(), System.currentTimeMillis(),
                preferences.getCalendarWeekStart());
        previewGrid.removeAllViews();
        for (CalendarMonth.Day day : month.days) {
            previewGrid.addView(createDayCell(day, false), cellParams());
        }
        previewDirection = direction;
        previewGrid.setTranslationX(direction * grid.getWidth());
        previewGrid.setVisibility(View.VISIBLE);
    }

    private void resetMonthLayers() {
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.setTranslationX(0f);
        previewGrid.setVisibility(View.INVISIBLE);
        previewGrid.removeAllViews();
        previewDirection = 0;
    }

    /** Aborts any in-flight page animation before a direct jump re-renders the grid, so the
     *  animation's deferred end action cannot advance the month a second time. */
    private void abortMonthAnimation() {
        monthAnimationEpoch++;
        monthAnimating = false;
        resetMonthLayers();
    }

    private void resetToToday() {
        abortMonthAnimation();
        long now = System.currentTimeMillis();
        visibleMonth.setTimeInMillis(now);
        selectedDate.setTimeInMillis(now);
        renderMonth();
    }

    /** Prompts for a target year/month within the existing compact quick-jump range. */
    private void showMonthPicker() {
        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(MIN_YEAR);
        yearPicker.setMaxValue(MAX_YEAR);
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setValue(Math.min(MAX_YEAR,
                Math.max(MIN_YEAR, visibleMonth.get(Calendar.YEAR))));

        String[] monthLabels = new String[12];
        for (int index = 0; index < 12; index++) {
            monthLabels[index] = getString(R.string.calendar_month_short, index + 1);
        }
        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(monthLabels);
        monthPicker.setWrapSelectorWheel(true);
        monthPicker.setValue(visibleMonth.get(Calendar.MONTH));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(16), dp(16), dp(16), 0);
        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        content.addView(yearPicker, pickerParams);
        content.addView(monthPicker, pickerParams);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.calendar_jump_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        jumpToMonth(yearPicker.getValue(), monthPicker.getValue()))
                .show();
    }

    private void jumpToMonth(int year, int month0) {
        abortMonthAnimation();
        visibleMonth.set(year, month0, 1);
        selectedDate.set(year, month0, 1);
        renderMonth();
        title.announceForAccessibility(title.getText());
    }

    private void renderMonth() {
        if (grid == null) return;
        TimeZone zone = appTimeZone();
        visibleMonth.setTimeZone(zone); selectedDate.setTimeZone(zone);
        int year = visibleMonth.get(Calendar.YEAR), month = visibleMonth.get(Calendar.MONTH);
        boolean english = preferences.isClockUseEnglish();
        title.setText(DateFormatter.format(english ? "MMMM yyyy" : "yyyy年M月", visibleMonth,
                english ? DateFormatter.Lang.ENGLISH : DateFormatter.Lang.CHINESE));
        CalendarMonth calendarMonth = CalendarMonth.create(year, month, zone,
                System.currentTimeMillis(), preferences.getCalendarWeekStart());
        visibleCalendar = calendarMonth;
        grid.removeAllViews();
        labelCarousels.clear();
        for (CalendarMonth.Day day : calendarMonth.days) grid.addView(createDayCell(day, true), cellParams());
        updateFooter();
    }

    private View createDayCell(CalendarMonth.Day day, boolean interactive) {
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(1), dp(2), dp(1), dp(2)); cell.setAlpha(day.currentMonth ? 1f : 0.38f);
        String dateKey = dateKey(day.year, day.month, day.dayOfMonth);
        LunarAlmanac almanac = LunarAlmanac.of(day.year, day.month, day.dayOfMonth);
        HolidayRepository.HolidayStatus status = holidayRepository.statusOn(dateKey);
        CalendarDayNumberView solar = new CalendarDayNumberView(requireContext());
        Typeface calendarTypeface = ClockTypefaceResolver.resolveTime(requireContext(),
            preferences.getFontFamily(), false);
        solar.setTypefaces(calendarTypeface, calendarTypeface);
        int dayColor = Color.WHITE;
        if (preferences.isCalendarHighlightWeekends() && isWeekend(day.dayOfWeek)) {
            dayColor = getColor(R.color.calendar_dashboard_weekend);
        }
        if (day.today) {
            dayColor = getColor(R.color.calendar_dashboard_green);
        }
        solar.setDay(String.valueOf(day.dayOfMonth), dayColor);
        if (status != null) {
            solar.setBadge(status.offDay ? getString(R.string.calendar_day_status_off)
                    : getString(R.string.calendar_day_status_work), status.offDay
                ? getColor(R.color.calendar_dashboard_green)
                : getColor(R.color.calendar_dashboard_amber));
        }
        cell.addView(solar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        List<String> labels = almanac.festivals(preferences.isCalendarMoreFestivals());
        List<String> carouselItems = new ArrayList<>();
        carouselItems.add(almanac.shortLabel());
        carouselItems.addAll(labels);
        CalendarLabelCarouselView lunar = new CalendarLabelCarouselView(requireContext());
        lunar.setItems(carouselItems);
        lunar.setTextSizePx(getResources().getDimension(R.dimen.calendar_dashboard_lunar_size));
        lunar.setTextColor(labels.isEmpty() ? getColor(R.color.calendar_dashboard_secondary)
            : getColor(R.color.calendar_dashboard_text));
        lunar.setTypeface(calendarTypeface);
        lunar.setActive(interactive && resumed);
        if (interactive) labelCarousels.add(lunar);
        cell.addView(lunar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            getResources().getDimensionPixelSize(R.dimen.calendar_label_height)));
        String description = getString(day.today
                        ? R.string.calendar_day_today_accessibility
                        : R.string.calendar_day_accessibility,
                day.dayOfMonth, almanac.shortLabel());
        if (!labels.isEmpty()) {
            String sep = preferences.isClockUseEnglish() ? ", " : "，";
            description += sep + android.text.TextUtils.join(sep, labels);
        }
        if (status != null) {
            description += getString(status.offDay
                    ? R.string.calendar_day_rest : R.string.calendar_day_makeup);
        }
        cell.setTag(description);
        applyCellSelection(cell, day);
        cell.setClickable(interactive);
        cell.setFocusable(interactive);
        if (interactive) cell.setOnClickListener(view -> selectDay(day));
        return cell;
    }

    /** Applies (or clears) the selected-day highlight and accessibility suffix on a cell
     *  without touching its lunar/festival carousel, so an in-progress rotation is preserved. */
    private void applyCellSelection(View cell, CalendarMonth.Day day) {
        boolean selected = sameDate(day, selectedDate);
        int left = cell.getPaddingLeft(), top = cell.getPaddingTop();
        int right = cell.getPaddingRight(), bottom = cell.getPaddingBottom();
        cell.setBackgroundResource(selected ? R.drawable.calendar_selected_background : 0);
        cell.setPadding(left, top, right, bottom);
        Object base = cell.getTag();
        if (base instanceof String) {
            cell.setContentDescription(selected
                    ? base + getString(R.string.calendar_selected_suffix) : (String) base);
        }
    }

    /** Refreshes only the selection highlight across the visible grid, leaving every cell's
     *  carousel running (used when the selected day changes within the visible month). */
    private void updateSelection() {
        if (grid == null || visibleCalendar == null) return;
        int count = Math.min(grid.getChildCount(), visibleCalendar.days.size());
        for (int index = 0; index < count; index++) {
            applyCellSelection(grid.getChildAt(index), visibleCalendar.days.get(index));
        }
    }

    private void setCarouselsActive(boolean active) {
        for (CalendarLabelCarouselView carousel : labelCarousels) carousel.setActive(active);
        if (footer != null) footer.setActive(active);
    }

    private void selectDay(CalendarMonth.Day day) {
        boolean monthChanged = day.year != visibleMonth.get(Calendar.YEAR)
                || day.month != visibleMonth.get(Calendar.MONTH);
        selectedDate.set(day.year, day.month, day.dayOfMonth);
        if (monthChanged) {
            abortMonthAnimation();
            // Switching to an adjacent month rebuilds the grid for the new month anyway.
            visibleMonth.set(day.year, day.month, 1);
            renderMonth();
        } else {
            // Same month: only move the highlight and refresh the footer so the per-cell
            // lunar/festival carousels keep running uninterrupted.
            updateSelection();
            updateFooter();
        }
    }

    private void updateFooter() {
        boolean english = preferences.isClockUseEnglish();
        LunarAlmanac almanac = LunarAlmanac.of(selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
        String datePattern = english ? preferences.getDatePatternEn() : preferences.getDatePatternCn();
        String formattedDate = DateFormatter.format(datePattern, selectedDate,
                com.clockmods.LocaleManager.dateLang(preferences.getClockLanguage()));
        String dateLine = getString(R.string.calendar_selected_date,
                formattedDate, almanac.naturalLabel());
        footer.setTypeface(ClockTypefaceResolver.resolveTime(requireContext(),
                preferences.getFontFamily(), false));
        footer.setTextSizePx(getResources().getDimension(R.dimen.calendar_footer_text_size));
        List<CalendarFooterCarouselView.Item> items = new ArrayList<>();
        items.add(new CalendarFooterCarouselView.Item(dateLine,
                getColor(R.color.calendar_dashboard_text)));
        List<String> yi = almanac.suitable();
        List<String> ji = almanac.avoid();
        if (!yi.isEmpty()) {
            items.add(new CalendarFooterCarouselView.Item(
                    getString(R.string.calendar_suitable_prefix)
                            + android.text.TextUtils.join(" ", yi),
                    getColor(R.color.calendar_dashboard_green)));
        }
        if (!ji.isEmpty()) {
            items.add(new CalendarFooterCarouselView.Item(
                    getString(R.string.calendar_avoid_prefix)
                            + android.text.TextUtils.join(" ", ji),
                    getColor(R.color.calendar_dashboard_red)));
        }
        footer.setItems(items);
        footer.setActive(resumed);
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private TimeZone appTimeZone() {
        String id = preferences == null ? "" : preferences.getTimeZoneId();
        return id == null || id.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(id);
    }

    private int getColor(int id) { return requireContext().getColor(id); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static boolean empty(String value) { return value == null || value.trim().length() == 0; }
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
    private static String dateKey(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }
    private static boolean sameDate(CalendarMonth.Day day, Calendar value) {
        return day.year == value.get(Calendar.YEAR) && day.month == value.get(Calendar.MONTH)
                && day.dayOfMonth == value.get(Calendar.DAY_OF_MONTH);
    }

    private static boolean isWeekend(int dayOfWeek) {
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    private boolean animationsEnabled() {
        try {
            return Settings.Global.getFloat(requireContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException ignored) { return true; }
    }

}
