package com.clockmods.pro;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

final class ProPagerAdapter extends FragmentStateAdapter {
    ProPagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ProPage page = ProPage.values()[position];
        if (page == ProPage.CLOCK) {
            return new ProClockFragment();
        }
        if (page == ProPage.CALENDAR) {
            return new ProCalendarFragment();
        }
        if (page == ProPage.POMODORO) {
            return ProTimerFragment.newPomodoroInstance();
        }
        if (page == ProPage.ALARM) {
            return new ProAlarmFragment();
        }
        if (page == ProPage.COUNTDOWN) {
            return ProTimerFragment.newCountdownInstance();
        }
        if (page == ProPage.STOPWATCH) {
            return new ProStopwatchFragment();
        }
        return ProPlaceholderFragment.newInstance(page.titleRes);
    }

    @Override
    public int getItemCount() {
        return ProPage.values().length;
    }
}