package com.clockmods.pro;

import com.nlf.calendar.Fu;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.ShuJiu;
import com.nlf.calendar.Solar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Computes the almanac for a single Gregorian day from the offline lunar-java engine (6tail):
 * lunar date, solar terms, festivals and 宜/忌. The engine is astronomical rather than table
 * bound, so it stays accurate well beyond year 2100 and no ICS data needs to be bundled.
 *
 * <p>Statutory 休/班 arrangements are not derived here — they are announced yearly and cannot be
 * computed, so they continue to come from {@link com.clockmods.calendar.HolidayRepository}.
 */
public final class LunarAlmanac {
    private final Solar solar;
    private final Lunar lunar;

    private LunarAlmanac(Solar solar) {
        this.solar = solar;
        this.lunar = solar.getLunar();
    }

    /** @param month0 zero-based month, as in {@link java.util.Calendar#MONTH}. */
    public static LunarAlmanac of(int year, int month0, int day) {
        return new LunarAlmanac(Solar.fromYmd(year, month0 + 1, day));
    }

    /** Short cell label: the lunar month name on a lunar month's first day, otherwise the day. */
    public String shortLabel() {
        return lunar.getDay() == 1 ? lunar.getMonthInChinese() + "月" : lunar.getDayInChinese();
    }

    /** Natural footer label, e.g. 丙午马年六月廿一. */
    public String naturalLabel() {
        return lunar.getYearInGanZhi() + lunar.getYearShengXiao() + "年"
                + lunar.getMonthInChinese() + "月" + lunar.getDayInChinese();
    }

    /**
     * Solar term, traditional/solar festivals and the first day of 数九/三伏, in display order.
     * 母亲节/父亲节/感恩节 are always included because the engine returns them from
     * {@code getFestivals()}.
     *
     * @param includeMinor when {@code true}, also include the engine's {@code getOtherFestivals()}
     *     lists (国际电影节, 世界人道主义日, 龙头节, …); when {@code false} they are omitted to keep
     *     the grid uncluttered. Controlled by the "月历显示更多节日" setting.
     */
    public List<String> festivals(boolean includeMinor) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        String jieQi = lunar.getJieQi();
        if (jieQi != null && jieQi.length() > 0) labels.add(jieQi);
        labels.addAll(lunar.getFestivals());
        labels.addAll(solar.getFestivals());
        if (includeMinor) {
            labels.addAll(lunar.getOtherFestivals());
            labels.addAll(solar.getOtherFestivals());
        }
        ShuJiu shuJiu = lunar.getShuJiu();
        if (shuJiu != null && shuJiu.getIndex() == 1) labels.add(shuJiu.getName());
        Fu fu = lunar.getFu();
        if (fu != null && fu.getIndex() == 1) labels.add(fu.getName());
        return new ArrayList<>(labels);
    }

    /** 今日宜. */
    public List<String> suitable() { return lunar.getDayYi(); }

    /** 今日忌. */
    public List<String> avoid() { return lunar.getDayJi(); }
}
