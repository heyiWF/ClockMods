package com.clockmods.background;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** A render-only ID carries both per-theme choices without altering persisted family IDs. */
public final class MixedFontResolver {
    // A common style lets each physical face retain its own weight. Using 700 also permits
    // Android's per-family fake bold when a system script only ships a regular physical cut.
    private static final int STYLE_WEIGHT = 700;
    private static final Map<String, Typeface> CACHE = new LinkedHashMap<>();
    private MixedFontResolver() { }
    public static String id(String western, String script, int scriptWeight, String language) { return "mixed|" + western + "|" + script + "|" + scriptWeight + "|" + language; }
    public static boolean isMixed(String id) { return id != null && id.startsWith("mixed|"); }
    public static String western(String id) { return isMixed(id) ? id.split("\\|", -1)[1] : id; }
    public static synchronized Typeface resolve(Context context, String id, int weight) {
        String key = android.os.Build.FINGERPRINT + "|" + id + "|" + weight + "|" + UserFontStore.revision();
        Typeface cached = CACHE.get(key); if (cached != null) return cached;
        String[] parts = id.split("\\|", -1);
        if (parts.length != 5) return Typeface.DEFAULT;
        try {
            FontFamily western = family(context, parts[1], weight, false, parts[4]);
            FontFamily script = family(context, parts[2], Integer.parseInt(parts[3]), true, parts[4]);
            Typeface.CustomFallbackBuilder builder = new Typeface.CustomFallbackBuilder(western);
            builder.addCustomFallback(script);
            Typeface result = builder.setStyle(new FontStyle(STYLE_WEIGHT, FontStyle.FONT_SLANT_UPRIGHT))
                    .setSystemFallback("sans-serif").build();
            if (CACHE.size() >= 128) CACHE.clear(); CACHE.put(key, result); return result;
        } catch (Exception e) { return Typeface.DEFAULT; }
    }
    private static FontFamily family(Context context, String id, int weight, boolean script, String language) throws Exception {
        FontCatalog.FontOption option = FontCatalog.optionForLanguage(id, script ? language : "en");
        int actualWeight = option.nearestWeight(weight), index = 0;
        boolean variable = option.isVariable(); String axes = option.variableAxes;
        UserFontStore.Family imported = UserFontStore.family(id);
        String sourceId;
        InputStream input;
        android.graphics.fonts.FontVariationAxis[] systemAxes = null;
        boolean systemFakeBold = false;
        if (option.isSystem()) {
            // Ask Android which physical font renders this script on this device. This respects
            // OEM fonts, locale-specific CJK faces and variable weight axes without fixed paths.
            android.graphics.Paint probe = new android.graphics.Paint();
            probe.setTypeface(Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), actualWeight, false));
            probe.setTextLocale(java.util.Locale.forLanguageTag(language));
            String sample = !script ? "A" : "ja".equals(language) ? "あ" : "ko".equals(language) ? "한" : "中";
            android.graphics.text.PositionedGlyphs glyphs = android.graphics.text.TextRunShaper.shapeTextRun(
                    sample, 0, sample.length(), 0, sample.length(), 0f, 0f, false, probe);
            Font source = glyphs.getFont(0);
            systemFakeBold = android.os.Build.VERSION.SDK_INT >= 35 ? glyphs.getFakeBold(0)
                    : actualWeight >= 600 && source.getStyle().getWeight() <= actualWeight - 200;
            index = source.getTtcIndex(); systemAxes = source.getAxes();
            File nativeFile = source.getFile();
            if (nativeFile != null && nativeFile.isFile()) {
                sourceId = "system-" + Integer.toHexString(android.os.Build.FINGERPRINT.hashCode()) + "-"
                        + nativeFile.getAbsolutePath().replaceAll("[^a-zA-Z0-9._-]", "_")
                        + "-" + nativeFile.length() + "-" + nativeFile.lastModified() + "-" + index;
                input = new FileInputStream(nativeFile);
            } else {
                java.nio.ByteBuffer buffer = source.getBuffer().duplicate(); buffer.clear();
                if (buffer.remaining() > OpenTypeFont.MAX_BYTES) throw new java.io.IOException("System font too large");
                byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes);
                input = new java.io.ByteArrayInputStream(bytes);
                sourceId = "system-" + Integer.toHexString(android.os.Build.FINGERPRINT.hashCode())
                        + "-" + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "-" + index;
            }
            // Android 15 may represent the selected variable weight as a shaping override,
            // not in Font.getAxes(). Reapply it explicitly to the private subset.
            java.util.List<android.graphics.fonts.FontVariationAxis> selectedAxes = new java.util.ArrayList<>();
            if (systemAxes != null) for (android.graphics.fonts.FontVariationAxis axis : systemAxes)
                if (!axis.getTag().equals("wght")) selectedAxes.add(axis);
            selectedAxes.add(new android.graphics.fonts.FontVariationAxis("wght", actualWeight));
            systemAxes = selectedAxes.toArray(new android.graphics.fonts.FontVariationAxis[0]);
            variable = false;
        } else if (imported != null) {
            UserFontStore.Face face = imported.faceFor(actualWeight, script ? language : "en");
            sourceId = face.source.id + "-" + face.font.index;
            index = face.font.index; variable = face.font.variable;
            input = new FileInputStream(face.source.file);
        } else {
            String asset = variable ? option.variableAsset : option.staticAssetFor(actualWeight);
            sourceId = asset.replaceAll("[^a-zA-Z0-9._-]", "_"); input = context.getAssets().open(asset);
        }
        File folder = new File(context.getCacheDir(), "font-script-subsets-v1");
        if (!folder.isDirectory() && !folder.mkdirs()) { input.close(); throw new java.io.IOException("Cannot create subset cache"); }
        File file = new File(folder, sourceId + (script ? "-east.font" : "-west.font"));
        try (InputStream source = input) {
            if (!file.isFile()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[16384]; int read;
                while ((read = source.read(buffer)) != -1) {
                    if (bytes.size() + (long)read > OpenTypeFont.MAX_BYTES) throw new java.io.IOException("Font too large");
                    bytes.write(buffer, 0, read);
                }
                byte[] subset = FontScriptSubset.build(bytes.toByteArray(), index, script);
                File staging = File.createTempFile("subset-", ".tmp", folder);
                try {
                    try (FileOutputStream output = new FileOutputStream(staging)) { output.write(subset); }
                    if (!staging.renameTo(file)) throw new java.io.IOException("Cannot save subset");
                } finally { if (staging.exists()) staging.delete(); }
            }
        }
        Font.Builder builder = new Font.Builder(file).setWeight(systemFakeBold ? 400 : STYLE_WEIGHT).setSlant(FontStyle.FONT_SLANT_UPRIGHT);
        if (systemAxes != null && systemAxes.length > 0) builder.setFontVariationSettings(systemAxes);
        if (variable) builder.setFontVariationSettings("'wght' " + actualWeight + (axes == null ? "" : ", " + axes));
        return new FontFamily.Builder(builder.build()).build();
    }
}
