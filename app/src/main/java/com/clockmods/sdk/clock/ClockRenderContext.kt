package com.clockmods.sdk.clock

/** Per-frame geometry and host policy supplied to a [ClockRenderer]. */
class ClockRenderContext(
    private val left: Float,
    private val top: Float,
    private val right: Float,
    private val bottom: Float,
    density: Float,
    scaledDensity: Float,
    private val frameTimeMillis: Long,
    private val background: ClockBackground?,
    bottomInset: Float,
    worldClockScroll: Float,
    private val worldClockStripHosted: Boolean,
    private val statusOverlay: ClockOverlayBounds?,
) {
    private val density = maxOf(0.01f, density)
    private val scaledDensity = maxOf(0.01f, scaledDensity)
    private val bottomInset: Float
    private val worldClockScroll = maxOf(0f, worldClockScroll)

    init {
        require(right >= left && bottom >= top) { "Render bounds must not be inverted" }
        this.bottomInset = bottomInset.coerceIn(0f, bottom - top)
    }

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        density: Float,
        scaledDensity: Float,
        frameTimeMillis: Long,
    ) : this(
        left, top, right, bottom, density, scaledDensity, frameTimeMillis,
        null, 0f, 0f, false, null,
    )

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        density: Float,
        scaledDensity: Float,
        frameTimeMillis: Long,
        background: ClockBackground?,
    ) : this(
        left, top, right, bottom, density, scaledDensity, frameTimeMillis,
        background, 0f, 0f, false, null,
    )

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        density: Float,
        scaledDensity: Float,
        frameTimeMillis: Long,
        background: ClockBackground?,
        bottomInset: Float,
    ) : this(
        left, top, right, bottom, density, scaledDensity, frameTimeMillis,
        background, bottomInset, 0f, false, null,
    )

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        density: Float,
        scaledDensity: Float,
        frameTimeMillis: Long,
        background: ClockBackground?,
        bottomInset: Float,
        worldClockScroll: Float,
    ) : this(
        left, top, right, bottom, density, scaledDensity, frameTimeMillis,
        background, bottomInset, worldClockScroll, false, null,
    )

    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        density: Float,
        scaledDensity: Float,
        frameTimeMillis: Long,
        background: ClockBackground?,
        bottomInset: Float,
        worldClockScroll: Float,
        worldClockStripHosted: Boolean,
    ) : this(
        left, top, right, bottom, density, scaledDensity, frameTimeMillis,
        background, bottomInset, worldClockScroll, worldClockStripHosted, null,
    )

    fun getLeft() = left
    fun getTop() = top
    fun getRight() = right
    fun getBottom() = bottom
    fun getWidth() = right - left
    fun getHeight() = bottom - top
    fun getCenterX() = (left + right) * 0.5f
    fun getCenterY() = (top + bottom) * 0.5f
    fun getDensity() = density
    fun getScaledDensity() = scaledDensity
    fun getFrameTimeMillis() = frameTimeMillis
    fun getBackground() = background
    fun getBottomInset() = bottomInset
    fun getWorldClockScroll() = worldClockScroll
    fun isWorldClockStripHosted() = worldClockStripHosted
    fun getStatusOverlay() = statusOverlay
}
