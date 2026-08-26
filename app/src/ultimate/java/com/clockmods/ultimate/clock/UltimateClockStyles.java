package com.clockmods.ultimate.clock;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
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
import com.clockmods.ui.ClockTimeText;

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
                .background(0xFFF0F4F7, 0xFFC9D3DC)
                .surfaceColor(0xF2F8FAFC)
                .primaryTextColor(0xFF14202B)
                .secondaryTextColor(0xFF556573)
                .accentColor(0xFFD9423A)
                .lineColor(0xFF8796A3)
                .fonts("sans-serif", "sans-serif")
                .strokeScale(1.05f).build();
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
                .background(0xFF111417, 0xFF050607)
                .surfaceColor(0xFF15191C)
                .primaryTextColor(0xFFF0ECE2)
                .secondaryTextColor(0xFF8D969B)
                .accentColor(0xFFD7A247)
                .lineColor(0xFF424B50)
                .fonts("sans-serif-condensed", "monospace")
                .strokeScale(1.08f).build();
    }

    private static ClockThemeTokens paperTokens() {
        return ClockThemeTokens.builder()
                .background(0xFFF4F1E9, 0xFFE2D9C9)
                .surfaceColor(0xFFFAF7F0)
                .primaryTextColor(0xFF24211D)
                .secondaryTextColor(0xFF696159)
                .accentColor(0xFFB54740)
                .lineColor(0xFFB5A88F)
                .fonts("serif", "sans-serif")
                .strokeScale(1f).build();
    }

    private static ClockThemeTokens orbitTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF10171A, 0xFF050809)
                .surfaceColor(0xFF122126)
                .primaryTextColor(0xFFEAFBFF)
                .secondaryTextColor(0xFF8EABB2)
                .accentColor(0xFF2ED9C7)
                .lineColor(0xFF28535D)
                .fonts("sans-serif-light", "monospace")
                .strokeScale(1.1f).build();
    }

    private static ClockThemeTokens digitalTokens() {
        return ClockThemeTokens.builder()
                .background(0xFF071110, 0xFF020505)
                .surfaceColor(0xFF0A1C1A)
                .primaryTextColor(0xFFA8F7CF)
                .secondaryTextColor(0xFF69A88E)
                .accentColor(0xFFFFC95C)
                .lineColor(0xFF245246)
                .fonts("monospace", "monospace")
                .strokeScale(1.08f).build();
    }

    private static ClockThemeTokens typeTokens() {
        return ClockThemeTokens.builder()
                .background(0xFFF1F0EB, 0xFFDDE3E4)
                .surfaceColor(0xFF17191C)
                .primaryTextColor(0xFF181A1C)
                .secondaryTextColor(0xFF62686C)
                .accentColor(0xFFDF4B38)
                .lineColor(0xFFABB3B5)
                .fonts("sans-serif", "sans-serif")
                .strokeScale(1.08f).build();
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

        protected static void fittedText(Canvas canvas, String value, float x, float baseline,
                float maxWidth, float preferredSize, int color, Paint.Align align,
                Typeface face) {
            if (maxWidth <= 0f) return;
            text(canvas, value, x, baseline, fitText(value, maxWidth, preferredSize, face),
                    color, align, face);
        }

        /**
         * Same as {@link #text} but for strings that contain a time, whose ':' separators
         * are lifted onto the optical centre of the digits instead of sitting on the
         * x-height. Plain prose keeps the typeface's own colon position, so this is only
         * for clock readouts.
         */
        protected static void drawTime(Canvas canvas, String value, float x, float baseline,
                float size, int color, Paint.Align align, Typeface face) {
            if (value == null || value.length() == 0 || size <= 0f) return;
            Paint p = fill(color);
            p.setTextSize(size);
            p.setTextAlign(align);
            p.setTypeface(face);
            ClockTimeText.draw(canvas, value, x, baseline, p);
        }

        protected static void fittedTime(Canvas canvas, String value, float x, float baseline,
                float maxWidth, float preferredSize, int color, Paint.Align align,
                Typeface face) {
            if (maxWidth <= 0f) return;
            drawTime(canvas, value, x, baseline, fitText(value, maxWidth, preferredSize, face),
                    color, align, face);
        }

        protected static String contextText(ClockState state) {
            if (state.getWeatherText().length() > 0) return state.getWeatherText();
            if (state.getStatusText().length() > 0) return state.getStatusText();
            return state.getTimeZoneText();
        }

        /**
         * Draws the context line from {@link #contextText}. It falls back to the time zone
         * when there is no weather or status to show, and only that fallback is treated as a
         * time; a status message the user typed keeps its own punctuation.
         */
        protected static void fittedContext(Canvas canvas, ClockState state, String value,
                float x, float baseline, float maxWidth, float preferredSize, int color,
                Paint.Align align, Typeface face) {
            if (value.equals(state.getTimeZoneText())) {
                fittedTime(canvas, value, x, baseline, maxWidth, preferredSize, color, align,
                        face);
            } else {
                fittedText(canvas, value, x, baseline, maxWidth, preferredSize, color, align,
                        face);
            }
        }

        protected static boolean hasCustomBackground(ClockRenderContext context) {
            ClockBackground value = context.getBackground();
            return value != null && !value.usesThemeSurface();
        }

        protected static void customBackgroundVeil(Canvas canvas, ClockRenderContext context,
                int color) {
            if (!hasCustomBackground(context)) return;
            canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                    context.getBottom(), fill(color));
        }

        protected static float lineWidth(ClockRenderContext context, ClockThemeTokens theme,
                float dp) {
            return Math.max(1f, context.getDensity() * dp * theme.getStrokeScale());
        }

        protected static float centeredBaseline(float centerY, float size, Typeface face) {
            Paint p = fill(Color.WHITE);
            p.setTextSize(size);
            p.setTypeface(face);
            Paint.FontMetrics metrics = p.getFontMetrics();
            return centerY - (metrics.ascent + metrics.descent) * .5f;
        }

        protected static float bottomOverlayShift(ClockRenderContext context,
                float preferredBottomBaseline) {
            float safeBaseline = context.getBottom() - context.getBottomInset();
            return Math.min(0f, safeBaseline - preferredBottomBaseline);
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

        protected static void cleanHand(Canvas canvas, float cx, float cy, float degrees,
                float length, float width, int color, float tail, Paint.Cap cap) {
            double radians = Math.toRadians(degrees);
            float dx = (float) Math.cos(radians);
            float dy = (float) Math.sin(radians);
            Paint paint = stroke(color, width);
            paint.setStrokeCap(cap);
            canvas.drawLine(cx - dx * tail, cy - dy * tail,
                    cx + dx * length, cy + dy * length, paint);
        }

        protected static void arcEnd(Canvas canvas, float cx, float cy, float radius,
                float startDegrees, float sweepDegrees, int color, float width) {
            double radians = Math.toRadians(startDegrees + sweepDegrees);
            float x = cx + (float) Math.cos(radians) * radius;
            float y = cy + (float) Math.sin(radians) * radius;
            canvas.drawCircle(x, y, width * .52f, fill(color));
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
            ClockTimeText.draw(canvas, time, centerX, baseline, timePaint);

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
            customBackgroundVeil(canvas, context, 0xA8E8EEF2);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            boolean landscape = w >= h * 1.12f;
            Typeface sans = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Typeface bold = typeface(theme.getDisplayFontFamily(), Typeface.BOLD);
            float infoLeft = context.getLeft() + w * (landscape ? .075f : .10f);
            float infoRight = context.getLeft() + w * (landscape ? .405f : .90f);
            float titleY = context.getTop() + h * (landscape ? .15f : .075f);
            float dateY = context.getTop() + h * (landscape ? .29f : .65f);
            float timeY = context.getTop() + h * (landscape ? .47f : .76f);
            float contextY = context.getTop() + h * (landscape ? .60f : .84f);
            float zoneY = context.getTop() + h * (landscape ? .69f : .895f);
            float small = Math.max(context.getDensity() * 6f, unit * .021f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            dateY += metadataShift;
            timeY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;

            fittedText(canvas, "ATELIER / 01", infoLeft, titleY, infoRight - infoLeft,
                    small, theme.getAccentColor(), Paint.Align.LEFT, bold);
            canvas.drawLine(infoLeft, titleY + unit * .035f,
                    infoLeft + Math.min(infoRight - infoLeft, unit * .16f),
                    titleY + unit * .035f,
                    stroke(alpha(theme.getAccentColor(), 190),
                            lineWidth(context, theme, 1.2f)));
            fittedText(canvas, state.getDateText(), infoLeft, dateY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .034f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, sans);
            String time = timeText(state.newCalendar(), state, false);
            fittedTime(canvas, time, infoLeft, timeY, infoRight - infoLeft,
                    unit * (landscape ? .145f : .12f), theme.getPrimaryTextColor(),
                    Paint.Align.LEFT, bold);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, infoLeft, contextY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .030f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sans);
            if (!contextLine.equals(state.getTimeZoneText())) {
                fittedTime(canvas, state.getTimeZoneText(), infoLeft, zoneY,
                        infoRight - infoLeft, small, theme.getSecondaryTextColor(),
                        Paint.Align.LEFT, sans);
            }

            float cx = landscape ? context.getLeft() + w * .69f : context.getCenterX();
            float cy = context.getTop() + h * (landscape ? .49f : .35f);
            float radius = landscape ? Math.min(h * .33f, w * .225f)
                    : Math.min(w * .36f, h * .225f);
            canvas.drawCircle(cx, cy + radius * .045f, radius * 1.10f, fill(0x2A162531));
            Paint bezel = fill(theme.getLineColor());
            bezel.setShader(new LinearGradient(cx - radius, cy - radius,
                    cx + radius, cy + radius, 0xFFFDFEFF, 0xFF9EABB6,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius * 1.085f, bezel);
            ring(canvas, cx, cy, radius * 1.085f, alpha(theme.getPrimaryTextColor(), 100),
                    lineWidth(context, theme, .75f));
            Paint face = fill(Color.WHITE);
            face.setShader(new RadialGradient(cx - radius * .28f, cy - radius * .32f,
                    radius * 1.32f, 0xFFFFFFFF, 0xFFDDE5EA, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, face);
            ring(canvas, cx, cy, radius * .94f, alpha(theme.getLineColor(), 120),
                    lineWidth(context, theme, .7f));

            Paint tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .8f));
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                tickPaint.setColor(major ? theme.getPrimaryTextColor()
                        : alpha(theme.getLineColor(), 180));
                tickPaint.setStrokeWidth(major ? radius * .016f : radius * .006f);
                tick(canvas, cx, cy, radius * .88f,
                        radius * (major ? .77f : .83f), i * 6f - 90f, tickPaint);
            }
            int[] numerals = {12, 3, 6, 9};
            float numeralSize = radius * .13f;
            for (int value : numerals) {
                double angle = Math.toRadians(value * 30d - 90d);
                float x = cx + (float) Math.cos(angle) * radius * .68f;
                float y = cy + (float) Math.sin(angle) * radius * .68f;
                text(canvas, Integer.toString(value), x,
                        centeredBaseline(y, numeralSize, bold), numeralSize,
                        theme.getPrimaryTextColor(), Paint.Align.CENTER, bold);
            }

            Calendar calendar = state.newCalendar();
            cleanHand(canvas, cx, cy, hourAngle(calendar, state, context), radius * .50f,
                    radius * .052f, theme.getPrimaryTextColor(), radius * .07f,
                    Paint.Cap.ROUND);
            cleanHand(canvas, cx, cy, minuteAngle(calendar, state, context), radius * .72f,
                    radius * .028f, theme.getPrimaryTextColor(), radius * .10f,
                    Paint.Cap.ROUND);
            if (state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                cleanHand(canvas, cx, cy, secondAngle(calendar, state, context), radius * .82f,
                        Math.max(lineWidth(context, theme, 1f), radius * .009f),
                        theme.getAccentColor(), radius * .17f, Paint.Cap.ROUND);
            }
            canvas.drawCircle(cx, cy, radius * .050f, fill(theme.getPrimaryTextColor()));
            canvas.drawCircle(cx, cy, radius * .026f, fill(theme.getAccentColor()));
        }
    }

    private static final class NoirInstrumentRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            customBackgroundVeil(canvas, context, 0xCE07090A);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            boolean landscape = w >= h * 1.15f;
            float split = landscape ? context.getLeft() + w * .56f
                    : context.getTop() + h * .56f;
            Paint datum = stroke(alpha(theme.getLineColor(), 155),
                    lineWidth(context, theme, .7f));
            if (landscape) {
                canvas.drawLine(split, context.getTop() + h * .10f, split,
                        context.getTop() + h * .86f, datum);
            } else {
                canvas.drawLine(context.getLeft() + w * .09f, split,
                        context.getRight() - w * .09f, split, datum);
            }
            for (int i = 0; i < 9; i++) {
                float x = context.getLeft() + w * (.08f + i * .105f);
                canvas.drawLine(x, context.getTop() + h * .075f, x,
                        context.getTop() + h * .09f,
                        stroke(alpha(theme.getLineColor(), 115),
                                lineWidth(context, theme, .65f)));
            }

            float cx = landscape ? context.getLeft() + w * .30f : context.getCenterX();
            float cy = context.getTop() + h * (landscape ? .48f : .31f);
            float radius = landscape ? Math.min(h * .34f, w * .225f)
                    : Math.min(w * .35f, h * .225f);
            canvas.drawCircle(cx, cy + radius * .035f, radius * 1.08f, fill(0x52000000));
            canvas.drawCircle(cx, cy, radius * 1.075f, fill(0xFF090B0C));
            ring(canvas, cx, cy, radius * 1.075f, alpha(theme.getAccentColor(), 145),
                    radius * .018f);
            canvas.drawCircle(cx, cy, radius, fill(theme.getSurfaceColor()));
            ring(canvas, cx, cy, radius, alpha(theme.getPrimaryTextColor(), 120),
                    lineWidth(context, theme, 1.1f));
            ring(canvas, cx, cy, radius * .92f, alpha(theme.getLineColor(), 175),
                    lineWidth(context, theme, .7f));
            Paint tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .8f));
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                tickPaint.setColor(major ? theme.getPrimaryTextColor()
                        : alpha(theme.getLineColor(), 190));
                tickPaint.setStrokeWidth(major ? radius * .014f : radius * .005f);
                tick(canvas, cx, cy, radius * .86f,
                        radius * (major ? .76f : .81f), i * 6f - 90f, tickPaint);
            }
            Typeface mono = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Typeface condensed = typeface(theme.getDisplayFontFamily(), Typeface.BOLD);
            int[] numerals = {12, 3, 6, 9};
            float numeralSize = radius * .115f;
            for (int value : numerals) {
                double angle = Math.toRadians(value * 30d - 90d);
                float x = cx + (float) Math.cos(angle) * radius * .66f;
                float y = cy + (float) Math.sin(angle) * radius * .66f;
                text(canvas, Integer.toString(value), x,
                        centeredBaseline(y, numeralSize, condensed), numeralSize,
                        theme.getPrimaryTextColor(), Paint.Align.CENTER, condensed);
            }
            Calendar c = state.newCalendar();
            cleanHand(canvas, cx, cy, hourAngle(c, state, context), radius * .48f,
                    radius * .042f, theme.getPrimaryTextColor(), radius * .07f,
                    Paint.Cap.SQUARE);
            cleanHand(canvas, cx, cy, minuteAngle(c, state, context), radius * .70f,
                    radius * .022f, theme.getPrimaryTextColor(), radius * .10f,
                    Paint.Cap.SQUARE);
            if (state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                cleanHand(canvas, cx, cy, secondAngle(c, state, context), radius * .80f,
                        Math.max(lineWidth(context, theme, .8f), radius * .007f),
                        theme.getAccentColor(), radius * .16f, Paint.Cap.SQUARE);
            }
            canvas.drawCircle(cx, cy, radius * .038f, fill(theme.getAccentColor()));
            canvas.drawCircle(cx, cy, radius * .016f, fill(0xFF0A0C0D));

            float infoLeft = context.getLeft() + w * (landscape ? .62f : .10f);
            float infoRight = context.getRight() - w * (landscape ? .075f : .10f);
            float titleY = context.getTop() + h * (landscape ? .17f : .62f);
            float timeY = context.getTop() + h * (landscape ? .38f : .71f);
            float dateY = context.getTop() + h * (landscape ? .49f : .78f);
            float contextY = context.getTop() + h * (landscape ? .62f : .85f);
            float zoneY = context.getTop() + h * (landscape ? .70f : .90f);
            float small = Math.max(context.getDensity() * 6f, unit * .020f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            timeY += metadataShift;
            dateY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, "INSTRUMENT / 24", infoLeft, titleY, infoRight - infoLeft,
                    small, theme.getAccentColor(), Paint.Align.LEFT, mono);
            fittedTime(canvas, timeText(c, state, false), infoLeft, timeY,
                    infoRight - infoLeft, unit * (landscape ? .14f : .12f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, condensed);
            fittedText(canvas, state.getDateText(), infoLeft, dateY,
                    infoRight - infoLeft, Math.max(context.getDensity() * 7f, unit * .030f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, Math.max(context.getDensity() * 7f, unit * .027f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, mono);
            if (!contextLine.equals(state.getTimeZoneText())) {
                fittedTime(canvas, state.getTimeZoneText(), infoLeft, zoneY,
                        infoRight - infoLeft, small, theme.getSecondaryTextColor(),
                        Paint.Align.LEFT, mono);
            }

            if (landscape) {
                float subX = context.getLeft() + w * .86f;
                float subY = context.getTop() + h * .77f;
                float subR = unit * .060f;
                ring(canvas, subX, subY, subR, alpha(theme.getLineColor(), 210),
                        lineWidth(context, theme, .8f));
                for (int i = 0; i < 12; i++) {
                    tick(canvas, subX, subY, subR * .87f, subR * .76f,
                            i * 30f - 90f,
                            stroke(i % 3 == 0 ? theme.getAccentColor()
                                            : theme.getLineColor(),
                                    lineWidth(context, theme, i % 3 == 0 ? 1f : .6f)));
                }
                if (state.isShowSeconds()
                        && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                    cleanHand(canvas, subX, subY, secondAngle(c, state, context), subR * .65f,
                            lineWidth(context, theme, .8f), theme.getAccentColor(), subR * .08f,
                            Paint.Cap.SQUARE);
                }
                String seconds = state.isShowSeconds()
                        && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                        ? String.format(Locale.US, "%02d", c.get(Calendar.SECOND)) : "--";
                float subSecondsSize = Math.min(subR * .55f,
                        Math.max(context.getDensity() * 6f, subR * .31f));
                text(canvas, seconds, subX, centeredBaseline(subY, subSecondsSize, mono),
                        subSecondsSize, theme.getPrimaryTextColor(), Paint.Align.CENTER, mono);
            }
        }
    }

    private static final class PaperStationRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            customBackgroundVeil(canvas, context, 0xE8F3EEE4);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            boolean landscape = w >= h * 1.12f;
            Paint rule = stroke(alpha(theme.getLineColor(), 58),
                    lineWidth(context, theme, .55f));
            float spacing = Math.max(context.getDensity() * 18f, unit * .075f);
            for (float y = context.getTop() + spacing * .72f;
                    y < context.getBottom(); y += spacing) {
                canvas.drawLine(context.getLeft(), y, context.getRight(), y, rule);
            }
            float marginX = context.getLeft() + w * .085f;
            canvas.drawLine(marginX, context.getTop(), marginX, context.getBottom(),
                    stroke(alpha(theme.getAccentColor(), 135),
                            lineWidth(context, theme, .9f)));

            float cx = landscape ? context.getLeft() + w * .31f : context.getCenterX();
            float cy = context.getTop() + h * (landscape ? .49f : .32f);
            float radius = landscape ? Math.min(h * .285f, w * .22f)
                    : Math.min(w * .34f, h * .215f);
            canvas.drawCircle(cx, cy + radius * .025f, radius * 1.075f, fill(0x18000000));
            canvas.drawCircle(cx, cy, radius * 1.055f, fill(theme.getSurfaceColor()));
            ring(canvas, cx, cy, radius * 1.055f, alpha(theme.getLineColor(), 175),
                    lineWidth(context, theme, .8f));
            ring(canvas, cx, cy, radius * .92f, alpha(theme.getLineColor(), 120),
                    lineWidth(context, theme, .55f));
            Paint tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .7f));
            for (int i = 0; i < 60; i++) {
                boolean major = i % 5 == 0;
                tickPaint.setColor(major ? theme.getPrimaryTextColor()
                        : alpha(theme.getLineColor(), 150));
                tickPaint.setStrokeWidth(major ? radius * .010f : radius * .004f);
                tick(canvas, cx, cy, radius * .85f,
                        radius * (major ? .77f : .82f), i * 6f - 90f, tickPaint);
            }
            Typeface serif = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            Typeface sans = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Typeface sansBold = typeface(theme.getSupportingFontFamily(), Typeface.BOLD);
            int[] numerals = {12, 3, 6, 9};
            float numeralSize = radius * .105f;
            for (int value : numerals) {
                double angle = Math.toRadians(value * 30d - 90d);
                float x = cx + (float) Math.cos(angle) * radius * .66f;
                float y = cy + (float) Math.sin(angle) * radius * .66f;
                text(canvas, Integer.toString(value), x,
                        centeredBaseline(y, numeralSize, serif), numeralSize,
                        theme.getPrimaryTextColor(), Paint.Align.CENTER, serif);
            }
            Calendar calendar = state.newCalendar();
            cleanHand(canvas, cx, cy, hourAngle(calendar, state, context), radius * .48f,
                    radius * .032f, theme.getPrimaryTextColor(), radius * .06f,
                    Paint.Cap.SQUARE);
            cleanHand(canvas, cx, cy, minuteAngle(calendar, state, context), radius * .70f,
                    radius * .018f, theme.getPrimaryTextColor(), radius * .08f,
                    Paint.Cap.SQUARE);
            if (state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                cleanHand(canvas, cx, cy, secondAngle(calendar, state, context), radius * .80f,
                        Math.max(lineWidth(context, theme, .75f), radius * .006f),
                        theme.getAccentColor(), radius * .15f, Paint.Cap.SQUARE);
            }
            canvas.drawCircle(cx, cy, radius * .037f, fill(theme.getAccentColor()));
            canvas.drawCircle(cx, cy, radius * .014f, fill(theme.getSurfaceColor()));

            float infoLeft = context.getLeft() + w * (landscape ? .59f : .13f);
            float infoRight = context.getRight() - w * (landscape ? .09f : .13f);
            float titleY = context.getTop() + h * (landscape ? .17f : .61f);
            float dateY = context.getTop() + h * (landscape ? .29f : .68f);
            float dividerY = context.getTop() + h * (landscape ? .35f : .72f);
            float timeY = context.getTop() + h * (landscape ? .52f : .80f);
            float contextY = context.getTop() + h * (landscape ? .64f : .86f);
            float zoneY = context.getTop() + h * (landscape ? .72f : .905f);
            String index = String.format(Locale.US, "PAPER / %02d.%02d",
                    calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
            float small = Math.max(context.getDensity() * 6f, unit * .020f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            dateY += metadataShift;
            dividerY += metadataShift;
            timeY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, index, infoLeft, titleY, infoRight - infoLeft, small,
                    theme.getAccentColor(), Paint.Align.LEFT, sansBold);
            fittedText(canvas, state.getDateText(), infoLeft, dateY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .032f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, serif);
            canvas.drawLine(infoLeft, dividerY, infoRight, dividerY,
                    stroke(theme.getAccentColor(), lineWidth(context, theme, 1f)));
            fittedTime(canvas, timeText(calendar, state, false), infoLeft, timeY,
                    infoRight - infoLeft, unit * (landscape ? .14f : .12f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sansBold);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, infoLeft, contextY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .027f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sans);
            if (!contextLine.equals(state.getTimeZoneText())) {
                fittedTime(canvas, state.getTimeZoneText(), infoLeft, zoneY,
                        infoRight - infoLeft, small, theme.getSecondaryTextColor(),
                        Paint.Align.LEFT, sans);
            }
        }
    }

    private static final class OrbitNeonRenderer extends RendererBase {
        @Override public void render(Canvas canvas, ClockRenderContext context, ClockState state,
                ClockThemeTokens theme) {
            background(canvas, context, theme);
            customBackgroundVeil(canvas, context, 0xC70A1012);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            boolean landscape = w >= h * 1.12f;
            float cx = landscape ? context.getLeft() + w * .35f : context.getCenterX();
            float cy = context.getTop() + h * (landscape ? .49f : .36f);
            float outer = landscape ? Math.min(h * .335f, w * .24f)
                    : Math.min(w * .34f, h * .235f);
            float middle = outer * .72f;
            float inner = outer * .47f;
            float outerWidth = Math.max(lineWidth(context, theme, 2f), unit * .024f);
            float middleWidth = Math.max(lineWidth(context, theme, 1.6f), unit * .018f);
            float innerWidth = Math.max(lineWidth(context, theme, 1.3f), unit * .014f);
            canvas.drawCircle(cx, cy, outer,
                    stroke(alpha(theme.getLineColor(), 115), outerWidth));
            canvas.drawCircle(cx, cy, middle,
                    stroke(alpha(theme.getLineColor(), 100), middleWidth));
            canvas.drawCircle(cx, cy, inner,
                    stroke(alpha(theme.getLineColor(), 85), innerWidth));
            Calendar c = state.newCalendar();
            float hourProgress = (c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) / 60f) / 12f;
            float minuteProgress = (c.get(Calendar.MINUTE) + secondValue(c, state, context) / 60f) / 60f;
            float secondProgress = secondValue(c, state, context) / 60f;
            RectF outerRect = new RectF(cx - outer, cy - outer, cx + outer, cy + outer);
            RectF midRect = new RectF(cx - middle, cy - middle, cx + middle, cy + middle);
            RectF innerRect = new RectF(cx - inner, cy - inner, cx + inner, cy + inner);
            canvas.drawArc(outerRect, -90f, hourProgress * 360f, false,
                    stroke(theme.getAccentColor(), outerWidth));
            arcEnd(canvas, cx, cy, outer, -90f, hourProgress * 360f,
                    theme.getAccentColor(), outerWidth);
            canvas.drawArc(midRect, -90f, minuteProgress * 360f, false,
                    stroke(0xFF62AEFF, middleWidth));
            arcEnd(canvas, cx, cy, middle, -90f, minuteProgress * 360f,
                    0xFF62AEFF, middleWidth);
            if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                canvas.drawArc(innerRect, -90f, secondProgress * 360f, false,
                        stroke(0xFFFF725E, innerWidth));
                arcEnd(canvas, cx, cy, inner, -90f, secondProgress * 360f,
                        0xFFFF725E, innerWidth);
            }
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30d - 90d);
                float px = cx + (float) Math.cos(a) * outer * .86f;
                float py = cy + (float) Math.sin(a) * outer * .86f;
                canvas.drawCircle(px, py, i % 3 == 0 ? unit * .006f : unit * .0035f,
                        fill(i % 3 == 0 ? alpha(theme.getPrimaryTextColor(), 190)
                                : alpha(theme.getLineColor(), 170)));
            }
            String time = timeText(c, state, false);
            Typeface display = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            Typeface mono = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            float timeSize = fitText(time, inner * 1.62f, unit * .105f, display);
            drawTime(canvas, time, cx, centeredBaseline(cy, timeSize, display), timeSize,
                    theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
            String seconds = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                    ? String.format(Locale.US, "%02d", c.get(Calendar.SECOND)) : "--";
            text(canvas, seconds, cx, cy + inner * .46f,
                    Math.max(context.getDensity() * 6f, unit * .023f),
                    0xFFFF725E, Paint.Align.CENTER, mono);

            float infoLeft = context.getLeft() + w * (landscape ? .65f : .10f);
            float infoRight = context.getRight() - w * (landscape ? .08f : .10f);
            float titleY = context.getTop() + h * (landscape ? .17f : .075f);
            float dateY = context.getTop() + h * (landscape ? .30f : .67f);
            float contextY = context.getTop() + h * (landscape ? .40f : .745f);
            float zoneY = context.getTop() + h * (landscape ? .48f : .80f);
            float small = Math.max(context.getDensity() * 6f, unit * .020f);
            float valuesBottomY = context.getTop() + h * (landscape ? .81f : .89f);
            float metadataShift = bottomOverlayShift(context, valuesBottomY);
            dateY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, "ORBIT / LIVE", infoLeft, titleY, infoRight - infoLeft,
                    small, theme.getAccentColor(), Paint.Align.LEFT, mono);
            fittedText(canvas, state.getDateText(), infoLeft, dateY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .031f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, display);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, infoLeft, contextY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .026f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);
            if (!contextLine.equals(state.getTimeZoneText())) {
                fittedTime(canvas, state.getTimeZoneText(), infoLeft, zoneY,
                        infoRight - infoLeft, small, theme.getSecondaryTextColor(),
                        Paint.Align.LEFT, mono);
            }

            int hourValue = c.get(Calendar.HOUR);
            if (hourValue == 0) hourValue = 12;
            if (landscape) {
                String[] values = {
                        String.format(Locale.US, "H  %02d / 12", hourValue),
                        String.format(Locale.US, "M  %02d / 60", c.get(Calendar.MINUTE)),
                        "S  " + seconds + " / 60"
                };
                int[] colors = {theme.getAccentColor(), 0xFF62AEFF, 0xFFFF725E};
                for (int i = 0; i < values.length; i++) {
                    float y = context.getTop() + h * (.62f + i * .095f) + metadataShift;
                    canvas.drawLine(infoLeft, y - small * .32f,
                            infoLeft + unit * .045f, y - small * .32f,
                            stroke(colors[i], lineWidth(context, theme, 1.6f)));
                    float valueX = infoLeft + unit * .072f;
                    fittedText(canvas, values[i], valueX, y, infoRight - valueX, small,
                            theme.getPrimaryTextColor(), Paint.Align.LEFT, mono);
                }
            } else {
                String[] values = {
                        String.format(Locale.US, "H %02d", hourValue),
                        String.format(Locale.US, "M %02d", c.get(Calendar.MINUTE)),
                        "S " + seconds
                };
                int[] colors = {theme.getAccentColor(), 0xFF62AEFF, 0xFFFF725E};
                for (int i = 0; i < values.length; i++) {
                    float x = context.getLeft() + w * (.20f + i * .30f);
                    fittedText(canvas, values[i], x,
                            context.getTop() + h * .89f + metadataShift, w * .24f, small,
                            colors[i], Paint.Align.CENTER, mono);
                }
            }
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
            customBackgroundVeil(canvas, context, 0xD407100F);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            float grid = Math.max(context.getDensity() * 18f, unit * .075f);
            Paint gridPaint = stroke(alpha(theme.getLineColor(), 60),
                    lineWidth(context, theme, .45f));
            for (float x = context.getLeft(); x <= context.getRight(); x += grid) {
                canvas.drawLine(x, context.getTop(), x, context.getBottom(), gridPaint);
            }
            for (float y = context.getTop(); y <= context.getBottom(); y += grid) {
                canvas.drawLine(context.getLeft(), y, context.getRight(), y, gridPaint);
            }
            Typeface mono = typeface(theme.getDisplayFontFamily(), Typeface.NORMAL);
            text(canvas, "DIGITAL GRID", context.getLeft() + w * .07f,
                    context.getTop() + h * .10f,
                    Math.max(context.getDensity() * 7f, unit * .028f),
                    theme.getAccentColor(), Paint.Align.LEFT, mono);
            Calendar c = state.newCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String digits = String.format(Locale.US, "%02d%02d", hour, c.get(Calendar.MINUTE));
            boolean landscape = w >= h * 1.2f;
            float blockTop = context.getTop() + h * (landscape ? .23f : .19f);
            float blockHeight = h * (landscape ? .39f : .29f);
            float left = context.getLeft() + w * (landscape ? .075f : .075f);
            float right = context.getLeft() + w * (landscape ? .79f : .925f);
            float digitGap = Math.max(context.getDensity() * 4f, w * .012f);
            float colonGap = Math.max(context.getDensity() * 12f,
                    w * (landscape ? .050f : .045f));
            float digitWidth = (right - left - digitGap * 2f - colonGap) / 4f;
            float[] positions = {
                    left,
                    left + digitWidth + digitGap,
                    left + digitWidth * 2f + digitGap + colonGap,
                    left + digitWidth * 3f + digitGap * 2f + colonGap
            };
            float panelPad = unit * .012f;
            float radius = unit * .010f;
            RectF hoursPanel = new RectF(positions[0] - panelPad, blockTop - panelPad,
                    positions[1] + digitWidth + panelPad, blockTop + blockHeight + panelPad);
            RectF minutesPanel = new RectF(positions[2] - panelPad, blockTop - panelPad,
                    positions[3] + digitWidth + panelPad, blockTop + blockHeight + panelPad);
            canvas.drawRoundRect(hoursPanel, radius, radius,
                    fill(alpha(theme.getSurfaceColor(), 225)));
            canvas.drawRoundRect(minutesPanel, radius, radius,
                    fill(alpha(theme.getSurfaceColor(), 225)));
            canvas.drawRoundRect(hoursPanel, radius, radius,
                    stroke(alpha(theme.getLineColor(), 185), lineWidth(context, theme, .7f)));
            canvas.drawRoundRect(minutesPanel, radius, radius,
                    stroke(alpha(theme.getLineColor(), 185), lineWidth(context, theme, .7f)));
            for (int i = 0; i < 4; i++) {
                float x = positions[i];
                RectF cell = new RectF(x, blockTop, x + digitWidth, blockTop + blockHeight);
                drawDigit(canvas, digits.charAt(i) - '0', cell, theme);
            }
            float colonX = (positions[1] + digitWidth + positions[2]) * .5f;
            float colonRadius = unit * .010f;
            // drawDigit centres its segments on the cell, so the dots straddle the same
            // midpoint rather than sitting low the way a typeset ':' would.
            canvas.drawCircle(colonX, blockTop + blockHeight * .375f, colonRadius,
                    fill(theme.getAccentColor()));
            canvas.drawCircle(colonX, blockTop + blockHeight * .625f, colonRadius,
                    fill(theme.getAccentColor()));

            RectF secPanel;
            if (landscape) {
                secPanel = new RectF(context.getLeft() + w * .835f, blockTop - panelPad,
                        context.getLeft() + w * .93f, blockTop + blockHeight + panelPad);
            } else {
                secPanel = new RectF(context.getLeft() + w * .34f,
                        context.getTop() + h * .54f, context.getLeft() + w * .66f,
                        context.getTop() + h * .635f);
            }
            canvas.drawRoundRect(secPanel, radius, radius,
                    fill(alpha(theme.getSurfaceColor(), 225)));
            canvas.drawRoundRect(secPanel, radius, radius,
                    stroke(alpha(theme.getAccentColor(), 190), lineWidth(context, theme, .8f)));
            boolean secondsVisible = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF;
            if (secondsVisible) {
                String seconds = String.format(Locale.US, "%02d", c.get(Calendar.SECOND));
                float innerGap = secPanel.width() * .06f;
                float secDigitWidth = (secPanel.width() - innerGap * 3f) * .5f;
                float secTop = secPanel.top + secPanel.height() * .13f;
                float secBottom = secPanel.top + secPanel.height() * .68f;
                RectF tens = new RectF(secPanel.left + innerGap, secTop,
                        secPanel.left + innerGap + secDigitWidth, secBottom);
                RectF ones = new RectF(tens.right + innerGap, secTop,
                        tens.right + innerGap + secDigitWidth, secBottom);
                drawDigit(canvas, seconds.charAt(0) - '0', tens, theme, theme.getAccentColor());
                drawDigit(canvas, seconds.charAt(1) - '0', ones, theme, theme.getAccentColor());
            } else {
                text(canvas, "--", secPanel.centerX(),
                        centeredBaseline(secPanel.centerY(), secPanel.height() * .31f, mono),
                        secPanel.height() * .31f, theme.getAccentColor(), Paint.Align.CENTER, mono);
            }
            text(canvas, "SEC", secPanel.centerX(), secPanel.top + secPanel.height() * .88f,
                    Math.max(context.getDensity() * 6f, unit * .018f),
                    theme.getSecondaryTextColor(), Paint.Align.CENTER, mono);

            float dateY = context.getTop() + h * (landscape ? .75f : .71f);
            float contextY = context.getTop() + h * (landscape ? .83f : .79f);
            float zoneY = context.getTop() + h * (landscape ? .89f : .845f);
            float textLeft = context.getLeft() + w * .075f;
            float textRight = context.getRight() - w * .075f;
            float small = Math.max(context.getDensity() * 6f, unit * .018f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            float minimumDateY = secPanel.bottom + Math.max(small, unit * .012f);
            metadataShift = Math.max(metadataShift,
                    Math.min(0f, minimumDateY - dateY));
            dateY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, state.getDateText(), textLeft, dateY,
                    textRight - textLeft,
                    Math.max(context.getDensity() * 7f, unit * .030f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, mono);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, textLeft, contextY,
                    textRight - textLeft,
                    Math.max(context.getDensity() * 7f, unit * .025f),
                    theme.getAccentColor(), Paint.Align.LEFT, mono);
            boolean zoneFits = zoneY <= context.getBottom() - context.getBottomInset();
            if (!contextLine.equals(state.getTimeZoneText()) && zoneFits) {
                fittedTime(canvas, state.getTimeZoneText(), textLeft, zoneY,
                        textRight - textLeft,
                        Math.max(context.getDensity() * 6f, unit * .019f),
                        theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);
            }
        }

        private static void drawDigit(Canvas canvas, int digit, RectF box, ClockThemeTokens theme) {
            drawDigit(canvas, digit, box, theme, theme.getPrimaryTextColor());
        }

        private static void drawDigit(Canvas canvas, int digit, RectF box, ClockThemeTokens theme,
                int activeColor) {
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
            int off = alpha(theme.getLineColor(), 32);
            int on = activeColor;
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
            customBackgroundVeil(canvas, context, 0xE4EEF0EC);
            float w = context.getWidth();
            float h = context.getHeight();
            float unit = Math.min(w, h);
            Typeface display = typeface(theme.getDisplayFontFamily(), Typeface.BOLD);
            Typeface bold = typeface(theme.getSupportingFontFamily(), Typeface.BOLD);
            Typeface regular = typeface(theme.getSupportingFontFamily(), Typeface.NORMAL);
            Calendar c = state.newCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String hours = String.format(Locale.US, "%02d", hour);
            String minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE));
            boolean landscape = w >= h * 1.12f;
            float small = Math.max(context.getDensity() * 6f, unit * .020f);
            float divider;
            if (landscape) {
                divider = context.getLeft() + w * .36f;
                canvas.drawRect(context.getLeft(), context.getTop(), divider,
                        context.getBottom(), fill(theme.getSurfaceColor()));
                canvas.drawRect(divider, context.getTop(), divider + unit * .008f,
                        context.getBottom(), fill(theme.getAccentColor()));
                float hourCenterX = (context.getLeft() + divider) * .5f;
                float minuteCenterX = divider + (context.getRight() - divider) * .50f;
                float centerY = context.getTop() + h * .47f;
                float hourSize = fitText(hours, (divider - context.getLeft()) * .78f,
                        unit * .34f, display);
                float minuteSize = fitText(minutes, (context.getRight() - divider) * .72f,
                        unit * .34f, display);
                text(canvas, hours, hourCenterX, centeredBaseline(centerY, hourSize, display),
                        hourSize, 0xFFF5F2EA, Paint.Align.CENTER, display);
                text(canvas, minutes, minuteCenterX,
                        centeredBaseline(centerY, minuteSize, display), minuteSize,
                        theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
            } else {
                divider = context.getTop() + h * .48f;
                canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                        divider, fill(theme.getSurfaceColor()));
                canvas.drawRect(context.getLeft(), divider, context.getRight(),
                        divider + unit * .008f, fill(theme.getAccentColor()));
                float hourSize = fitText(hours, w * .76f, unit * .34f, display);
                float minuteSize = fitText(minutes, w * .76f, unit * .34f, display);
                float hourCenterY = context.getTop() + h * .27f;
                float minuteCenterY = context.getTop() + h * .64f;
                text(canvas, hours, context.getCenterX(),
                        centeredBaseline(hourCenterY, hourSize, display), hourSize,
                        0xFFF5F2EA, Paint.Align.CENTER, display);
                text(canvas, minutes, context.getCenterX(),
                        centeredBaseline(minuteCenterY, minuteSize, display), minuteSize,
                        theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
            }

            float titleX = context.getLeft() + w * .07f;
            float titleY = context.getTop() + h * .105f;
            text(canvas, "TYPE / 06", titleX, titleY, small, 0xCCF5F2EA,
                    Paint.Align.LEFT, bold);
            String seconds = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                    ? String.format(Locale.US, "%02d", c.get(Calendar.SECOND)) : "--";
            float secondsX = context.getRight() - w * .07f;
            float secondsY = context.getTop() + h * .16f;
            text(canvas, seconds, secondsX, secondsY, unit * .065f,
                    theme.getAccentColor(), Paint.Align.RIGHT, bold);
            float secondsLabelY = secondsY + Math.max(small * 1.15f, unit * .045f);
            text(canvas, "SEC", secondsX, secondsLabelY, small,
                    landscape ? theme.getSecondaryTextColor() : 0xA8F5F2EA,
                    Paint.Align.RIGHT, regular);

            float dotX = landscape ? divider : context.getRight() - w * .13f;
            float dotCenterY = landscape ? context.getTop() + h * .47f : divider;
            float dotRadius = unit * .013f;
            canvas.drawCircle(dotX, dotCenterY - unit * .050f, dotRadius,
                    fill(theme.getAccentColor()));
            canvas.drawCircle(dotX, dotCenterY + unit * .050f, dotRadius,
                    fill(theme.getAccentColor()));

            float barLeft = context.getLeft() + w * (landscape ? .43f : .10f);
            float barRight = context.getRight() - w * .075f;
            float barY = context.getTop() + h * (landscape ? .64f : .755f);
            float secProgress = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                    ? secondValue(c, state, context) / 60f : 0f;
            canvas.drawLine(barLeft, barY, barRight, barY,
                    stroke(alpha(theme.getLineColor(), 150),
                            Math.max(lineWidth(context, theme, 1f), unit * .004f)));
            canvas.drawLine(barLeft, barY,
                    barLeft + (barRight - barLeft) * secProgress, barY,
                    stroke(theme.getAccentColor(),
                            Math.max(lineWidth(context, theme, 1.4f), unit * .007f)));

            float infoLeft = context.getLeft() + w * (landscape ? .43f : .10f);
            float infoRight = context.getRight() - w * (landscape ? .075f : .10f);
            float dateY = context.getTop() + h * (landscape ? .75f : .825f);
            float contextY = context.getTop() + h * (landscape ? .83f : .885f);
            float metadataShift = bottomOverlayShift(context, contextY);
            dateY += metadataShift;
            contextY += metadataShift;
            fittedText(canvas, state.getDateText(), infoLeft, dateY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .031f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, bold);
            String contextLine = contextText(state);
            fittedContext(canvas, state, contextLine, infoLeft, contextY, infoRight - infoLeft,
                    Math.max(context.getDensity() * 7f, unit * .024f),
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, regular);
            if (!contextLine.equals(state.getTimeZoneText())) {
                float zoneX = context.getLeft() + w * .07f;
                float zoneY = context.getTop() + h * (landscape ? .83f : .435f);
                if (landscape) zoneY += metadataShift;
                fittedTime(canvas, state.getTimeZoneText(), zoneX, zoneY,
                        w * (landscape ? .22f : .45f), small, 0xBDF5F2EA,
                        Paint.Align.LEFT, regular);
            }
        }
    }
}
