package com.clockmods.background;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Keeps outlines intact and restricts cmap so a mixed typeface cannot steal the other script. */
public final class FontScriptSubset {
    private FontScriptSubset() { }
    public static boolean isEastAsian(int cp) {
        return cp >= 0x3400 && cp <= 0x9fff || cp >= 0xf900 && cp <= 0xfaff
                || cp >= 0x20000 && cp <= 0x323af || cp >= 0x3040 && cp <= 0x30ff
                || cp >= 0x31f0 && cp <= 0x31ff || cp >= 0xff66 && cp <= 0xff9d
                || cp >= 0x1100 && cp <= 0x11ff || cp >= 0x3130 && cp <= 0x318f
                || cp >= 0xa960 && cp <= 0xa97f || cp >= 0xac00 && cp <= 0xd7ff;
    }
    public static byte[] build(byte[] input, int index, boolean eastAsian) throws IOException {
        // Validate all offsets and lengths before consulting the directory below.
        List<OpenTypeFont> parsed = OpenTypeFont.read(input);
        if (index < 0 || index >= parsed.size()) throw new IOException("Invalid face");
        ByteBuffer source = ByteBuffer.wrap(input).order(ByteOrder.BIG_ENDIAN);
        int base = source.getInt(0) == 0x74746366 ? source.getInt(12 + index * 4) : 0;
        List<Integer> tags = new ArrayList<>(); List<byte[]> contents = new ArrayList<>();
        for (int i = 0; i < u16(source, base + 4); i++) {
            int at = base + 12 + i * 16, tag = source.getInt(at), start = source.getInt(at + 8), length = source.getInt(at + 12);
            if (tag == 0x44534947) continue; // A modified cmap cannot retain a digital signature.
            byte[] table;
            if (tag == 0x636d6170) table = cmap(source, start, length, eastAsian);
            else { table = new byte[length]; System.arraycopy(input, start, table, 0, length); }
            if (tag == 0x68656164 && length >= 12) for (int j = 8; j < 12; j++) table[j] = 0;
            tags.add(tag); contents.add(table);
        }
        int total = 12 + tags.size() * 16;
        for (byte[] table : contents) total += (table.length + 3) & ~3;
        if (total > OpenTypeFont.MAX_BYTES + 16 * 1024 * 1024) throw new IOException("Subset too large");
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
        int count = tags.size(), power = Integer.highestOneBit(count);
        out.putInt(source.getInt(base)).putShort((short)count).putShort((short)(power * 16))
                .putShort((short)Integer.numberOfTrailingZeros(power)).putShort((short)((count - power) * 16));
        int offset = 12 + count * 16, head = -1;
        for (int i = 0; i < count; i++) {
            byte[] table = contents.get(i);
            out.position(12 + i * 16); out.putInt(tags.get(i)).putInt(checksum(table)).putInt(offset).putInt(table.length);
            out.position(offset); out.put(table);
            if (tags.get(i) == 0x68656164) head = offset;
            offset += (table.length + 3) & ~3;
        }
        if (head >= 0) out.putInt(head + 8, 0xb1b0afba - checksum(out.array()));
        return out.array();
    }
    private static byte[] cmap(ByteBuffer source, int start, int length, boolean eastAsian) throws IOException {
        int chosen = -1, rank = -1;
        for (int i = 0; i < u16(source, start + 2); i++) {
            int at = start + 4 + i * 8, platform = u16(source, at), encoding = u16(source, at + 2);
            if (platform != 0 && !(platform == 3 && (encoding == 1 || encoding == 10))) continue;
            int sub = start + source.getInt(at + 4), format = u16(source, sub);
            int score = format == 12 ? 5 : format == 4 ? 4 : format == 13 ? 3 : format == 6 ? 2 : format == 0 ? 1 : -1;
            if (score > rank) { rank = score; chosen = sub; }
        }
        if (chosen < 0) throw new IOException("No Unicode cmap");
        List<int[]> groups = new ArrayList<>(); int format = u16(source, chosen);
        if (format >= 12) {
            for (int i = 0; i < source.getInt(chosen + 12); i++) {
                int at = chosen + 16 + i * 12, first = source.getInt(at), last = source.getInt(at + 4), glyph = source.getInt(at + 8);
                for (int cp = first; cp <= last; cp++) add(groups, cp, glyph + (format == 12 ? cp - first : 0), eastAsian);
            }
        } else if (format == 4) {
            int count = u16(source, chosen + 6) / 2;
            for (int i = 0; i < count; i++) {
                int last = u16(source, chosen + 14 + 2 * i), first = u16(source, chosen + 16 + 2 * count + 2 * i);
                int delta = source.getShort(chosen + 16 + 4 * count + 2 * i);
                int at = chosen + 16 + 6 * count + 2 * i, offset = u16(source, at);
                for (int cp = first; cp <= last && cp != 0xffff; cp++) {
                    int glyph;
                    if (offset == 0) glyph = (cp + delta) & 65535;
                    else {
                        long pos = (long)at + offset + 2L * (cp - first);
                        if (pos + 2 > (long)start + length) throw new IOException("Invalid glyph offset");
                        glyph = u16(source, (int)pos); if (glyph != 0) glyph = (glyph + delta) & 65535;
                    }
                    add(groups, cp, glyph, eastAsian);
                }
            }
        } else if (format == 0) {
            for (int cp = 0; cp < 256; cp++) add(groups, cp, source.get(chosen + 6 + cp) & 255, eastAsian);
        } else {
            int first = u16(source, chosen + 6);
            for (int i = 0; i < u16(source, chosen + 8); i++) add(groups, first + i, u16(source, chosen + 10 + 2 * i), eastAsian);
        }
        ByteBuffer out = ByteBuffer.allocate(28 + groups.size() * 12).order(ByteOrder.BIG_ENDIAN);
        out.putShort((short)0).putShort((short)1).putShort((short)3).putShort((short)10).putInt(12);
        out.putShort((short)12).putShort((short)0).putInt(out.capacity() - 12).putInt(0).putInt(groups.size());
        for (int[] group : groups) out.putInt(group[0]).putInt(group[1]).putInt(group[2]);
        return out.array();
    }
    private static void add(List<int[]> groups, int cp, int glyph, boolean eastAsian) throws IOException {
        if (glyph == 0 || isEastAsian(cp) != eastAsian) return;
        if (!groups.isEmpty()) {
            int[] last = groups.get(groups.size() - 1);
            if (cp == last[1] + 1 && glyph == last[2] + cp - last[0]) { last[1] = cp; return; }
        }
        if (groups.size() > 262144) throw new IOException("Too many glyph groups");
        groups.add(new int[]{cp, cp, glyph});
    }
    private static int checksum(byte[] bytes) {
        int sum = 0;
        for (int i = 0; i < bytes.length; i += 4) {
            int word = 0; for (int j = 0; j < 4; j++) word = (word << 8) | (i + j < bytes.length ? bytes[i + j] & 255 : 0);
            sum += word;
        }
        return sum;
    }
    private static int u16(ByteBuffer b, int at) { return b.getShort(at) & 65535; }
}
