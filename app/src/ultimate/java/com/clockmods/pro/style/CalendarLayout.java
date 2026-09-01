package com.clockmods.pro.style;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.pro.CalendarTheme;
import com.clockmods.weather.WeatherModels;

import java.util.Set;

/**
 * One calendar composition: it owns an entire view subtree and everything about how that subtree
 * looks and measures.
 *
 * <p>Unlike {@code ClockRenderer}, which is stateless and draws onto a Canvas handed to it, a
 * calendar layout is stateful and holds view references. The calendar has 42 hit targets, 42
 * accessibility nodes and 42 phase-shared carousels; reimplementing those on Canvas would mean
 * hand-writing hit testing and an {@code ExploreByTouchHelper}, and would lose the frugal
 * {@code CalendarCarouselTimeline} scheduling. So one instance per host, created by
 * {@link CalendarStyle#newLayout()}.</p>
 */
public interface CalendarLayout {
    /** Inflates the subtree. Called once; the returned view is added to the page's host frame. */
    View onCreateView(LayoutInflater inflater, ViewGroup container, CalendarLayoutHost host);

    /**
     * Re-applies the palette and every preference-derived tint, visibility and typeface. Called
     * whenever settings change, before {@link #bind}.
     */
    void applySettings(CalendarTheme theme, ClockPreferences preferences,
            BackgroundRepository background);

    /**
     * Re-asserts typefaces after the host's global font pass. {@code ProFontApplier} caches each
     * view's first-seen weight, so anything whose weight depends on a setting must be set again
     * here or the bold-text toggle silently reverts.
     */
    void applyTypefaces();

    /** Rebuilds the weekday header. Separate from {@link #bind} because it survives page turns. */
    void bindWeekdays(CalendarPageState state);

    /** Rebuilds the page: title, cells and footer. */
    void bind(CalendarPageState state);

    /**
     * Moves the selection within the page already bound. Must stay cheap and must not rebuild
     * cells — an in-progress lunar/festival rotation has to keep running.
     */
    void updateSelection(CalendarPageState state);

    /** Re-derives every size from the layout's own measured geometry. */
    void applyResponsiveSizing();

    /** Starts or stops the status capsule and every carousel; driven by the host's lifecycle. */
    void setActive(boolean active);

    void bindClock(CalendarClockState clock);

    void bindWeather(WeatherModels.WeatherState state);

    void bindForecast(WeatherModels.DailyForecastState state);

    /** Speaks the page heading after a jump, for users who cannot see the page change. */
    void announcePage();

    /** What the host must resolve for every day in a page. */
    Set<CalendarPageState.DayDetail> requiredDayDetails();

    /** What the host must resolve for the selected day only. */
    Set<CalendarPageState.DayDetail> requiredSelectionDetails();

    /** Null unless the layout declares {@link CalendarLayoutCapabilities.Capability#PAGE_SWIPE}. */
    @Nullable CalendarPager getPager();

    void onDestroyView();
}
