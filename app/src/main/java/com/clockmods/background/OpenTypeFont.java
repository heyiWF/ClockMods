package com.clockmods.background;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/** Bounded OpenType metadata reader. Coverage comes from cmap, never Android's fallback fonts. */
public final class OpenTypeFont {
    public static final int MAX_BYTES = 32 * 1024 * 1024;
    private final Map<String, ByteBuffer> tables = new HashMap<>();
    public final int index;
    public final String family;
    public final String identity;
    public final String fingerprint;
    public final String subfamily;
    public final int weight;
    public final boolean italic;
    public final int[] weights;
    public final boolean variable;
    private final int glyphCount;
    private final List<ByteBuffer> cmaps = new ArrayList<>();

    public static List<OpenTypeFont> read(byte[] bytes) throws IOException {
        if (bytes.length < 12 || bytes.length > MAX_BYTES) throw invalid();
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        try {
            List<OpenTypeFont> result = new ArrayList<>();
            Map<Long, ByteBuffer> retainedCmaps = new HashMap<>();
            long[] retainedBytes = {0};
            if (b.getInt(0) == 0x74746366) {
                int count = b.getInt(8);
                if (count < 1 || count > 64) throw invalid();
                range(b, 12, count * 4);
                for (int i = 0; i < count; i++) result.add(new OpenTypeFont(b, b.getInt(12 + i * 4), i, retainedCmaps, retainedBytes));
            } else result.add(new OpenTypeFont(b, 0, 0, retainedCmaps, retainedBytes));
            return Collections.unmodifiableList(result);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new IOException("Invalid OpenType table", e);
        }
    }

    private OpenTypeFont(ByteBuffer bytes, int offset, int index,
            Map<Long, ByteBuffer> retainedCmaps, long[] retainedBytes) throws IOException {
        ByteBuffer data = bytes; this.index = index;
        range(data, offset, 12);
        int signature = data.getInt(offset);
        if (signature != 0x00010000 && signature != 0x4f54544f && signature != 0x74727565) throw invalid();
        int count = u16(data, offset + 4);
        if (count < 1 || count > 256) throw invalid();
        range(data, offset + 12, count * 16);
        for (int i = 0; i < count; i++) {
            int at = offset + 12 + 16 * i;
            byte[] tag = new byte[4];
            for (int j = 0; j < 4; j++) tag[j] = data.get(at + j);
            String key = new String(tag, StandardCharsets.US_ASCII);
            int start = data.getInt(at + 8), size = data.getInt(at + 12);
            range(data, start, size);
            ByteBuffer view = data.duplicate(); view.position(start); view.limit(start + size);
            if (tables.put(key, view.slice().order(ByteOrder.BIG_ENDIAN)) != null) throw invalid();
        }
        long tableBytes = 0;
        for (ByteBuffer table : tables.values()) tableBytes += table.remaining();
        if (tableBytes > MAX_BYTES) throw invalid();
        fingerprint = fingerprint();
        ByteBuffer names = required("name", 6);
        String typographic = name(names, 16), legacy = name(names, 1);
        family = clean(typographic.isEmpty() ? legacy : typographic);
        if (family.isEmpty()) throw invalid();
        String typographicStyle = name(names, 17);
        subfamily = clean(typographicStyle.isEmpty() ? name(names, 2) : typographicStyle);
        ByteBuffer os2 = required("OS/2", 8);
        weight = Math.max(1, Math.min(1000, u16(os2, 4)));
        glyphCount = u16(required("maxp", 6), 4);
        if (glyphCount == 0) throw invalid();
        italic = os2.limit() >= 64 && (u16(os2, 62) & 1) != 0;
        // Typographic family names intentionally group Regular, Bold, Light and Italic files.
        identity = Normalizer.normalize(family, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
        TreeSet<Integer> stops = new TreeSet<>();
        ByteBuffer fvar = tables.get("fvar");
        boolean hasWeightAxis = false;
        if (fvar != null) {
            range(fvar, 0, 16);
            int start = u16(fvar, 4), axes = u16(fvar, 8), size = u16(fvar, 10);
            if (axes > 64 || size < 20) throw invalid();
            range(fvar, start, axes * size);
            for (int i = 0; i < axes; i++) {
                int at = start + size * i;
                if (fvar.getInt(at) != 0x77676874) continue;
                float min = fvar.getInt(at + 4) / 65536f, def = fvar.getInt(at + 8) / 65536f;
                float max = fvar.getInt(at + 12) / 65536f;
                if (min < 1 || max > 1000 || min > def || def > max) throw invalid();
                stops.add(Math.round(min)); stops.add(Math.round(def)); stops.add(Math.round(max));
                for (int w = 100; w <= 900; w += 100) if (w >= min && w <= max) stops.add(w);
                hasWeightAxis = true;
            }
        }
        variable = hasWeightAxis;
        if (!variable) stops.add(weight);
        weights = new int[stops.size()]; int n = 0;
        for (int w : stops) weights[n++] = w;
        ByteBuffer cmap = required("cmap", 4);
        int maps = u16(cmap, 2);
        if (maps > 128) throw invalid();
        range(cmap, 4, maps * 8);
        for (int i = 0; i < maps; i++) {
            int at = 4 + i * 8, platform = u16(cmap, at), encoding = u16(cmap, at + 2);
            if (platform != 0 && !(platform == 3 && (encoding == 1 || encoding == 10))) continue;
            int start = cmap.getInt(at + 4); range(cmap, start, 4);
            int format = u16(cmap, start);
            if (format != 4 && format != 12 && format != 13 && format != 0 && format != 6) continue;
            if (format >= 12) range(cmap, start, 16);
            int length = format >= 12 ? cmap.getInt(start + 4) : u16(cmap, start + 2);
            range(cmap, start, length);
            ByteBuffer view = cmap.duplicate(); view.position(start); view.limit(start + length);
            ByteBuffer sub = view.slice().order(ByteOrder.BIG_ENDIAN);
            validateCmap(sub, format);
            // Collections and Unicode encoding records commonly share the same cmap bytes.
            // Deduplicate them and bound all retained maps across the entire uploaded file.
            long key = ((long)(cmap.arrayOffset() + start) << 32) | length;
            ByteBuffer retained = retainedCmaps.get(key);
            if (retained == null) {
                retainedBytes[0] += length;
                if (retainedBytes[0] > MAX_BYTES) throw invalid();
                byte[] copy = new byte[length]; sub.get(copy);
                retained = ByteBuffer.wrap(copy).order(ByteOrder.BIG_ENDIAN);
                retainedCmaps.put(key, retained);
            }
            if (!cmaps.contains(retained)) cmaps.add(retained);
        }
        if (cmaps.isEmpty()) throw invalid();
        tables.clear();
    }

    private static void validateCmap(ByteBuffer b, int format) throws IOException {
        if (format == 4) {
            range(b, 0, 16); int count = u16(b, 6) / 2;
            if (count < 1 || (u16(b, 6) & 1) != 0) throw invalid();
            range(b, 14, 8 * count + 2);
            int previous = -1;
            for (int i = 0; i < count; i++) {
                int end = u16(b, 14 + 2 * i), start = u16(b, 16 + 2 * count + 2 * i);
                if (start > end || start <= previous) throw invalid();
                previous = end;
                int at = 16 + 6 * count + 2 * i, offset = u16(b, at);
                if (offset != 0) range(b, at + offset, 2 * (end - start + 1));
            }
        } else if (format >= 12) {
            range(b, 0, 16); int count = b.getInt(12);
            if (count < 0 || count > 0x110000) throw invalid();
            range(b, 16, count * 12);
            long last = -1;
            for (int i = 0; i < count; i++) {
                int at = 16 + i * 12; long start = u32(b, at), end = u32(b, at + 4);
                if (start <= last || start > end || end > 0x10ffff) throw invalid();
                last = end;
            }
        } else if (format == 0) range(b, 0, 262);
        else { range(b, 0, 10); range(b, 10, u16(b, 8) * 2); }
    }

    public boolean hasGlyph(int codePoint) {
        if (codePoint < 0 || codePoint > 0x10ffff) return false;
        for (ByteBuffer b : cmaps) {
            int format = u16(b, 0);
            if (format == 0 && codePoint < 256 && validGlyph(b.get(6 + codePoint) & 255)) return true;
            if (format == 6) {
                int i = codePoint - u16(b, 6);
                if (i >= 0 && i < u16(b, 8) && validGlyph(u16(b, 10 + i * 2))) return true;
            }
            if (format == 4 && codePoint <= 0xffff) {
                int count = u16(b, 6) / 2;
                for (int i = 0; i < count; i++) {
                    int end = u16(b, 14 + 2 * i), start = u16(b, 16 + 2 * count + 2 * i);
                    if (codePoint < start || codePoint > end) continue;
                    int delta = b.getShort(16 + 4 * count + 2 * i);
                    int at = 16 + 6 * count + 2 * i, ro = u16(b, at);
                    int glyph;
                    if (ro == 0) glyph = (codePoint + delta) & 0xffff;
                    else {
                        long pos = (long) at + ro + 2L * (codePoint - start);
                        if (pos < 0 || pos + 2 > b.limit()) continue;
                        glyph = u16(b, (int) pos);
                        if (glyph != 0) glyph = (glyph + delta) & 0xffff;
                    }
                    if (validGlyph(glyph)) return true;
                }
            }
            if (format == 12 || format == 13) {
                int low = 0, high = b.getInt(12) - 1;
                while (low <= high) {
                    int mid = (low + high) >>> 1, at = 16 + mid * 12;
                    long start = u32(b, at), end = u32(b, at + 4);
                    if (codePoint < start) high = mid - 1;
                    else if (codePoint > end) low = mid + 1;
                    else { if (validGlyph(u32(b, at + 8) + (format == 12 ? codePoint - start : 0))) return true; break; }
                }
            }
        }
        return false;
    }

    private boolean validGlyph(long glyph) { return glyph > 0 && glyph < glyphCount; }

    public int coverage(String sample) {
        int total = 0, present = 0;
        for (int i = 0; i < sample.length();) {
            int cp = sample.codePointAt(i); i += Character.charCount(cp); total++;
            if (hasGlyph(cp)) present++;
        }
        return total == 0 ? 0 : Math.round(100f * present / total);
    }

    private String name(ByteBuffer b, int id) throws IOException {
        int count = u16(b, 2), strings = u16(b, 4), best = -1; String found = "";
        if (count > 4096) throw invalid();
        range(b, 6, count * 12);
        for (int i = 0; i < count; i++) {
            int at = 6 + i * 12, platform = u16(b, at), language = u16(b, at + 4);
            if (u16(b, at + 6) != id || (platform != 0 && platform != 3 && platform != 1)) continue;
            int size = u16(b, at + 8), offset = strings + u16(b, at + 10);
            range(b, offset, size);
            int score = (platform == 3 ? 20 : platform == 0 ? 10 : 0) + (language == 0x409 ? 5 : 0);
            if (score <= best || size > 2048) continue;
            byte[] bytes = new byte[size]; for (int j = 0; j < size; j++) bytes[j] = b.get(offset + j);
            found = new String(bytes, platform == 1 ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE);
            best = score;
        }
        return found;
    }

    private String fingerprint() {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, ByteBuffer> entry : new java.util.TreeMap<>(tables).entrySet()) {
                String tag = entry.getKey();
                if (tag.equals("name") || tag.equals("DSIG")) continue;
                digest.update(tag.getBytes(StandardCharsets.US_ASCII));
                ByteBuffer table = entry.getValue().duplicate();
                if (tag.equals("head") && table.remaining() >= 36) {
                    byte[] head = new byte[table.remaining()]; table.get(head);
                    java.util.Arrays.fill(head, 8, 12, (byte)0);
                    java.util.Arrays.fill(head, 20, 36, (byte)0);
                    digest.update(head);
                } else digest.update(table);
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private ByteBuffer required(String tag, int size) throws IOException {
        ByteBuffer b = tables.get(tag); if (b == null) throw invalid(); range(b, 0, size); return b;
    }
    private static String clean(String name) { return name.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim(); }
    private static int u16(ByteBuffer b, int at) { return b.getShort(at) & 0xffff; }
    private static long u32(ByteBuffer b, int at) { return b.getInt(at) & 0xffffffffL; }
    private static void range(ByteBuffer b, int offset, int size) throws IOException {
        if (offset < 0 || size < 0 || (long) offset + size > b.limit()) throw invalid();
    }
    private static IOException invalid() { return new IOException("Invalid or unsupported OpenType font"); }
}
