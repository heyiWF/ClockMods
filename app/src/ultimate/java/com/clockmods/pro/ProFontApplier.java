package com.clockmods.pro;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;
import com.clockmods.ui.ClockTypefaceResolver;

import java.util.Map;
import java.util.WeakHashMap;

public final class ProFontApplier {
    private static final Map<TextView, Boolean> ORIGINAL_BOLD = new WeakHashMap<>();

    private ProFontApplier() {
    }

    /**
     * Applies the app-wide font, for chrome that belongs to no theme (navigation, the alarm and
     * timer screens). Theme-owned surfaces use {@link #apply(View, String)} instead.
     */
    public static void apply(View root) {
        apply(root, null);
    }

    /** Applies the font chosen for {@code scopeId} (a calendar theme or clock style). */
    public static void apply(View root, String scopeId) {
        if (root == null) return;
        ClockPreferences preferences = new ClockPreferences(root.getContext());
        String family = scopeId == null
                ? preferences.getFontFamily() : preferences.getFontFamily(scopeId);
        int weight = scopeId == null
                ? (preferences.isBoldText() ? ClockPreferences.BOLD_WEIGHT
                        : FontCatalog.DEFAULT_WEIGHT)
                : preferences.getFontWeight(scopeId);
        apply(root, family, weight, scopeId);
    }

    private static void apply(View view, String fontFamily, int weight, String scopeId) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Boolean originalBold = ORIGINAL_BOLD.get(textView);
            if (originalBold == null) {
                Typeface current = textView.getTypeface();
                originalBold = current != null && current.isBold();
                ORIGINAL_BOLD.put(textView, originalBold);
            }
            // A text view stamped bold by its own layout (not by this pass) keeps a visibly heavier
            // tier than the base the user chose; everything else sits exactly on the base.
            int weightForView = originalBold
                    ? FontCatalog.optionFor(fontFamily).emphasizedWeight(weight) : weight;
            textView.setTypeface(ClockTypefaceResolver.resolve(
                    textView.getContext(), fontFamily, weightForView));
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            apply(group.getChildAt(index), fontFamily, weight, scopeId);
        }
    }
}