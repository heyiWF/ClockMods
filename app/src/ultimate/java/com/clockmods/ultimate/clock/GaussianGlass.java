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
import com.clockmods.sdk.clock.ClockThemeTokens;

import java.util.WeakHashMap;
import java.util.HashMap;

/** A background-anchored blur sampled through moving card masks, including software previews. */
final class GaussianGlass {
    private static final int MAX_EDGE = 192;
    // Keys do not keep the host's full-size background alive after replacement or view disposal.
    private static final WeakHashMap<Bitmap, GaussianGlass> CACHE = new WeakHashMap<>();
    private final Bitmap bitmap;
    private final BitmapShader shader;
    private final Matrix imageToBackground = new Matrix();
    private final Matrix sampling = new Matrix();
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF edgeBounds = new RectF();
    private final float left, top, right, bottom;
    private final int strength, brightness;
    private final float imageLeft, imageTop, scaleX, scaleY;
    private final HashMap<Long, Paint> paints = new HashMap<>();
    private final HashMap<Integer, ClockPalette> palettes = new HashMap<>();
    private final ClockPalette defaultPalette;
    private ClockPalette activePalette;

    static synchronized GaussianGlass create(ClockRenderContext context, ClockThemeTokens theme) {
        if (!theme.isGaussianBlur() || context.getBackground() == null
                || !context.getBackground().hasImage()) return null;
        Bitmap source = context.getBackground().getBitmap();
        GaussianGlass cached = CACHE.get(source);
        if (cached != null && cached.left == context.getLeft() && cached.top == context.getTop()
                && cached.right == context.getRight() && cached.bottom == context.getBottom()
                && cached.strength == theme.getBlurStrength()
                && cached.brightness == theme.getBlurBrightness()) return cached;
        Bitmap blur = cached != null && cached.strength == theme.getBlurStrength()
                ? cached.bitmap : blurred(source, theme.getBlurStrength());
        GaussianGlass result = new GaussianGlass(context, blur,
                source.getWidth(), source.getHeight(), theme.getBlurStrength(), theme.getBlurBrightness());
        CACHE.put(source, result);
        return result;
    }

    private GaussianGlass(ClockRenderContext context, Bitmap bitmap, int sourceWidth, int sourceHeight,
            int strength, int brightness) {
        this.bitmap = bitmap;
        this.strength = strength;
        this.brightness = brightness;
        left = context.getLeft();
        top = context.getTop();
        right = context.getRight();
        bottom = context.getBottom();
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = Math.max(context.getWidth() / sourceWidth, context.getHeight() / sourceHeight);
        scaleX = sourceWidth * scale / bitmap.getWidth();
        scaleY = sourceHeight * scale / bitmap.getHeight();
        imageLeft = context.getCenterX() - sourceWidth * scale * .5f;
        imageTop = context.getCenterY() - sourceHeight * scale * .5f;
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
        int foreground = activePalette.onPanel;
        long key = ((long) foreground << 32) | (surface & 0xFFFFFFFFL);
        Paint cached = paints.get(key);
        if (cached != null) return cached;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(shader);
        // Keep the complete image range readable, even when a card crosses a black/white edge.
        int[] range = brightnessRange(brightness, foreground);
        float span = range[1] - range[0];
        int maximum = Math.max((surface >> 16) & 255, Math.max((surface >> 8) & 255, surface & 255));
        float r = span / 255f * ((surface >> 16) & 255) / Math.max(1f, maximum);
        float g = span / 255f * ((surface >> 8) & 255) / Math.max(1f, maximum);
        float b = span / 255f * (surface & 255) / Math.max(1f, maximum);
        float offset = range[0];
        paint.setColorFilter(new ColorMatrixColorFilter(new float[] {
                r, 0, 0, 0, offset, 0, g, 0, 0, offset, 0, 0, b, 0, offset, 0, 0, 0, 1, 0}));
        if (paints.size() >= 128) paints.clear();
        paints.put(key, paint);
        return paint;
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
        float scale = Math.min(1f, MAX_EDGE / (float) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(source, width, height, true);
        int[] pixels = new int[width * height];
        small.getPixels(pixels, 0, width, 0, 0, width, height);
        if (small != source) small.recycle();
        return Bitmap.createBitmap(blurPixels(pixels, width, height, strength),
                width, height, Bitmap.Config.ARGB_8888);
    }

    /** Separable Gaussian convolution with clamped edges; transparent pixels composite on black. */
    static int[] blurPixels(int[] pixels, int width, int height) {
        return blurPixels(pixels, width, height, ClockThemeTokens.DEFAULT_BLUR_STRENGTH);
    }

    static int[] blurPixels(int[] pixels, int width, int height, int strength) {
        float sigma = Math.max(0, Math.min(100, strength)) * .06f;
        int radius = (int) Math.ceil(sigma * 3f);
        float[] kernel = kernel(radius, sigma);
        float[] horizontal = new float[pixels.length * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * 3;
                for (int k = -radius; k <= radius; k++) {
                    int color = pixels[y * width + Math.max(0, Math.min(width - 1, x + k))];
                    float weight = kernel[k + radius] * (color >>> 24) / 255f;
                    for (int c = 0; c < 3; c++) {
                        horizontal[offset + c] += ((color >>> (c * 8)) & 255) * weight;
                    }
                }
            }
        }
        int[] result = new int[pixels.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = 0xFF000000;
                for (int c = 0; c < 3; c++) {
                    float value = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int offset = (Math.max(0, Math.min(height - 1, y + k)) * width + x) * 3;
                        value += horizontal[offset + c] * kernel[k + radius];
                    }
                    color |= Math.min(255, Math.round(value)) << (c * 8);
                }
                result[y * width + x] = color;
            }
        }
        return result;
    }

    private static float[] kernel(int radius, float sigma) {
        if (radius == 0) return new float[] {1f};
        float[] weights = new float[radius * 2 + 1];
        float sum = 0;
        for (int i = -radius; i <= radius; i++) {
            weights[i + radius] = (float) Math.exp(-i * i / (2d * sigma * sigma));
            sum += weights[i + radius];
        }
        for (int i = 0; i < weights.length; i++) weights[i] /= sum;
        return weights;
    }

    static int[] brightnessRange(int brightness, int foreground) {
        boolean lightText = brightness <= 50;
        int low = 0, high = 255;
        while (low < high) {
            int mid = lightText ? (low + high + 1) / 2 : (low + high) / 2;
            int gray = 0xFF000000 | mid << 16 | mid << 8 | mid;
            boolean readable = ClockPalette.contrast(foreground, gray) >= 4.5;
            if (lightText ? readable : !readable) low = lightText ? mid : mid + 1;
            else high = lightText ? mid - 1 : mid;
        }
        int center = ClockPalette.glassCenter(brightness);
        int radius = lightText ? Math.min(center, low - center) : Math.min(center - low, 255 - center);
        radius = Math.max(0, radius);
        return new int[] {center - radius, center + radius};
    }
}
