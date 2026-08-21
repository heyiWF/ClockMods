package com.clockmods.pro;

import com.clockmods.R;

enum ProPage {
    CLOCK(R.string.pro_page_clock, android.R.drawable.ic_lock_idle_alarm),
    CALENDAR(R.string.pro_page_calendar, android.R.drawable.ic_menu_month),
    POMODORO(R.string.pro_page_pomodoro, android.R.drawable.ic_media_play),
    ALARM(R.string.pro_page_alarm, android.R.drawable.ic_lock_idle_alarm),
    COUNTDOWN(R.string.pro_page_countdown, android.R.drawable.ic_menu_recent_history),
    STOPWATCH(R.string.pro_page_stopwatch, android.R.drawable.ic_menu_compass);

    final int titleRes;
    final int iconRes;

    ProPage(int titleRes, int iconRes) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
    }
}