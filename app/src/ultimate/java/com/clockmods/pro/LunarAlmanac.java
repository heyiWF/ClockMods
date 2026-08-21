package com.clockmods.pro;

import com.clockmods.calendar.LunarCalendar;
import com.tyme.culture.Taboo;
import com.tyme.culture.dog.DogDay;
import com.tyme.culture.nine.NineDay;
import com.tyme.festival.LunarFestival;
import com.tyme.festival.SolarFestival;
import com.tyme.lunar.LunarDay;
import com.tyme.lunar.LunarMonth;
import com.tyme.solar.SolarDay;
import com.tyme.solar.SolarTermDay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Computes the almanac for a single Gregorian day from the offline 6tail tyme4j engine: lunar date,
 * solar terms, festivals and 宜/忌. The engine is astronomical rather than table bound, so it stays
 * accurate for years 1–9999.
 *
 * <p>The Chinese month/day/年 wording is rendered through {@link LunarCalendar} so the calendar and
 * the main clock read identically. Statutory 休/班 arrangements are not derived here — they are
 * announced yearly and cannot be computed, so they continue to come from
 * {@link com.clockmods.calendar.HolidayRepository}.
 */
public final class LunarAlmanac {
    private final SolarDay solarDay;
    private final LunarDay lunarDay;

    private LunarAlmanac(SolarDay solarDay) {
        this.solarDay = solarDay;
        this.lunarDay = solarDay.getLunarDay();
    }

    /** @param month0 zero-based month, as in {@link java.util.Calendar#MONTH}. */
    public static LunarAlmanac of(int year, int month0, int day) {
        return new LunarAlmanac(SolarDay.fromYmd(year, month0 + 1, day));
    }

    /** Short cell label: the lunar month name on a lunar month's first day, otherwise the day. */
    public String shortLabel() {
        LunarMonth month = lunarDay.getLunarMonth();
        return lunarDay.getDay() == 1
                ? LunarCalendar.formatMonth(Math.abs(month.getMonthWithLeap()), month.isLeap())
                : LunarCalendar.formatDay(lunarDay.getDay());
    }

    /** Natural footer label, e.g. 丙午马年六月廿一. */
    public String naturalLabel() {
        LunarMonth month = lunarDay.getLunarMonth();
        return LunarCalendar.stemZodiacYear(month.getLunarYear().getYear(), false)
                + LunarCalendar.formatMonth(Math.abs(month.getMonthWithLeap()), month.isLeap())
                + LunarCalendar.formatDay(lunarDay.getDay());
    }

    /**
     * Solar term (only on its 交节 day), traditional lunar/solar festivals and the first day of each
     * 数九/三伏, in display order. tyme4j returns a single curated festival per calendar, so there is
     * no separate "minor festival" tier.
     */
    public List<String> festivals() {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        SolarTermDay termDay = solarDay.getTermDay();
        if (termDay != null && termDay.getDayIndex() == 0) {
            labels.add(termDay.getSolarTerm().getName());
        }
        LunarFestival lunarFestival = lunarDay.getFestival();
        if (lunarFestival != null) {
            labels.add(lunarFestival.getName());
        }
        SolarFestival solarFestival = solarDay.getFestival();
        if (solarFestival != null) {
            labels.add(solarFestival.getName());
        }
        NineDay nineDay = solarDay.getNineDay();
        if (nineDay != null && nineDay.getDayIndex() == 0) {
            labels.add(nineDay.getNine().getName());
        }
        DogDay dogDay = solarDay.getDogDay();
        if (dogDay != null && dogDay.getDayIndex() == 0) {
            labels.add(dogDay.getDog().getName());
        }
        return new ArrayList<>(labels);
    }

    /** 今日宜. */
    public List<String> suitable() {
        return toNames(lunarDay.getRecommends());
    }

    /** 今日忌. */
    public List<String> avoid() {
        return toNames(lunarDay.getAvoids());
    }

    private static List<String> toNames(List<Taboo> taboos) {
        List<String> names = new ArrayList<>(taboos.size());
        for (Taboo taboo : taboos) {
            names.add(taboo.getName());
        }
        return names;
    }
}
