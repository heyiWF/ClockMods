package com.clockmods.ui

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object DateFormatter {
    enum class Lang { CHINESE, TRADITIONAL, ENGLISH }

    const val MAX_PATTERN_LENGTH = 200
    const val DEFAULT_PATTERN_CN = "yyyy年M月d日 EEEE"
    const val DEFAULT_PATTERN_EN = "yyyy/M/d EEEE"

    private val cnDigits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val cnUnits = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val cnWeekFull = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    private val cnWeekShort = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    private val cnWeekShortTw = arrayOf("週日", "週一", "週二", "週三", "週四", "週五", "週六")
    private val enWeekFull = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val enWeekShort = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val enMonthFull = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    private val enMonthShort = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    private val cnDateCores = arrayOf(
        "yyyy年MM月dd日", "yyyy年M月d日", "yy年M月d日", "yy年MM月dd日",
        "yyyy年M月d号", "yyyy年MM月dd号", "MM月dd日", "M月d日",
        "yyyy/MM/dd", "yyyy/M/d", "yy/M/d", "yyyy-MM-dd", "yyyy-M-d",
        "yyyy.MM.dd", "yyyy.M.d", "MM/dd", "M/d", "dd/MM", "d/M",
        "MM-dd", "dd-MM",
        "YYY年MMM月DD日", "YYYY年MMM月DD日", "YYY年MMM月DD号", "YYYY年MMM月DD号",
        "MMM月DD日", "MMM月DD号",
    )

    private val enDateCores = arrayOf(
        "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
        "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
        "yyyy/MM/dd", "yyyy/M/d", "yy/MM/dd", "yyyy-MM-dd", "yyyy-M-d",
        "MM-dd-yyyy", "M-d-yyyy", "dd-MM-yyyy", "d-M-yyyy",
        "yyyy.MM.dd", "yyyy.M.d", "dd.MM.yyyy",
        "MMMM d, yyyy", "MMMM dd, yyyy", "MMM d, yyyy", "MMM dd, yyyy",
        "d MMMM yyyy", "dd MMMM yyyy", "d MMM yyyy", "dd MMM yyyy",
        "MM/dd", "M/d", "dd/MM", "d/M", "MM-dd", "M-d", "dd-MM", "d-M",
        "MM.dd", "M.d", "dd.MM", "d.M",
        "MMMM d", "MMM d", "MMMM dd", "MMM dd", "d MMMM", "d MMM",
        "dd MMMM", "dd MMM", "MMMM-d", "MMM-d", "d-MMMM", "d-MMM",
    )

    private val cnWeekdayCombos = arrayOf(
        "DATE",
        "DATE EEEE", "DATE E",
        "DATE，EEEE", "DATE，E",
        "DATE（EEEE）", "DATE（E）",
        "（EEEE）DATE", "（E）DATE",
        "DATE [EEEE]", "DATE [E]",
        "[EEEE] DATE", "[E] DATE",
        "DATE - EEEE", "DATE - E",
        "DATE · EEEE", "DATE · E",
        "EEEE DATE", "E DATE",
        "EEEE，DATE", "E，DATE",
        "DATE | EEEE", "DATE | E",
        "EEEE | DATE", "E | DATE",
    )
    private val enWeekdayCombos = arrayOf(
        "DATE",
        "DATE EEEE", "DATE E",
        "DATE, EEEE", "DATE, E",
        "DATE (EEEE)", "DATE (E)",
        "(EEEE) DATE", "(E) DATE",
        "DATE [EEEE]", "DATE [E]",
        "[EEEE] DATE", "[E] DATE",
        "DATE - EEEE", "DATE - E",
        "DATE · EEEE", "DATE · E",
        "EEEE DATE", "E DATE",
        "EEEE, DATE", "E, DATE",
        "DATE | EEEE", "DATE | E",
        "EEEE | DATE", "E | DATE",
    )

    private val cnFixedFormats = arrayOf(
        "yyyy/MM/dd EEEE", "yyyy/M/d EEEE",
        "yyyy年MM月dd日 EEEE", "yyyy年M月d日 EEEE",
        "yyyy.MM.dd EEEE", "yyyy.M.d EEEE",
        "yyyy.MM.dd E", "yyyy.M.d E",
        "yyyy年MM月dd日 E", "yyyy年M月d日 E",
        "MM/dd EEEE", "M/d EEEE",
        "MM月dd日 EEEE", "M月d日 EEEE",
        "MM.dd EEEE", "M.d EEEE",
        "MM.dd E", "M.d E",
        "MM月dd日 E", "M月d日 E",
    )
    private val enFixedFormats = arrayOf(
        "yyyy/MM/dd EEEE", "yyyy/M/d EEEE",
        "yyyy-MM-dd EEEE", "yyyy-M-d EEEE",
        "yyyy.MM.dd EEEE", "yyyy.M.d EEEE",
        "EEEE, MMMM d, yyyy", "MMM d, E", "MMM d, yyyy",
        "MM/dd EEEE", "M/d EEEE",
        "MM-dd EEEE", "M-d EEEE",
        "MM.dd EEEE", "M.d EEEE",
        "EEEE, MMMM d", "MMM d",
    )

    @JvmStatic
    fun format(pattern: String?, cal: Calendar, lang: Lang): String {
        if (pattern == null) return ""
        val output = StringBuilder(pattern.length + 16)
        appendFormatted(output, pattern, cal, lang)
        return TextSpacing.pangu(output.toString())!!
    }

    @JvmStatic
    fun isValidPattern(pattern: String?): Boolean {
        if (pattern.isNullOrEmpty() || pattern.length > MAX_PATTERN_LENGTH) return false
        return appendFormatted(
            StringBuilder(pattern.length + 16),
            pattern,
            exampleDate(),
            Lang.CHINESE,
        ) > 0
    }

    @JvmStatic
    fun exampleDate(): Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.AUGUST, 7, 12, 0, 0)
    }

    @JvmStatic
    fun preview(pattern: String?, lang: Lang): String = format(pattern, exampleDate(), lang)

    @JvmStatic
    fun dateCores(lang: Lang): Array<String> =
        (if (lang == Lang.ENGLISH) enDateCores else cnDateCores).clone()

    @JvmStatic
    fun weekdayCombos(lang: Lang): Array<String> =
        (if (lang == Lang.ENGLISH) enWeekdayCombos else cnWeekdayCombos).clone()

    @JvmStatic
    fun fixedFormats(lang: Lang): Array<String> =
        (if (lang == Lang.ENGLISH) enFixedFormats else cnFixedFormats).clone()

    @JvmStatic
    fun composeCombo(comboTemplate: String?, core: String?): String {
        if (comboTemplate == null) return core ?: ""
        return comboTemplate.replace("DATE", core ?: "")
    }

    @JvmStatic
    fun comboLabel(comboTemplate: String?, lang: Lang): String =
        preview(composeCombo(comboTemplate, "…"), lang)

    private fun appendFormatted(output: StringBuilder, pattern: String, cal: Calendar, lang: Lang): Int {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        if (dayOfWeek !in 0..6) dayOfWeek = 0
        var tokenCount = 0
        var index = 0
        while (index < pattern.length) {
            val character = pattern[index]
            if (character == '\'') {
                index++
                if (index < pattern.length && pattern[index] == '\'') {
                    output.append('\'')
                    index++
                    continue
                }
                while (index < pattern.length && pattern[index] != '\'') {
                    val codePoint = pattern.codePointAt(index)
                    output.appendCodePoint(codePoint)
                    index += Character.charCount(codePoint)
                }
                if (index < pattern.length) index++
                continue
            }
            if (isTokenLetter(character)) {
                var run = 1
                while (index + run < pattern.length && pattern[index + run] == character) run++
                appendToken(output, character, run, year, month, day, dayOfWeek, lang)
                tokenCount++
                index += run
                continue
            }
            val codePoint = pattern.codePointAt(index)
            output.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
        }
        return tokenCount
    }

    private fun isTokenLetter(character: Char): Boolean = character in charArrayOf('y', 'Y', 'M', 'd', 'D', 'E')

    private fun appendToken(
        output: StringBuilder,
        letter: Char,
        run: Int,
        year: Int,
        month: Int,
        day: Int,
        dayOfWeek: Int,
        lang: Lang,
    ) {
        when (letter) {
            'y' -> output.append(if (run == 2) String.format(Locale.US, "%02d", Math.floorMod(year, 100)) else year.toString())
            'Y' -> output.append(if (lang == Lang.ENGLISH) year.toString() else chineseYear(year, if (run == 3) '〇' else '零'))
            'M' -> if (lang == Lang.ENGLISH) {
                output.append(
                    when (run) {
                        1 -> month.toString()
                        2 -> String.format(Locale.US, "%02d", month)
                        3 -> enMonthShort[month - 1]
                        else -> enMonthFull[month - 1]
                    },
                )
            } else {
                output.append(
                    when (run) {
                        1 -> month.toString()
                        2 -> String.format(Locale.US, "%02d", month)
                        else -> chineseCardinal(month)
                    },
                )
            }
            'd' -> output.append(if (run == 1) day.toString() else String.format(Locale.US, "%02d", day))
            'D' -> output.append(if (lang == Lang.ENGLISH) day.toString() else chineseCardinal(day))
            'E' -> output.append(
                when (lang) {
                    Lang.ENGLISH -> if (run >= 4) enWeekFull[dayOfWeek] else enWeekShort[dayOfWeek]
                    Lang.TRADITIONAL -> if (run >= 4) cnWeekFull[dayOfWeek] else cnWeekShortTw[dayOfWeek]
                    Lang.CHINESE -> if (run >= 4) cnWeekFull[dayOfWeek] else cnWeekShort[dayOfWeek]
                },
            )
        }
    }

    @JvmStatic
    fun chineseYear(year: Int, zero: Char): String {
        val digits = abs(year).toString()
        return buildString(digits.length) {
            for (digit in digits) append(if (digit == '0') zero else cnDigits[digit - '0'][0])
        }
    }

    @JvmStatic
    fun chineseCardinal(number: Int): String {
        if (number <= 0 || number > 99) return number.toString()
        if (number < 10) return cnUnits[number]
        if (number == 10) return "十"
        if (number < 20) return "十" + cnUnits[number - 10]
        val tens = number / 10
        val ones = number % 10
        return cnUnits[tens] + "十" + if (ones == 0) "" else cnUnits[ones]
    }
}
