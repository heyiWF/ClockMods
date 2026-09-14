package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.clockmods.sdk.clock.ClockBackground;
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
