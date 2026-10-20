package com.clockmods.background;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Private, content-addressed font files. Publishing and deletion replace one immutable snapshot. */
public final class UserFontStore {
    public static final String PREFIX = "imported:";
    private static final long MAX_TOTAL_BYTES = 128L * 1024 * 1024;
    private static final int MAX_FILES = 64;
    private static File directory;
    private static Context application;
    private static volatile List<ImportedFile> files = Collections.emptyList();
    private static volatile Map<String, Family> families = Collections.emptyMap();
    private static final Map<String, Typeface> TYPEFACES = new LinkedHashMap<>();
    private static long revision;

    public static final class FontException extends IOException {
        public final String reason;
        public FontException(String reason) { super(reason); this.reason = reason; }
    }
    public static final class ImportedFile {
        public final String id, name;
        public final File file;
        public final List<OpenTypeFont> faces;
        ImportedFile(String id, String name, File file, List<OpenTypeFont> faces) {
            this.id = id; this.name = name; this.file = file; this.faces = faces;
        }
    }
    public static final class Face {
        public final ImportedFile source;
        public final OpenTypeFont font;
        Face(ImportedFile source, OpenTypeFont font) { this.source = source; this.font = font; }
    }
    public static final class Family {
        public final String id, name;
        public final int[] weights;
        public final List<Face> faces;
        Family(String id, List<Face> faces) {
            this.id = id; name = faces.get(0).font.family;
            this.faces = Collections.unmodifiableList(faces);
            java.util.TreeSet<Integer> stops = new java.util.TreeSet<>();
            for (Face face : faces) for (int weight : face.font.weights) stops.add(weight);
            weights = new int[stops.size()]; int i = 0;
            for (int w : stops) weights[i++] = w;
        }
        public Face faceFor(int weight) { return faceFor(weight, null); }
        public Face faceFor(int weight, String language) {
            Face chosen = faces.get(0); int distance = Integer.MAX_VALUE;
            for (Face face : faces) for (int stop : face.font.weights) {
                if (language != null && !ScriptFontPreferences.supportsFace(face.font, language)) continue;
                // Prefer an upright cut when both styles cover the requested weight.
                int delta = Math.abs(stop - weight) * 2 + (face.font.italic ? 1 : 0);
                if (delta < distance) { distance = delta; chosen = face; }
            }
            return chosen;
        }
    }
    private UserFontStore() { }

    public static synchronized void initialize(Context context) {
        File root = new File(context.getFilesDir(), "imported-fonts-v1");
        if (root.equals(directory)) return;
        application = context.getApplicationContext(); directory = root;
        List<ImportedFile> loaded = new ArrayList<>();
        File[] candidates = root.listFiles();
        if (candidates != null) {
            Arrays.sort(candidates, Comparator.comparing(File::getName));
            long bytes = 0;
            for (File file : candidates) {
                if (!file.getName().matches("[0-9a-f]{64}\\.font")) continue;
                if (loaded.size() >= MAX_FILES || bytes + file.length() > MAX_TOTAL_BYTES) break;
                try (InputStream input = new FileInputStream(file)) {
                    byte[] content = readLimited(input);
                    String id = file.getName().substring(0, 64);
                    if (!id.equals(hash(content))) continue;
                    List<OpenTypeFont> faces = OpenTypeFont.read(content);
                    String name = application.getSharedPreferences("imported_font_names", 0).getString(id, faces.get(0).family);
                    loaded.add(new ImportedFile(id, name, file, faces)); bytes += content.length;
                } catch (IOException | RuntimeException ignored) { /* A corrupt file never hides good imports. */ }
            }
        }
        publish(loaded);
    }

    public static synchronized ImportedFile importUri(Context context, Uri uri) throws IOException {
        initialize(context);
        String name = "Font";
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (RuntimeException ignored) { }
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new FontException("invalid");
            return importStream(context, input, name);
        }
    }

    public static synchronized ImportedFile importStream(Context context, InputStream input, String name) throws IOException {
        initialize(context);
        byte[] bytes = readLimited(input);
        String id = hash(bytes);
        for (ImportedFile file : files) if (file.id.equals(id)) throw new FontException("duplicate");
        List<OpenTypeFont> parsed;
        try { parsed = OpenTypeFont.read(bytes); }
        catch (IOException | RuntimeException e) { throw new FontException("invalid"); }
        boolean supported = false;
        for (OpenTypeFont font : parsed) if (supportsAppLanguage(font)) supported = true;
        if (!supported) throw new FontException("language");
        boolean allDuplicates = true;
        for (OpenTypeFont face : parsed) {
            boolean duplicate = false;
            for (ImportedFile file : files) for (OpenTypeFont existing : file.faces) {
                if (face.identity.equals(existing.identity) && face.italic == existing.italic
                        && Arrays.equals(face.weights, existing.weights) && face.fingerprint.equals(existing.fingerprint)) duplicate = true;
            }
            if (!duplicate) allDuplicates = false;
        }
        if (allDuplicates) throw new FontException("duplicate");
        long size = bytes.length; for (ImportedFile file : files) size += file.file.length();
        if (files.size() >= MAX_FILES || size > MAX_TOTAL_BYTES) throw new FontException("limit");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create font directory");
        File staging = File.createTempFile("pending-", ".tmp", directory);
        File destination = new File(directory, id + ".font");
        try {
            try (FileOutputStream output = new FileOutputStream(staging)) { output.write(bytes); output.getFD().sync(); }
            // Android's parser must also accept every collection face before the file is published.
            for (OpenTypeFont face : parsed) {
                new android.graphics.fonts.Font.Builder(staging).setTtcIndex(face.index).build();
            }
            if (!staging.renameTo(destination)) throw new IOException("Cannot publish font");
        } catch (IllegalArgumentException e) { throw new FontException("invalid"); }
        finally { if (staging.exists()) staging.delete(); }
        String displayName = name == null ? parsed.get(0).family : name.replaceAll("[\\p{Cc}\\p{Cf}]", "");
        if (displayName.length() > 200) displayName = displayName.substring(0, 200);
        application.getSharedPreferences("imported_font_names", 0).edit().putString(id, displayName).apply();
        ImportedFile imported = new ImportedFile(id, displayName, destination, parsed);
        List<ImportedFile> next = new ArrayList<>(files); next.add(imported); publish(next);
        return imported;
    }

    public static synchronized boolean delete(String id) {
        List<ImportedFile> next = new ArrayList<>(files);
        for (ImportedFile file : files) if (file.id.equals(id)) {
            if (file.file.exists() && !file.file.delete()) return false;
            File[] derived = new File(application.getCacheDir(), "font-script-subsets-v1").listFiles();
            if (derived != null) for (File subset : derived) {
                if (subset.getName().startsWith(id + "-")) subset.delete();
            }
            next.remove(file);
            application.getSharedPreferences("imported_font_names", 0).edit().remove(id).apply();
            publish(next); return true;
        }
        return false;
    }

    public static List<ImportedFile> files() { return files; }
    public static List<Family> families() { return new ArrayList<>(families.values()); }
    public static Family family(String id) { return families.get(id); }
    public static boolean isImported(String id) { return id != null && id.startsWith(PREFIX); }
    public static synchronized long revision() { return revision; }
    public static String familyId(OpenTypeFont face) { return PREFIX + hash(face.identity.getBytes(StandardCharsets.UTF_8)); }
    public static boolean supportsAppLanguage(OpenTypeFont font) {
        for (String language : new String[]{"zh-Hans", "zh-Hant", "en", "ja", "ko"}) {
            if (ScriptFontPreferences.supportsFace(font, language)) return true;
        }
        return false;
    }
    public static synchronized Typeface resolve(String id, int weight) {
        Family family = families.get(id);
        if (family == null) return Typeface.DEFAULT;
        String key = id + ":" + weight;
        Typeface cached = TYPEFACES.get(key); if (cached != null) return cached;
        Face face = family.faceFor(weight);
        try {
            Typeface.Builder builder = new Typeface.Builder(face.source.file).setTtcIndex(face.font.index)
                    .setWeight(weight).setItalic(face.font.italic).setFallback("sans-serif");
            if (face.font.variable) builder.setFontVariationSettings("'wght' " + weight);
            Typeface typeface = builder.build();
            if (typeface == null) return Typeface.DEFAULT;
            if (TYPEFACES.size() >= 128) TYPEFACES.clear();
            TYPEFACES.put(key, typeface); return typeface;
        } catch (RuntimeException e) { return Typeface.DEFAULT; }
    }

    private static void publish(List<ImportedFile> next) {
        Map<String, List<Face>> groups = new TreeMap<>();
        for (ImportedFile file : next) for (OpenTypeFont font : file.faces) {
            String id = familyId(font);
            List<Face> list = groups.get(id); if (list == null) { list = new ArrayList<>(); groups.put(id, list); }
            list.add(new Face(file, font));
        }
        List<Family> ordered = new ArrayList<>();
        for (Map.Entry<String, List<Face>> entry : groups.entrySet()) ordered.add(new Family(entry.getKey(), entry.getValue()));
        ordered.sort(Comparator.comparing(f -> f.name));
        Map<String, Family> snapshot = new LinkedHashMap<>(); for (Family family : ordered) snapshot.put(family.id, family);
        files = Collections.unmodifiableList(new ArrayList<>(next)); families = Collections.unmodifiableMap(snapshot);
        TYPEFACES.clear(); revision++;
    }
    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[16384]; int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + (long) read > OpenTypeFont.MAX_BYTES) throw new FontException("limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
    private static String hash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
}
