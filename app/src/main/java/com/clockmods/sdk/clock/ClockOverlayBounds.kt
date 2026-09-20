package com.clockmods.sdk.clock

/** Rectangle occupied by a host-owned overlay drawn above the clock face. */
class ClockOverlayBounds(rawLeft: Float, rawTop: Float, rawRight: Float, rawBottom: Float) {
    private val left = minOf(rawLeft, rawRight)
    private val top = minOf(rawTop, rawBottom)
    private val right = maxOf(rawLeft, rawRight)
    private val bottom = maxOf(rawTop, rawBottom)

    fun getLeft() = left
    fun getTop() = top
    fun getRight() = right
    fun getBottom() = bottom
    fun getWidth() = right - left
    fun getHeight() = bottom - top
    fun spansHorizontally(rowLeft: Float, rowRight: Float) = rowRight > left && rowLeft < right
    fun spansVertically(rowTop: Float, rowBottom: Float) = rowBottom > top && rowTop < bottom

    override fun equals(other: Any?): Boolean =
        this === other || other is ClockOverlayBounds &&
            left.compareTo(other.left) == 0 && top.compareTo(other.top) == 0 &&
            right.compareTo(other.right) == 0 && bottom.compareTo(other.bottom) == 0

    override fun hashCode(): Int {
        var result = left.toBits()
        result = 31 * result + top.toBits()
        result = 31 * result + right.toBits()
        return 31 * result + bottom.toBits()
    }
}
