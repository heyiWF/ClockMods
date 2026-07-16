package com.clockmods.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WeatherIcon {
    private static final Pattern PATH_PATTERN = Pattern.compile("<path[^>]*d=\"([^\"]+)\"");
    private static final Map<String, WeatherIcon> CACHE = new HashMap<>();
    private final Path path;
    private final Matrix matrix = new Matrix();
    private final Path scaled = new Path();
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap;
    private int bitmapSize;
    private int bitmapColor;
    private int bitmapAlpha;

    private WeatherIcon(Path path) { this.path = path; }

    static WeatherIcon load(Context context, String code) {
        if (code == null || !code.matches("[0-9]+")) return null;
        synchronized (CACHE) {
            if (CACHE.containsKey(code)) return CACHE.get(code);
            WeatherIcon icon = read(context, code);
            CACHE.put(code, icon);
            return icon;
        }
    }

    private static WeatherIcon read(Context context, String code) {
        try (InputStream input = context.getAssets().open("qweather-icons/" + code + "-fill.svg");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            StringBuilder svg = new StringBuilder();
            String line; while ((line = reader.readLine()) != null) svg.append(line);
            Matcher matcher = PATH_PATTERN.matcher(svg.toString());
            Path combined = new Path();
            boolean found = false;
            while (matcher.find()) {
                combined.addPath(SvgPath.parse(matcher.group(1)));
                found = true;
            }
            return found ? new WeatherIcon(combined) : null;
        } catch (Exception ignored) { return null; }
    }

    void draw(Canvas canvas, float left, float top, float size, Paint paint) {
        int targetSize = Math.max(1, (int) Math.ceil(size));
        int color = paint.getColor();
        int alpha = paint.getAlpha();
        if (bitmap == null || bitmapSize != targetSize || bitmapColor != color || bitmapAlpha != alpha) {
            bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(bitmap);
            matrix.reset();
            matrix.setScale(targetSize / 16f, targetSize / 16f);
            scaled.reset();
            path.transform(matrix, scaled);
            iconPaint.setColor(color);
            iconPaint.setAlpha(alpha);
            iconPaint.setStyle(Paint.Style.FILL);
            bitmapCanvas.drawPath(scaled, iconPaint);
            bitmapSize = targetSize;
            bitmapColor = color;
            bitmapAlpha = alpha;
        }
        canvas.drawBitmap(bitmap, left, top, null);
    }
}