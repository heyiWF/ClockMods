package com.clockmods.ultimate.clock

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.clockmods.sdk.clock.ClockBackground
import com.clockmods.sdk.clock.ClockOverlayBounds
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleRegistry
import com.clockmods.sdk.clock.WorldClockEntry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field
import java.util.Locale
import java.util.TimeZone

/** JVM smoke coverage for every built-in renderer and its host-owned overlay contract. */
class UltimateClockRendererSmokeTest {
    companion object {
        private val NON_PRO_STYLE_IDS = arrayOf(
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
            UltimateClockStyles.STYLE_PAPER_STATION,
            UltimateClockStyles.STYLE_ORBIT_NEON,
            UltimateClockStyles.STYLE_DIGITAL_GRID,
            UltimateClockStyles.STYLE_TYPOGRAPHIC,
            UltimateClockStyles.STYLE_DUAL_BLOCKS,
            UltimateClockStyles.STYLE_ORBIT,
            UltimateClockStyles.STYLE_BUBBLES,
            UltimateClockStyles.STYLE_BLEND,
            UltimateClockStyles.STYLE_RIBBON,
        )
        private val VIEWPORTS = arrayOf(intArrayOf(1600, 900), intArrayOf(900, 1600), intArrayOf(166, 94))
        private val CORE_STYLE_IDS = arrayOf(
            UltimateClockStyles.STYLE_PRO_CLASSIC,
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
            UltimateClockStyles.STYLE_PAPER_STATION,
            UltimateClockStyles.STYLE_ORBIT_NEON,
            UltimateClockStyles.STYLE_DIGITAL_GRID,
            UltimateClockStyles.STYLE_TYPOGRAPHIC,
        )
        private const val CORNER_MARGIN_X = 40f
        private const val CORNER_MARGIN_Y = 32f
        private const val CAPSULE_WIDTH = 90f
        private const val CAPSULE_HEIGHT = 64f
        private const val VIEW_WIDTH = 2560f
        private const val VIEW_HEIGHT = 1440f
    }

    @Test
    fun nonProRenderersHandleLongChineseDateAtSupportedViewportSizes() {
        val registry = UltimateClockStyles.createRegistry()
        val state = longChineseState()
        val paints = PaintPoolFixture.install()
        try {
            for (styleId in NON_PRO_STYLE_IDS) {
                val style = registry.find(styleId)
                assertNotNull(styleId, style)
                for (viewport in VIEWPORTS) {
                    assertRendersWithoutLeakingCanvasState(style!!, state, viewport[0], viewport[1])
                }
            }
        } finally {
            paints.restore()
        }
    }

    @Test
    fun coreBuiltInsKeepTextInsideANarrowPortraitViewport() {
        val registry = UltimateClockStyles.createRegistry()
        val state = longChineseState(false)
        val paints = PaintPoolFixture.install()
        try {
            for (styleId in CORE_STYLE_IDS) {
                val style = registry.find(styleId)!!
                val canvas = RecordingCanvas(720f, 1600f)
                val context = ClockRenderContext(
                    0f, 0f, 720f, 1600f, 2f, 2f, state.getTimeMillis(),
                    ClockBackground.theme(false), 2f * 28f,
                )
                style.getRenderer().render(canvas, context, state, style.getThemeTokens())
                canvas.assertTextInside(styleId)
            }
        } finally {
            paints.restore()
        }
    }

    @Test
    fun proClassicNonStackedTimeHonorsTimeScale() {
        val style = UltimateClockStyles.createRegistry()
            .find(UltimateClockStyles.STYLE_PRO_CLASSIC)!!
        val paints = PaintPoolFixture.install()
        try {
            fun renderedTimeSize(scale: Float): Float {
                val state = ClockState.builder(1787633430123L)
                    .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                    .locale(Locale.US)
                    .use24Hour(true)
                    .showSeconds(false)
                    .portraitStacked(false)
                    .timeScale(scale)
                    .build()
                val canvas = RecordingCanvas(VIEW_WIDTH, VIEW_HEIGHT)
                val context = ClockRenderContext(
                    0f,
                    0f,
                    VIEW_WIDTH,
                    VIEW_HEIGHT,
                    2f,
                    2f,
                    state.getTimeMillis(),
                )
                style.getRenderer().render(canvas, context, state, style.getThemeTokens())
                return canvas.textBounds.maxOf { it.textSize }
            }

            val compact = renderedTimeSize(.5f)
            val enlarged = renderedTimeSize(1.25f)

            assertEquals(VIEW_HEIGHT * .42f * .5f, compact, .01f)
            assertEquals(VIEW_HEIGHT * .42f * 1.25f, enlarged, .01f)
            assertTrue(enlarged > compact)
        } finally {
            paints.restore()
        }
    }

    @Test
    fun largerDateWrapsBeforeShrinkingAndReturnsToOneLine() {
        val solar = "2026 年 9 月 13 日 星期日"
        val lunar = "丙午[马]年八月初三"
        val combined = "$solar / $lunar"
        val style = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_ORBIT)!!
        val paints = PaintPoolFixture.install()
        try {
            for (scale in floatArrayOf(.6f, 1.8f, .6f)) {
                val state = ClockState.builder(1787633430123L).dateText(combined)
                    .locale(Locale.SIMPLIFIED_CHINESE).dateScale(scale).build()
                val canvas = RecordingCanvas(2560f, 1440f)
                val context = ClockRenderContext(0f, 0f, 2560f, 1440f, 2f, 2f, state.getTimeMillis())
                style.getRenderer().render(canvas, context, state, style.getThemeTokens())
                canvas.assertTextInside(UltimateClockStyles.STYLE_ORBIT)
                if (scale < 1f) {
                    assertTrue(canvas.textBounds.any { it.text == combined })
                } else {
                    val first = canvas.textBounds.firstOrNull { it.text == solar }
                        ?: error("Missing solar row")
                    val second = canvas.textBounds.firstOrNull { it.text == lunar }
                        ?: error("Missing lunar row")
                    assertEquals(1440f * .032f * scale, first.textSize, .01f)
                    assertEquals(first.textSize, second.textSize, .01f)
                    assertTrue(second.top > first.bottom)
                }
            }
        } finally {
            paints.restore()
        }
    }

    @Test
    fun dateWrappingHonorsWidthAndNaturalBoundaries() {
        val paint = JvmPaint()
        paint.textSize = 30f
        val date = "September 13, 2026 Sunday"
        assertArrayEquals(arrayOf(date, ""), UltimateClockStyles.dateLines(date, paint, paint.measureText(date), false, Locale.US))
        assertArrayEquals(arrayOf("September 13,", "2026 Sunday"), UltimateClockStyles.dateLines(date, paint, 240f, false, Locale.US))
        assertArrayEquals(arrayOf("", ""), UltimateClockStyles.dateLines("", paint, 100f, false, Locale.US))
        val solar = "09 / 13 / 2026"
        val lunar = "丙午[马]年八月初三"
        assertArrayEquals(arrayOf(solar, lunar), UltimateClockStyles.dateLines("$solar / $lunar", paint, 1000f, true, Locale.SIMPLIFIED_CHINESE))
    }

    private fun capsuleAtEnd(): ClockOverlayBounds {
        val right = VIEW_WIDTH - CORNER_MARGIN_X
        return ClockOverlayBounds(right - CAPSULE_WIDTH, CORNER_MARGIN_Y, right, CORNER_MARGIN_Y + CAPSULE_HEIGHT)
    }

    private fun capsuleAtStart(): ClockOverlayBounds =
        ClockOverlayBounds(CORNER_MARGIN_X, CORNER_MARGIN_Y, CORNER_MARGIN_X + CAPSULE_WIDTH, CORNER_MARGIN_Y + CAPSULE_HEIGHT)

    @Test
    fun portraitDateStaysLevelWithNearbyStatusCapsule() {
        val width = 920f
        val height = 2048f
        val density = 2f
        val state = ClockState.builder(1787633430123L)
            .locale(Locale.SIMPLIFIED_CHINESE)
            .dateText("2026 年 9 月 24 日 星期四 / 丙午[马]年八月十四")
            .dateScale(1.15f)
            .build()
        val paints = PaintPoolFixture.install()
        try {
            for (styleId in arrayOf(UltimateClockStyles.STYLE_BUBBLES,
                UltimateClockStyles.STYLE_RIBBON, UltimateClockStyles.STYLE_ORBIT)) {
                val style = UltimateClockStyles.createRegistry().find(styleId)!!
                val placement = UltimateClockStyles.statusCapsuleBounds(
                    styleId, width, height, density, width * .055f, height * .037f, 210f, 76f,
                )
                val overlay = ClockOverlayBounds(placement[0], placement[1], placement[2], placement[3])
                fun dateTop(status: ClockOverlayBounds?): Float {
                    val canvas = RecordingCanvas(width, height)
                    val context = ClockRenderContext(
                        0f, 0f, width, height, density, density, state.getTimeMillis(),
                        ClockBackground.theme(false), 0f, 0f, true, status,
                    )
                    style.getRenderer().render(canvas, context, state, style.getThemeTokens())
                    return canvas.textBounds.first { it.text.startsWith("2026") }.top
                }
                val plainTop = dateTop(null)
                val statusTop = dateTop(overlay)
                assertEquals("$styleId must not push a date that ends before the capsule",
                    plainTop, statusTop, .01f)
                assertTrue("$styleId date and capsule should start on the same row",
                    kotlin.math.abs(statusTop - overlay.getTop()) < 12f)
            }
        } finally {
            paints.restore()
        }
    }

    @Test
    fun wideStatusCapsuleLeavesRoomForBubbleDateOnItsOwnRow() {
        val width = 920f
        val height = 2048f
        val density = 2f
        val style = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_BUBBLES)!!
        val state = ClockState.builder(1787633430123L)
            .locale(Locale.SIMPLIFIED_CHINESE)
            .dateText("2026 年 9 月 24 日 星期四 / 丙午[马]年八月十四")
            .dateScale(1.15f)
            .build()
        val placement = UltimateClockStyles.statusCapsuleBounds(
            UltimateClockStyles.STYLE_BUBBLES, width, height, density,
            width * .055f, height * .037f, 410f, 120f,
        )
        val overlay = ClockOverlayBounds(placement[0], placement[1], placement[2], placement[3])
        val paints = PaintPoolFixture.install()
        try {
            val canvas = RecordingCanvas(width, height)
            val context = ClockRenderContext(
                0f, 0f, width, height, density, density, state.getTimeMillis(),
                ClockBackground.theme(false), 0f, 0f, true, overlay,
            )
            style.getRenderer().render(canvas, context, state, style.getThemeTokens())
            val date = canvas.textBounds.first { it.text.startsWith("2026") }
            assertTrue("the solar date should remain complete", date.text.endsWith("星期四"))
            assertTrue("the date should remain level with the wide capsule",
                kotlin.math.abs(date.top - overlay.getTop()) < 12f)
            assertTrue("the date should end before the wide capsule",
                date.right <= overlay.getLeft() - density * 4f + 1f)
        } finally {
            paints.restore()
        }
    }

    @Test
    fun topRightMetadataClearsTheHostStatusCapsule() {
        val styleIds = arrayOf(UltimateClockStyles.STYLE_ORBIT, UltimateClockStyles.STYLE_BUBBLES, UltimateClockStyles.STYLE_RIBBON)
        val paints = PaintPoolFixture.install()
        try {
            for (styleId in styleIds) {
                val style = UltimateClockStyles.createRegistry().find(styleId)
                assertNotNull(styleId, style)
                val unbounded = cornerTextTop(style!!, null, true)
                val bounded = cornerTextTop(style, capsuleAtEnd(), true)
                assertFalse("$styleId drew no top-right row to check", unbounded.isNaN())
                assertTrue("$styleId no longer reproduces the overlap, fixture is stale: $unbounded", unbounded < CORNER_MARGIN_Y + CAPSULE_HEIGHT)
                assertTrue("$styleId still reaches into the capsule: $bounded", bounded >= CORNER_MARGIN_Y + CAPSULE_HEIGHT - 1f)
            }
        } finally {
            paints.restore()
        }
    }

    @Test
    fun leftParkedCapsuleOnlyMovesTheRowOnItsOwnSide() {
        val style = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_ORBIT)!!
        val paints = PaintPoolFixture.install()
        try {
            val leftPlain = cornerTextTop(style, null, false)
            val rightPlain = cornerTextTop(style, null, true)
            val leftParked = cornerTextTop(style, capsuleAtStart(), false)
            val rightParked = cornerTextTop(style, capsuleAtStart(), true)
            assertFalse(leftPlain.isNaN() || rightPlain.isNaN())
            assertTrue("the left context row should make room: $leftParked", leftParked >= CORNER_MARGIN_Y + CAPSULE_HEIGHT - 1f)
            assertEquals("the right date row should not move", rightPlain, rightParked, .01f)
        } finally {
            paints.restore()
        }
    }

    private fun cornerTextTop(style: ClockStyle, overlay: ClockOverlayBounds?, rightColumn: Boolean): Float {
        val state = longChineseState(false)
        val canvas = RecordingCanvas(VIEW_WIDTH, VIEW_HEIGHT)
        val context = ClockRenderContext(
            0f, 0f, VIEW_WIDTH, VIEW_HEIGHT, 2f, 2f, state.getTimeMillis(),
            ClockBackground.theme(false), 0f, 0f, true, overlay,
        )
        style.getRenderer().render(canvas, context, state, style.getThemeTokens())
        var highest = Float.NaN
        for (bounds in canvas.textBounds) {
            val inColumn = if (rightColumn) bounds.right > VIEW_WIDTH - 400f else bounds.left < 400f
            if (!inColumn || bounds.top > 400f) continue
            highest = if (highest.isNaN()) bounds.top else minOf(highest, bounds.top)
        }
        return highest
    }

    @Test
    fun statusCapsuleFollowsTheOwningPanel() {
        val w = 2560f
        val h = 1440f
        val density = 2f
        val marginX = 40f
        val marginY = 32f
        val capsuleW = 190f
        val capsuleH = 56f
        val dual = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_DUAL_BLOCKS, w, h, density, marginX, marginY, capsuleW, capsuleH)
        val dualPanelRight = w - w * .029f
        assertEquals(190f, dual[2] - dual[0], .01f)
        assertTrue("双块's capsule must sit inside the minutes panel: ${dual[2]}", dual[2] < dualPanelRight)
        assertEquals("双块's capsule starts on the date's line", h * .037f + minOf(density * 20f, h * .926f * .05f), dual[1], .01f)
        assertTrue("双块's capsule must sit below the panel top: ${dual[1]}", dual[1] > h * .037f)

        val blend = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_BLEND, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertTrue("混合's capsule must sit inside the digital panel: ${blend[2]}", blend[2] < w - w * .027f)
        assertTrue("混合's capsule must sit below the panel top: ${blend[1]}", blend[1] > h * .046f)

        val orbit = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_ORBIT, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("轨道's capsule lines up with its context row", w * .029f, orbit[0], .01f)
        assertEquals("轨道 landscape keeps the page margin", marginY, orbit[1], .01f)
        val orbitPortrait = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_ORBIT, h, w, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("轨道 portrait keeps the row's inset", h * .029f, orbitPortrait[0], .01f)
        assertTrue("轨道 portrait must drop the capsule to the row: ${orbitPortrait[1]}", orbitPortrait[1] > marginY)
        val rowTop = w * .072f - minOf(w, h) * .027f * 1.15f
        assertTrue("轨道 portrait must clear the context row: ${orbitPortrait[1]} + $capsuleH vs $rowTop", orbitPortrait[1] + capsuleH <= rowTop)

        val plain = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_RIBBON, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("丝带's capsule shares its context row's edge", w - w * .029f, plain[2], .01f)
        assertEquals(marginY, plain[1], .01f)
        val bubblesLandscape = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_BUBBLES, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("气泡's landscape capsule shares its context row's edge", w - w * .029f, bubblesLandscape[2], .01f)
        val bubblesPortrait = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_BUBBLES, h, w, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("气泡's portrait capsule has no row to align with", h - marginX, bubblesPortrait[2], .01f)
        assertEquals("气泡's portrait capsule starts on the date's line", minOf(minOf(h, w) * .045f, density * 20f), bubblesPortrait[1], .01f)

        val typographic = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_TYPOGRAPHIC, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("字形时刻's capsule shares its seconds block's edge", w - w * .07f, typographic[2], .01f)
        val gridLandscape = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_DIGITAL_GRID, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("数字网格's capsule shares its SEC panel's edge", w - w * .07f, gridLandscape[2], .01f)
        val gridPortrait = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_DIGITAL_GRID, h, w, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("数字网格's portrait capsule has nothing to align with", h - marginX, gridPortrait[2], .01f)
        val glass = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_GLASS_ATELIER, w, h, density, marginX, marginY, capsuleW, capsuleH)
        assertEquals("a face with no right column keeps the host margin", w - marginX, glass[2], .01f)
        val dualPortrait = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_DUAL_BLOCKS, h, w, density, marginX, marginY, capsuleW, capsuleH)
        assertTrue("双块 portrait must sit inside the hours panel: ${dualPortrait[2]}", dualPortrait[2] < h - h * .055f)
        assertTrue("双块 portrait must sit below the panel top: ${dualPortrait[1]}", dualPortrait[1] > w * .037f)
        val blendPortrait = UltimateClockStyles.statusCapsuleBounds(UltimateClockStyles.STYLE_BLEND, h, w, density, marginX, marginY, capsuleW, capsuleH)
        assertTrue("混合 portrait must sit inside the analogue panel: ${blendPortrait[2]}", blendPortrait[2] < h - h * .05f)
        assertTrue("混合 portrait must sit below the panel top: ${blendPortrait[1]}", blendPortrait[1] > w * .035f)
    }

    private fun assertRendersWithoutLeakingCanvasState(style: ClockStyle, state: ClockState, width: Int, height: Int) {
        val label = "${style.getMetadata().getId()} at ${width}x${height}"
        val canvas = TrackingCanvas()
        val initialSaveCount = canvas.currentSaveCount
        val context = ClockRenderContext(0f, 0f, width.toFloat(), height.toFloat(), 2f, 2f, state.getTimeMillis(), ClockBackground.theme(false), 2f * 56f)
        try {
            style.getRenderer().render(canvas, context, state, style.getThemeTokens())
        } catch (error: Throwable) {
            throw AssertionError("$label must render without throwing", error)
        }
        assertEquals("$label leaked Canvas save state", initialSaveCount, canvas.currentSaveCount)
        assertEquals("$label left save/restore calls unbalanced", canvas.saveCalls, canvas.restoreCalls)
        assertTrue("$label did not isolate its Canvas state", canvas.saveCalls > 0)
        assertEquals("$label distorted the face with a non-uniform scale", 0, canvas.nonUniformScaleCalls)
    }

    private fun longChineseState(): ClockState = longChineseState(true)

    private fun longChineseState(withWorldClocks: Boolean): ClockState {
        val builder = ClockState.builder(1787633430123L)
            .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
            .locale(Locale.SIMPLIFIED_CHINESE)
            .use24Hour(true)
            .showSeconds(true)
            .secondHandMotion(ClockState.SecondHandMotion.SWEEP)
            .dateText("2026 年 8 月 25 日 星期二 / 丙午[马]年七月十三 处暑 长中文日期压力测试")
            .timeZoneText("GMT+08:00 中国标准时间")
            .weatherText("深圳宝安 30 阴 / 体感温度 34 摄氏度")
            .timeScale(1.5f)
            .dateScale(1.5f)
            .supportingScale(1.5f)
        if (withWorldClocks) {
            builder.worldClocks(
                listOf(
                    WorldClockEntry("beijing", "北京", "中国", "Asia/Shanghai", "CN"),
                    WorldClockEntry("tokyo", "东京", "日本", "Asia/Tokyo", "JP"),
                    WorldClockEntry("london", "伦敦", "英国", "Europe/London", "GB"),
                    WorldClockEntry("new_york", "纽约", "美国", "America/New_York", "US"),
                    WorldClockEntry("sydney", "悉尼", "澳大利亚", "Australia/Sydney", "AU"),
                    WorldClockEntry("paris", "巴黎", "法国", "Europe/Paris", "FR"),
                ),
            )
        }
        return builder.build()
    }

    private open class TrackingCanvas : Canvas() {
        var currentSaveCount = 1
        var saveCalls = 0
        var restoreCalls = 0
        var nonUniformScaleCalls = 0

        override fun save(): Int {
            val checkpoint = currentSaveCount
            currentSaveCount++
            saveCalls++
            return checkpoint
        }

        override fun restore() {
            if (currentSaveCount <= 1) error("Canvas restore underflow")
            currentSaveCount--
            restoreCalls++
        }

        override fun restoreToCount(count: Int) {
            require(count in 1..currentSaveCount) { "Invalid Canvas save count $count" }
            currentSaveCount = count
            restoreCalls++
        }

        override fun getSaveCount(): Int = currentSaveCount

        override fun scale(sx: Float, sy: Float) {
            if (kotlin.math.abs(sx - sy) > .0001f) nonUniformScaleCalls++
        }
    }

    private class RecordingCanvas(private val width: Float, private val height: Float) : TrackingCanvas() {
        val textBounds = ArrayList<TextBounds>()

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            record(text, x, y, paint)
        }

        override fun drawText(text: String, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
            record(text.substring(start, end), x, y, paint)
        }

        private fun record(text: String, x: Float, baseline: Float, paint: Paint) {
            if (x == 0f) return
            val measured = paint.measureText(text)
            var left = x
            if (paint.textAlign == Paint.Align.CENTER) left -= measured * .5f
            else if (paint.textAlign == Paint.Align.RIGHT) left -= measured
            val metrics = paint.fontMetrics
            textBounds += TextBounds(text, left, baseline + metrics.ascent, left + measured, baseline + metrics.descent, paint.textSize)
        }

        fun assertTextInside(styleId: String) {
            assertFalse("$styleId drew no text", textBounds.isEmpty())
            for (bounds in textBounds) {
                val label = "$styleId text '${bounds.text}'"
                assertTrue("$label overflowed left: ${bounds.left}", bounds.left >= -1f)
                assertTrue("$label overflowed right: ${bounds.right}", bounds.right <= width + 1f)
                assertTrue("$label overflowed top: ${bounds.top}", bounds.top >= -1f)
                assertTrue("$label overflowed bottom: ${bounds.bottom}", bounds.bottom <= height + 1f)
                if (bounds.text.contains("年") || bounds.text.startsWith("深圳")) {
                    assertTrue("$label became smaller than 12sp: ${bounds.textSize}", bounds.textSize >= 24f - .1f)
                }
            }
        }
    }

    private data class TextBounds(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float, val textSize: Float)

    private class JvmPaint : Paint() {
        private var size = 0f
        private var align = Align.LEFT
        override fun reset() { size = 0f; align = Align.LEFT }
        override fun setTextSize(size: Float) { this.size = size }
        override fun getTextSize(): Float = size
        override fun measureText(text: String): Float = text.length * size * .55f
        override fun measureText(text: String, start: Int, end: Int): Float = (end - start).coerceAtLeast(0) * size * .55f
        override fun setTextAlign(align: Align?) { this.align = align ?: Align.LEFT }
        override fun getTextAlign(): Align = align
        override fun getFontMetrics(): FontMetrics = FontMetrics().also {
            it.top = -size; it.ascent = -size * .8f; it.descent = size * .2f; it.bottom = size * .25f
        }
    }

    private class PaintPoolFixture private constructor(
        private val paints: MutableList<Paint>,
        private val originalPaints: List<Paint>,
        private val pool: Any,
        private val nextIndex: Field,
        private val frameDepth: Field,
        private val originalNextIndex: Int,
        private val originalFrameDepth: Int,
    ) {
        companion object {
            private const val PAINT_CAPACITY = 128

            @Suppress("UNCHECKED_CAST")
            fun install(): PaintPoolFixture {
                val rendererBase = Class.forName(UltimateClockStyles::class.java.name + "\$RendererBase")
                val threadLocalField = rendererBase.getDeclaredField("PAINT_POOL").also { it.isAccessible = true }
                val pool = (threadLocalField.get(null) as ThreadLocal<*>).get()!!
                val paintsField = pool.javaClass.getDeclaredField("paints").also { it.isAccessible = true }
                val paints = paintsField.get(pool) as MutableList<Paint>
                val originalPaints = paints.toList()
                val nextIndex = pool.javaClass.getDeclaredField("nextIndex").also { it.isAccessible = true }
                val frameDepth = pool.javaClass.getDeclaredField("frameDepth").also { it.isAccessible = true }
                val originalNextIndex = nextIndex.getInt(pool)
                val originalFrameDepth = frameDepth.getInt(pool)
                paints.clear()
                repeat(PAINT_CAPACITY) { paints += JvmPaint() }
                nextIndex.setInt(pool, 0)
                frameDepth.setInt(pool, 0)
                return PaintPoolFixture(paints, originalPaints, pool, nextIndex, frameDepth, originalNextIndex, originalFrameDepth)
            }
        }

        fun restore() {
            paints.clear()
            paints.addAll(originalPaints)
            nextIndex.setInt(pool, originalNextIndex)
            frameDepth.setInt(pool, originalFrameDepth)
        }
    }
}
