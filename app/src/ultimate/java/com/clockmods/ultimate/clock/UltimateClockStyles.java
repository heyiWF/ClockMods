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
import com.clockmods.sdk.clock.WorldClockEntry;
import com.clockmods.ui.ClockTimeText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.text.BreakIterator;

/** Built-in Ultimate styles. Each renderer has a different composition, not merely a palette. */
public final class UltimateClockStyles {
    public static final String STYLE_PRO_CLASSIC = "pro.classic";
    public static final String STYLE_GLASS_ATELIER = "glass.atelier";
    public static final String STYLE_NOIR_INSTRUMENT = "noir.instrument";
    public static final String STYLE_PAPER_STATION = "paper.station";
    public static final String STYLE_ORBIT_NEON = "orbit.neon";
    public static final String STYLE_DIGITAL_GRID = "digital.grid";
    public static final String STYLE_TYPOGRAPHIC = "typographic.poster";
    public static final String STYLE_DUAL_BLOCKS = "ultimate.dual_blocks";
    public static final String STYLE_ORBIT = "ultimate.orbit";
    public static final String STYLE_BUBBLES = "ultimate.bubbles";
    public static final String STYLE_BLEND = "ultimate.blend";
    public static final String STYLE_RIBBON = "ultimate.ribbon";

    private static final ClockStyleRegistry SHARED_REGISTRY = createRegistry();

    private UltimateClockStyles() { }

    static float secondProgress(int second, int millisecond,
            ClockState.SecondHandMotion motion, boolean reducedMotion) {
        int normalizedSecond = Math.max(0, Math.min(59, second));
        if (motion == ClockState.SecondHandMotion.SWEEP && !reducedMotion) {
            return normalizedSecond + Math.max(0, Math.min(999, millisecond)) / 1000f;
        }
        return normalizedSecond;
    }

    /**
     * Bounds reserved for the horizontally scrollable world-clock cards.
     *
     * <p>The old implementation placed the strip at fixed percentages and then compressed the
     * complete clock face to 72% of its height.  Besides turning circles into ovals, that made the
     * problem especially visible on short landscape displays.  Keeping this calculation shared
     * with {@link UltimateClockView} also means the touch target always follows the cards when a
     * bottom overlay is present.
     */
    static RectF worldClockStripBounds(float left, float top, float right, float bottom,
            float density, float bottomInset) {
        float height = Math.max(0f, bottom - top);
        float safeInset = Math.min(Math.max(0f, bottomInset), height * .08f);
        float usableBottom = bottom - safeInset - Math.min(density * 12f, height * .035f);
        float preferredHeight = height * .19f;
        float minimumHeight = Math.max(1f, density) * 80f;
        float maximumHeight = height * .25f;
        float stripHeight = Math.min(maximumHeight,
                Math.max(preferredHeight, Math.min(minimumHeight, maximumHeight)));
        // Assign fields explicitly so the pure-JVM renderer smoke tests can inspect the geometry;
        // the mockable android.jar intentionally leaves RectF constructors as no-ops.
        RectF bounds = new RectF();
        bounds.left = left;
        bounds.top = usableBottom - stripHeight;
        bounds.right = right;
        bounds.bottom = usableBottom;
        return bounds;
    }

    static float worldClockCardWidth(float viewportWidth, float viewportHeight, float density) {
        float unit = Math.min(Math.max(0f, viewportWidth), Math.max(0f, viewportHeight));
        return Math.max(unit * .30f, Math.max(1f, density) * 145f);
    }

    static float worldClockCardGap(float viewportWidth, float viewportHeight, float density) {
        float unit = Math.min(Math.max(0f, viewportWidth), Math.max(0f, viewportHeight));
        return Math.max(Math.max(1f, density) * 10f, unit * .018f);
    }

    /** Match the left edge of the face's background panel, with a modest inset on bare faces. */
    static float worldClockContentInset(String styleId, float width, float faceHeight,
            float density) {
        boolean portrait = faceHeight > width;
        if (STYLE_DUAL_BLOCKS.equals(styleId)) return width * (portrait ? .055f : .029f);
        if (STYLE_BUBBLES.equals(styleId)) return width * (portrait ? .09f : .029f);
        if (STYLE_BLEND.equals(styleId)) return width * (portrait ? .05f : .030f);
        if (STYLE_RIBBON.equals(styleId)) return width * .05f;
        return Math.max(density * 20f, width * .029f);
    }

    static float worldClockContentWidth(int count, float width, float height, float density) {
        return count <= 0 ? 0f : count * worldClockCardWidth(width, height, density)
                + (count - 1) * worldClockCardGap(width, height, density);
    }

    /** Shared card artwork for the native scrolling host and standalone theme previews. */
    static void drawWorldClockCards(Canvas canvas, ClockRenderContext context, ClockState state,
            ClockThemeTokens theme, float height, float originX, float originY) {
        int marker = RendererBase.beginPaintFrame();
        try {
            MigratedRenderer.drawWorldCards(canvas, context, theme, state,
                    RendererBase.supportingTypeface(theme, Typeface.BOLD), height, originX, originY);
        } finally {
            RendererBase.endPaintFrame(marker);
        }
    }

    static float ribbonSecondsTextSize(float timeTextSize) {
        return Math.max(0f, timeTextSize) * .5f;
    }

    /** Hour panel and minute bubble geometry for the vertically stacked Bubbles composition. */
    static float[] bubblesPortraitGeometry(float width, float height, float density) {
        float top = height * .13f;
        float bottom = height * .91f;
        float gap = Math.max(Math.max(1f, density) * 14f, width * .05f);
        float span = Math.max(1f, bottom - top);
        float shapeSpace = Math.max(1f, span - gap);
        float hourHeight = Math.min(width * .50f, shapeSpace * .42f);
        float minuteDiameter = Math.min(width * .74f,
                Math.max(1f, shapeSpace - hourHeight));
        float occupied = hourHeight + gap + minuteDiameter;
        // Put only a quarter of surplus space above the shapes. This keeps the composition close
        // to its header while still using the extra height on very tall screens.
        float hourTop = top + Math.max(0f, span - occupied) * .25f;
        float hourBottom = hourTop + hourHeight;
        float minuteRadius = minuteDiameter * .5f;
        float minuteCenterY = hourBottom + gap + minuteRadius;
        return new float[] {hourTop, hourBottom, minuteCenterY, minuteRadius, gap};
    }

    /** Center line and heights for a portrait ribbon that remains wider than it is tall. */
    static float[] ribbonPortraitGeometry(float width, float height) {
        float centerY = height * .53f;
        float outerHeight = Math.min(height * .36f, width * .42f);
        float ribbonHeight = Math.min(outerHeight * .64f, width * .27f);
        return new float[] {centerY, outerHeight, ribbonHeight};
    }

    /** Splits the host's "solar / lunar" value without mistaking slashes in the date format. */
    static String[] splitDateAndLunar(String value) {
        String clean = value == null ? "" : value.trim();
        int divider = clean.lastIndexOf(" / ");
        if (divider <= 0 || divider + 3 >= clean.length()) {
            return new String[] {clean, ""};
        }
        String lunar = clean.substring(divider + 3).trim();
        // LunarCalendar always contains both markers.  This guard leaves custom dates such as
        // "09 / 11 / 2026" alone instead of inventing a second line for their final component.
        if (lunar.indexOf('年') < 0 || lunar.indexOf('月') < 0) {
            return new String[] {clean, ""};
        }
        return new String[] {clean.substring(0, divider).trim(), lunar};
    }

    /** Measures at the requested size before fitting; prefer the solar/lunar boundary. */
    static String[] dateLines(String value, Paint paint, float maxWidth,
            boolean stackLunar, Locale locale) {
        String clean = value == null ? "" : value.trim();
        String[] lunar = splitDateAndLunar(clean);
        if (stackLunar && !lunar[1].isEmpty()) return lunar;
        if (paint.measureText(clean) <= maxWidth) return new String[] {clean, ""};
        if (!lunar[1].isEmpty()) return lunar;

        // Without a lunar date, wrap at a locale-aware line boundary (not inside a word).
        BreakIterator breaks = BreakIterator.getLineInstance(locale);
        breaks.setText(clean);
        int split = 0;
        float bestWidth = Float.MAX_VALUE;
        for (int index = breaks.first(); index != BreakIterator.DONE; index = breaks.next()) {
            if (index <= 0 || index >= clean.length()) continue;
            String first = clean.substring(0, index).trim();
            String second = clean.substring(index).trim();
            if (first.isEmpty() || second.isEmpty()) continue;
            float width = Math.max(paint.measureText(first), paint.measureText(second));
            if (width < bestWidth) {
                bestWidth = width;
                split = index;
            }
        }
        return split == 0 ? new String[] {clean, ""}
                : new String[] {clean.substring(0, split).trim(), clean.substring(split).trim()};
    }

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
        List<ClockStyle> styles = new ArrayList<ClockStyle>(12);
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
        ClockStyleCapabilities.Capability[] migrated = new ClockStyleCapabilities.Capability[] {
                ClockStyleCapabilities.Capability.SECONDS,
                ClockStyleCapabilities.Capability.DATE,
                ClockStyleCapabilities.Capability.TIME_ZONE,
                ClockStyleCapabilities.Capability.WEATHER,
                ClockStyleCapabilities.Capability.STATUS,
                ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                ClockStyleCapabilities.Capability.REDUCED_MOTION,
                ClockStyleCapabilities.Capability.WORLD_CLOCK
        };
        ClockStyleCapabilities.Capability[] migratedSmooth = new ClockStyleCapabilities.Capability[] {
                ClockStyleCapabilities.Capability.SECONDS,
                ClockStyleCapabilities.Capability.SMOOTH_SECONDS,
                ClockStyleCapabilities.Capability.DATE,
                ClockStyleCapabilities.Capability.TIME_ZONE,
                ClockStyleCapabilities.Capability.WEATHER,
                ClockStyleCapabilities.Capability.STATUS,
                ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                ClockStyleCapabilities.Capability.REDUCED_MOTION,
                ClockStyleCapabilities.Capability.WORLD_CLOCK
        };
        styles.add(style(STYLE_DUAL_BLOCKS, "双块", "小时与分钟的双块布局。",
                ClockStyleMetadata.Kind.DIGITAL, dualBlocksTokens(), migrated,
                new DualBlocksRenderer()));
        styles.add(style(STYLE_ORBIT, "轨道", "环形轨道与连续秒点。",
                ClockStyleMetadata.Kind.HYBRID, orbitMigratedTokens(), migratedSmooth,
                new OrbitRenderer()));
        styles.add(style(STYLE_BUBBLES, "气泡", "同心气泡与动态秒点。",
                ClockStyleMetadata.Kind.DIGITAL, bubblesTokens(), migrated,
                new BubblesRenderer()));
        styles.add(style(STYLE_BLEND, "混合", "几何块面与圆泡混合布局。",
                ClockStyleMetadata.Kind.HYBRID, blendTokens(), migratedSmooth,
                new BlendRenderer()));
        styles.add(style(STYLE_RIBBON, "丝带", "横向丝带承载时间信息。",
                ClockStyleMetadata.Kind.DIGITAL, ribbonTokens(), migrated,
                new RibbonRenderer()));
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

    private static ClockThemeTokens dualBlocksTokens() {
        return migratedTokens();
    }

    private static ClockThemeTokens orbitMigratedTokens() {
        return migratedTokens();
    }

    private static ClockThemeTokens bubblesTokens() {
        return migratedTokens();
    }

    private static ClockThemeTokens blendTokens() {
        return migratedTokens();
    }

    private static ClockThemeTokens ribbonTokens() {
        return migratedTokens();
    }

    /** Palette sampled from the five reference faces. */
    private static ClockThemeTokens migratedTokens() {
        return ClockThemeTokens.builder().background(0xFF154974, 0xFF154974)
                .surfaceColor(0xFF9ECAFC).primaryTextColor(0xFF003256)
                .secondaryTextColor(0xFFD0E4FF).accentColor(0xFF8DBAE2)
                .lineColor(0xFF23557F).fonts("sans-serif", "sans-serif")
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
            boolean previousPhotoText = RendererBase.PAINT_POOL.get().photoText;
            RendererBase.PAINT_POOL.get().photoText = false;
            int saveCount = canvas.save();
            try {
                delegate.render(canvas, context, state, theme);
            } finally {
                canvas.restoreToCount(saveCount);
                RendererBase.endPaintFrame(paintMarker);
                RendererBase.PAINT_POOL.get().photoText = previousPhotoText;
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
            boolean photoText;
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

        /**
         * The display face for {@code theme}: the user's per-theme font when the host supplied
         * one, otherwise the theme's own family.
         *
         * <p>{@code style} is dropped once an override is in play. It exists to ask the platform
         * for a synthesized bold, and the override already carries a weight the user picked from
         * the stops the font really has — layering a fake bold on top of Medium would render
         * something the family cannot actually produce.
         */
        protected static Typeface displayTypeface(ClockThemeTokens theme, int style) {
            Typeface override = theme.getDisplayTypeface();
            return override != null ? override : typeface(theme.getDisplayFontFamily(), style);
        }

        /** As {@link #displayTypeface(ClockThemeTokens, int)}, for the supporting family. */
        protected static Typeface supportingTypeface(ClockThemeTokens theme, int style) {
            Typeface override = theme.getSupportingTypeface();
            return override != null ? override : typeface(theme.getSupportingFontFamily(), style);
        }

        protected static void background(Canvas canvas, ClockRenderContext context,
                ClockThemeTokens theme) {
            background(canvas, context, theme, true);
        }

        protected static void background(Canvas canvas, ClockRenderContext context,
                ClockThemeTokens theme, boolean respectDimming) {
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
                if (hostBackground.isDimmed() && respectDimming) {
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
            if (PAINT_POOL.get().photoText) {
                photoTextOutline(p, size);
                canvas.drawText(value, x, baseline, p);
                photoTextFill(p, color, size);
            }
            canvas.drawText(value, x, baseline, p);
        }

        private static void photoTextOutline(Paint paint, float size) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, Math.min(2f, size * .012f)));
            paint.setColor(0xE6000000);
        }

        private static void photoTextFill(Paint paint, int color, float size) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setShadowLayer(Math.max(2f, size * .035f), 0f, 1f, 0x99000000);
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
         * A viewport-aware floor for functional text.  Twelve sp is retained on normal phone
         * canvases, while tiny gallery thumbnails may scale below it instead of overflowing.
         */
        protected static float readableSize(ClockRenderContext context, float preferredSize,
                float minimumSp) {
            float unit = Math.min(context.getWidth(), context.getHeight());
            float compactFloor = unit * (minimumSp >= 12f ? .045f : .038f);
            float scaledFloor = context.getScaledDensity() * minimumSp;
            return Math.max(preferredSize, Math.min(scaledFloor, compactFloor));
        }

        /** Keeps metadata readable and ellipsizes overflow instead of shrinking it indefinitely. */
        protected static void readableText(Canvas canvas, ClockRenderContext context,
                String value, float x, float baseline, float maxWidth, float preferredSize,
                float minimumSp, int color, Paint.Align align, Typeface face) {
            if (maxWidth <= 0f) return;
            float floor = readableSize(context, 0f, minimumSp);
            float size = Math.max(floor,
                    fitText(value, maxWidth, Math.max(floor, preferredSize), face));
            text(canvas, ellipsize(value, maxWidth, size, face), x, baseline, size,
                    color, align, face);
        }

        /**
         * Draws a date as two rows when requested or too wide at the chosen font size.
         * The host joins solar/lunar values with the
         * final " / "; {@link #splitDateAndLunar(String)} deliberately preserves slashes that are
         * part of the user's Gregorian date format.
         */
        protected static void readableDate(Canvas canvas, ClockRenderContext context,
                ClockState state, float x, float lowerBaseline, float maxWidth,
                float preferredSize, int color, Paint.Align align, Typeface face,
                boolean stackLunar) {
            float inset = Math.min(Math.min(context.getWidth(), context.getHeight()) * .045f,
                    context.getDensity() * 20f);
            float safeLeft = context.getLeft() + inset;
            float safeRight = context.getRight() - inset;
            x = Math.max(safeLeft, Math.min(safeRight, x));
            float availableWidth = align == Paint.Align.LEFT ? safeRight - x
                    : align == Paint.Align.RIGHT ? x - safeLeft
                    : 2f * Math.min(x - safeLeft, safeRight - x);
            maxWidth = Math.min(maxWidth, availableWidth);
            if (maxWidth <= 0f) return;
            float floor = readableSize(context, 0f, 12f);
            float requested = Math.max(floor, preferredSize * state.getDateScale());
            Paint requestedPaint = fill(Color.WHITE);
            requestedPaint.setTypeface(face);
            requestedPaint.setTextSize(requested);
            String[] lines = dateLines(state.getDateText(), requestedPaint, maxWidth,
                    stackLunar, state.getLocale());
            if (lines[1].length() == 0) {
                float size = Math.max(floor,
                        fitText(state.getDateText(), maxWidth, requested, face));
                Paint metricsPaint = fill(Color.WHITE);
                metricsPaint.setTypeface(face);
                metricsPaint.setTextSize(size);
                Paint.FontMetrics metrics = metricsPaint.getFontMetrics();
                float safeBaseline = Math.max(lowerBaseline,
                        context.getTop() + inset - metrics.ascent);
                safeBaseline = Math.min(safeBaseline,
                        context.getBottom() - inset - metrics.descent);
                text(canvas, ellipsize(state.getDateText(), maxWidth, size, face), x,
                        safeBaseline, size, color, align, face);
                return;
            }
            float size = Math.max(floor, Math.min(
                    fitText(lines[0], maxWidth, requested, face),
                    fitText(lines[1], maxWidth, requested, face)));
            float lineGap = size * 1.45f;
            // Headers grow downward when they wrap; footers retain their bottom anchor.
            boolean designedStack = stackLunar
                    && !splitDateAndLunar(state.getDateText())[1].isEmpty();
            if (!designedStack && lowerBaseline < context.getCenterY()) {
                lowerBaseline += lineGap;
            }
            Paint metricsPaint = fill(Color.WHITE);
            metricsPaint.setTypeface(face);
            metricsPaint.setTextSize(size);
            Paint.FontMetrics metrics = metricsPaint.getFontMetrics();
            float safeLowerBaseline = Math.max(lowerBaseline,
                    context.getTop() + inset + lineGap - metrics.ascent);
            safeLowerBaseline = Math.min(safeLowerBaseline,
                    context.getBottom() - inset - metrics.descent);
            text(canvas, ellipsize(lines[0], maxWidth, size, face), x,
                    safeLowerBaseline - lineGap, size, color, align, face);
            text(canvas, ellipsize(lines[1], maxWidth, size, face), x, safeLowerBaseline,
                    size, color, align, face);
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
            if (PAINT_POOL.get().photoText) {
                photoTextOutline(p, size);
                ClockTimeText.draw(canvas, value, x, baseline, p);
                photoTextFill(p, color, size);
            }
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
            if (state.getWeatherText().length() == 0) return state.getStatusText();
            if (state.getStatusText().length() == 0) return state.getWeatherText();
            return state.getWeatherText() + "  ·  " + state.getStatusText();
        }

        /**
         * Draws the context line from {@link #contextText}. It falls back to the time zone
         * when there is no weather or status to show, and only that fallback is treated as a
         * time; a status message the user typed keeps its own punctuation.
         */
        protected static void fittedContext(Canvas canvas, ClockState state, String value,
                float x, float baseline, float maxWidth, float preferredSize, int color,
                Paint.Align align, Typeface face) {
            fittedText(canvas, value, x, baseline, maxWidth, preferredSize, color, align, face);
        }

        /** Ellipsizes at a stable readable size instead of shrinking metadata into illegibility. */
        protected static String ellipsize(String value, float maxWidth, float size,
                Typeface face) {
            if (value == null || value.length() == 0 || maxWidth <= 0f) return "";
            Paint p = fill(Color.WHITE);
            p.setTypeface(face);
            p.setTextSize(size);
            if (p.measureText(value) <= maxWidth) return value;
            String suffix = "…";
            float suffixWidth = p.measureText(suffix);
            int end = value.length();
            while (end > 0 && p.measureText(value, 0, end) + suffixWidth > maxWidth) end--;
            return end == 0 ? suffix : value.substring(0, end).trim() + suffix;
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
            return secondProgress(c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND),
                    state.getSecondHandMotion(), context.isReducedMotion());
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
            Typeface display = displayTypeface(theme, Typeface.NORMAL);
            Typeface supporting = supportingTypeface(theme, Typeface.NORMAL);
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

            float dateSize = readableSize(context, unit * .045f * state.getDateScale(), 12f);
            float supportingSize = readableSize(context,
                    unit * .040f * state.getSupportingScale(), 12f);
            float supportingGap = Math.max(context.getDensity() * 7f,
                    Math.max(dateSize, supportingSize) * .55f);
            readableDate(canvas, context, state, centerX,
                    baseline + metrics.ascent - supportingGap, width * .88f,
                    unit * .045f, theme.getSecondaryTextColor(), Paint.Align.CENTER,
                    supporting, height > width);
            String contextText = contextText(state);
            readableText(canvas, context, contextText, centerX,
                    baseline + metrics.descent + supportingGap + supportingSize,
                    width * .88f, unit * .040f * state.getSupportingScale(), 12f,
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
            Typeface sans = supportingTypeface(theme, Typeface.NORMAL);
            Typeface bold = displayTypeface(theme, Typeface.BOLD);
            float infoLeft = context.getLeft() + w * (landscape ? .075f : .10f);
            float infoRight = context.getLeft() + w * (landscape ? .405f : .90f);
            float titleY = context.getTop() + h * (landscape ? .15f : .075f);
            float dateY = context.getTop() + h * (landscape ? .29f : .65f);
            float timeY = context.getTop() + h * (landscape ? .47f : .76f);
            float contextY = context.getTop() + h * (landscape ? .60f : .84f);
            float zoneY = context.getTop() + h * (landscape ? .69f : .895f);
            float small = readableSize(context, unit * .021f, 10f);
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
            readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
                    unit * .034f, theme.getSecondaryTextColor(), Paint.Align.LEFT, sans,
                    !landscape);
            String time = timeText(state.newCalendar(), state, false);
            fittedTime(canvas, time, infoLeft, timeY, infoRight - infoLeft,
                    unit * (landscape ? .145f : .12f), theme.getPrimaryTextColor(),
                    Paint.Align.LEFT, bold);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, unit * .030f * state.getSupportingScale(), 12f,
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sans);

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
            Typeface mono = supportingTypeface(theme, Typeface.NORMAL);
            Typeface condensed = displayTypeface(theme, Typeface.BOLD);
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
            float small = readableSize(context, unit * .020f, 10f);
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
            readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
                    unit * .030f, theme.getSecondaryTextColor(), Paint.Align.LEFT, mono,
                    !landscape);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, unit * .027f * state.getSupportingScale(), 12f,
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, mono);

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
                        readableSize(context, subR * .31f, 10f));
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
            Typeface serif = displayTypeface(theme, Typeface.NORMAL);
            Typeface sans = supportingTypeface(theme, Typeface.NORMAL);
            Typeface sansBold = supportingTypeface(theme, Typeface.BOLD);
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
            float small = readableSize(context, unit * .020f, 10f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            dateY += metadataShift;
            dividerY += metadataShift;
            timeY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, index, infoLeft, titleY, infoRight - infoLeft, small,
                    theme.getAccentColor(), Paint.Align.LEFT, sansBold);
            readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
                    unit * .032f, theme.getSecondaryTextColor(), Paint.Align.LEFT, serif,
                    !landscape);
            canvas.drawLine(infoLeft, dividerY, infoRight, dividerY,
                    stroke(theme.getAccentColor(), lineWidth(context, theme, 1f)));
            fittedTime(canvas, timeText(calendar, state, false), infoLeft, timeY,
                    infoRight - infoLeft, unit * (landscape ? .14f : .12f),
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sansBold);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, unit * .027f * state.getSupportingScale(), 12f,
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, sans);
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
            Typeface display = displayTypeface(theme, Typeface.NORMAL);
            Typeface mono = supportingTypeface(theme, Typeface.NORMAL);
            float timeSize = fitText(time, inner * 1.62f, unit * .105f, display);
            drawTime(canvas, time, cx, centeredBaseline(cy, timeSize, display), timeSize,
                    theme.getPrimaryTextColor(), Paint.Align.CENTER, display);
            String seconds = state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
                    ? String.format(Locale.US, "%02d", c.get(Calendar.SECOND)) : "--";
            text(canvas, seconds, cx, cy + inner * .46f,
                    readableSize(context, unit * .023f, 12f),
                    0xFFFF725E, Paint.Align.CENTER, mono);

            float infoLeft = context.getLeft() + w * (landscape ? .65f : .10f);
            float infoRight = context.getRight() - w * (landscape ? .08f : .10f);
            float titleY = context.getTop() + h * (landscape ? .17f : .075f);
            float dateY = context.getTop() + h * (landscape ? .30f : .67f);
            float contextY = context.getTop() + h * (landscape ? .40f : .745f);
            float zoneY = context.getTop() + h * (landscape ? .48f : .80f);
            float small = readableSize(context, unit * .020f, 10f);
            float valuesBottomY = context.getTop() + h * (landscape ? .81f : .89f);
            float metadataShift = bottomOverlayShift(context, valuesBottomY);
            dateY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            fittedText(canvas, "ORBIT / LIVE", infoLeft, titleY, infoRight - infoLeft,
                    small, theme.getAccentColor(), Paint.Align.LEFT, mono);
            readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
                    unit * .031f, theme.getPrimaryTextColor(), Paint.Align.LEFT, display,
                    !landscape);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, unit * .026f * state.getSupportingScale(), 12f,
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, mono);

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
            Typeface mono = displayTypeface(theme, Typeface.NORMAL);
            text(canvas, "DIGITAL GRID", context.getLeft() + w * .07f,
                    context.getTop() + h * .10f,
                    readableSize(context, unit * .028f, 10f),
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
                    readableSize(context, unit * .018f, 10f),
                    theme.getSecondaryTextColor(), Paint.Align.CENTER, mono);

            float dateY = context.getTop() + h * (landscape ? .75f : .71f);
            float contextY = context.getTop() + h * (landscape ? .83f : .79f);
            float zoneY = context.getTop() + h * (landscape ? .89f : .845f);
            float textLeft = context.getLeft() + w * .075f;
            float textRight = context.getRight() - w * .075f;
            float small = readableSize(context, unit * .018f, 10f);
            float metadataShift = bottomOverlayShift(context, zoneY);
            float minimumDateY = secPanel.bottom + Math.max(small, unit * .012f);
            metadataShift = Math.max(metadataShift,
                    Math.min(0f, minimumDateY - dateY));
            dateY += metadataShift;
            contextY += metadataShift;
            zoneY += metadataShift;
            readableDate(canvas, context, state, textLeft, dateY,
                    textRight - textLeft, unit * .030f, theme.getPrimaryTextColor(),
                    Paint.Align.LEFT, mono, !landscape);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, textLeft, contextY,
                    textRight - textLeft, unit * .025f * state.getSupportingScale(), 12f,
                    theme.getAccentColor(), Paint.Align.LEFT, mono);
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
            Typeface display = displayTypeface(theme, Typeface.BOLD);
            Typeface bold = supportingTypeface(theme, Typeface.BOLD);
            Typeface regular = supportingTypeface(theme, Typeface.NORMAL);
            Calendar c = state.newCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String hours = String.format(Locale.US, "%02d", hour);
            String minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE));
            boolean landscape = w >= h * 1.12f;
            float small = readableSize(context, unit * .020f, 10f);
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
            readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
                    unit * .031f, theme.getPrimaryTextColor(), Paint.Align.LEFT, bold,
                    !landscape);
            String contextLine = contextText(state);
            readableText(canvas, context, contextLine, infoLeft, contextY,
                    infoRight - infoLeft, unit * .024f * state.getSupportingScale(), 12f,
                    theme.getSecondaryTextColor(), Paint.Align.LEFT, regular);
        }
    }

    /** The five migrated compositions, measured from the supplied 16:9 reference captures. */
    private abstract static class MigratedRenderer extends RendererBase {
        private GaussianGlass glass;
        protected abstract int mode();

        private void panel(Canvas canvas, RectF bounds, float rx, float ry, int color) {
            if (glass == null) canvas.drawRoundRect(bounds, rx, ry, fill(color));
            else glass.roundRect(canvas, bounds, rx, ry, color, 0f, 0f, 0f);
        }

        private void bubble(Canvas canvas, float x, float y, float radius, int color) {
            if (glass == null) canvas.drawCircle(x, y, radius, fill(color));
            else glass.circle(canvas, x, y, radius, color);
        }

        @Override public final void render(Canvas canvas, ClockRenderContext context,
                ClockState state, ClockThemeTokens theme) {
            glass = GaussianGlass.create(context, theme);
            background(canvas, context, theme, glass == null);
            ClockPalette colors = glass == null ? ClockPalette.fromTokens(theme) : glass.palette();
            if (context.getBackground() != null
                    && context.getBackground().getMode() == ClockBackground.Mode.COLOR) {
                colors = colors.withColor(0, context.getBackground().getColor());
            }
            if (context.getBackground() != null && context.getBackground().isDimmed()
                    && !context.getBackground().hasImage()) {
                colors = colors.withColor(0, ClockPalette.mix(colors.background, 0xFF000000, .4f));
            }
            Typeface display = displayTypeface(theme, Typeface.BOLD);
            Typeface supporting = supportingTypeface(theme, Typeface.BOLD);
            Calendar c = state.newCalendar();
            String time = timeText(c, state, false);
            int style = mode();
            boolean withWorldClocks = !state.getWorldClocks().isEmpty();
            ClockRenderContext faceContext = context;
            if (withWorldClocks) {
                RectF strip = worldClockStripBounds(context.getLeft(), context.getTop(),
                        context.getRight(), context.getBottom(), context.getDensity(),
                        context.getBottomInset());
                float gap = Math.min(context.getDensity() * 8f, context.getHeight() * .025f);
                float contentBottom = Math.max(context.getTop(), strip.top - gap);
                faceContext = new ClockRenderContext(context.getLeft(), context.getTop(),
                        context.getRight(), contentBottom, context.getDensity(),
                        context.getScaledDensity(), context.getFrameTimeMillis(),
                        context.isReducedMotion(), context.getBackground(), 0f,
                        context.getWorldClockScroll());
            }
            int save = canvas.save();
            if (style == 0) drawDual(canvas, faceContext, state, c, display, supporting, colors);
            else if (style == 1) drawOrbit(canvas, faceContext, state, c, time, display, supporting, colors);
            else if (style == 2) drawBubbles(canvas, faceContext, state, c, display, supporting, colors);
            else if (style == 3) drawBlend(canvas, faceContext, state, c, time, display, supporting, colors);
            else drawRibbon(canvas, faceContext, state, c, time, display, supporting, colors);
            canvas.restoreToCount(save);
            if (!context.isWorldClockStripHosted()) {
                drawWorldStrip(canvas, context, theme, state, supporting);
            }
        }

        private void drawDual(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, Typeface face, Typeface supporting, ClockPalette colors) {
            float w = context.getWidth();
            float h = context.getHeight();
            boolean landscape = w >= h;
            float marginX = w * (landscape ? .029f : .055f);
            float marginY = h * .037f;
            float gap = w * (landscape ? .022f : .035f);
            RectF first;
            RectF second;
            if (landscape) {
                first = new RectF(context.getLeft() + marginX, context.getTop() + marginY,
                        context.getCenterX() - gap * .5f, context.getBottom() - marginY);
                second = new RectF(context.getCenterX() + gap * .5f,
                        context.getTop() + marginY, context.getRight() - marginX,
                        context.getBottom() - marginY);
            } else {
                first = new RectF(context.getLeft() + marginX, context.getTop() + marginY,
                        context.getRight() - marginX, context.getCenterY() - gap * .5f);
                second = new RectF(context.getLeft() + marginX, context.getCenterY() + gap * .5f,
                        context.getRight() - marginX, context.getBottom() - marginY);
            }
            float radius = Math.min(first.width(), first.height()) * .075f;
            panel(canvas, first, radius, radius, colors.panel);
            panel(canvas, second, radius, radius, colors.accent);

            int hourValue = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) {
                hourValue %= 12;
                if (hourValue == 0) hourValue = 12;
            }
            String hours = String.format(Locale.US, "%02d", hourValue);
            String minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE));
            float size = Math.min(first.height() * .40f, first.width() * .59f)
                    * state.getTimeScale();
            size = fitText(hours, first.width() * .76f, size, face);
            float timeY = first.centerY();
            drawTime(canvas, hours, first.centerX(), centeredBaseline(timeY, size, face), size,
                    colors.onPanel, Paint.Align.CENTER, face);
            size = fitText(minutes, second.width() * .76f, size, face);
            drawTime(canvas, minutes, second.centerX(),
                    centeredBaseline(second.centerY(), size, face), size,
                    colors.onAccent, Paint.Align.CENTER, face);

            float supportScale = state.getSupportingScale();
            float labelSize = readableSize(context,
                    Math.min(w, h) * .027f * supportScale, 10f);
            String hourLabel = isChinese(state) ? "小时" : "HOUR";
            String minuteLabel = isChinese(state) ? "分钟" : "MINUTE";
            text(canvas, hourLabel, first.left + first.width() * .05f,
                    first.bottom - first.height() * .052f, labelSize, colors.mutedPanel,
                    Paint.Align.LEFT, supporting);
            text(canvas, minuteLabel, second.right - second.width() * .05f,
                    second.bottom - second.height() * .052f, labelSize, colors.onAccent,
                    Paint.Align.RIGHT, supporting);
            float dateBaseline = first.top + first.height() * .078f;
            float dateSize = readableSize(context,
                    Math.min(w, h) * .034f * state.getDateScale(), 12f);
            Paint datePaint = fill(colors.onPanel);
            datePaint.setTypeface(supporting);
            datePaint.setTextSize(dateSize);
            String[] dateRows = dateLines(state.getDateText(), datePaint, first.width() * .74f,
                    !landscape, state.getLocale());
            dateSize = Math.max(readableSize(context, 0f, 12f), Math.min(
                    fitText(dateRows[0], first.width() * .74f, dateSize, supporting),
                    dateRows[1].isEmpty() ? dateSize
                            : fitText(dateRows[1], first.width() * .74f, dateSize, supporting)));
            datePaint.setTextSize(dateSize);
            float secondLine = !landscape && !splitDateAndLunar(state.getDateText())[1].isEmpty()
                    ? dateSize * 1.45f : 0f;
            float paddedBaseline = first.top + Math.min(context.getDensity() * 20f,
                    first.height() * .05f) - datePaint.getFontMetrics().ascent + secondLine;
            dateBaseline = landscape ? Math.max(dateBaseline, paddedBaseline) : paddedBaseline;
            drawDate(canvas, context, state, first.left + first.width() * .05f,
                    dateBaseline, first.width() * .74f,
                    Paint.Align.LEFT, colors.onPanel, supporting, Math.min(w, h) * .034f);
            drawContext(canvas, context, state, second.right - second.width() * .05f,
                    second.top + second.height() * .078f, second.width() * .62f,
                    Paint.Align.RIGHT, colors.onAccent, supporting, Math.min(w, h) * .028f);
            if (secondsVisible(state)) {
                float markerSize = readableSize(context,
                        Math.min(w, h) * .034f * supportScale, 16f);
                float markerRadius = Math.max(Math.min(w, h) * .038f, markerSize * .88f);
                float markerX = second.left + second.width() * .10f;
                float markerY = second.bottom - Math.max(second.height() * .079f, markerRadius * 1.25f);
                drawScallopedCircle(canvas, markerX, markerY, markerRadius,
                        colors.badge);
                text(canvas, String.format(Locale.US, "%02d", c.get(Calendar.SECOND)), markerX,
                        centeredBaseline(markerY, markerSize, supporting), markerSize,
                        colors.onBadge, Paint.Align.CENTER, supporting);
            }
        }

        private void drawOrbit(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, String time, Typeface face, Typeface supporting, ClockPalette colors) {
            float cx = context.getCenterX();
            float h = context.getHeight();
            float w = context.getWidth();
            float cy = context.getTop() + h * .522f;
            float outer = Math.min(h * .382f, w * .32f);
            ring(canvas, cx, cy, outer, colors.ring(.20f), Math.max(2f, h * .0055f));
            ring(canvas, cx, cy, outer * .755f, colors.ring(.15f), Math.max(2f, h * .004f));
            ring(canvas, cx, cy, outer * .515f, colors.ring(.10f), Math.max(1f, h * .0026f));
            if (secondsVisible(state)) {
                float angle = secondAngle(c, state, context);
                double rad = Math.toRadians(angle);
                float dotX = cx + (float) Math.cos(rad) * outer;
                float dotY = cy + (float) Math.sin(rad) * outer;
                bubble(canvas, dotX, dotY, Math.max(context.getDensity() * 4f, h * .0105f), colors.accent);
            }
            float dialSize = fitText(time, outer * 1.90f,
                    h * .278f * state.getTimeScale(), face);
            boolean previousPhotoText = RendererBase.PAINT_POOL.get().photoText;
            RendererBase.PAINT_POOL.get().photoText = glass != null;
            try {
                drawTime(canvas, time, cx, centeredBaseline(cy, dialSize, face), dialSize,
                        colors.onBackground, Paint.Align.CENTER, face);
            } finally {
                RendererBase.PAINT_POOL.get().photoText = previousPhotoText;
            }
            drawDate(canvas, context, state, context.getRight() - w * .029f,
                    context.getTop() + h * .072f, w * .46f, Paint.Align.RIGHT, colors.onBackground,
                    supporting, Math.min(w, h) * .032f);
            drawContext(canvas, context, state, context.getLeft() + w * .029f,
                    context.getTop() + h * .072f, w * .40f, Paint.Align.LEFT, colors.mutedBackground,
                    supporting, Math.min(w, h) * .027f);
            if (secondsVisible(state)) {
                float bubbleRadius = h * .040f;
                float y = cy + outer;
                drawScallopedCircle(canvas, cx, y, bubbleRadius, colors.accent);
                float bubbleSize = Math.min(bubbleRadius * 1.15f,
                        readableSize(context, bubbleRadius * .95f, 16f));
                String seconds = String.format(Locale.US, "%02d", c.get(Calendar.SECOND));
                Paint secondsPaint = fill(colors.onAccent);
                secondsPaint.setTypeface(supporting);
                secondsPaint.setTextSize(bubbleSize);
                secondsPaint.setTextAlign(Paint.Align.CENTER);
                android.graphics.Rect glyphBounds = new android.graphics.Rect();
                secondsPaint.getTextBounds(seconds, 0, seconds.length(), glyphBounds);
                // Align the visible digits, not the font's ascent/descent box, to the ring.
                canvas.drawText(seconds, cx,
                        y - (glyphBounds.top + glyphBounds.bottom) * .5f, secondsPaint);
            }
        }

        private void drawBubbles(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, Typeface face, Typeface supporting, ClockPalette colors) {
            float w = context.getWidth();
            float h = context.getHeight();
            boolean landscape = w >= h;
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12; }
            String hours = String.format(Locale.US, "%02d", hour);
            String minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE));
            if (!landscape) {
                drawBubblesPortrait(canvas, context, state, c, face, supporting, hours, minutes, colors);
                return;
            }
            RectF hourPanel = new RectF(context.getLeft() + w * .029f,
                    context.getTop() + h * .218f, context.getLeft() + w * .374f,
                    context.getTop() + h * .829f);
            float hourRadius = h * .050f;
            panel(canvas, hourPanel, hourRadius, hourRadius, colors.panel);
            float minuteX = context.getLeft() + w * .585f;
            float minuteY = context.getTop() + h * .516f;
            float minuteRadius = h * .315f;
            drawScallopedCircle(canvas, minuteX, minuteY, minuteRadius, colors.accent);
            float secondX = context.getLeft() + w * .884f;
            float secondY = context.getTop() + h * .522f;
            float secondRadius = h * .143f;
            bubble(canvas, secondX, secondY, secondRadius, colors.panelAlt);

            float numberSize = h * .235f * state.getTimeScale();
            float hourSize = fitText(hours, hourPanel.width() * .68f, numberSize, face);
            float minuteSize = fitText(minutes, minuteRadius * 1.25f, numberSize, face);
            drawTime(canvas, hours, hourPanel.centerX(),
                    centeredBaseline(hourPanel.centerY(), hourSize, face),
                    hourSize, colors.onPanel, Paint.Align.CENTER, face);
            drawTime(canvas, minutes, minuteX,
                    centeredBaseline(minuteY, minuteSize, face), minuteSize,
                    colors.onAccent, Paint.Align.CENTER, face);
            if (secondsVisible(state)) {
                float secondSize = fitText("00", secondRadius * 1.15f,
                        h * .075f * state.getSupportingScale(), supporting);
                String seconds = String.format(Locale.US, "%02d", c.get(Calendar.SECOND));
                text(canvas, seconds, secondX,
                        centeredBaseline(secondY, secondSize, supporting), secondSize,
                        colors.onPanelAlt, Paint.Align.CENTER, supporting);
            }
            float labelSize = readableSize(context,
                    Math.min(w, h) * .027f * state.getSupportingScale(), 10f);
            text(canvas, isChinese(state) ? "小时" : "HOUR",
                    hourPanel.left + hourPanel.width() * .045f,
                    hourPanel.bottom - hourPanel.height() * .06f,
                    labelSize, colors.mutedPanel, Paint.Align.LEFT, supporting);
            text(canvas, isChinese(state) ? "分钟" : "MINUTE", minuteX,
                    minuteY + minuteRadius * .82f, labelSize, colors.onAccent,
                    Paint.Align.CENTER, supporting);
            drawDate(canvas, context, state, context.getLeft() + w * .029f,
                    context.getTop() + h * .072f, w * .55f, Paint.Align.LEFT, colors.mutedBackground,
                    supporting, Math.min(w, h) * .032f);
            drawContext(canvas, context, state, context.getRight() - w * .029f,
                    context.getTop() + h * .072f, w * .35f, Paint.Align.RIGHT, colors.mutedBackground,
                    supporting, Math.min(w, h) * .027f);
        }

        private void drawBubblesPortrait(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, Typeface face, Typeface supporting, String hours, String minutes, ClockPalette colors) {
            float w = context.getWidth();
            float h = context.getHeight();
            drawDate(canvas, context, state, context.getLeft() + w * .06f,
                    context.getTop() + h * .055f, w * .76f, Paint.Align.LEFT, colors.mutedBackground,
                    supporting, w * .040f);
            float[] geometry = bubblesPortraitGeometry(w, h, context.getDensity());
            RectF hour = new RectF(context.getLeft() + w * .09f,
                    context.getTop() + geometry[0], context.getRight() - w * .09f,
                    context.getTop() + geometry[1]);
            float hourCorner = Math.min(hour.width(), hour.height()) * .11f;
            panel(canvas, hour, hourCorner, hourCorner, colors.panel);

            float minuteX = context.getLeft() + w * .42f;
            float minuteY = context.getTop() + geometry[2];
            float minuteRadius = geometry[3];
            drawScallopedCircle(canvas, minuteX, minuteY, minuteRadius, colors.accent);

            float hourPreferred = Math.min(hour.height() * .44f, w * .24f)
                    * state.getTimeScale();
            float hourSize = fitText(hours, hour.width() * .72f, hourPreferred, face);
            float minutePreferred = Math.min(minuteRadius * .72f, w * .24f)
                    * state.getTimeScale();
            float minuteSize = fitText(minutes, minuteRadius * 1.30f,
                    minutePreferred, face);
            drawTime(canvas, hours, hour.centerX(),
                    centeredBaseline(hour.centerY(), hourSize, face), hourSize,
                    colors.onPanel, Paint.Align.CENTER, face);
            drawTime(canvas, minutes, minuteX,
                    centeredBaseline(minuteY, minuteSize, face), minuteSize,
                    colors.onAccent, Paint.Align.CENTER, face);

            float labelSize = readableSize(context,
                    Math.min(w, h) * .027f * state.getSupportingScale(), 10f);
            text(canvas, isChinese(state) ? "小时" : "HOUR",
                    hour.left + hour.width() * .06f,
                    hour.bottom - hour.height() * .055f, labelSize, colors.mutedPanel,
                    Paint.Align.LEFT, supporting);
            text(canvas, isChinese(state) ? "分钟" : "MINUTE", minuteX,
                    minuteY + minuteRadius * .79f, labelSize, colors.onAccent,
                    Paint.Align.CENTER, supporting);

            if (secondsVisible(state)) {
                float sr = Math.min(w * .09f, minuteRadius * .27f);
                float desiredX = minuteX + minuteRadius + sr + geometry[4] * .40f;
                float sx = Math.min(context.getRight() - w * .03f - sr, desiredX);
                float sy = minuteY + minuteRadius * .32f;
                bubble(canvas, sx, sy, sr, colors.panelAlt);
                float ss = fitText("00", sr * 1.28f, sr * .76f, supporting);
                text(canvas, String.format(Locale.US, "%02d", c.get(Calendar.SECOND)), sx,
                        centeredBaseline(sy, ss, supporting), ss, colors.onPanelAlt,
                        Paint.Align.CENTER, supporting);
            }
            drawContext(canvas, context, state, context.getLeft() + w * .06f,
                    context.getTop() + h * .965f, w * .88f, Paint.Align.LEFT, colors.mutedBackground,
                    supporting, w * .034f);
        }

        private void drawBlend(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, String time, Typeface face, Typeface supporting, ClockPalette colors) {
            float w = context.getWidth();
            float h = context.getHeight();
            boolean landscape = w >= h;
            RectF analog;
            RectF digital;
            if (landscape) {
                analog = new RectF(context.getLeft() + w * .030f, context.getTop() + h * .046f,
                        context.getLeft() + w * .527f, context.getBottom() - h * .034f);
                digital = new RectF(context.getLeft() + w * .550f, context.getTop() + h * .046f,
                        context.getRight() - w * .027f, context.getBottom() - h * .034f);
            } else {
                analog = new RectF(context.getLeft() + w * .05f, context.getTop() + h * .035f,
                        context.getRight() - w * .05f, context.getTop() + h * .55f);
                digital = new RectF(context.getLeft() + w * .05f, context.getTop() + h * .575f,
                        context.getRight() - w * .05f, context.getBottom() - h * .035f);
            }
            float radius = Math.min(analog.width(), analog.height()) * .06f;
            panel(canvas, analog, radius, radius, colors.panel);
            panel(canvas, digital, radius, radius, colors.accent);
            drawAnalog(canvas, context, state, c, analog, colors);
            float timeSize = fitText(time, digital.width() * .88f,
                    Math.min(digital.height() * .30f, digital.width() * .29f)
                            * state.getTimeScale(), face);
            drawTime(canvas, time, digital.centerX(),
                    centeredBaseline(digital.centerY() - digital.height() * .03f,
                            timeSize, face), timeSize, colors.onAccent, Paint.Align.CENTER, face);
            drawDateAndLunar(canvas, context, state,
                    digital.left + digital.width() * .045f,
                    digital.bottom - digital.height() * .050f, digital.width() * .72f,
                    Paint.Align.LEFT, colors.mutedAccent, supporting,
                    Math.min(w, h) * .032f);
            drawContext(canvas, context, state, digital.right - digital.width() * .045f,
                    digital.top + digital.height() * .067f, digital.width() * .48f,
                    Paint.Align.RIGHT, colors.mutedAccent, supporting,
                    Math.min(w, h) * .027f);
            if (secondsVisible(state)) {
                float secondSize = readableSize(context,
                        Math.min(w, h) * .034f * state.getSupportingScale(), 16f);
                float sr = Math.max(Math.min(w, h) * .038f, secondSize * .88f);
                float sx = digital.right - digital.width() * .09f;
                float sy = digital.bottom - Math.max(digital.height() * .067f, sr * 1.25f);
                drawScallopedCircle(canvas, sx, sy, sr, colors.badge);
                text(canvas, String.format(Locale.US, "%02d", c.get(Calendar.SECOND)), sx,
                        centeredBaseline(sy, secondSize, supporting), secondSize,
                        colors.onBadge, Paint.Align.CENTER, supporting);
            }
        }

        private void drawAnalog(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, RectF panel, ClockPalette colors) {
            float radius = Math.min(panel.width() * .46f, panel.height() * .44f);
            float cx = panel.centerX();
            float cy = panel.centerY();
            bubble(canvas, cx, cy, radius, colors.panelAlt);
            for (int index = 0; index < 60; index++) {
                boolean major = index % 5 == 0;
                double angle = Math.toRadians(index * 6f - 90f);
                float outside = radius * .93f;
                float inside = radius * (major ? .80f : .86f);
                float x1 = cx + (float) Math.cos(angle) * inside;
                float y1 = cy + (float) Math.sin(angle) * inside;
                float x2 = cx + (float) Math.cos(angle) * outside;
                float y2 = cy + (float) Math.sin(angle) * outside;
                Paint tick = stroke(colors.onPanelAlt, Math.max(context.getDensity() * (major ? 3f : 1f),
                        radius * (major ? .012f : .005f)));
                tick.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(x1, y1, x2, y2, tick);
            }
            cleanHand(canvas, cx, cy, hourAngle(c, state, context), radius * .50f,
                    radius * .055f, colors.onPanelAlt, radius * .03f, Paint.Cap.ROUND);
            cleanHand(canvas, cx, cy, minuteAngle(c, state, context), radius * .69f,
                    radius * .039f, colors.onPanelAlt, radius * .04f, Paint.Cap.ROUND);
            if (secondsVisible(state)) {
                cleanHand(canvas, cx, cy, secondAngle(c, state, context), radius * .77f,
                        Math.max(context.getDensity() * 1.5f, radius * .010f), colors.hand(),
                        radius * .04f, Paint.Cap.ROUND);
            }
            canvas.drawCircle(cx, cy, radius * .070f, fill(colors.hand()));
        }

        private void drawRibbon(Canvas canvas, ClockRenderContext context, ClockState state,
                Calendar c, String time, Typeface face, Typeface supporting, ClockPalette colors) {
            float w = context.getWidth();
            float h = context.getHeight();
            boolean portrait = h > w;
            RectF outer;
            RectF ribbon;
            if (portrait) {
                float[] geometry = ribbonPortraitGeometry(w, h);
                float centerY = context.getTop() + geometry[0];
                outer = new RectF(context.getLeft() + w * .050f,
                        centerY - geometry[1] * .5f, context.getRight() - w * .040f,
                        centerY + geometry[1] * .5f);
                ribbon = new RectF(context.getLeft() + w * .078f,
                        centerY - geometry[2] * .5f, context.getRight() - w * .069f,
                        centerY + geometry[2] * .5f);
            } else {
                outer = new RectF(context.getLeft() + w * .050f,
                        context.getTop() + h * .205f, context.getRight() - w * .040f,
                        context.getTop() + h * .850f);
                ribbon = new RectF(context.getLeft() + w * .078f,
                        context.getTop() + h * .265f, context.getRight() - w * .069f,
                        context.getTop() + h * .800f);
            }
            int save = canvas.save();
            canvas.rotate(-2f, outer.centerX(), outer.centerY());
            if (glass == null) {
                panel(canvas, outer, outer.height() * .085f, outer.height() * .085f, colors.panel);
            } else {
                glass.roundRect(canvas, outer, outer.height() * .085f, outer.height() * .085f,
                        colors.panel, 0f, 0f, -2f);
            }
            canvas.restoreToCount(save);
            panel(canvas, ribbon, ribbon.height() * .5f, ribbon.height() * .5f, colors.accent);
            float timeX = ribbon.left + ribbon.width() * .055f;
            float timeSize = fitText(time, ribbon.width() * (secondsVisible(state) ? .58f : .86f),
                    Math.min(ribbon.height() * .56f, w * .30f) * state.getTimeScale(), face);
            float secondsSize = ribbonSecondsTextSize(timeSize);
            float secondsRadius = Math.max(secondsSize * .76f, Math.min(w, h) * .042f);
            float secondsX = ribbon.right - ribbon.width() * .055f - secondsRadius;
            if (secondsVisible(state)) {
                float timeMaxWidth = Math.max(1f, secondsX - secondsRadius
                        - ribbon.width() * .035f - timeX);
                timeSize = fitText(time, timeMaxWidth, timeSize, face);
                secondsSize = ribbonSecondsTextSize(timeSize);
                secondsRadius = Math.max(secondsSize * .76f, Math.min(w, h) * .042f);
                secondsX = ribbon.right - ribbon.width() * .055f - secondsRadius;
            }
            drawTime(canvas, time, timeX,
                    centeredBaseline(ribbon.centerY(), timeSize, face), timeSize,
                    colors.onAccent, Paint.Align.LEFT, face);
            if (secondsVisible(state)) {
                float sy = ribbon.centerY();
                drawScallopedCircle(canvas, secondsX, sy, secondsRadius, colors.badge);
                text(canvas, String.format(Locale.US, "%02d", c.get(Calendar.SECOND)), secondsX,
                        centeredBaseline(sy, secondsSize, supporting), secondsSize,
                        colors.onBadge, Paint.Align.CENTER, supporting);
            }
            drawDate(canvas, context, state, context.getLeft() + w * .038f,
                    context.getTop() + h * .079f, w * .55f, Paint.Align.LEFT, colors.onBackground,
                    supporting, Math.min(w, h) * .032f);
            drawContext(canvas, context, state, context.getRight() - w * .029f,
                    context.getTop() + h * .079f, w * .35f, Paint.Align.RIGHT, colors.mutedBackground,
                    supporting, Math.min(w, h) * .027f);
        }

        private void drawDate(Canvas canvas, ClockRenderContext context, ClockState state,
                float x, float baseline,
                float maxWidth, Paint.Align align, int color, Typeface face, float size) {
            boolean previous = RendererBase.PAINT_POOL.get().photoText;
            RendererBase.PAINT_POOL.get().photoText = glass != null && (mode() == 1 || mode() == 2 || mode() == 4);
            try {
                readableDate(canvas, context, state, x, baseline, maxWidth, size, color, align,
                        face, context.getHeight() > context.getWidth());
            } finally {
                RendererBase.PAINT_POOL.get().photoText = previous;
            }
        }

        /** Draws the Gregorian date above its lunar counterpart, both at one readable size. */
        private void drawDateAndLunar(Canvas canvas, ClockRenderContext context,
                ClockState state, float x, float lowerBaseline, float maxWidth, Paint.Align align,
                int color, Typeface face, float size) {
            readableDate(canvas, context, state, x, lowerBaseline, maxWidth, size, color,
                    align, face, true);
        }

        private void drawContext(Canvas canvas, ClockRenderContext context, ClockState state,
                float x, float baseline, float maxWidth, Paint.Align align, int color,
                Typeface face, float size) {
            String value = contextText(state);
            if (value.length() == 0) return;
            boolean previous = RendererBase.PAINT_POOL.get().photoText;
            RendererBase.PAINT_POOL.get().photoText = glass != null && (mode() == 1 || mode() == 2 || mode() == 4);
            try {
                readableText(canvas, context, value, x, baseline, maxWidth,
                        size * state.getSupportingScale(), 12f, color, align, face);
            } finally {
                RendererBase.PAINT_POOL.get().photoText = previous;
            }
        }

        private static boolean secondsVisible(ClockState state) {
            return state.isShowSeconds()
                    && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF;
        }

        private static boolean isChinese(ClockState state) {
            String language = state.getLocale().getLanguage();
            return Locale.CHINESE.getLanguage().equals(language);
        }

        private void drawScallopedCircle(Canvas canvas, float cx, float cy,
                float radius, int color) {
            android.graphics.Path path = new android.graphics.Path();
            final int points = 96;
            for (int index = 0; index <= points; index++) {
                double angle = Math.PI * 2d * index / points - Math.PI / 2d;
                float wave = 1f + .055f * (float) Math.cos(angle * 12d);
                float x = cx + (float) Math.cos(angle) * radius * wave;
                float y = cy + (float) Math.sin(angle) * radius * wave;
                if (index == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            if (glass == null) canvas.drawPath(path, fill(color));
            else glass.path(canvas, path, color);
        }

        private void drawWorldStrip(Canvas canvas, ClockRenderContext context,
                ClockThemeTokens theme, ClockState state, Typeface supporting) {
            List<WorldClockEntry> entries = state.getWorldClocks();
            if (entries.isEmpty()) return;
            float w = context.getWidth();
            float h = context.getHeight();
            RectF strip = worldClockStripBounds(context.getLeft(), context.getTop(),
                    context.getRight(), context.getBottom(), context.getDensity(),
                    context.getBottomInset());
            float top = strip.top;
            float bottom = strip.bottom;
            String[] styleIds = {STYLE_DUAL_BLOCKS, STYLE_ORBIT, STYLE_BUBBLES,
                    STYLE_BLEND, STYLE_RIBBON};
            float faceHeight = top - context.getTop()
                    - Math.min(context.getDensity() * 8f, h * .025f);
            float inset = worldClockContentInset(styleIds[mode()], w, faceHeight,
                    context.getDensity());
            float total = worldClockContentWidth(entries.size(), w, h, context.getDensity())
                    + inset * 2f;
            float scroll = Math.min(context.getWorldClockScroll(),
                    Math.max(0f, total - strip.width()));
            int save = canvas.save();
            canvas.clipRect(strip.left, top, strip.right, bottom);
            canvas.translate(strip.left + inset - scroll, top);
            drawWorldCards(canvas, context, theme, state, supporting, bottom - top,
                    strip.left + inset - scroll, top);
            canvas.restoreToCount(save);
        }

        private static void drawWorldCards(Canvas canvas, ClockRenderContext context,
                ClockThemeTokens theme, ClockState state, Typeface supporting, float height,
                float originX, float originY) {
            GaussianGlass glass = GaussianGlass.create(context, theme);
            ClockPalette colors = glass == null ? ClockPalette.fromTokens(theme) : glass.palette();
            float unit = Math.min(context.getWidth(), context.getHeight());
            float cardWidth = worldClockCardWidth(context.getWidth(), context.getHeight(),
                    context.getDensity());
            float gap = worldClockCardGap(context.getWidth(), context.getHeight(),
                    context.getDensity());
            int save = canvas.save();
            Typeface timeFace = displayTypeface(theme, Typeface.BOLD);
            int cardIndex = 0;
            for (WorldClockEntry entry : state.getWorldClocks()) {
                float cardOriginX = originX + cardIndex++ * (cardWidth + gap);
                RectF card = new RectF(0f, 0f, cardWidth, height);
                if (canvas.quickReject(card)) {
                    canvas.translate(cardWidth + gap, 0f);
                    continue;
                }
                float radius = Math.min(context.getDensity() * 20f, card.height() * .18f);
                if (glass != null) colors = glass.paletteAt(cardOriginX, originY, cardWidth, height);
                if (glass == null) canvas.drawRoundRect(card, radius, radius, fill(colors.panel));
                else glass.roundRect(canvas, card, radius, radius, colors.panel,
                        cardOriginX, originY, 0f);
                float padding = cardWidth * .09f;
                float contentWidth = cardWidth - padding * 2f;
                float supportScale = state.getSupportingScale();
                float citySize = readableSize(context,
                        unit * .030f * supportScale, 12f);
                citySize = Math.min(citySize, card.height() * .18f);
                float headerY = card.top + card.height() * .23f;
                float flagWidth = citySize * 1.7f;
                text(canvas, entry.getFlagEmoji(), padding,
                        centeredBaseline(headerY, citySize, supporting), citySize,
                        colors.onPanel, Paint.Align.LEFT, supporting);
                String city = ellipsize(entry.getCity(), contentWidth - flagWidth,
                        citySize, supporting);
                text(canvas, city, padding + flagWidth,
                        centeredBaseline(headerY, citySize, supporting), citySize,
                        colors.onPanel, Paint.Align.LEFT, supporting);
                Calendar local = Calendar.getInstance(TimeZone.getTimeZone(entry.getZoneId()),
                        state.getLocale());
                local.setTimeInMillis(state.getTimeMillis());
                String localTime = timeText(local, state, false);
                Paint timePaint = fill(colors.onPanel);
                timePaint.setTypeface(timeFace);
                float timeSize = fitText(localTime, contentWidth,
                        Math.min(card.height() * .30f, readableSize(context,
                                unit * .062f * supportScale, 22f)), timeFace);
                timePaint.setTextSize(timeSize);
                timePaint.setTextAlign(Paint.Align.LEFT);
                ClockTimeText.draw(canvas, localTime, padding,
                        centeredBaseline(card.top + card.height() * .54f, timeSize, timeFace),
                        timePaint);
                float zoneSize = Math.min(card.height() * .14f, readableSize(context,
                        unit * .020f * supportScale, 10f));
                String zone = ellipsize(entry.getZoneId(), contentWidth, zoneSize, supporting);
                text(canvas, zone, padding,
                        centeredBaseline(card.top + card.height() * .81f, zoneSize, supporting),
                        zoneSize, colors.mutedPanel, Paint.Align.LEFT, supporting);
                canvas.translate(cardWidth + gap, 0f);
            }
            canvas.restoreToCount(save);
        }
    }

    private static final class DualBlocksRenderer extends MigratedRenderer {
        @Override protected int mode() { return 0; }
    }

    private static final class OrbitRenderer extends MigratedRenderer {
        @Override protected int mode() { return 1; }
    }

    private static final class BubblesRenderer extends MigratedRenderer {
        @Override protected int mode() { return 2; }
    }

    private static final class BlendRenderer extends MigratedRenderer {
        @Override protected int mode() { return 3; }
    }

    private static final class RibbonRenderer extends MigratedRenderer {
        @Override protected int mode() { return 4; }
    }

    /** Small organic silhouette used by the blend composition. */
    private static final class PathShape {
        private static void drawOrganic(Canvas canvas, RectF rect, Paint paint) {
            android.graphics.Path path = new android.graphics.Path();
            float r = rect.height() * .24f;
            path.moveTo(rect.left + r, rect.top);
            path.cubicTo(rect.left, rect.top, rect.left, rect.bottom, rect.left + r, rect.bottom);
            path.lineTo(rect.right - r, rect.bottom);
            path.cubicTo(rect.right, rect.bottom, rect.right, rect.top, rect.right - r, rect.top);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
