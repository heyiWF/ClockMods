package com.clockmods.ultimate.clock;

import android.content.Context;
import android.graphics.Typeface;

import com.clockmods.background.ClockPreferences;
import com.clockmods.sdk.clock.ClockThemeTokens;
import com.clockmods.ui.ClockTypefaceResolver;

/**
 * Folds the user's per-theme font choice into a style's theme tokens.
 *
 * <p>A style ships tokens naming the families its design was drawn with. The user may override
 * those per clock style, and this is where the two meet: the override is resolved once and handed
 * to the renderer as a typeface, so no renderer needs to know that per-theme typography exists.
 *
 * <p>Resolution is cached because {@code onDraw} runs every frame — rebuilding tokens per frame
 * would churn objects at 60fps for a value that only changes when the user opens settings.
 */
public final class ClockTypography {
    private String cachedScopeId;
    private String cachedFamily;
    private int cachedWeight;
    private ClockThemeTokens cachedSource;
    private ClockThemeTokens cachedResult;

    /**
     * Returns {@code tokens} with the font chosen for {@code scopeId} applied, or {@code tokens}
     * unchanged when that scope is still on the system font (which is what every style's own
     * family already resolves to).
     */
    public ClockThemeTokens apply(Context context, ClockThemeTokens tokens, String scopeId) {
        if (context == null || tokens == null) return tokens;
        ClockPreferences preferences = new ClockPreferences(context);
        return apply(context, tokens, scopeId,
                preferences.getFontFamily(scopeId), preferences.getFontWeight(scopeId));
    }

    /** As {@link #apply(Context, ClockThemeTokens, String)}, with the choice already read. */
    public ClockThemeTokens apply(Context context, ClockThemeTokens tokens, String scopeId,
            String family, int weight) {
        if (context == null || tokens == null) return tokens;
        if (cachedResult != null && tokens == cachedSource && weight == cachedWeight
                && equal(scopeId, cachedScopeId) && equal(family, cachedFamily)) {
            return cachedResult;
        }
        Typeface face = ClockTypefaceResolver.resolve(context, family, weight);
        ClockThemeTokens result = face == null ? tokens
                : tokens.toBuilder().typefaces(face, face).build();
        cachedScopeId = scopeId;
        cachedFamily = family;
        cachedWeight = weight;
        cachedSource = tokens;
        cachedResult = result;
        return result;
    }

    /** Drops the memoised tokens, so the next draw re-reads the user's choice. */
    public void invalidate() {
        cachedResult = null;
        cachedSource = null;
    }

    private static boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
