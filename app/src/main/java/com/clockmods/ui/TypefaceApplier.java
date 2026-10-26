package com.clockmods.ui;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Applies the bundled Roboto face to every text view in a tree so the whole app uses one font.
 * Chinese characters have no Roboto glyphs and fall back to the platform font per glyph.
 */
public final class TypefaceApplier {
    private TypefaceApplier() {
    }

    public static void applyToTree(View root) {
        if (root == null) {
            return;
        }
        if (root instanceof TextView) {
            TextView text = (TextView) root;
            Typeface current = text.getTypeface();
            boolean bold = current != null && current.isBold();
            text.setTypeface(ClockTypefaceResolver.resolve(text.getContext(), bold));
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyToTree(group.getChildAt(index));
            }
        }
    }
}
