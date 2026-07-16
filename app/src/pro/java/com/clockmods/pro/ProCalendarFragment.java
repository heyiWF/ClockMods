package com.clockmods.pro;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.clockmods.calendar.CalendarMonth;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ProCalendarFragment extends Fragment {
    private static final String[] WEEKDAYS = {"日", "一", "二", "三", "四", "五", "六"};

    private TextView title;
    private GridLayout grid;
    private final Calendar visibleMonth = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_calendar, container, false);
        title = root.findViewById(R.id.calendar_month_title);
        grid = root.findViewById(R.id.calendar_grid);
        populateWeekdays(root.findViewById(R.id.calendar_weekdays));
        root.findViewById(R.id.calendar_previous).setOnClickListener(view -> changeMonth(-1));
        root.findViewById(R.id.calendar_next).setOnClickListener(view -> changeMonth(1));
        root.findViewById(R.id.calendar_today).setOnClickListener(view -> {
            visibleMonth.setTimeInMillis(System.currentTimeMillis());
            renderMonth();
        });
        renderMonth();
        return root;
    }

    private void populateWeekdays(GridLayout weekdays) {
        for (String weekday : WEEKDAYS) {
            TextView label = new TextView(requireContext());
            label.setText(weekday);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(13);
            label.setAlpha(0.72f);
            weekdays.addView(label, cellParams(1));
        }
    }

    private void changeMonth(int offset) {
        visibleMonth.add(Calendar.MONTH, offset);
        renderMonth();
    }

    private void renderMonth() {
        int year = visibleMonth.get(Calendar.YEAR);
        int month = visibleMonth.get(Calendar.MONTH);
        title.setText(String.format(Locale.CHINA, "%d年%d月", year, month + 1));
        CalendarMonth calendarMonth = CalendarMonth.create(year, month,
                TimeZone.getDefault(), System.currentTimeMillis());
        grid.removeAllViews();
        for (CalendarMonth.Day day : calendarMonth.days) {
            grid.addView(createDayCell(day), cellParams(6));
        }
    }

    private View createDayCell(CalendarMonth.Day day) {
        LinearLayout cell = new LinearLayout(requireContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(3), dp(2), dp(3));
        cell.setAlpha(day.currentMonth ? 1f : 0.38f);
        if (day.today) {
            cell.setBackgroundResource(R.drawable.calendar_today_background);
        }

        TextView solar = new TextView(requireContext());
        solar.setText(String.valueOf(day.dayOfMonth));
        solar.setTextSize(16);
        solar.setGravity(Gravity.CENTER);
        if (day.today) solar.setTextColor(Color.WHITE);
        cell.addView(solar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView lunar = new TextView(requireContext());
        lunar.setText(day.lunarLabel);
        lunar.setTextSize(10);
        lunar.setGravity(Gravity.CENTER);
        lunar.setSingleLine(true);
        lunar.setAlpha(0.76f);
        if (day.today) lunar.setTextColor(Color.WHITE);
        cell.addView(lunar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return cell;
    }

    private GridLayout.LayoutParams cellParams(int rowSpan) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}