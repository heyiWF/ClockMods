package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class TextSpacingTest {
    @Test public void separatesChineseFromLatinLettersAndDigitsInBothDirections() {
        for (int letter : new int[]{'A', 'z', '0', '9'}) {
            Assert.assertTrue(TextSpacing.needsPanguSpace('年', letter));
            Assert.assertTrue(TextSpacing.needsPanguSpace(letter, '年'));
        }
    }
    @Test public void preservesSpacesPunctuationAndEmoji() {
        for (int symbol : new int[]{' ', '℃', '%', '（', 0x1F4C5}) {
            Assert.assertFalse(TextSpacing.needsPanguSpace('年', symbol));
            Assert.assertFalse(TextSpacing.needsPanguSpace(symbol, '年'));
        }
        Assert.assertFalse(TextSpacing.needsPanguSpace('星', '期'));
        Assert.assertFalse(TextSpacing.needsPanguSpace('A', '1'));
    }
    @Test public void recognizesSupplementaryChineseIdeographs() {
        Assert.assertTrue(TextSpacing.needsPanguSpace(0x20000, 'A'));
        Assert.assertTrue(TextSpacing.needsPanguSpace('1', 0x20000));
    }
}
