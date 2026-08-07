package com.clockmods.pro;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clockmods.background.ClockPreferences;
import com.clockmods.ui.ClockTypefaceResolver;

import java.util.Map;
import java.util.WeakHashMap;

public final class ProFontApplier {
    private static final Map<TextView, Boolean> ORIGINAL_BOLD = new WeakHashMap<>();

    private ProFontApplier() {
    }

    public static void apply(View root) {
        if (root == null) return;
        ClockPreferences preferences = new ClockPreferences(root.getContext());
        apply(root, preferences.getFontFamily());
    }

    private static void apply(View view, String fontFamily) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Boolean originalBold = ORIGINAL_BOLD.get(textView);
            if (originalBold == null) {
                Typeface current = textView.getTypeface();
                originalBold = current != null && current.isBold();
                ORIGINAL_BOLD.put(textView, originalBold);
            }
            textView.setTypeface(ClockTypefaceResolver.resolveTime(
                    textView.getContext(), fontFamily, originalBold));
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            apply(group.getChildAt(index), fontFamily);
        }
    }
}