package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.clockmods.sdk.clock.ClockBackground;
import com.clockmods.sdk.clock.ClockOverlayBounds;
import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleRegistry;
import com.clockmods.sdk.clock.WorldClockEntry;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class UltimateClockRendererSmokeTest {
    private static final String[] NON_PRO_STYLE_IDS = {
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
            UltimateClockStyles.STYLE_RIBBON
    };
    private static final int[][] VIEWPORTS = {
            {1600, 900},
            {900, 1600},
            {166, 94}
    };

    private static final String[] CORE_STYLE_IDS = {
            UltimateClockStyles.STYLE_PRO_CLASSIC,
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
            UltimateClockStyles.STYLE_PAPER_STATION,
            UltimateClockStyles.STYLE_ORBIT_NEON,
            UltimateClockStyles.STYLE_DIGITAL_GRID,
            UltimateClockStyles.STYLE_TYPOGRAPHIC
    };

    @Test
    public void nonProRenderersHandleLongChineseDateAtSupportedViewportSizes()
            throws Exception {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
        ClockState state = longChineseState();
        PaintPoolFixture paints = PaintPoolFixture.install();
        try {
            for (String styleId : NON_PRO_STYLE_IDS) {
                ClockStyle style = registry.find(styleId);
                Assert.assertNotNull(styleId, style);
                for (int[] viewport : VIEWPORTS) {
                    assertRendersWithoutLeakingCanvasState(style, state,
                            viewport[0], viewport[1]);
                }
            }
        } finally {
            paints.restore();
        }
    }

    @Test
    public void coreBuiltInsKeepTextInsideANarrowPortraitViewport() throws Exception {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
        ClockState state = longChineseState(false);
        PaintPoolFixture paints = PaintPoolFixture.install();
        try {
            for (String styleId : CORE_STYLE_IDS) {
                ClockStyle style = registry.find(styleId);
                RecordingCanvas canvas = new RecordingCanvas(720f, 1600f);
                ClockRenderContext context = new ClockRenderContext(
                        0f, 0f, 720f, 1600f, 2f, 2f, state.getTimeMillis(),
                        ClockBackground.theme(false), 2f * 28f);
                style.getRenderer().render(canvas, context, state, style.getThemeTokens());
                canvas.assertTextInside(styleId);
            }
        } finally {
            paints.restore();
        }
    }

    @Test public void largerDateWrapsBeforeShrinkingAndReturnsToOneLine() throws Exception {
        String solar = "2026 年 9 月 13 日 星期日";
        String lunar = "丙午[马]年八月初三";
        String combined = solar + " / " + lunar;
        ClockStyle style = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_ORBIT);
        PaintPoolFixture paints = PaintPoolFixture.install();
        try {
            for (float scale : new float[] {.6f, 1.8f, .6f}) {
                ClockState state = ClockState.builder(1787633430123L).dateText(combined)
                        .locale(Locale.SIMPLIFIED_CHINESE).dateScale(scale).build();
                RecordingCanvas canvas = new RecordingCanvas(2560f, 1440f);
                ClockRenderContext context = new ClockRenderContext(0f, 0f, 2560f, 1440f,
                        2f, 2f, state.getTimeMillis());
                style.getRenderer().render(canvas, context, state, style.getThemeTokens());
                canvas.assertTextInside(UltimateClockStyles.STYLE_ORBIT);
                if (scale < 1f) {
                    Assert.assertTrue(canvas.textBounds.stream().anyMatch(b -> combined.equals(b.text)));
                } else {
                    TextBounds first = canvas.textBounds.stream().filter(b -> solar.equals(b.text))
                            .findFirst().orElseThrow(() -> new AssertionError("Missing solar row"));
                    TextBounds second = canvas.textBounds.stream().filter(b -> lunar.equals(b.text))
                            .findFirst().orElseThrow(() -> new AssertionError("Missing lunar row"));
                    Assert.assertEquals(1440f * .032f * scale, first.textSize, .01f);
                    Assert.assertEquals(first.textSize, second.textSize, .01f);
                    Assert.assertTrue(second.top > first.bottom);
                }
            }
        } finally {
            paints.restore();
        }
    }

    @Test public void dateWrappingHonorsWidthAndNaturalBoundaries() {
        JvmPaint paint = new JvmPaint();
        paint.setTextSize(30f);
        String date = "September 13, 2026 Sunday";
        Assert.assertArrayEquals(new String[] {date, ""}, UltimateClockStyles.dateLines(
                date, paint, paint.measureText(date), false, Locale.US));
        Assert.assertArrayEquals(new String[] {"September 13,", "2026 Sunday"},
                UltimateClockStyles.dateLines(date, paint, 240f, false, Locale.US));
        Assert.assertArrayEquals(new String[] {"", ""}, UltimateClockStyles.dateLines(
                "", paint, 100f, false, Locale.US));
        String solar = "09 / 13 / 2026";
        String lunar = "丙午[马]年八月初三";
        Assert.assertArrayEquals(new String[] {solar, lunar}, UltimateClockStyles.dateLines(
                solar + " / " + lunar, paint, 1000f, true, Locale.SIMPLIFIED_CHINESE));
    }

    /** A 90x64px capsule parked 20dp/16dp in from a corner, as the host computes it at 2x density. */
    private static final float CORNER_MARGIN_X = 40f;
    private static final float CORNER_MARGIN_Y = 32f;
    private static final float CAPSULE_WIDTH = 90f;
    private static final float CAPSULE_HEIGHT = 64f;
    private static final float VIEW_WIDTH = 2560f;
    private static final float VIEW_HEIGHT = 1440f;

    private static ClockOverlayBounds capsuleAtEnd() {
        float right = VIEW_WIDTH - CORNER_MARGIN_X;
        return new ClockOverlayBounds(right - CAPSULE_WIDTH, CORNER_MARGIN_Y, right,
                CORNER_MARGIN_Y + CAPSULE_HEIGHT);
    }

    private static ClockOverlayBounds capsuleAtStart() {
        return new ClockOverlayBounds(CORNER_MARGIN_X, CORNER_MARGIN_Y,
                CORNER_MARGIN_X + CAPSULE_WIDTH, CORNER_MARGIN_Y + CAPSULE_HEIGHT);
    }

    /**
     * A right-anchored metadata row has to drop below a capsule parked in the right corner.
     *
     * <p>Only 轨道, 气泡 and 丝带 are asserted here: {@code dual_blocks} and {@code blend} anchor
     * their frames with {@link android.graphics.RectF}, whose geometry methods are stubs in Gradle's
     * mockable android.jar, so their rows are drawn at x=0 on the JVM and cannot be measured. Their
     * clearance was verified on device.</p>
     */
    @Test public void topRightMetadataClearsTheHostStatusCapsule() throws Exception {
        String[] styleIds = {
                UltimateClockStyles.STYLE_ORBIT,
                UltimateClockStyles.STYLE_BUBBLES,
                UltimateClockStyles.STYLE_RIBBON
        };
        PaintPoolFixture paints = PaintPoolFixture.install();
        try {
            for (String styleId : styleIds) {
                ClockStyle style = UltimateClockStyles.createRegistry().find(styleId);
                Assert.assertNotNull(styleId, style);
                float unbounded = cornerTextTop(style, null, true);
                float bounded = cornerTextTop(style, capsuleAtEnd(), true);
                Assert.assertFalse(styleId + " drew no top-right row to check",
                        Float.isNaN(unbounded));
                Assert.assertTrue(styleId + " no longer reproduces the overlap, fixture is stale: "
                                + unbounded,
                        unbounded < CORNER_MARGIN_Y + CAPSULE_HEIGHT);
                Assert.assertTrue(styleId + " still reaches into the capsule: " + bounded,
                        bounded >= CORNER_MARGIN_Y + CAPSULE_HEIGHT - 1f);
            }
        } finally {
            paints.restore();
        }
    }

    /**
     * The capsule is a box, not a strip: parking it in the left corner must move 轨道's context row
     * — which is the row on that side — and leave the date row on the far side untouched.
     */
    @Test public void leftParkedCapsuleOnlyMovesTheRowOnItsOwnSide() throws Exception {
        ClockStyle style = UltimateClockStyles.createRegistry()
                .find(UltimateClockStyles.STYLE_ORBIT);
        Assert.assertNotNull(style);
        PaintPoolFixture paints = PaintPoolFixture.install();
        try {
            float leftPlain = cornerTextTop(style, null, false);
            float rightPlain = cornerTextTop(style, null, true);
            float leftParked = cornerTextTop(style, capsuleAtStart(), false);
            float rightParked = cornerTextTop(style, capsuleAtStart(), true);
            Assert.assertFalse(Float.isNaN(leftPlain) || Float.isNaN(rightPlain));
            Assert.assertTrue("the left context row should make room: " + leftParked,
                    leftParked >= CORNER_MARGIN_Y + CAPSULE_HEIGHT - 1f);
            Assert.assertEquals("the right date row should not move", rightPlain, rightParked, .01f);
        } finally {
            paints.restore();
        }
    }

    /**
     * The top edge of the highest text drawn in one side column, or NaN when there is none. Text
     * below {@code CORNER_DEPTH} is ignored: it cannot collide with a capsule along the top edge.
     */
    private static float cornerTextTop(ClockStyle style, ClockOverlayBounds overlay,
            boolean rightColumn) {
        float cornerDepth = 400f;
        ClockState state = longChineseState(false);
        RecordingCanvas canvas = new RecordingCanvas(VIEW_WIDTH, VIEW_HEIGHT);
        ClockRenderContext context = new ClockRenderContext(
                0f, 0f, VIEW_WIDTH, VIEW_HEIGHT, 2f, 2f, state.getTimeMillis(),
                ClockBackground.theme(false), 0f, 0f, true, overlay);
        style.getRenderer().render(canvas, context, state, style.getThemeTokens());
        float highest = Float.NaN;
        for (TextBounds bounds : canvas.textBounds) {
            boolean inColumn = rightColumn ? bounds.right > VIEW_WIDTH - 400f : bounds.left < 400f;
            if (!inColumn || bounds.top > cornerDepth) continue;
            highest = Float.isNaN(highest) ? bounds.top : Math.min(highest, bounds.top);
        }
        return highest;
    }

    /**
     * The capsule has to land where the style's own composition can live with it: inside the panel
     * that owns the corner for the two card faces, on the context row's inset for 轨道, and in the
     * plain corner for everything else.
     */
    @Test public void statusCapsuleFollowsTheOwningPanel() {
        float w = 2560f, h = 1440f, density = 2f;
        float marginX = 40f, marginY = 32f, capsuleW = 190f, capsuleH = 56f;

        float[] dual = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_DUAL_BLOCKS, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        float dualPanelRight = w - w * .029f;
        Assert.assertEquals(190f, dual[2] - dual[0], .01f);
        Assert.assertTrue("双块's capsule must sit inside the minutes panel: " + dual[2],
                dual[2] < dualPanelRight);
        // The date starts at min(density*20, panelHeight*.05) below the panel's top edge, so the
        // capsule has to start there too or the two cards' contents begin on different lines.
        Assert.assertEquals("双块's capsule starts on the date's line",
                h * .037f + Math.min(density * 20f, h * .926f * .05f), dual[1], .01f);
        Assert.assertTrue("双块's capsule must sit below the panel top: " + dual[1],
                dual[1] > h * .037f);

        float[] blend = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_BLEND, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertTrue("混合's capsule must sit inside the digital panel: " + blend[2],
                blend[2] < w - w * .027f);
        Assert.assertTrue("混合's capsule must sit below the panel top: " + blend[1],
                blend[1] > h * .046f);

        float[] orbit = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_ORBIT, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("轨道's capsule lines up with its context row",
                w * .029f, orbit[0], .01f);
        Assert.assertEquals("轨道 landscape keeps the page margin", marginY, orbit[1], .01f);

        // Portrait: the context row sits at h*.072, far enough down that the capsule has to follow
        // it instead of staying up in the corner.
        float[] orbitPortrait = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_ORBIT, h, w, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("轨道 portrait keeps the row's inset",
                h * .029f, orbitPortrait[0], .01f);
        Assert.assertTrue("轨道 portrait must drop the capsule to the row: " + orbitPortrait[1],
                orbitPortrait[1] > marginY);
        float rowTop = w * .072f - Math.min(w, h) * .027f * 1.15f;
        Assert.assertTrue("轨道 portrait must clear the context row: "
                        + orbitPortrait[1] + " + " + capsuleH + " vs " + rowTop,
                orbitPortrait[1] + capsuleH <= rowTop);

        float[] plain = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_RIBBON, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("丝带's capsule shares its context row's edge",
                w - w * .029f, plain[2], .01f);
        Assert.assertEquals(marginY, plain[1], .01f);

        // 气泡 only has a row on that edge in landscape; portrait moves both rows to the left.
        float[] bubblesLandscape = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_BUBBLES, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("气泡's landscape capsule shares its context row's edge",
                w - w * .029f, bubblesLandscape[2], .01f);
        float[] bubblesPortrait = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_BUBBLES, h, w, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("气泡's portrait capsule has no row to align with",
                h - marginX, bubblesPortrait[2], .01f);
        // Portrait stacks its metadata down the left, so the capsule starts on the two-line date
        // block's safe inset rather than on the page margin.
        Assert.assertEquals("气泡's portrait capsule starts on the date's line",
                Math.min(Math.min(h, w) * .045f, density * 20f), bubblesPortrait[1], .01f);

        // 字形时刻 right-aligns its seconds block, and 数字网格's landscape SEC panel ends there.
        float[] typographic = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_TYPOGRAPHIC, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("字形时刻's capsule shares its seconds block's edge",
                w - w * .07f, typographic[2], .01f);
        float[] gridLandscape = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_DIGITAL_GRID, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("数字网格's capsule shares its SEC panel's edge",
                w - w * .07f, gridLandscape[2], .01f);
        float[] gridPortrait = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_DIGITAL_GRID, h, w, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("数字网格's portrait capsule has nothing to align with",
                h - marginX, gridPortrait[2], .01f);

        // A face with nothing right-aligned in that corner keeps the host's page margin.
        float[] glass = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_GLASS_ATELIER, w, h, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertEquals("a face with no right column keeps the host margin",
                w - marginX, glass[2], .01f);

        // Portrait stacks the panels, so the corner belongs to the top card there too.
        float[] dualPortrait = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_DUAL_BLOCKS, h, w, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertTrue("双块 portrait must sit inside the hours panel: " + dualPortrait[2],
                dualPortrait[2] < h - h * .055f);
        Assert.assertTrue("双块 portrait must sit below the panel top: " + dualPortrait[1],
                dualPortrait[1] > w * .037f);

        float[] blendPortrait = UltimateClockStyles.statusCapsuleBounds(
                UltimateClockStyles.STYLE_BLEND, h, w, density,
                marginX, marginY, capsuleW, capsuleH);
        Assert.assertTrue("混合 portrait must sit inside the analogue panel: " + blendPortrait[2],
                blendPortrait[2] < h - h * .05f);
        Assert.assertTrue("混合 portrait must sit below the panel top: " + blendPortrait[1],
                blendPortrait[1] > w * .035f);
    }

    private static void assertRendersWithoutLeakingCanvasState(ClockStyle style,
            ClockState state, int width, int height) {
        String label = style.getMetadata().getId() + " at " + width + "x" + height;
        TrackingCanvas canvas = new TrackingCanvas();
        int initialSaveCount = canvas.getSaveCount();
        ClockRenderContext context = new ClockRenderContext(
                0f, 0f, width, height, 2f, 2f, state.getTimeMillis(),
                ClockBackground.theme(false), 2f * 56f);

        try {
            style.getRenderer().render(canvas, context, state, style.getThemeTokens());
        } catch (Throwable error) {
            throw new AssertionError(label + " must render without throwing", error);
        }

        Assert.assertEquals(label + " leaked Canvas save state",
                initialSaveCount, canvas.getSaveCount());
        Assert.assertEquals(label + " left save/restore calls unbalanced",
                canvas.saveCalls, canvas.restoreCalls);
        Assert.assertTrue(label + " did not isolate its Canvas state",
                canvas.saveCalls > 0);
        Assert.assertEquals(label + " distorted the face with a non-uniform scale",
                0, canvas.nonUniformScaleCalls);
    }

    private static ClockState longChineseState() {
        return longChineseState(true);
    }

    private static ClockState longChineseState(boolean withWorldClocks) {
        ClockState.Builder builder = ClockState.builder(1787633430123L)
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .locale(Locale.SIMPLIFIED_CHINESE)
                .use24Hour(true)
                .showSeconds(true)
                .secondHandMotion(ClockState.SecondHandMotion.SWEEP)
                .dateText("2026 \u5e74 8 \u6708 25 \u65e5 \u661f\u671f\u4e8c / "
                        + "\u4e19\u5348[\u9a6c]\u5e74\u4e03\u6708\u5341\u4e09 "
                        + "\u5904\u6691 \u957f\u4e2d\u6587\u65e5\u671f\u538b\u529b\u6d4b\u8bd5")
                .timeZoneText("GMT+08:00 \u4e2d\u56fd\u6807\u51c6\u65f6\u95f4")
                .weatherText("\u6df1\u5733\u5b9d\u5b89 30 \u9634 / "
                        + "\u4f53\u611f\u6e29\u5ea6 34 \u6444\u6c0f\u5ea6")
                .statusText("Wi-Fi / 82%")
                .timeScale(1.5f)
                .dateScale(1.5f)
                .supportingScale(1.5f);
        if (withWorldClocks) {
            builder.worldClocks(Arrays.asList(
                    new WorldClockEntry("beijing", "北京", "中国", "Asia/Shanghai", "CN"),
                    new WorldClockEntry("tokyo", "东京", "日本", "Asia/Tokyo", "JP"),
                    new WorldClockEntry("london", "伦敦", "英国", "Europe/London", "GB"),
                    new WorldClockEntry("new_york", "纽约", "美国", "America/New_York", "US"),
                    new WorldClockEntry("sydney", "悉尼", "澳大利亚", "Australia/Sydney", "AU"),
                    new WorldClockEntry("paris", "巴黎", "法国", "Europe/Paris", "FR")));
        }
        return builder.build();
    }

    /** Tracks the save stack that the mockable Android Canvas does not implement. */
    private static class TrackingCanvas extends Canvas {
        private int saveCount = 1;
        private int saveCalls;
        private int restoreCalls;
        private int nonUniformScaleCalls;

        @Override public int save() {
            int checkpoint = saveCount;
            saveCount++;
            saveCalls++;
            return checkpoint;
        }

        @Override public void restore() {
            if (saveCount <= 1) throw new IllegalStateException("Canvas restore underflow");
            saveCount--;
            restoreCalls++;
        }

        @Override public void restoreToCount(int count) {
            if (count < 1 || count > saveCount) {
                throw new IllegalArgumentException("Invalid Canvas save count " + count);
            }
            saveCount = count;
            restoreCalls++;
        }

        @Override public int getSaveCount() {
            return saveCount;
        }

        @Override public void scale(float sx, float sy) {
            if (Math.abs(sx - sy) > .0001f) nonUniformScaleCalls++;
        }

    }

    /** Captures text geometry using the same deterministic metrics as {@link JvmPaint}. */
    private static final class RecordingCanvas extends TrackingCanvas {
        private final float width;
        private final float height;
        private final List<TextBounds> textBounds = new ArrayList<TextBounds>();

        RecordingCanvas(float width, float height) {
            this.width = width;
            this.height = height;
        }

        @Override public void drawText(String text, float x, float y, Paint paint) {
            record(text, x, y, paint);
        }

        @Override public void drawText(String text, int start, int end, float x, float y,
                Paint paint) {
            record(text == null ? "" : text.substring(start, end), x, y, paint);
        }

        private void record(String text, float x, float baseline, Paint paint) {
            // RectF geometry methods are stubs in mockable android.jar. Ignore the zero anchor
            // produced by those methods (for example Digital Grid's SEC panel); all core metadata
            // and titles use direct viewport coordinates and remain fully checked below.
            if (x == 0f) return;
            float measured = paint.measureText(text);
            float left = x;
            if (paint.getTextAlign() == Paint.Align.CENTER) left -= measured * .5f;
            else if (paint.getTextAlign() == Paint.Align.RIGHT) left -= measured;
            Paint.FontMetrics metrics = paint.getFontMetrics();
            textBounds.add(new TextBounds(text, left, baseline + metrics.ascent,
                    left + measured, baseline + metrics.descent, paint.getTextSize()));
        }

        void assertTextInside(String styleId) {
            Assert.assertFalse(styleId + " drew no text", textBounds.isEmpty());
            for (TextBounds bounds : textBounds) {
                String label = styleId + " text '" + bounds.text + "'";
                Assert.assertTrue(label + " overflowed left: " + bounds.left,
                        bounds.left >= -1f);
                Assert.assertTrue(label + " overflowed right: " + bounds.right,
                        bounds.right <= width + 1f);
                Assert.assertTrue(label + " overflowed top: " + bounds.top,
                        bounds.top >= -1f);
                Assert.assertTrue(label + " overflowed bottom: " + bounds.bottom,
                        bounds.bottom <= height + 1f);
                if (bounds.text.contains("年") || bounds.text.startsWith("深圳")) {
                    Assert.assertTrue(label + " became smaller than 12sp: " + bounds.textSize,
                            bounds.textSize >= 24f - .1f);
                }
            }
        }
    }

    private static final class TextBounds {
        final String text;
        final float left;
        final float top;
        final float right;
        final float bottom;
        final float textSize;

        TextBounds(String text, float left, float top, float right, float bottom,
                float textSize) {
            this.text = text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.textSize = textSize;
        }
    }

    /** Supplies deterministic text metrics missing from Gradle's mockable android.jar. */
    private static final class JvmPaint extends Paint {
        private float textSize;
        private Align textAlign = Align.LEFT;

        @Override public void reset() {
            textSize = 0f;
            textAlign = Align.LEFT;
        }

        @Override public void setTextSize(float size) {
            textSize = size;
        }

        @Override public float getTextSize() {
            return textSize;
        }

        @Override public float measureText(String text) {
            return text == null ? 0f : text.length() * textSize * 0.55f;
        }

        @Override public float measureText(String text, int start, int end) {
            return text == null ? 0f : Math.max(0, end - start) * textSize * 0.55f;
        }

        @Override public void setTextAlign(Align align) {
            textAlign = align == null ? Align.LEFT : align;
        }

        @Override public Align getTextAlign() {
            return textAlign;
        }

        @Override public FontMetrics getFontMetrics() {
            FontMetrics metrics = new FontMetrics();
            metrics.top = -textSize;
            metrics.ascent = -textSize * 0.8f;
            metrics.descent = textSize * 0.2f;
            metrics.bottom = textSize * 0.25f;
            return metrics;
        }
    }

    private static final class PaintPoolFixture {
        private static final int PAINT_CAPACITY = 128;

        private final List<Paint> paints;
        private final List<Paint> originalPaints;
        private final Object pool;
        private final Field nextIndex;
        private final Field frameDepth;
        private final int originalNextIndex;
        private final int originalFrameDepth;

        private PaintPoolFixture(List<Paint> paints, List<Paint> originalPaints,
                Object pool, Field nextIndex, Field frameDepth,
                int originalNextIndex, int originalFrameDepth) {
            this.paints = paints;
            this.originalPaints = originalPaints;
            this.pool = pool;
            this.nextIndex = nextIndex;
            this.frameDepth = frameDepth;
            this.originalNextIndex = originalNextIndex;
            this.originalFrameDepth = originalFrameDepth;
        }

        @SuppressWarnings("unchecked")
        static PaintPoolFixture install() throws Exception {
            Class<?> rendererBase = Class.forName(
                    UltimateClockStyles.class.getName() + "$RendererBase");
            Field threadLocalField = rendererBase.getDeclaredField("PAINT_POOL");
            threadLocalField.setAccessible(true);
            Object pool = ((ThreadLocal<?>) threadLocalField.get(null)).get();

            Field paintsField = pool.getClass().getDeclaredField("paints");
            paintsField.setAccessible(true);
            List<Paint> paints = (List<Paint>) paintsField.get(pool);
            List<Paint> originalPaints = new ArrayList<Paint>(paints);

            Field nextIndex = pool.getClass().getDeclaredField("nextIndex");
            Field frameDepth = pool.getClass().getDeclaredField("frameDepth");
            nextIndex.setAccessible(true);
            frameDepth.setAccessible(true);
            int originalNextIndex = nextIndex.getInt(pool);
            int originalFrameDepth = frameDepth.getInt(pool);

            paints.clear();
            for (int index = 0; index < PAINT_CAPACITY; index++) {
                paints.add(new JvmPaint());
            }
            nextIndex.setInt(pool, 0);
            frameDepth.setInt(pool, 0);
            return new PaintPoolFixture(paints, originalPaints, pool, nextIndex, frameDepth,
                    originalNextIndex, originalFrameDepth);
        }

        void restore() throws IllegalAccessException {
            paints.clear();
            paints.addAll(originalPaints);
            nextIndex.setInt(pool, originalNextIndex);
            frameDepth.setInt(pool, originalFrameDepth);
        }
    }
}
