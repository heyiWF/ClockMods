package com.clockmods.calendar

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.solar.SolarDay
import java.util.Calendar

/** Formats lunar dates using the offline tyme4j engine. */
object LunarCalendar {
    private val TIAN_GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val DI_ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ZODIAC = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val MONTH_NAMES = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val DAY_PREFIX = arrayOf("初", "十", "廿", "三")
    private val DAY_NUMBERS = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    @JvmStatic
    fun format(calendar: Calendar): String {
        val lunarDate = fromSolar(calendar) ?: return ""
        return stemZodiacYear(lunarDate.year, true) + formatMonth(lunarDate.month, lunarDate.leap) + formatDay(lunarDate.day)
    }

    @JvmStatic
    fun formatNatural(calendar: Calendar): String {
        val lunarDate = fromSolar(calendar) ?: return ""
        return stemZodiacYear(lunarDate.year, false) + formatMonth(lunarDate.month, lunarDate.leap) + formatDay(lunarDate.day)
    }

    @JvmStatic
    fun formatShort(calendar: Calendar): String {
        val lunarDate = fromSolar(calendar) ?: return ""
        return if (lunarDate.day == 1) formatMonth(lunarDate.month, lunarDate.leap) else formatDay(lunarDate.day)
    }

    @JvmStatic
    fun fromSolar(solar: Calendar): LunarDate {
        val lunarDay: LunarDay = SolarDay.fromYmd(
            solar.get(Calendar.YEAR), solar.get(Calendar.MONTH) + 1, solar.get(Calendar.DAY_OF_MONTH),
        ).lunarDay
        val lunarMonth: LunarMonth = lunarDay.lunarMonth
        return LunarDate(
            lunarMonth.lunarYear.year,
            kotlin.math.abs(lunarMonth.monthWithLeap),
            lunarDay.day,
            lunarMonth.isLeap,
        )
    }

    @JvmStatic
    fun stemZodiacYear(lunarYear: Int, bracketZodiac: Boolean): String {
        val index = Math.floorMod(lunarYear - 4, 60)
        val stemBranchYear = TIAN_GAN[index % 10] + DI_ZHI[index % 12]
        val zodiac = ZODIAC[index % 12]
        return if (bracketZodiac) "$stemBranchYear[$zodiac]年" else "$stemBranchYear${zodiac}年"
    }

    @JvmStatic
    fun formatMonth(month: Int, leap: Boolean): String =
        (if (leap) "闰" else "") + MONTH_NAMES[month - 1] + "月"

    @JvmStatic
    fun formatDay(day: Int): String {
        if (day == 10) return "初十"
        if (day == 20) return "二十"
        if (day == 30) return "三十"
        return DAY_PREFIX[(day - 1) / 10] + DAY_NUMBERS[(day - 1) % 10]
    }

    class LunarDate(
        @JvmField val year: Int,
        @JvmField val month: Int,
        @JvmField val day: Int,
        @JvmField val leap: Boolean,
    )
}
