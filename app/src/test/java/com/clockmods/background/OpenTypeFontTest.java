package com.clockmods.background;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.util.Arrays;
import static org.junit.Assert.*;

public class OpenTypeFontTest {
    private byte[] asset(String name) throws IOException {
        File file = new File("src/main/assets/fonts/" + name);
        if (!file.isFile()) file = new File("src/ultimate/assets/fonts/" + name);
        return Files.readAllBytes(file.toPath());
    }
    @Test public void readsStaticFamilyWeightsAndRealCoverage() throws Exception {
        OpenTypeFont regular = OpenTypeFont.read(asset("GoogleSansDisplay-Regular.ttf")).get(0);
        OpenTypeFont medium = OpenTypeFont.read(asset("GoogleSansDisplay-Medium.ttf")).get(0);
        OpenTypeFont bold = OpenTypeFont.read(asset("GoogleSansDisplay-Bold.ttf")).get(0);
        assertEquals(regular.identity, medium.identity);
        assertEquals(regular.identity, bold.identity);
        assertEquals(400, regular.weight); assertEquals(500, medium.weight); assertEquals(700, bold.weight);
        assertTrue(regular.hasGlyph('A')); assertTrue(regular.hasGlyph('8'));
        assertFalse(regular.hasGlyph('宜')); assertFalse(regular.hasGlyph(0x10ffff));
    }
    @Test public void readsVariableAxisStops() throws Exception {
        OpenTypeFont font = OpenTypeFont.read(asset("Lora-Variable.ttf")).get(0);
        assertTrue(font.variable); assertArrayEquals(new int[]{400,500,600,700}, font.weights);
    }
    @Test public void acceptsOpenTypeCffOutlines() throws Exception {
        OpenTypeFont font = OpenTypeFont.read(asset("SFProRounded-Regular.otf")).get(0);
        assertTrue(font.hasGlyph('A')); assertEquals(400, font.weight);
    }
    @Test public void rejectsTruncationAndOutOfBoundsDirectory() throws Exception {
        byte[] bytes = asset("GoogleSansDisplay-Regular.ttf");
        for (int size : new int[]{0, 4, 12, bytes.length / 2}) reject(Arrays.copyOf(bytes, size));
        ByteBuffer.wrap(bytes).putInt(20, Integer.MAX_VALUE); reject(bytes);
    }
    @Test public void rejectsUnknownContainerAndOversizedFont() throws Exception {
        reject("not a font file at all".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        reject(new byte[OpenTypeFont.MAX_BYTES + 1]);
    }
    @Test public void doesNotClaimCoverageForMissingGlyphOutlines() throws Exception {
        byte[] bytes = asset("GoogleSansDisplay-Regular.ttf");
        ByteBuffer data = ByteBuffer.wrap(bytes);
        for (int i = 0; i < (data.getShort(4) & 65535); i++) {
            int at = 12 + i * 16;
            if (data.getInt(at) == 0x6d617870) data.putShort(data.getInt(at + 8) + 4, (short)1);
        }
        OpenTypeFont font = OpenTypeFont.read(bytes).get(0);
        assertFalse(font.hasGlyph('A')); assertFalse(font.hasGlyph('8'));
    }
    @Test public void scriptSubsetPreservesWesternGlyphsAndDropsOtherScripts() throws Exception {
        byte[] bytes = asset("GoogleSansDisplay-Regular.ttf");
        OpenTypeFont west = OpenTypeFont.read(FontScriptSubset.build(bytes, 0, false)).get(0);
        OpenTypeFont east = OpenTypeFont.read(FontScriptSubset.build(bytes, 0, true)).get(0);
        assertTrue(west.hasGlyph('A')); assertTrue(west.hasGlyph('8')); assertTrue(west.hasGlyph(':'));
        assertFalse(east.hasGlyph('A')); assertFalse(east.hasGlyph('8')); assertFalse(east.hasGlyph(':'));
        assertEquals(west.identity, east.identity);
    }
    @Test public void classifiesLettersSeparatelyFromNumbersAndPunctuation() {
        for (int cp : new int[]{'中', 'あ', 'ア', '한', 0x20000}) assertTrue(FontScriptSubset.isEastAsian(cp));
        for (int cp : new int[]{'A', '7', ':', '，', '。', '℃', 0x1f600}) assertFalse(FontScriptSubset.isEastAsian(cp));
    }
    private void reject(byte[] bytes) throws Exception {
        try { OpenTypeFont.read(bytes); fail("Malformed font accepted"); } catch (IOException expected) { }
    }
}
