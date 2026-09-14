package com.clockmods.pro;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.CalendarMonth;
import com.clockmods.calendar.HolidayRepository;
import com.clockmods.pro.style.CalendarClockState;
import com.clockmods.pro.style.CalendarLayout;
import com.clockmods.pro.style.CalendarLayoutCapabilities.Capability;
import com.clockmods.pro.style.CalendarLayoutHost;
import com.clockmods.pro.style.CalendarPageState;
import com.clockmods.pro.style.CalendarPager;
import com.clockmods.pro.style.CalendarStyle;
import com.clockmods.pro.style.UltimateCalendarStyles;
import com.clockmods.ui.ClockTimeFormatter;
import com.clockmods.ui.DateFormatter;
import com.clockmods.weather.DailyForecastController;
import com.clockmods.weather.WeatherController;
import com.clockmods.weather.WeatherModels;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Hosts whichever {@link CalendarStyle} the user picked. The fragment owns everything that outlives
 * a composition — preferences, the time zone, the ticker, the weather pollers, the holiday and
 * almanac lookups, the visible/selected dates and the page-animation guard — and hands the chosen
 * {@link CalendarLayout} nothing but pre-formatted strings.
 */
public final class ProCalendarFragment extends Fragment implements CalendarLayoutHost {
    private static final String STATE_MONTH = "visible_month";
    private static final String STATE_SELECTED = "selected_date";
    private static final int MIN_YEAR = 1901;
    private static final int MAX_YEAR = 2099;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Calendar visibleMonth = Calendar.getInstance();
    private final Calendar selectedDate = Calendar.getInstance();
    private final LunarAlmanac.Cache almanacCache = new LunarAlmanac.Cache();
    private final Runnable timeTicker = new Runnable() {
        @Override public void run() {
            updateTime();
            handler.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L);
        }
    };

    private FrameLayout styleHost;
    private CalendarStyle style;
    private CalendarLayout layout;
    private CalendarPageState pageState;

    private ClockPreferences preferences;
    private HolidayRepository holidayRepository;
    private WeatherController weatherController;
    private DailyForecastController forecastController;
    private WeatherModels.WeatherState lastWeatherState;
    private WeatherModels.DailyForecastState lastForecastState;

    private boolean resumed;
    private boolean monthAnimating;
    // Bumped whenever a page animation starts or is aborted, so a deferred end action can tell
    // whether it was superseded by a direct jump and must skip its month offset.
    private int monthAnimationEpoch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_calendar, container, false);
        styleHost = root.findViewById(R.id.calendar_style_host);
        preferences = new ClockPreferences(requireContext());
        holidayRepository = new HolidayRepository(requireContext());
        restoreState(savedInstanceState);
        weatherController = new WeatherController(requireContext(), state -> {
            lastWeatherState = state;
            if (layout != null) layout.bindWeather(state);
        }, true);
        forecastController = new DailyForecastController(requireContext(), state -> {
            lastForecastState = state;
            if (layout != null) layout.bindForecast(state);
        });
        refreshSettings(root);
        return root;
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
        if (layout != null) layout.setActive(true);
        handler.post(timeTicker);
        startWeatherIfEnabled();
    }

    @Override public void onPause() {
        resumed = false;
        handler.removeCallbacks(timeTicker);
        if (layout != null) layout.setActive(false);
        if (weatherController != null) weatherController.stop();
        if (forecastController != null) forecastController.stop();
        super.onPause();
    }

    @Override public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (layout != null) {
            layout.setActive(false);
            layout.onDestroyView();
        }
        layout = null;
        style = null;
        styleHost = null;
        pageState = null;
        almanacCache.clear();
        if (weatherController != null) weatherController.shutdown();
        if (forecastController != null) forecastController.shutdown();
        weatherController = null;
        forecastController = null;
        lastWeatherState = null;
        lastForecastState = null;
        super.onDestroyView();
    }

    // region settings

    void refreshSettings() {
        View root = getView();
        if (root == null) return;
        refreshSettings(root);
    }

    private void refreshSettings(View root) {
        preferences = new ClockPreferences(requireContext());
        installStyle(preferences.getCalendarTheme());
        BackgroundRepository background = new BackgroundRepository(requireContext());
        layout.applySettings(style.getTheme(), preferences, background);
        TimeZone zone = appTimeZone();
        visibleMonth.setTimeZone(zone);
        selectedDate.setTimeZone(zone);
        pageState = buildPageState(visibleMonth);
        layout.bindWeekdays(pageState);
        updateTime();
        layout.bind(pageState);
        ProFontApplier.apply(root, ClockPreferences.calendarScope(style.getTheme().id));
        // Re-assert the bold-aware typefaces after the global font pass: ProFontApplier caches each
        // view's first-seen weight and would otherwise revert the bold-text toggle.
        layout.applyTypefaces();
        if (lastWeatherState != null) layout.bindWeather(lastWeatherState);
        if (lastForecastState != null) layout.bindForecast(lastForecastState);
        layout.applyResponsiveSizing();
        if (resumed) startWeatherIfEnabled();
    }

    /** Swaps in the composition for {@code styleId}, or leaves the current one alone if unchanged. */
    private void installStyle(String styleId) {
        CalendarStyle resolved = UltimateCalendarStyles.sharedRegistry()
                .resolveForApi(styleId, Build.VERSION.SDK_INT);
        if (layout != null && resolved.getMetadata().equals(style.getMetadata())) return;
        // An in-flight page animation must not settle onto the composition that replaces it. Bump
        // the epoch rather than calling abortMonthAnimation(), which would reset pages through the
        // layout being torn down.
        monthAnimationEpoch++;
        monthAnimating = false;
        if (layout != null) {
            layout.setActive(false);
            layout.onDestroyView();
            styleHost.removeAllViews();
        }
        style = resolved;
        layout = resolved.newLayout();
        styleHost.addView(layout.onCreateView(getLayoutInflater(), styleHost, this),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        if (resumed) layout.setActive(true);
    }

    void onCalendarDestinationEntered(boolean fromAnotherDestination) {
        if (fromAnotherDestination) resetToToday();
    }

    void onCalendarDestinationExited() {
        if (weatherController != null) weatherController.stop();
        if (forecastController != null) forecastController.stop();
    }

    private void startWeatherIfEnabled() {
        if (weatherController == null || forecastController == null || style == null) return;
        // Pure-calendar presets hide the cards the readings would land in, so don't poll for them.
        if (!style.getMetadata().getCapabilities().supports(Capability.WEATHER)
                || !preferences.isWeatherEnabled()) {
            weatherController.stop();
            forecastController.stop();
            return;
        }
        weatherController.start(preferences.getWeatherIntervalMinutes());
        forecastController.start();
    }

    // endregion
    // region clock

    private void updateTime() {
        if (layout == null) return;
        TimeZone zone = appTimeZone();
        Calendar now = Calendar.getInstance(zone);
        boolean use24Hour = preferences.isUse24Hour();
        // The formatters must honour the app time zone as well; otherwise an absolute Date is
        // rendered in the device default zone while the AM/PM period below uses the app zone.
        SimpleDateFormat timeFormat =
                new SimpleDateFormat(use24Hour ? "HH:mm" : "hh:mm", Locale.CHINA);
        timeFormat.setTimeZone(zone);
        SimpleDateFormat secondsFormat = new SimpleDateFormat(":ss", Locale.CHINA);
        secondsFormat.setTimeZone(zone);
        layout.bindClock(new CalendarClockState(
                timeFormat.format(now.getTime()),
                secondsFormat.format(now.getTime()),
                preferences.isShowSeconds(),
                use24Hour ? "" : ClockTimeFormatter.periodText(
                        now.get(Calendar.HOUR_OF_DAY), preferences.isClockUseEnglish()),
                !use24Hour));
    }

    // endregion
    // region page state

    /**
     * Builds the page for the window {@code anchor} falls in. Which window that is comes from the
     * layout: a month grid pages by month, 周程 by week, 老黄历 by day. Everything downstream — the
     * selection index, the adjacent page, the swipe step — follows the same unit, so a style only
     * has to declare {@link CalendarPager#unit()} once.
     */
    private CalendarPageState buildPageState(Calendar anchor) {
        TimeZone zone = appTimeZone();
        boolean english = preferences.isClockUseEnglish();
        boolean highlightWeekends = preferences.isCalendarHighlightWeekends();
        int firstDayOfWeek = preferences.getCalendarWeekStart();
        CalendarPager.PageUnit unit = pageUnit();
        long now = System.currentTimeMillis();
        int anchorYear = anchor.get(Calendar.YEAR);
        int anchorMonth = anchor.get(Calendar.MONTH);
        int anchorDay = anchor.get(Calendar.DAY_OF_MONTH);
        CalendarMonth calendarMonth;
        switch (unit) {
            case DAY:
                calendarMonth = CalendarMonth.createDay(anchorYear, anchorMonth, anchorDay, zone,
                        now);
                break;
            case WEEK:
                calendarMonth = CalendarMonth.createWeek(anchorYear, anchorMonth, anchorDay, zone,
                        now, firstDayOfWeek);
                break;
            default:
                calendarMonth = CalendarMonth.create(anchorYear, anchorMonth, zone, now,
                        firstDayOfWeek);
                break;
        }
        List<CalendarPageState.DayInfo> days = new ArrayList<>(calendarMonth.days.size());
        Set<CalendarPageState.DayDetail> details = layout.requiredDayDetails();
        int selectedIndex = -1;
        for (int index = 0; index < calendarMonth.days.size(); index++) {
            CalendarMonth.Day day = calendarMonth.days.get(index);
            if (sameDate(day, selectedDate)) selectedIndex = index;
            days.add(buildDayInfo(day, english, details));
        }
        String[] weekdayNames = new String[7];
        boolean[] weekdayWeekend = new boolean[7];
        String[] names = getResources().getStringArray(R.array.calendar_weekday_names);
        for (int offset = 0; offset < 7; offset++) {
            int dayOfWeek = (firstDayOfWeek - Calendar.SUNDAY + offset) % 7 + Calendar.SUNDAY;
            weekdayNames[offset] = names[dayOfWeek - Calendar.SUNDAY];
            weekdayWeekend[offset] = isWeekend(dayOfWeek);
        }
        DateFormatter.Lang lang =
                english ? DateFormatter.Lang.ENGLISH : DateFormatter.Lang.CHINESE;
        // The masthead halves use the spelled-out month (八月 / August) rather than the toolbar's
        // numeric form, because a wordmark set in digits reads as a label, not as typography.
        CalendarPageState.Title title = new CalendarPageState.Title(
                DateFormatter.format(english ? "MMMM yyyy" : "yyyy年M月", anchor, lang),
                DateFormatter.format(english ? "MMMM" : "MMMM月", anchor, lang),
                DateFormatter.format("yyyy", anchor, lang),
                titleSub(unit, anchor, lang));
        return new CalendarPageState(calendarMonth.columns, days, buildSelection(selectedIndex),
                title, weekdayNames, weekdayWeekend, highlightWeekends);
    }

    /**
     * The qualifier under the heading: which week of the year a week page shows, or which weekday a
     * day page shows. A month page's heading already names its whole window, so it gets nothing.
     */
    private String titleSub(CalendarPager.PageUnit unit, Calendar anchor, DateFormatter.Lang lang) {
        if (unit == CalendarPager.PageUnit.WEEK) {
            Calendar counter = (Calendar) anchor.clone();
            counter.setFirstDayOfWeek(preferences.getCalendarWeekStart());
            // Week 1 is whichever week owns 1 January, which is the convention 第N周 is read with
            // rather than the four-day rule Calendar applies for some locales.
            counter.setMinimalDaysInFirstWeek(1);
            return getString(R.string.calendar_week_of_year, counter.get(Calendar.WEEK_OF_YEAR));
        }
        if (unit == CalendarPager.PageUnit.DAY) {
            return DateFormatter.format("EEEE", anchor, lang);
        }
        return "";
    }

    /**
     * Resolves one day to the depth {@code details} asks for. A layout that wants no lunar text pays
     * no almanac construction, and one that wants no statutory arrangement pays no holiday lookup —
     * which is what makes the poster style cost less per page than the dashboard rather than more.
     */
    private CalendarPageState.DayInfo buildDayInfo(CalendarMonth.Day day, boolean english,
            Set<CalendarPageState.DayDetail> details) {
        boolean wantsLunar = details.contains(CalendarPageState.DayDetail.LUNAR);
        boolean wantsFestivals = details.contains(CalendarPageState.DayDetail.FESTIVALS);
        LunarAlmanac almanac = wantsLunar || wantsFestivals
                ? almanacCache.get(day.year, day.month, day.dayOfMonth) : null;
        String lunarShort = wantsLunar ? almanac.shortLabel() : "";
        List<String> festivals = wantsFestivals ? almanac.festivals()
                : Collections.<String>emptyList();
        HolidayRepository.HolidayStatus status =
                details.contains(CalendarPageState.DayDetail.HOLIDAY)
                        ? holidayRepository.statusOn(dateKey(day.year, day.month, day.dayOfMonth))
                        : null;
        // "农历 " with nothing after it is worse than saying only the date, so the plain wording is
        // a separate string rather than the lunar one with an empty argument.
        String description = wantsLunar
                ? getString(day.today
                                ? R.string.calendar_day_today_accessibility
                                : R.string.calendar_day_accessibility,
                        day.dayOfMonth, lunarShort)
                : getString(day.today
                                ? R.string.calendar_day_today_number_accessibility
                                : R.string.calendar_day_number_accessibility,
                        day.dayOfMonth);
        if (!festivals.isEmpty()) {
            String separator = english ? ", " : "，";
            description += separator + android.text.TextUtils.join(separator, festivals);
        }
        if (status != null) {
            description += getString(status.offDay
                    ? R.string.calendar_day_rest : R.string.calendar_day_makeup);
        }
        CalendarPageState.DayInfo.Builder builder = new CalendarPageState.DayInfo.Builder(
                day.year, day.month, day.dayOfMonth, day.dayOfWeek, day.currentMonth, day.today,
                isWeekend(day.dayOfWeek), String.valueOf(day.dayOfMonth))
                .lunar(lunarShort)
                .festivals(festivals)
                .contentDescription(description);
        if (status != null) {
            builder.holiday(status.name, getString(status.offDay
                    ? R.string.calendar_day_status_off : R.string.calendar_day_status_work),
                    status.offDay);
        }
        return builder.build();
    }

    private CalendarPageState.Selection buildSelection(int index) {
        Set<CalendarPageState.DayDetail> details = layout.requiredSelectionDetails();
        // A layout with no footer wants no lines under it; skip the almanac entirely rather than
        // formatting three strings nothing will read.
        if (details.isEmpty()) return new CalendarPageState.Selection(index, "", "", "");
        boolean english = preferences.isClockUseEnglish();
        LunarAlmanac almanac = almanacCache.get(selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
        String datePattern =
                english ? preferences.getDatePatternEn() : preferences.getDatePatternCn();
        String formattedDate = DateFormatter.format(datePattern, selectedDate,
                com.clockmods.LocaleManager.dateLang(preferences.getClockLanguage()));
        String dateLine = getString(R.string.calendar_selected_date, formattedDate,
                almanac.naturalLabel());
        boolean wantsTaboo = details.contains(CalendarPageState.DayDetail.ALMANAC_TABOO);
        List<String> suitable = wantsTaboo ? almanac.suitable() : Collections.<String>emptyList();
        List<String> avoid = wantsTaboo ? almanac.avoid() : Collections.<String>emptyList();
        return new CalendarPageState.Selection(index, dateLine,
                suitable.isEmpty() ? "" : getString(R.string.calendar_suitable_prefix)
                        + android.text.TextUtils.join(" ", suitable),
                avoid.isEmpty() ? "" : getString(R.string.calendar_avoid_prefix)
                        + android.text.TextUtils.join(" ", avoid));
    }

    private CalendarPageState buildAdjacentPageState(int direction) {
        Calendar adjacent = (Calendar) visibleMonth.clone();
        CalendarPager.PageUnit unit = pageUnit();
        if (unit == CalendarPager.PageUnit.MONTH) {
            adjacent.add(Calendar.MONTH, direction);
            return buildPageState(adjacent);
        }
        // A week or day preview is mostly a rendering of its selected day, so it has to show the
        // selection it is sliding towards rather than the one it is leaving. Move the selection for
        // the build and put it straight back: nothing has committed yet, and a drag that snaps back
        // has to find the old selection intact.
        adjacent.add(Calendar.DAY_OF_MONTH,
                direction * (unit == CalendarPager.PageUnit.WEEK ? 7 : 1));
        long previous = selectedDate.getTimeInMillis();
        selectedDate.setTimeInMillis(adjacent.getTimeInMillis());
        try {
            return buildPageState(adjacent);
        } finally {
            selectedDate.setTimeInMillis(previous);
        }
    }

    private void renderMonth() {
        if (layout == null) return;
        TimeZone zone = appTimeZone();
        visibleMonth.setTimeZone(zone);
        selectedDate.setTimeZone(zone);
        pageState = buildPageState(visibleMonth);
        layout.bind(pageState);
    }

    // endregion
    // region host callbacks

    @Override public void onDaySelected(CalendarPageState.DayInfo day) {
        boolean monthChanged = day.year != visibleMonth.get(Calendar.YEAR)
                || day.month != visibleMonth.get(Calendar.MONTH);
        boolean monthPage = pageUnit() == CalendarPager.PageUnit.MONTH;
        selectedDate.set(day.year, day.month, day.dayOfMonth);
        // Week and day windows are anchored on a date rather than on a month, and that date is the
        // selection: the anchor follows the tap so the next swipe steps on from the day the user is
        // actually looking at.
        if (!monthPage) visibleMonth.set(day.year, day.month, day.dayOfMonth);
        if (monthChanged) {
            abortMonthAnimation();
            // Switching to an adjacent month rebuilds the grid for the new month anyway. A week
            // straddling a month boundary has to rebuild too, or its heading would go on naming the
            // month it just left.
            if (monthPage) visibleMonth.set(day.year, day.month, 1);
            renderMonth();
        } else if (pageState != null && layout != null) {
            // Same month: only move the highlight and refresh the footer so the per-cell
            // lunar/festival carousels keep running uninterrupted.
            pageState = pageState.withSelection(buildSelection(indexOf(pageState, day)));
            layout.updateSelection(pageState);
        }
    }

    @Override public void onPageRequested(int direction) {
        animateMonthChange(direction);
    }

    @Override public void onTodayRequested() {
        resetToToday();
    }

    @Override public void onMonthPickerRequested() {
        showMonthPicker();
    }

    @Override public void onAttributionClicked() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.qweather.com")));
    }

    @Override public void onNavigationRequested() {
        if (getActivity() instanceof ProMainActivity) {
            ((ProMainActivity) getActivity()).showChromeTemporarily();
        }
    }

    @Override public void onPageDragged(float offsetPx) {
        if (monthAnimating) return;
        CalendarPager pager = pager();
        if (pager == null) return;
        int direction = offsetPx < 0f ? 1 : -1;
        if (pager.preparedDirection() != direction) {
            pager.bindPreview(direction, buildAdjacentPageState(direction));
        }
        pager.setPageOffset(offsetPx);
    }

    @Override public void onPageDragFinished(int direction) {
        if (direction == 0) animateMonthSnapBack();
        else animateMonthChange(direction);
    }

    /**
     * The child manager, not the activity's: a dialog a layout opens belongs to this page, so it
     * should go away with the page rather than outlive a style switch.
     */
    @Override public FragmentManager fragmentManager() { return getChildFragmentManager(); }

    // endregion
    // region paging

    @Nullable
    private CalendarPager pager() {
        return layout == null ? null : layout.getPager();
    }

    /** A style with no pager still builds pages; it just never leaves the one it starts on. */
    private CalendarPager.PageUnit pageUnit() {
        CalendarPager pager = pager();
        return pager == null ? CalendarPager.PageUnit.MONTH : pager.unit();
    }

    private void applyPageOffset(int offset) {
        CalendarPager.PageUnit unit = pageUnit();
        if (unit == CalendarPager.PageUnit.MONTH) {
            int targetDay = selectedDate.get(Calendar.DAY_OF_MONTH);
            visibleMonth.add(Calendar.MONTH, offset);
            selectedDate.set(Calendar.YEAR, visibleMonth.get(Calendar.YEAR));
            selectedDate.set(Calendar.MONTH, visibleMonth.get(Calendar.MONTH));
            selectedDate.set(Calendar.DAY_OF_MONTH,
                    Math.min(targetDay, visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)));
        } else {
            // Keeping anchor and selection locked together is what makes the same weekday come
            // round again a week later, and what makes a day page advance one day at a time.
            visibleMonth.add(Calendar.DAY_OF_MONTH,
                    offset * (unit == CalendarPager.PageUnit.WEEK ? 7 : 1));
            selectedDate.setTimeInMillis(visibleMonth.getTimeInMillis());
        }
        renderMonth();
        if (layout != null) layout.announcePage();
    }

    private void animateMonthChange(int direction) {
        CalendarPager pager = pager();
        if (pager == null || monthAnimating || direction == 0 || pager.pageWidth() == 0f) return;
        if (pager.preparedDirection() != direction) {
            pager.bindPreview(direction, buildAdjacentPageState(direction));
        }
        monthAnimating = true;
        final int epoch = ++monthAnimationEpoch;
        pager.animateCommit(direction, () -> {
            // A direct jump (today / picker / adjacent-month day) during the animation bumps the
            // epoch and already re-rendered; skip the offset to avoid advancing the month twice.
            if (epoch != monthAnimationEpoch) return;
            applyPageOffset(direction);
            resetPages();
            monthAnimating = false;
        });
    }

    private void animateMonthSnapBack() {
        CalendarPager pager = pager();
        if (pager == null || monthAnimating) return;
        pager.animateSnapBack(this::resetPages);
    }

    private void resetPages() {
        CalendarPager pager = pager();
        if (pager != null) pager.resetPages();
    }

    /**
     * Aborts any in-flight page animation before a direct jump re-renders the page, so the
     * animation's deferred end action cannot advance the month a second time.
     */
    private void abortMonthAnimation() {
        monthAnimationEpoch++;
        monthAnimating = false;
        resetPages();
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
        if (layout != null) layout.announcePage();
    }

    // endregion
    // region helpers

    private static int indexOf(CalendarPageState state, CalendarPageState.DayInfo day) {
        for (int index = 0; index < state.days.size(); index++) {
            CalendarPageState.DayInfo candidate = state.days.get(index);
            if (candidate.year == day.year && candidate.month == day.month
                    && candidate.dayOfMonth == day.dayOfMonth) {
                return index;
            }
        }
        return -1;
    }

    private TimeZone appTimeZone() {
        String id = preferences == null ? "" : preferences.getTimeZoneId();
        return id == null || id.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(id);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    // endregion
}
