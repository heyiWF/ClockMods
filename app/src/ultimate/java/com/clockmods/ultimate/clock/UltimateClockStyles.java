package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;

import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockBackground;
import com.clockmods.sdk.clock.ClockRenderer;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleMetadata;
import com.clockmods.sdk.clock.ClockStyleRegistry;
import com.clockmods.sdk.clock.ClockThemeTokens;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Built-in Ultimate styles. Each renderer has a different composition, not merely a palette. */
public final class UltimateClockStyles {
    public static final String STYLE_PRO_CLASSIC = "pro.classic";
    public static final String STYLE_GLASS_ATELIER = "glass.atelier";
    public static final String STYLE_NOIR_INSTRUMENT = "noir.instrument";
    public static final String STYLE_PAPER_STATION = "paper.station";
    public static final String STYLE_ORBIT_NEON = "orbit.neon";
    public static final String STYLE_DIGITAL_GRID = "digital.grid";
    public static final String STYLE_TYPOGRAPHIC = "typographic.poster";

    private static final ClockStyleRegistry SHARED_REGISTRY = createRegistry();

    private UltimateClockStyles() { }

    /** Returns a fresh registry so an app or plugin may add styles without global mutable state. */
    public static ClockStyleRegistry createRegistry() {
        ClockStyleRegistry registry = new ClockStyleRegistry();
        for (ClockStyle style : builtIns()) registry.register(style);
        registry.setFallback(STYLE_GLASS_ATELIER);
        return registry;
    }

    /** Process-local extension point for styles registered by the host before views are created. */
    public static ClockStyleRegistry sharedRegistry() {
        return SHARED_REGISTRY;
    }

    public static List<ClockStyle> builtIns() {
        List<ClockStyle> styles = new ArrayList<ClockStyle>(7);
        styles.add(style(STYLE_PRO_CLASSIC, "Pro Classic",
                "The original ClockMods Pro face with its full set of display customizations.",
                ClockStyleMetadata.Kind.DIGITAL, proClassicTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR
                }, new ProClassicRenderer()));
        styles.add(style(STYLE_GLASS_ATELIER, "Glass Atelier",
                "A luminous metal and glass watch face inspired by premium industrial design.",
                ClockStyleMetadata.Kind.ANALOG, glassTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.SMOOTH_SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new GlassAtelierRenderer()));
        styles.add(style(STYLE_NOIR_INSTRUMENT, "Noir Instrument",
                "A calibrated black instrument panel with a secondary seconds gauge.",
                ClockStyleMetadata.Kind.ANALOG, noirTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.SMOOTH_SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new NoirInstrumentRenderer()));
        styles.add(style(STYLE_PAPER_STATION, "Paper Station",
                "A quiet paper planner face with ink marks, calendar rules, and a red index hand.",
                ClockStyleMetadata.Kind.ANALOG, paperTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.SMOOTH_SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new PaperStationRenderer()));
        styles.add(style(STYLE_ORBIT_NEON, "Orbit Neon",
                "Concentric orbital progress rings turn time into a living instrument.",
                ClockStyleMetadata.Kind.HYBRID, orbitTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.SMOOTH_SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new OrbitNeonRenderer()));
        styles.add(style(STYLE_DIGITAL_GRID, "Digital Grid",
                "A modular seven-segment display laid over a precise technical grid.",
                ClockStyleMetadata.Kind.DIGITAL, digitalTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new DigitalGridRenderer()));
        styles.add(style(STYLE_TYPOGRAPHIC, "Typographic",
                "A bold editorial layout where time, date, and context form a measured poster.",
                ClockStyleMetadata.Kind.DIGITAL, typeTokens(),
                new ClockStyleCapabilities.Capability[] {
                        ClockStyleCapabilities.Capability.SECONDS,
                        ClockStyleCapabilities.Capability.DATE,
                        ClockStyleCapabilities.Capability.TIME_ZONE,
                        ClockStyleCapabilities.Capability.WEATHER,
                        ClockStyleCapabilities.Capability.STATUS,
                        ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                        ClockStyleCapabilities.Capability.REDUCED_MOTION
                }, new TypographicRenderer()));
        return styles;
    }

    private static ClockStyle style(String id, String name, String description,
            ClockStyleMetadata.Kind kind, ClockThemeTokens tokens,
            ClockStyleCapabilities.Capability[] capabilities, ClockRenderer renderer) {
        return new BuiltInStyle(new ClockStyleMetadata(id, name, description, kind,
                ClockStyleCapabilities.of(capabilities), 1, 14), tokens, renderer);
    }

    private static ClockThemeTokens glassTokens() {
        return ClockThemeTokens.builder()
                .background(0xFFF7F9FC, 0xFFE1E6EF)
                .surfaceColor(0xD9F9FBFF)
                .primaryTextColor(0xFF1F2632)
                .secondaryTextColor(0xFF697181)
                .accentColor(0xFFFF554F)
                .lineColor(0xFF697181)
                .fonts("sans-serif", "sans-serif")
                .strokeScale(1f).build();
    }

    private static ClockThemeTokens proClassicTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF000000, 0xFF000000)
                .surfaceColor(0xFF000000)
                .primaryTextColor(0xFFFFFFFF)
                .secondaryTextColor(0xD9FFFFFF)
                .accentColor(0xFFFFFFFF)
                .lineColor(0x66FFFFFF)
                .fonts("sans-serif", "sans-serif")
                .strokeScale(1f).build();
    }

    private static ClockThemeTokens noirTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF17191D, 0xFF050608)
                .surfaceColor(0xFF171A1F)
                .primaryTextColor(0xFFE8E5DB)
                .secondaryTextColor(0xFF8C928D)
                .accentColor(0xFFFFB34D)
                .lineColor(0xFF4F5758)
                .fonts("sans-serif-condensed", "monospace")
                .strokeScale(1f).build();
    }

    private static ClockThemeTokens paperTokens() {
        return ClockThemeTokens.builder()
                .background(0xFFF6F0E4, 0xFFE5D7C3)
                .surfaceColor(0xFFF9F5EC)
                .primaryTextColor(0xFF29251F)
                .secondaryTextColor(0xFF756B5D)
                .accentColor(0xFFC84F45)
                .lineColor(0xFFB7A78F)
                .fonts("serif", "sans-serif")
                .strokeScale(0.95f).build();
    }

    private static ClockThemeTokens orbitTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF101A25, 0xFF071016)
                .surfaceColor(0xFF132534)
                .primaryTextColor(0xFFEAFBFF)
                .secondaryTextColor(0xFF87A6B5)
                .accentColor(0xFF37E5D4)
                .lineColor(0xFF2A6172)
                .fonts("sans-serif-light", "monospace")
                .strokeScale(1.05f).build();
    }

    private static ClockThemeTokens digitalTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF071314, 0xFF020708)
                .surfaceColor(0xFF0B2020)
                .primaryTextColor(0xFFA9FFD2)
                .secondaryTextColor(0xFF5B9B80)
                .accentColor(0xFFFFD166)
                .lineColor(0xFF1F5A4D)
                .fonts("monospace", "monospace")
                .strokeScale(1f).build();
    }

    private static ClockThemeTokens typeTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF25262B, 0xFF111217)
                .surfaceColor(0xFF30323A)
                .primaryTextColor(0xFFF4F0E8)
                .secondaryTextColor(0xFFB1B4BE)
                .accentColor(0xFFE66A45)
                .lineColor(0xFF626775)
                .fonts("sans-serif-light", "sans-serif")
                .strokeScale(1f).build();
    }

    private static final class BuiltInStyle implements ClockStyle {
        private final ClockStyleMetadata metadata;
        private final ClockThemeTokens tokens;
        private final ClockRenderer renderer;

        BuiltInStyle(ClockStyleMetadata metadata, ClockThemeTokens tokens, ClockRenderer renderer) {
            this.metadata = metadata;
            this.tokens = tokens;
            this.renderer = new IsolatedRenderer(renderer);
        }

        @Override public ClockStyleMetadata getMetadata() { return metadata; }
        @Override public ClockThemeTokens getThemeTokens() { return tokens; }
        @Override public ClockRenderer getRenderer() { return renderer; }
    }

    private static final class IsolatedRenderer implements ClockRenderer {
        private final ClockRenderer delegate;

        IsolatedRenderer(ClockRenderer delegate) {
            this.delegate = delegate;
        }

        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            int paintMarker = RendererBase.beginPaintFrame();
            int saveCount = canvas.save();
            try {
                delegate.render(canvas, context, state, theme);
            } finally {
                canvas.restoreToCount(saveCount);
                RendererBase.endPaintFrame(paintMarker);
            }
        }
    }

    private abstract static class RendererBase implements ClockRenderer {
        protected static final float TWO_PI = (float) (Math.PI * 2d);
        private static final ThreadLocal<PaintPool> PAINT_POOL = new ThreadLocal<PaintPool>() {
            @Override protected PaintPool initialValue() {
                return new PaintPool();
            }
        };

        private static int beginPaintFrame() {
            return PAINT_POOL.get().beginFrame();
        }

        private static void endPaintFrame(int marker) {
            PAINT_POOL.get().endFrame(marker);
        }

        protected static Paint fill(int color) {
            Paint p = PAINT_POOL.get().obtain();
            p.reset();
            p.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            return p;
        }

        protected static Paint stroke(int color, float width) {
            Paint p = PAINT_POOL.get().obtain();
            p.reset();
            p.setFlags(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(color);
            return p;
        }

        private static final class PaintPool {
            private final List<Paint> paints = new ArrayList<Paint>();
            private int nextIndex;
            private int frameDepth;

            int beginFrame() {
                if (frameDepth == 0) nextIndex = 0;
                int marker = nextIndex;
                frameDepth++;
                return marker;
            }

            void endFrame(int marker) {
                nextIndex = marker;
                if (frameDepth > 0) frameDepth--;
                if (frameDepth == 0) nextIndex = 0;
            }

            Paint obtain() {
                if (nextIndex == paints.size()) paints.add(new Paint());
                return paints.get(nextIndex++);
            }
        }

        protected static int alpha(int color, int value) {
            return Color.argb(value, Color.red(color), Color.green(color), Color.blue(color));
        }

        protected static Typeface typeface(String family, int style) {
            return Typeface.create(family == null ? "sans-serif" : family, style);
        }

        protected static void background(Canvas canvas, ClockRenderContext context,
                ClockThemeTokens theme) {
            ClockBackground hostBackground = context.getBackground();
            if (hostBackground != null && !hostBackground.usesThemeSurface()) {
                float left = context.getLeft();
                float top = context.getTop();
                float right = context.getRight();
                float bottom = context.getBottom();
                if (hostBackground.hasImage()) {
                    Bitmap bitmap = hostBackground.getBitmap();
                    float width = Math.max(1f, right - left);
                    float height = Math.max(1f, bottom - top);
                    float scale = Math.max(width / bitmap.getWidth(),
                            height / bitmap.getHeight());
                    float drawWidth = bitmap.getWidth() * scale;
                    float drawHeight = bitmap.getHeight() * scale;
                    RectF destination = new RectF(
                            left + (width - drawWidth) * .5f,
                            top + (height - drawHeight) * .5f,
                            left + (width + drawWidth) * .5f,
                            top + (height + drawHeight) * .5f);
                    Paint imagePaint = fill(Color.WHITE);
                    imagePaint.setFilterBitmap(true);
                    canvas.drawBitmap(bitmap, null, destination, imagePaint);
                } else {
                    canvas.drawRect(left, top, right, bottom, fill(hostBackground.getColor()));
                }
                if (hostBackground.isDimmed()) {
                    canvas.drawRect(left, top, right, bottom, fill(0x66000000));
                }
                return;
            }
            Paint p = fill(Color.WHITE);
            p.setShader(new LinearGradient(context.getLeft(), context.getTop(),
                    context.getRight(), context.getBottom(), theme.getBackgroundStartColor(),
                    theme.getBackgroundEndColor(), Shader.TileMode.CLAMP));
            canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                    context.getBottom(), p);
            if (hostBackground != null && hostBackground.isDimmed()) {
                canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                        context.getBottom(), fill(0x66000000));
            }
        }

        protected static void panel(Canvas canvas, RectF rect, int color, float radius,
                int lineColor) {
            canvas.drawRoundRect(rect, radius, radius, fill(color));
            canvas.drawRoundRect(rect, radius, radius, stroke(alpha(lineColor, 120), 1f));
        }

        protected static void text(Canvas canvas, String value, float x, float baseline,
                float size, int color, Paint.Align align, Typeface face) {
            if (value == null || value.length() == 0 || size <= 0f) return;
            Paint p = fill(color);
            p.setTextSize(size);
            p.setTextAlign(align);
            p.setTypeface(face);
            canvas.drawText(value, x, baseline, p);
        }

        protected static float fitText(String value, float maxWidth, float size, Typeface face) {
            if (value == null || value.length() == 0) return size;
            Paint p = fill(Color.WHITE);
            p.setTypeface(face);
            p.setTextSize(size);
            float measured = p.measureText(value);
            return measured > maxWidth && measured > 0f ? size * maxWidth / measured : size;
        }

        protected static String timeText(Calendar c, ClockState state, boolean seconds) {
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) {
                hour %= 12;
                if (hour == 0) hour = 12;
            }
            return seconds
                    ? String.format(Locale.US, "%02d:%02d:%02d", hour,
                            c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
                    : String.format(Locale.US, "%02d:%02d", hour, c.get(Calendar.MINUTE));
        }

        protected static float secondValue(Calendar c, ClockState state,
                ClockRenderContext context) {
            float value = c.get(Calendar.SECOND);
            if (state.getSecondHandMotion() == ClockState.SecondHandMotion.SWEEP
                    && !context.isReducedMotion()) {
                value += c.get(Calendar.MILLISECOND) / 1000f;
            }
            return value;
        }

        protected static float hourAngle(Calendar c, ClockState state,
                ClockRenderContext context) {
            return (c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) / 60f
                    + secondValue(c, state, context) / 3600f) * 30f - 90f;
        }

        protected static float minuteAngle(Calendar c, ClockState state,
                ClockRenderContext context) {
            return (c.get(Calendar.MINUTE) + secondValue(c, state, context) / 60f) * 6f - 90f;
        }

        protected static float secondAngle(Calendar c, ClockState state,
                ClockRenderContext context) {
            return secondValue(c, state, context) * 6f - 90f;
        }

        protected static void hand(Canvas canvas, float cx, float cy, float degrees,
                float length, float width, int color, float tail) {
            double radians = Math.toRadians(degrees);
            float dx = (float) Math.cos(radians);
            float dy = (float) Math.sin(radians);
            Paint outer = stroke(alpha(Color.BLACK, 70), width + 3f);
            canvas.drawLine(cx - dx * tail, cy - dy * tail,
                    cx + dx * length, cy + dy * length, outer);
            canvas.drawLine(cx - dx * tail, cy - dy * tail,
                    cx + dx * length, cy + dy * length, stroke(color, width));
        }

        protected static void tick(Canvas canvas, float cx, float cy, float outer,
                float inner, float degrees, Paint paint) {
            double radians = Math.toRadians(degrees);
            float dx = (float) Math.cos(radians);
            float dy = (float) Math.sin(radians);
            canvas.drawLine(cx + dx * inner, cy + dy * inner,
                    cx + dx * outer, cy + dy * outer, paint);
        }

        protected static void ring(Canvas canvas, float cx, float cy, float radius,
                int color, float width) {
            canvas.drawCircle(cx, cy, radius, stroke(color, width));
        }

        protected static void footer(Canvas canvas, ClockRenderContext context,
                ClockState state, ClockThemeTokens theme, String label) {
            float x = context.getLeft() + context.getWidth() * .07f;
            float right = context.getRight() - context.getWidth() * .07f;
            float bottom = context.getBottom() - context.getHeight() * .045f;
            Typeface small = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            text(canvas, label, x, bottom, Math.max(9f, context.getWidth() * .018f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, small);
            String rightText = state.getWeatherText().length() == 0
                    ? state.getTimeZoneText() : state.getWeatherText();
            text(canvas, rightText, right, bottom, Math.max(9f, context.getWidth() * .018f),
                    theme.getSecondaryTextColor(), Paint.Align.RIGHT, small);
        }

        protected static void analogFace(Canvas canvas, float cx, float cy, float radius,
                ClockRenderContext context, ClockState state, ClockThemeTokens theme,
                int faceTop, int faceBottom, int tickColor, int majorColor,
                boolean numerals, boolean allNumerals) {
            Paint face = fill(faceTop);
            face.setShader(new RadialGradient(cx - radius * .25f, cy - radius * .3f,
                    radius * 1.3f, faceTop, faceBottom, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, face);
            ring(canvas, cx, cy, radius, alpha(majorColor, 150), Math.max(1f, radius * .012f));
            ring(canvas, cx, cy, radius * .93f, alpha(tickColor, 140), Math.max(1f, radius * .006f));
            Paint tickPaint = stroke(tickColor, 1f);
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                tickPaint.setColor(major ? majorColor : tickColor);
                tickPaint.setStrokeWidth(Math.max(1f,
                        radius * (major ? .018f : .007f)));
                tick(canvas, cx, cy, radius * .88f, radius * (major ? .77f : .82f),
                        i * 6f - 90f, tickPaint);
            }
            if (numerals) {
                Typeface faceType = typeface(theme.getDisplayFontFamily(),
                        allNumerals ? Typeface.NORMAL : Typeface.BOLD);
                int[] values = allNumerals
                        ? new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}
                        : new int[] {12, 3, 6, 9};
                float size = radius * (allNumerals ? .115f : .145f);
                Paint numeralPaint = fill(majorColor);
                numeralPaint.setTextSize(size);
                numeralPaint.setTextAlign(Paint.Align.CENTER);
                numeralPaint.setTypeface(faceType);
                Paint.FontMetrics numeralMetrics = numeralPaint.getFontMetrics();
                for (int value : values) {
                    double angle = Math.toRadians(value * 30d - 90d);
                    float tx = cx + (float) Math.cos(angle) * radius * .68f;
                    float ty = cy + (float) Math.sin(angle) * radius * .68f;
                    canvas.drawText(Integer.toString(value), tx,
                            ty - (numeralMetrics.ascent + numeralMetrics.descent) * .5f,
                            numeralPaint);
                }
            }
        }

        protected static void hands(Canvas canvas, float cx, float cy, float radius,
                ClockRenderContext context, ClockState state, ClockThemeTokens theme,
                boolean second) {
            Calendar c = state.newCalendar();
            hand(canvas, cx, cy, hourAngle(c, state, context), radius * .52f,
                    Math.max(3f, radius * .055f), theme.getPrimaryTextColor(), radius * .08f);
            hand(canvas, cx, cy, minuteAngle(c, state, context), radius * .72f,
                    Math.max(2f, radius * .035f), theme.getPrimaryTextColor(), radius * .10f);
            if (second && state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                hand(canvas, cx, cy, secondAngle(c, state, context), radius * .81f,
                        Math.max(1f, radius * .012f), theme.getAccentColor(), radius * .17f);
            }
            canvas.drawCircle(cx, cy, Math.max(3f, radius * .048f), fill(theme.getAccentColor()));
            canvas.drawCircle(cx, cy, Math.max(1.5f, radius * .022f),
                    fill(theme.getPrimaryTextColor()));
        }
    }

    /** Gallery preview only; the live Pro Classic style is hosted by the original ClockView. */
    private static final class ProClassicRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float width = context.getWidth();
            float height = context.getHeight();
            float unit = Math.min(width, height);
            float centerX = context.getCenterX();
            float centerY = context.getCenterY();
            Typeface display = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            Typeface supporting = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            String time = timeText(state.newCalendar(), state, state.isShowSeconds());
            float timeSize = fitText(time, width * .94f, unit * .42f, display);
            Paint timePaint = fill(theme.getPrimaryTextColor());
            timePaint.setTypeface(display);
            timePaint.setTextSize(timeSize);
            timePaint.setTextAlign(Paint.Align.CENTER);
            timePaint.setShadowLayer(Math.max(2f, unit * .035f), 0f,
                    Math.max(1f, unit * .012f), 0x66000000);
            Paint.FontMetrics metrics = timePaint.getFontMetrics();
            float baseline = centerY - (metrics.ascent + metrics.descent) * .5f;
            canvas.drawText(time, centerX, baseline, timePaint);

            float supportingSize = Math.max(8f, unit * .095f);
            float supportingGap = Math.max(7f, supportingSize * .55f);
            text(canvas, state.getDateText(), centerX,
                    baseline + metrics.ascent - supportingGap,
                    fitText(state.getDateText(), width * .88f, supportingSize, supporting),
                    theme.getSecondaryTextColor(), Paint.Align.CENTER, supporting);
            String contextText = state.getWeatherText().length() == 0
                    ? state.getTimeZoneText() : state.getWeatherText();
            text(canvas, contextText, centerX,
                    baseline + metrics.descent + supportingGap + supportingSize,
                    fitText(contextText, width * .88f, supportingSize, supporting),
                    theme.getSecondaryTextColor(), Paint.Align.CENTER, supporting);
        }
    }

    private static final class GlassAtelierRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            Paint glow = fill(alpha(Color.WHITE, 48));
            canvas.drawCircle(context.getLeft() + w * .12f, context.getTop() + h * .05f,
                    Math.min(w, h) * .42f, glow);
            canvas.drawCircle(context.getRight() - w * .08f, context.getBottom() - h * .05f,
                    Math.min(w, h) * .28f, fill(alpha(theme.getAccentColor(), 25)));
            RectF card = new RectF(context.getLeft() + w * .035f, context.getTop() + h * .035f,
                    context.getRight() - w * .035f, context.getBottom() - h * .035f);
            panel(canvas, card, theme.getSurfaceColor(), Math.min(w, h) * .065f,
                    theme.getLineColor());
            Typeface sans = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Typeface bold = typeface(theme.getDisplayFontFamily(), Typeface.BOLD);
            float unit = Math.min(w, h);
            text(canvas, "GLASS ATELIER", card.left + unit * .08f, card.top + unit * .10f,
                    Math.max(9f, unit * .025f), theme.getSecondaryTextColor(), Paint.Align.LEFT, sans);
            text(canvas, "ULTIMATE", card.right - unit * .08f, card.top + unit * .10f,
                    Math.max(8f, unit * .021f), theme.getAccentColor(), Paint.Align.RIGHT, bold);
            float cx = context.getCenterX();
            float cy = context.getTop() + h * .505f;
            float radius = Math.min(w, h) * .335f;
            Paint bezel = fill(theme.getLineColor());
            bezel.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                    0xFFFFFFFF, 0xFFBEC5D0, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius * 1.12f, bezel);
            ring(canvas, cx, cy, radius * 1.12f, alpha(theme.getPrimaryTextColor(), 90), 1f);
            analogFace(canvas, cx, cy, radius, context, state, theme,
                    0xFFFFFFFF, 0xFFE7EBF2, theme.getLineColor(), theme.getPrimaryTextColor(),
                    true, false);
            hands(canvas, cx, cy, radius, context, state, theme, true);
            String date = state.getDateText();
            if (date.length() > 0) {
                text(canvas, date, cx, cy + radius * .48f, Math.max(9f, unit * .024f),
                        theme.getSecondaryTextColor(), Paint.Align.CENTER, sans);
            }
            footer(canvas, context, state, theme, "MECHANICAL / 01");
        }
    }

    private static final class NoirInstrumentRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            RectF panelRect = new RectF(context.getLeft() + w * .04f, context.getTop() + h * .06f,
                    context.getRight() - w * .04f, context.getBottom() - h * .06f);
            panel(canvas, panelRect, theme.getSurfaceColor(), Math.min(w, h) * .025f,
                    theme.getLineColor());
            float cx;
            float cy;
            float radius;
            boolean landscape = w >= h * 1.15f;
            if (landscape) {
                cx = context.getLeft() + w * .34f;
                cy = context.getCenterY();
                radius = Math.min(h * .34f, w * .25f);
                canvas.drawLine(context.getLeft() + w * .62f, panelRect.top + h * .05f,
                        context.getLeft() + w * .62f, panelRect.bottom - h * .05f,
                        stroke(alpha(theme.getLineColor(), 150), 1f));
            } else {
                cx = context.getCenterX();
                cy = context.getTop() + h * .40f;
                radius = Math.min(w * .34f, h * .29f);
                canvas.drawLine(panelRect.left + w * .08f, context.getTop() + h * .68f,
                        panelRect.right - w * .08f, context.getTop() + h * .68f,
                        stroke(alpha(theme.getLineColor(), 150), 1f));
            }
            ring(canvas, cx, cy, radius * 1.11f, alpha(theme.getAccentColor(), 100), radius * .025f);
            analogFace(canvas, cx, cy, radius, context, state, theme,
                    0xFF24282D, 0xFF0C0F12, theme.getLineColor(), theme.getPrimaryTextColor(),
                    true, false);
            hands(canvas, cx, cy, radius, context, state, theme, true);
            // A separate seconds gauge makes this an instrument panel rather than a recoloured dial.
            float subX = landscape ? context.getLeft() + w * .53f : cx;
            float subY = landscape ? context.getTop() + h * .79f : context.getTop() + h * .82f;
            float subR = Math.max(15f, radius * .22f);
            ring(canvas, subX, subY, subR, alpha(theme.getLineColor(), 180), 2f);
            ring(canvas, subX, subY, subR * .78f, alpha(theme.getAccentColor(), 130), 1f);
            Calendar c = state.newCalendar();
            if (state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                float sec = secondValue(c, state, context);
                hand(canvas, subX, subY, sec * 6f - 90f, subR * .70f, 1.5f,
                        theme.getAccentColor(), subR * .12f);
            }
            Typeface mono = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Typeface condensed = typeface(theme.getDisplayFontFamily(), Typeface.BOLD);
            float infoLeft = landscape ? context.getLeft() + w * .67f : panelRect.left + w * .08f;
            float infoRight = landscape ? panelRect.right - w * .08f : panelRect.right - w * .08f;
            float infoY = landscape ? panelRect.top + h * .19f : context.getTop() + h * .75f;
            text(canvas, "NOIR / INSTRUMENT", infoLeft, infoY,
                    Math.max(9f, w * .026f), theme.getAccentColor(), Paint.Align.LEFT, mono);
            String time = timeText(c, state, state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF);
            float timeSize = fitText(time, infoRight - infoLeft, Math.max(22f, w * .085f), condensed);
            text(canvas, time, infoLeft, infoY + timeSize * 1.35f, timeSize,
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, condensed);
            text(canvas, state.getDateText(), infoLeft, infoY + timeSize * 1.90f,
                    Math.max(10f, w * .024f), theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);
            text(canvas, state.getTimeZoneText(), infoLeft, infoY + timeSize * 2.32f,
                    Math.max(9f, w * .020f), theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);
            text(canvas, state.getWeatherText(), infoRight, infoY + timeSize * 2.32f,
                    Math.max(9f, w * .020f), theme.getAccentColor(), Paint.Align.RIGHT, mono);
            footer(canvas, context, state, theme, "CALIBRATED 01 / 24");
        }
    }

    private static final class PaperStationRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            // Notebook ruling gives the face a paper-station identity in every orientation.
            Paint rule = stroke(alpha(theme.getLineColor(), 70), 1f);
            float spacing = Math.max(24f, Math.min(w, h) * .075f);
            for (float y = context.getTop() + spacing; y < context.getBottom(); y += spacing) {
                canvas.drawLine(context.getLeft(), y, context.getRight(), y, rule);
            }
            float marginX = context.getLeft() + w * .10f;
            canvas.drawLine(marginX, context.getTop(), marginX, context.getBottom(),
                    stroke(alpha(theme.getAccentColor(), 95), 1.3f));
            boolean landscape = w >= h * 1.12f;
            float cx = landscape ? context.getLeft() + w * .34f : context.getCenterX();
            float cy = landscape ? context.getCenterY() : context.getTop() + h * .39f;
            float radius = landscape ? Math.min(h * .32f, w * .25f) : Math.min(w * .34f, h * .27f);
            Paint face = fill(theme.getSurfaceColor());
            canvas.drawCircle(cx, cy, radius * 1.08f, face);
            ring(canvas, cx, cy, radius * 1.08f, alpha(theme.getLineColor(), 150), 1.5f);
            ring(canvas, cx, cy, radius * 1.015f, alpha(theme.getLineColor(), 100), 1f);
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30d - 90d);
                float px = cx + (float) Math.cos(a) * radius * .84f;
                float py = cy + (float) Math.sin(a) * radius * .84f;
                canvas.drawCircle(px, py, i % 3 == 0 ? radius * .032f : radius * .018f,
                        fill(i % 3 == 0 ? theme.getPrimaryTextColor() : theme.getLineColor()));
                if (i % 3 == 0) {
                    text(canvas, Integer.toString(i == 0 ? 12 : i), px, py + radius * .075f,
                            radius * .11f, theme.getPrimaryTextColor(), Paint.Align.CENTER,
                            typeface(theme.getDisplayFontFamily(), Typeface.NORMAL));
                }
            }
            hands(canvas, cx, cy, radius, context, state, theme, true);
            float infoLeft = landscape ? context.getLeft() + w * .65f : context.getLeft() + w * .15f;
            float infoRight = context.getRight() - w * .12f;
            float infoY = landscape ? context.getTop() + h * .25f : context.getTop() + h * .73f;
            Typeface serif = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            Typeface sans = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            text(canvas, "PAPER STATION", infoLeft, infoY,
                    Math.max(10f, w * .032f), theme.getAccentColor(), Paint.Align.LEFT, sans);
            text(canvas, state.getDateText(), infoLeft, infoY + Math.max(25f, h * .07f),
                    fitText(state.getDateText(), infoRight - infoLeft, Math.max(18f, w * .055f), serif),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, serif);
            canvas.drawLine(infoLeft, infoY + Math.max(34f, h * .095f), infoRight,
                    infoY + Math.max(34f, h * .095f), stroke(theme.getAccentColor(), 1.5f));
            text(canvas, timeText(state.newCalendar(), state, false), infoLeft,
                    infoY + Math.max(62f, h * .17f), Math.max(20f, w * .07f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT,
                    typeface(theme.getSupportingFontFamily(), Typeface.BOLD));
            text(canvas, state.getWeatherText().length() == 0 ? state.getTimeZoneText()
                    : state.getWeatherText(), infoLeft, infoY + Math.max(84f, h * .23f),
                    Math.max(10f, w * .024f), theme.getSecondaryTextColor(), Paint.Align.LEFT, sans);
            footer(canvas, context, state, theme, "FIELD NOTES / TODAY");
        }
    }

    private static final class OrbitNeonRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            float cx = context.getCenterX();
            float cy = context.getTop() + h * .52f;
            float unit = Math.min(w, h);
            float outer = unit * .36f;
            Paint faint = stroke(alpha(theme.getLineColor(), 180), Math.max(1f, unit * .006f));
            canvas.drawCircle(cx, cy, outer, faint);
            canvas.drawCircle(cx, cy, outer * .72f, stroke(alpha(theme.getLineColor(), 160), 1f));
            canvas.drawCircle(cx, cy, outer * .43f, stroke(alpha(theme.getLineColor(), 130), 1f));
            Calendar c = state.newCalendar();
            float hourProgress = (c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) / 60f) / 12f;
            float minuteProgress = (c.get(Calendar.MINUTE) + secondValue(c, state, context) / 60f) / 60f;
            float secondProgress = secondValue(c, state, context) / 60f;
            RectF outerRect = new RectF(cx - outer, cy - outer, cx + outer, cy + outer);
            RectF midRect = new RectF(cx - outer * .72f, cy - outer * .72f,
                    cx + outer * .72f, cy + outer * .72f);
            RectF innerRect = new RectF(cx - outer * .43f, cy - outer * .43f,
                    cx + outer * .43f, cy + outer * .43f);
            canvas.drawArc(outerRect, -90f, hourProgress * 360f, false,
                    stroke(theme.getAccentColor(), Math.max(3f, unit * .018f)));
            canvas.drawArc(midRect, -90f, minuteProgress * 360f, false,
                    stroke(0xFF65B8FF, Math.max(2f, unit * .012f)));
            if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                canvas.drawArc(innerRect, -90f, secondProgress * 360f, false,
                        stroke(0xFFFF7A63, Math.max(2f, unit * .009f)));
            }
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30d - 90d);
                float px = cx + (float) Math.cos(a) * outer * .90f;
                float py = cy + (float) Math.sin(a) * outer * .90f;
                canvas.drawCircle(px, py, i % 3 == 0 ? unit * .012f : unit * .006f,
                        fill(i % 3 == 0 ? theme.getPrimaryTextColor() : theme.getLineColor()));
            }
            String time = timeText(c, state, false);
            Typeface display = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            float timeSize = fitText(time, outer * 1.05f, unit * .115f, display);
            text(canvas, time, cx, cy + timeSize * .34f, timeSize,
                    theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
            text(canvas, "ORBIT / LIVE SYSTEM", cx, cy - outer * .12f,
                    Math.max(8f, unit * .020f), theme.getSecondaryTextColor(), Paint.Align.CENTER,
                    typeface(theme.getSupportingFontFamily(), Typeface.NORMAL));
            text(canvas, state.getDateText(), cx, cy + outer * .56f,
                    Math.max(9f, unit * .022f), theme.getSecondaryTextColor(), Paint.Align.CENTER,
                    typeface(theme.getSupportingFontFamily(), Typeface.NORMAL));
            text(canvas, state.getWeatherText().length() == 0 ? state.getTimeZoneText()
                    : state.getWeatherText(), context.getLeft() + w * .07f,
                    context.getTop() + h * .09f, Math.max(9f, unit * .022f),
                    theme.getAccentColor(), Paint.Align.LEFT,
                    typeface(theme.getSupportingFontFamily(), Typeface.NORMAL));
            text(canvas, state.getStatusText(), context.getRight() - w * .07f,
                    context.getTop() + h * .09f, Math.max(9f, unit * .022f),
                    theme.getSecondaryTextColor(), Paint.Align.RIGHT,
                    typeface(theme.getSupportingFontFamily(), Typeface.NORMAL));
            footer(canvas, context, state, theme, "HOURS  /  MINUTES  /  SECONDS");
        }
    }

    private static final class DigitalGridRenderer extends RendererBase {
        private static final int[][] DIGITS = {
                {1, 1, 1, 1, 1, 1, 0}, {0, 1, 1, 0, 0, 0, 0},
                {1, 1, 0, 1, 1, 0, 1}, {1, 1, 1, 1, 0, 0, 1},
                {0, 1, 1, 0, 0, 1, 1}, {1, 0, 1, 1, 0, 1, 1},
                {1, 0, 1, 1, 1, 1, 1}, {1, 1, 1, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 0, 1, 1}
        };

        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            float grid = Math.max(22f, Math.min(w, h) * .075f);
            Paint gridPaint = stroke(alpha(theme.getLineColor(), 120), 1f);
            for (float x = context.getLeft(); x <= context.getRight(); x += grid) {
                canvas.drawLine(x, context.getTop(), x, context.getBottom(), gridPaint);
            }
            for (float y = context.getTop(); y <= context.getBottom(); y += grid) {
                canvas.drawLine(context.getLeft(), y, context.getRight(), y, gridPaint);
            }
            Typeface mono = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            text(canvas, "DIGITAL GRID", context.getLeft() + w * .07f,
                    context.getTop() + h * .10f, Math.max(10f, w * .027f),
                    theme.getAccentColor(), Paint.Align.LEFT, mono);
            text(canvas, state.getTimeZoneText(), context.getRight() - w * .07f,
                    context.getTop() + h * .10f, Math.max(9f, w * .021f),
                    theme.getSecondaryTextColor(), Paint.Align.RIGHT, mono);
            Calendar c = state.newCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String digits = String.format(Locale.US, "%02d%02d", hour, c.get(Calendar.MINUTE));
            boolean landscape = w >= h * 1.2f;
            float blockTop = context.getTop() + (landscape ? h * .27f : h * .22f);
            float blockHeight = landscape ? h * .42f : h * .35f;
            float left = context.getLeft() + w * .09f;
            float right = context.getRight() - w * .09f;
            float secWidth = Math.max(20f, w * .08f);
            float gap = Math.max(8f, w * .018f);
            float digitWidth = (right - left - secWidth - gap * 4f) / 4f;
            if (digitWidth < 10f) digitWidth = 10f;
            for (int i = 0; i < 4; i++) {
                float x = left + i * (digitWidth + gap);
                RectF cell = new RectF(x, blockTop, x + digitWidth, blockTop + blockHeight);
                canvas.drawRoundRect(cell, 4f, 4f, fill(alpha(theme.getSurfaceColor(), 220)));
                canvas.drawRoundRect(cell, 4f, 4f, stroke(alpha(theme.getLineColor(), 230), 1f));
                drawDigit(canvas, digits.charAt(i) - '0', cell, theme);
            }
            float secX = left + 4f * (digitWidth + gap);
            RectF secCell = new RectF(secX, blockTop, right, blockTop + blockHeight);
            canvas.drawRoundRect(secCell, 4f, 4f, fill(alpha(theme.getSurfaceColor(), 220)));
            canvas.drawRoundRect(secCell, 4f, 4f, stroke(alpha(theme.getAccentColor(), 190), 1f));
            String seconds = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                    ? String.format(Locale.US, "%02d", c.get(Calendar.SECOND)) : "--";
            float secSize = fitText(seconds, secCell.width() * .82f, blockHeight * .28f, mono);
            text(canvas, seconds, secCell.centerX(), blockTop + blockHeight * .56f, secSize,
                    theme.getAccentColor(), Paint.Align.CENTER, mono);
            text(canvas, "SEC", secCell.centerX(), blockTop + blockHeight * .84f,
                    Math.max(8f, blockHeight * .08f), theme.getSecondaryTextColor(),
                    Paint.Align.CENTER, mono);
            float baseline = blockTop + blockHeight + Math.max(34f, h * .12f);
            text(canvas, state.getDateText(), left, baseline, Math.max(11f, w * .026f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, mono);
            text(canvas, state.getWeatherText(), right, baseline, Math.max(10f, w * .022f),
                    theme.getAccentColor(), Paint.Align.RIGHT, mono);
            footer(canvas, context, state, theme, "GRID 07 / LOCAL CLOCK");
        }

        private static void drawDigit(Canvas canvas, int digit, RectF box, ClockThemeTokens theme) {
            float x = box.left;
            float y = box.top;
            float w = box.width();
            float h = box.height();
            float pad = Math.min(w, h) * .18f;
            float left = x + pad;
            float right = x + w - pad;
            float top = y + pad;
            float middle = y + h * .5f;
            float bottom = y + h - pad;
            float width = Math.max(2f, Math.min(w, h) * .07f);
            int[] active = DIGITS[Math.max(0, Math.min(9, digit))];
            int off = alpha(theme.getLineColor(), 75);
            int on = theme.getPrimaryTextColor();
            segment(canvas, left + width, top, right - width, top, active[0] != 0, on, off, width);
            segment(canvas, right, top + width, right, middle - width, active[1] != 0, on, off, width);
            segment(canvas, right, middle + width, right, bottom - width, active[2] != 0, on, off, width);
            segment(canvas, left + width, bottom, right - width, bottom, active[3] != 0, on, off, width);
            segment(canvas, left, middle + width, left, bottom - width, active[4] != 0, on, off, width);
            segment(canvas, left, top + width, left, middle - width, active[5] != 0, on, off, width);
            segment(canvas, left + width, middle, right - width, middle, active[6] != 0, on, off, width);
        }

        private static void segment(Canvas canvas, float x1, float y1, float x2, float y2,
                boolean active, int on, int off, float width) {
            Paint p = stroke(active ? on : off, width);
            p.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawLine(x1, y1, x2, y2, p);
        }
    }

    private static final class TypographicRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            Typeface display = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            Typeface bold = typeface(theme.getSupportingFontFamily(), Typeface.BOLD);
            Typeface regular = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Calendar c = state.newCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String hours = String.format(Locale.US, "%02d", hour);
            String minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE));
            boolean landscape = w >= h * 1.12f;
            float header = context.getTop() + h * .10f;
            text(canvas, "TIME / AS COMPOSITION", context.getLeft() + w * .07f, header,
                    Math.max(9f, unit * .021f), theme.getSecondaryTextColor(), Paint.Align.LEFT, regular);
            text(canvas, state.getTimeZoneText(), context.getRight() - w * .07f, header,
                    Math.max(9f, unit * .021f), theme.getAccentColor(), Paint.Align.RIGHT, regular);
            float divider;
            if (landscape) {
                divider = context.getLeft() + w * .52f;
                float size = fitText(hours, w * .37f, unit * .29f, display);
                text(canvas, hours, context.getLeft() + w * .08f, context.getTop() + h * .61f,
                        size, theme.getPrimaryTextColor(), Paint.Align.LEFT, display);
                size = fitText(minutes, w * .37f, unit * .29f, display);
                text(canvas, minutes, context.getRight() - w * .08f, context.getTop() + h * .61f,
                        size, theme.getPrimaryTextColor(), Paint.Align.RIGHT, display);
                canvas.drawLine(divider, context.getTop() + h * .24f, divider,
                        context.getTop() + h * .72f, stroke(theme.getAccentColor(), 2f));
                text(canvas, ":", divider, context.getTop() + h * .61f,
                        unit * .18f, theme.getAccentColor(), Paint.Align.CENTER, bold);
            } else {
                divider = context.getTop() + h * .52f;
                float size = fitText(hours, w * .78f, unit * .27f, display);
                text(canvas, hours, context.getCenterX(), context.getTop() + h * .47f,
                        size, theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
                size = fitText(minutes, w * .78f, unit * .27f, display);
                text(canvas, minutes, context.getCenterX(), context.getTop() + h * .78f,
                        size, theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
                canvas.drawLine(context.getLeft() + w * .16f, divider,
                        context.getRight() - w * .16f, divider, stroke(theme.getAccentColor(), 2f));
                text(canvas, ":", context.getRight() - w * .16f, divider - h * .035f,
                        unit * .12f, theme.getAccentColor(), Paint.Align.RIGHT, bold);
            }
            if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                String seconds = String.format(Locale.US, "%02d", c.get(Calendar.SECOND));
                float secProgress = secondValue(c, state, context) / 60f;
                float barLeft = context.getLeft() + w * .07f;
                float barRight = context.getRight() - w * .07f;
                float barY = landscape ? context.getBottom() - h * .18f : context.getBottom() - h * .12f;
                canvas.drawLine(barLeft, barY, barRight, barY,
                        stroke(alpha(theme.getLineColor(), 160), Math.max(2f, unit * .008f)));
                canvas.drawLine(barLeft, barY, barLeft + (barRight - barLeft) * secProgress, barY,
                        stroke(theme.getAccentColor(), Math.max(3f, unit * .012f)));
                text(canvas, seconds, barRight, barY - unit * .028f, Math.max(16f, unit * .065f),
                        theme.getAccentColor(), Paint.Align.RIGHT, bold);
            }
            float infoY = landscape ? context.getTop() + h * .89f : context.getTop() + h * .92f;
            text(canvas, state.getDateText(), context.getLeft() + w * .07f, infoY,
                    Math.max(10f, unit * .024f), theme.getPrimaryTextColor(), Paint.Align.LEFT, regular);
            text(canvas, state.getWeatherText().length() == 0 ? state.getStatusText()
                    : state.getWeatherText(), context.getRight() - w * .07f, infoY,
                    Math.max(10f, unit * .022f), theme.getSecondaryTextColor(), Paint.Align.RIGHT, regular);
            footer(canvas, context, state, theme, "TYPOGRAPHIC EDITION / 06");
        }
    }
}
