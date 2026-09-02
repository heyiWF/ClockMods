package com.clockmods.pro.style;

import androidx.fragment.app.FragmentManager;

/**
 * What a {@link CalendarLayout} may ask of the page that owns it. A layout never advances a date,
 * never decides whether animations run and never touches preferences — it reports intent and the
 * host answers with a new {@link CalendarPageState}.
 */
public interface CalendarLayoutHost {
    /** The user tapped a day cell. */
    void onDaySelected(CalendarPageState.DayInfo day);

    /** A button asked for the adjacent page; ±1 in the layout's own {@link CalendarPager#unit()}. */
    void onPageRequested(int direction);

    void onTodayRequested();

    void onMonthPickerRequested();

    void onAttributionClicked();

    /**
     * The user is dragging the page. Called for every frame of the gesture, so the host must not
     * rebuild the page here — see {@link CalendarPager#preparedDirection()}.
     *
     * @param offsetPx signed distance from rest; negative means the next page is coming in.
     */
    void onPageDragged(float offsetPx);

    /** The drag ended: ±1 to commit that direction, 0 to snap back. */
    void onPageDragFinished(int direction);

    /**
     * The user tapped a non-interactive area of the layout and expects the navigation bar to
     * reappear. Layouts that suppress the month-picker on their year/month display (poster,
     * paper) forward a tap there here instead.
     */
    void onNavigationRequested();

    /**
     * The manager a layout shows its own dialogs from. Material's pickers are {@code
     * DialogFragment}s rather than plain dialogs, so they need one to survive a rotation instead of
     * dying with the view that opened them.
     */
    FragmentManager fragmentManager();
}
