package com.clockmods.ui

object TextSpacing {
    @JvmStatic
    fun pangu(text: String?): String? {
        if (text == null || text.length < 2) return text
        val result = StringBuilder(text.length + 8)
        var previous = -1
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            if (previous != -1 && needsPanguSpace(previous, codePoint)) result.append(' ')
            result.appendCodePoint(codePoint)
            previous = codePoint
            offset += Character.charCount(codePoint)
        }
        return result.toString()
    }

    @JvmStatic
    fun needsPanguSpace(left: Int, right: Int): Boolean =
        (isCjk(left) && isLatinAlphanumeric(right)) ||
            (isLatinAlphanumeric(left) && isCjk(right))

    private fun isLatinAlphanumeric(codePoint: Int): Boolean =
        codePoint in '0'.code..'9'.code || codePoint in 'A'.code..'Z'.code || codePoint in 'a'.code..'z'.code

    private fun isCjk(codePoint: Int): Boolean =
        codePoint in 0x4E00..0x9FFF || codePoint in 0x3400..0x4DBF ||
            codePoint in 0xF900..0xFAFF || codePoint in 0x20000..0x2A6DF || codePoint == 0x3007
}
