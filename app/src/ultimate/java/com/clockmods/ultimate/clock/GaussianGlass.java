package com.clockmods.ultimate.clock;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockOverlayBounds;
import com.clockmods.sdk.clock.ClockThemeTokens;

import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** A background-anchored blur sampled through moving card masks, including software previews. */
public final class GaussianGlass {
    private static final int BASE_BLUR_EDGE = 192;
    // A weak blur preserves fine detail, so its working bitmap must also preserve enough detail.
    // Keeping every strength at BASE_BLUR_EDGE makes individual texels visible when that bitmap is
    // enlarged to a phone screen. The cap bounds memory while reducing a 2400 px texture's texels
    // from 12.5 screen pixels to at most 2.35 screen pixels.
    private static final int MAX_DETAIL_EDGE = 1024;
    // Above this sigma there is little quality benefit in adding taps instead of downsampling.
    private static final float TARGET_WORKING_SIGMA = 4f;
    private static final float SIGMA_PER_STRENGTH = .06f;
    // Keep texture visible at both slider ends instead of fading the blur to solid black/white.
    private static final float MAX_BRIGHTNESS_OVERLAY_ALPHA = .55f;
    // Keys do not keep the host's full-size background alive after replacement or view disposal.
    private static final WeakHashMap<Bitmap, GaussianGlass> CACHE = new WeakHashMap<>();
    private static final WeakHashMap<Bitmap, LinkedHashMap<Integer, Bitmap>> PREPARED =
            new WeakHashMap<>();
    private final Bitmap bitmap;
    private final BitmapShader shader;
    private final Matrix imageToBackground = new Matrix();
    private final Matrix sampling = new Matrix();
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF edgeBounds = new RectF();
    private final float left, top, right, bottom;
    private final int strength, brightness;
    private final float imageLeft, imageTop, scaleX, scaleY;
    private final Paint materialPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final HashMap<Integer, ClockPalette> palettes = new HashMap<>();
    private final ClockPalette defaultPalette;
    private ClockPalette activePalette;

    static synchronized GaussianGlass create(ClockRenderContext context, ClockThemeTokens theme) {
        if (!theme.isGaussianBlur() || context.getBackground() == null
                || !context.getBackground().hasImage()) return null;
        Bitmap source = context.getBackground().getBitmap();
        GaussianGlass cached = CACHE.get(source);
        ClockOverlayBounds canvasBounds = context.getCanvasBounds();
        if (cached != null && cached.left == canvasBounds.getLeft()
                && cached.top == canvasBounds.getTop()
                && cached.right == canvasBounds.getRight()
                && cached.bottom == canvasBounds.getBottom()
                && cached.strength == theme.getBlurStrength()
                && cached.brightness == theme.getBlurBrightness()) return cached;
        Bitmap blur = cached != null && cached.strength == theme.getBlurStrength()
                ? cached.bitmap : preparedBitmap(source, theme.getBlurStrength());
        if (blur == null) return null;
        GaussianGlass result = new GaussianGlass(context, blur,
                source.getWidth(), source.getHeight(), theme.getBlurStrength(), theme.getBlurBrightness());
        CACHE.put(source, result);
        return result;
    }

    /** The draw path only reads completed variants; image processing runs on a worker. */
    public static synchronized boolean isPrepared(Bitmap source, int strength) {
        int value = Math.max(0, Math.min(100, strength));
        GaussianGlass cached = CACHE.get(source);
        return cached != null && cached.strength == value
                || preparedBitmap(source, value) != null;
    }

    public static void prepare(Bitmap source, int strength) {
        if (source == null || source.isRecycled()) return;
        int value = Math.max(0, Math.min(100, strength));
        synchronized (source) {
            if (isPrepared(source, value)) return;
            Bitmap result = blurred(source, value);
            synchronized (PREPARED) {
                LinkedHashMap<Integer, Bitmap> variants = PREPARED.get(source);
                if (variants == null) {
                    variants = new LinkedHashMap<>(8, .75f, true);
                    PREPARED.put(source, variants);
                }
                variants.put(value, result);
                if (variants.size() > 6) variants.remove(variants.keySet().iterator().next());
            }
        }
    }

    private static Bitmap preparedBitmap(Bitmap source, int strength) {
        synchronized (PREPARED) {
            LinkedHashMap<Integer, Bitmap> variants = PREPARED.get(source);
            return variants == null ? null : variants.get(strength);
        }
    }

    private GaussianGlass(ClockRenderContext context, Bitmap bitmap, int sourceWidth, int sourceHeight,
            int strength, int brightness) {
        this.bitmap = bitmap;
        this.strength = strength;
        this.brightness = brightness;
        ClockOverlayBounds canvasBounds = context.getCanvasBounds();
        left = canvasBounds.getLeft();
        top = canvasBounds.getTop();
        right = canvasBounds.getRight();
        bottom = canvasBounds.getBottom();
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        materialPaint.setShader(shader);
        float[] brightnessTransform = brightnessTransform(brightness);
        if (brightness != 50) {
            float scale = brightnessTransform[0];
            float offset = brightnessTransform[1];
            materialPaint.setColorFilter(new ColorMatrixColorFilter(new float[] {
                    scale, 0, 0, 0, offset,
                    0, scale, 0, 0, offset,
                    0, 0, scale, 0, offset,
                    0, 0, 0, 1, 0}));
        }
        float scale = Math.max(canvasBounds.getWidth() / sourceWidth,
                canvasBounds.getHeight() / sourceHeight);
        scaleX = sourceWidth * scale / bitmap.getWidth();
        scaleY = sourceHeight * scale / bitmap.getHeight();
        imageLeft = (left + right) * .5f - sourceWidth * scale * .5f;
        imageTop = (top + bottom) * .5f - sourceHeight * scale * .5f;
        imageToBackground.setScale(scaleX, scaleY);
        imageToBackground.postTranslate(imageLeft, imageTop);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(1f, context.getDensity()));
        edge.setColor(0x38FFFFFF);
        defaultPalette = paletteAt(left, top, right - left, bottom - top);
    }

    ClockPalette palette() {
        activePalette = defaultPalette;
        return activePalette;
    }

    /** Each moving city card derives its foreground hue from the image directly underneath it. */
    ClockPalette paletteAt(float x, float y, float width, float height) {
        int red = 0, green = 0, blue = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int ix = Math.max(0, Math.min(bitmap.getWidth() - 1,
                        (int) ((x + width * (col + .5f) / 3f - imageLeft) / scaleX)));
                int iy = Math.max(0, Math.min(bitmap.getHeight() - 1,
                        (int) ((y + height * (row + .5f) / 3f - imageTop) / scaleY)));
                int color = bitmap.getPixel(ix, iy);
                red += (color >> 16) & 255;
                green += (color >> 8) & 255;
                blue += color & 255;
            }
        }
        // Small buckets prevent foreground shimmer and keep the scrolling paint cache bounded.
        int tint = 0xFF000000 | (red / 9 & 248) << 16 | (green / 9 & 248) << 8 | (blue / 9 & 248);
        activePalette = palettes.get(tint);
        if (activePalette == null) {
            if (palettes.size() >= 64) palettes.clear();
            activePalette = ClockPalette.glass(brightness, tint);
            palettes.put(tint, activePalette);
        }
        return activePalette;
    }

    Paint paint(int surface) {
        return paint(surface, 0f, 0f);
    }

    /** Origin of the card's local canvas in the background view, updated on every scroll frame. */
    Paint paint(int surface, float originX, float originY) {
        sampling.set(imageToBackground);
        sampling.postTranslate(-originX, -originY);
        shader.setLocalMatrix(sampling);
        return material(surface);
    }

    private Paint material(int surface) {
        return materialPaint;
    }

    void roundRect(Canvas canvas, RectF bounds, float rx, float ry, int surface,
            float originX, float originY, float rotation) {
        Paint fill = paint(surface, originX, originY);
        if (rotation != 0f) {
            sampling.postRotate(-rotation, bounds.centerX(), bounds.centerY());
            shader.setLocalMatrix(sampling);
        }
        canvas.drawRoundRect(bounds, rx, ry, fill);
        float inset = edge.getStrokeWidth() * .5f;
        edgeBounds.set(bounds);
        edgeBounds.inset(inset, inset);
        canvas.drawRoundRect(edgeBounds, Math.max(0f, rx - inset), Math.max(0f, ry - inset), edge);
    }

    void circle(Canvas canvas, float cx, float cy, float radius, int surface) {
        canvas.drawCircle(cx, cy, radius, paint(surface));
        canvas.drawCircle(cx, cy, Math.max(0f, radius - edge.getStrokeWidth() * .5f), edge);
    }

    void path(Canvas canvas, Path path, int surface) {
        canvas.drawPath(path, paint(surface));
        int save = canvas.save();
        canvas.clipPath(path);
        canvas.drawPath(path, edge);
        canvas.restoreToCount(save);
    }

    private static Bitmap blurred(Bitmap source, int strength) {
        if (strength == 0) {
            // Preserve full detail at zero, without retaining the weak cache's source key.
            Bitmap copy = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(copy);
            canvas.drawColor(0xFF000000);
            canvas.drawBitmap(source, 0f, 0f, null);
            return copy;
        }
        int sourceLongEdge = Math.max(source.getWidth(), source.getHeight());
        int targetLongEdge = workingLongEdge(sourceLongEdge, strength);
        float scale = Math.min(1f, targetLongEdge / (float) sourceLongEdge);
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(source, width, height, true);
        int[] pixels = new int[width * height];
        small.getPixels(pixels, 0, width, 0, 0, width, height);
        if (small != source) small.recycle();
        float sigma = blurSigma(strength, Math.max(width, height), sourceLongEdge);
        return Bitmap.createBitmap(blurPixels(pixels, width, height, sigma),
                width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Selects an adaptive working resolution. Strong blur can safely use the compact legacy
     * texture, while weak blur gets progressively more source samples instead of magnifying the
     * same 192 texels across the display.
     */
    static int workingLongEdge(int sourceLongEdge, int strength) {
        int sourceEdge = Math.max(1, sourceLongEdge);
        int value = Math.max(0, Math.min(100, strength));
        if (value == 0) return sourceEdge;
        int baseEdge = Math.min(sourceEdge, BASE_BLUR_EDGE);
        int qualityEdge = Math.round(baseEdge * TARGET_WORKING_SIGMA
                / (value * SIGMA_PER_STRENGTH));
        return Math.min(sourceEdge, Math.min(MAX_DETAIL_EDGE, Math.max(baseEdge, qualityEdge)));
    }

    /** Keeps the Gaussian radius in source-image coordinates stable as resolution changes. */
    static float blurSigma(int strength, int workingLongEdge, int sourceLongEdge) {
        int value = Math.max(0, Math.min(100, strength));
        int sourceEdge = Math.max(1, sourceLongEdge);
        int baseEdge = Math.min(sourceEdge, BASE_BLUR_EDGE);
        int workingEdge = Math.max(1, Math.min(sourceEdge, workingLongEdge));
        return value * SIGMA_PER_STRENGTH * workingEdge / baseEdge;
    }

    /** Blur with clamped edges; transparent pixels composite on black. */
    static int[] blurPixels(int[] pixels, int width, int height) {
        return blurPixels(pixels, width, height, ClockThemeTokens.DEFAULT_BLUR_STRENGTH);
    }

    static int[] blurPixels(int[] pixels, int width, int height, int strength) {
        float sigma = Math.max(0, Math.min(100, strength)) * SIGMA_PER_STRENGTH;
        return blurPixels(pixels, width, height, sigma);
    }

    private static int[] blurPixels(int[] pixels, int width, int height, float sigma) {
        return fastBlurPixels(pixels, width, height, sigma);
    }

    /** Three running-sum box passes approximate a Gaussian in time linear in image size. */
    static int[] fastBlurPixels(int[] pixels, int width, int height, float sigma) {
        int[] current = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int alpha = color >>> 24;
            int red = ((color >>> 16 & 255) * alpha + 127) / 255;
            int green = ((color >>> 8 & 255) * alpha + 127) / 255;
            int blue = ((color & 255) * alpha + 127) / 255;
            current[i] = 0xFF000000 | red << 16 | green << 8 | blue;
        }
        float idealWidth = (float) Math.sqrt(1f + 4f * sigma * sigma);
        int lowerWidth = Math.max(1, ((int) Math.floor(idealWidth)) | 1);
        int upperWidth = lowerWidth + 2;
        int lowerPasses = Math.max(0, Math.min(3, Math.round(
                (12f * sigma * sigma - 3f * lowerWidth * lowerWidth
                        - 12f * lowerWidth - 9f) / (-4f * lowerWidth - 4f))));
        int[] horizontal = new int[pixels.length];
        int[] next = new int[pixels.length];
        for (int pass = 0; pass < 3; pass++) {
            int radius = (pass < lowerPasses ? lowerWidth : upperWidth) / 2;
            if (radius == 0) continue;
            boxBlur(current, horizontal, width, height, radius, true);
            boxBlur(horizontal, next, width, height, radius, false);
            int[] old = current;
            current = next;
            next = old;
        }
        return current;
    }

    private static void boxBlur(int[] input, int[] output, int width, int height,
            int radius, boolean horizontal) {
        int lines = horizontal ? height : width;
        int length = horizontal ? width : height;
        int stride = horizontal ? 1 : width;
        int divisor = radius * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int start = horizontal ? line * width : line;
            int first = input[start];
            int red = (first >>> 16 & 255) * (radius + 1);
            int green = (first >>> 8 & 255) * (radius + 1);
            int blue = (first & 255) * (radius + 1);
            for (int i = 1; i <= radius; i++) {
                int color = input[start + Math.min(i, length - 1) * stride];
                red += color >>> 16 & 255;
                green += color >>> 8 & 255;
                blue += color & 255;
            }
            for (int i = 0; i < length; i++) {
                output[start + i * stride] = 0xFF000000
                        | red / divisor << 16 | green / divisor << 8 | blue / divisor;
                int leaving = input[start + Math.max(0, i - radius) * stride];
                int entering = input[start + Math.min(length - 1, i + radius + 1) * stride];
                red += (entering >>> 16 & 255) - (leaving >>> 16 & 255);
                green += (entering >>> 8 & 255) - (leaving >>> 8 & 255);
                blue += (entering & 255) - (leaving & 255);
            }
        }
    }

    /**
     * Models a translucent black/white layer over the already blurred image. A value of 50 is
     * deliberately the identity transform; the bounded endpoint alpha preserves image texture.
     */
    static float[] brightnessTransform(int brightness) {
        int value = Math.max(0, Math.min(100, brightness));
        float alpha = Math.abs(value - 50) / 50f * MAX_BRIGHTNESS_OVERLAY_ALPHA;
        return new float[] {1f - alpha, value > 50 ? 255f * alpha : 0f};
    }

    /** Applies the same transform used by the rendering paint to a sampled color. */
    static int applyBrightnessOverlay(int color, int brightness) {
        float[] transform = brightnessTransform(brightness);
        int result = color & 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = (color >>> shift) & 255;
            int adjusted = Math.max(0, Math.min(255,
                    Math.round(channel * transform[0] + transform[1])));
            result |= adjusted << shift;
        }
        return result;
    }
}
