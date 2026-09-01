package com.clockmods.pro;

import com.clockmods.calendar.LunarCalendar;
import com.tyme.culture.Taboo;
import com.tyme.culture.dog.DogDay;
import com.tyme.culture.fetus.FetusDay;
import com.tyme.culture.nine.NineDay;
import com.tyme.culture.star.twentyeight.TwentyEightStar;
import com.tyme.festival.LunarFestival;
import com.tyme.festival.SolarFestival;
import com.tyme.lunar.LunarDay;
import com.tyme.lunar.LunarMonth;
import com.tyme.sixtycycle.SixtyCycleDay;
import com.tyme.solar.SolarDay;
import com.tyme.solar.SolarTermDay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
    /**
     * The same day seen through the 干支 calendar, whose year and month are cut at 立春 and at the
     * solar terms rather than at the lunar new year. tyme4j retired {@code LunarDay}'s own pillar
     * accessors in favour of this view, so the pillars are read from here.
     */
    private final SixtyCycleDay sixtyCycleDay;

    private LunarAlmanac(SolarDay solarDay) {
        this.solarDay = solarDay;
        this.lunarDay = solarDay.getLunarDay();
        this.sixtyCycleDay = solarDay.getSixtyCycleDay();
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

    /** Day pillar, e.g. 甲子. */
    public String ganzhiDay() {
        return lunarDay.getSixtyCycle().getName();
    }

    /** Month pillar. tyme4j cuts it at the solar terms, which is what an almanac expects. */
    public String ganzhiMonth() {
        return sixtyCycleDay.getMonth().getName();
    }

    /** Year pillar, cut at 立春 rather than at the lunar new year. */
    public String ganzhiYear() {
        return sixtyCycleDay.getYear().getName();
    }

    /** Lunar year in words, e.g. 丙午马年 — the same wording the clock uses. */
    public String zodiacYearLabel() {
        return LunarCalendar.stemZodiacYear(lunarDay.getLunarMonth().getLunarYear().getYear(),
                false);
    }

    /** 建除十二值日 as the bare name; the caller appends 日. */
    public String duty() {
        return lunarDay.getDuty().getName();
    }

    /** 值神, e.g. 青龙. */
    public String twelveStar() {
        return lunarDay.getTwelveStar().getName();
    }

    /** 黄道 or 黑道 — how auspicious {@link #twelveStar()} is. */
    public String ecliptic() {
        return lunarDay.getTwelveStar().getEcliptic().getName();
    }

    /** 星宿 in its traditional three-glyph form, e.g. 角木蛟. */
    public String twentyEightStar() {
        TwentyEightStar star = lunarDay.getTwentyEightStar();
        return star.getName() + star.getSevenStar().getName() + star.getAnimal().getName();
    }

    /** 彭祖百忌 for the day pillar; tyme4j returns both the stem and the branch line. */
    public String pengZu() {
        return lunarDay.getSixtyCycle().getPengZu().getName();
    }

    /** 胎神占方, e.g. 占门碓外东南. */
    public String fetus() {
        FetusDay fetusDay = lunarDay.getFetusDay();
        return fetusDay.getName() + fetusDay.getSide().getName()
                + fetusDay.getDirection().getName();
    }

    /** 七十二候, e.g. 鹰乃祭鸟. Unlike the rest of the almanac this one hangs off the solar day. */
    public String phenology() {
        return solarDay.getPhenology().getName();
    }

    /** The zodiac the day clashes with, e.g. 鼠; the caller renders it as 冲鼠. */
    public String clashZodiac() {
        return lunarDay.getSixtyCycle().getEarthBranch().getOpposite().getZodiac().getName();
    }

    /** The 煞 direction, e.g. 北. There is no single accessor — it comes off the day's branch. */
    public String ominousDirection() {
        return lunarDay.getSixtyCycle().getEarthBranch().getOminous().getName();
    }

    private static List<String> toNames(List<Taboo> taboos) {
        List<String> names = new ArrayList<>(taboos.size());
        for (Taboo taboo : taboos) {
            names.add(taboo.getName());
        }
        return names;
    }

    /**
     * Memoises almanacs by date. tyme4j computes each day astronomically rather than reading a
     * table, and a single calendar page asks for 42 of them — twice over, once for the page being
     * rendered and once for the swipe preview beside it.
     *
     * <p>Deliberately an instance rather than a static: the owner clears it with the view it feeds,
     * so a long session cannot accumulate every date the user has ever paged past.</p>
     */
    public static final class Cache {
        /** Three month pages plus fling headroom. */
        private static final int MAX_ENTRIES = 192;

        private final LinkedHashMap<Integer, LunarAlmanac> entries =
                new LinkedHashMap<Integer, LunarAlmanac>(256, 0.75f, true) {
                    @Override protected boolean removeEldestEntry(
                            Map.Entry<Integer, LunarAlmanac> eldest) {
                        return size() > MAX_ENTRIES;
                    }
                };

        /** @param month0 zero-based month, as in {@link java.util.Calendar#MONTH}. */
        public LunarAlmanac get(int year, int month0, int day) {
            // month0 is 0–11 and day is 1–31, so four and five bits hold them without overlapping.
            int key = (year << 9) | (month0 << 5) | day;
            LunarAlmanac cached = entries.get(key);
            if (cached == null) {
                cached = LunarAlmanac.of(year, month0, day);
                entries.put(key, cached);
            }
            return cached;
        }

        public void clear() {
            entries.clear();
        }
    }
}
