package com.clockmods.pro

import com.clockmods.calendar.LunarCalendar
import com.tyme.culture.Taboo
import com.tyme.culture.dog.DogDay
import com.tyme.culture.fetus.FetusDay
import com.tyme.culture.nine.NineDay
import com.tyme.culture.star.twentyeight.TwentyEightStar
import com.tyme.festival.LunarFestival
import com.tyme.festival.SolarFestival
import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.sixtycycle.SixtyCycleDay
import com.tyme.solar.SolarDay
import com.tyme.solar.SolarTermDay
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import kotlin.math.abs

/** Computes one Gregorian day's traditional almanac using the offline tyme4j engine. */
class LunarAlmanac private constructor(private val solarDay: SolarDay) {
    private val lunarDay: LunarDay = solarDay.getLunarDay()
    private val sixtyCycleDay: SixtyCycleDay = solarDay.getSixtyCycleDay()

    /** Short cell label: the lunar month name on a lunar month's first day, otherwise the day. */
    fun shortLabel(): String {
        val month = lunarDay.getLunarMonth()
        return if (lunarDay.getDay() == 1) {
            LunarCalendar.formatMonth(abs(month.getMonthWithLeap()), month.isLeap())
        } else {
            LunarCalendar.formatDay(lunarDay.getDay())
        }
    }

    /** Natural footer label, e.g. 丙午马年六月廿一. */
    fun naturalLabel(): String {
        val month = lunarDay.getLunarMonth()
        return LunarCalendar.stemZodiacYear(month.getLunarYear().getYear(), false) +
            LunarCalendar.formatMonth(abs(month.getMonthWithLeap()), month.isLeap()) +
            LunarCalendar.formatDay(lunarDay.getDay())
    }

    /** Solar terms, traditional festivals and the first day of each seasonal period. */
    fun festivals(): List<String> {
        val labels = LinkedHashSet<String>()
        val termDay: SolarTermDay? = solarDay.getTermDay()
        if (termDay != null && termDay.getDayIndex() == 0) {
            labels.add(termDay.getSolarTerm().getName())
        }
        val lunarFestival: LunarFestival? = lunarDay.getFestival()
        if (lunarFestival != null) labels.add(lunarFestival.getName())
        val solarFestival: SolarFestival? = solarDay.getFestival()
        if (solarFestival != null) labels.add(solarFestival.getName())
        val nineDay: NineDay? = solarDay.getNineDay()
        if (nineDay != null && nineDay.getDayIndex() == 0) labels.add(nineDay.getNine().getName())
        val dogDay: DogDay? = solarDay.getDogDay()
        if (dogDay != null && dogDay.getDayIndex() == 0) labels.add(dogDay.getDog().getName())
        return ArrayList(labels)
    }

    /** 今日宜. */
    fun suitable(): List<String> = toNames(lunarDay.getRecommends())

    /** 今日忌. */
    fun avoid(): List<String> = toNames(lunarDay.getAvoids())

    fun ganzhiDay(): String = lunarDay.getSixtyCycle().getName()

    fun ganzhiMonth(): String = sixtyCycleDay.getMonth().getName()

    fun ganzhiYear(): String = sixtyCycleDay.getYear().getName()

    fun zodiacYearLabel(): String = LunarCalendar.stemZodiacYear(
        lunarDay.getLunarMonth().getLunarYear().getYear(), false
    )

    fun duty(): String = lunarDay.getDuty().getName()

    fun twelveStar(): String = lunarDay.getTwelveStar().getName()

    fun ecliptic(): String = lunarDay.getTwelveStar().getEcliptic().getName()

    fun twentyEightStar(): String {
        val star: TwentyEightStar = lunarDay.getTwentyEightStar()
        return star.getName() + star.getSevenStar().getName() + star.getAnimal().getName()
    }

    fun pengZu(): String = lunarDay.getSixtyCycle().getPengZu().getName()

    fun fetus(): String {
        val fetusDay: FetusDay = lunarDay.getFetusDay()
        return fetusDay.getName() + fetusDay.getSide().getName() + fetusDay.getDirection().getName()
    }

    fun phenology(): String = solarDay.getPhenology().getName()

    fun clashZodiac(): String =
        lunarDay.getSixtyCycle().getEarthBranch().getOpposite().getZodiac().getName()

    fun ominousDirection(): String =
        lunarDay.getSixtyCycle().getEarthBranch().getOminous().getName()

    /** Bounded access-order cache owned by the calendar surface that consumes it. */
    class Cache {
        private val entries = object : LinkedHashMap<Int, LunarAlmanac>(256, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, LunarAlmanac>?): Boolean =
                size > MAX_ENTRIES
        }

        /** [month0] is zero-based, as in `java.util.Calendar.MONTH`. */
        fun get(year: Int, month0: Int, day: Int): LunarAlmanac {
            val key = (year shl 9) or (month0 shl 5) or day
            return entries[key] ?: of(year, month0, day).also { entries[key] = it }
        }

        fun clear() {
            entries.clear()
        }

        private companion object {
            const val MAX_ENTRIES = 192
        }
    }

    companion object {
        /** [month0] is zero-based, as in `java.util.Calendar.MONTH`. */
        @JvmStatic
        fun of(year: Int, month0: Int, day: Int): LunarAlmanac =
            LunarAlmanac(SolarDay.fromYmd(year, month0 + 1, day))

        private fun toNames(taboos: List<Taboo>): List<String> = taboos.mapTo(ArrayList(taboos.size)) {
            it.getName()
        }
    }
}
