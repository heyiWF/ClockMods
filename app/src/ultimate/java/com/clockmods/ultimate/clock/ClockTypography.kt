package com.clockmods.ultimate.clock

import android.content.Context
import com.clockmods.background.ClockPreferences
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.ui.ClockTypefaceResolver

/** Folds the user's per-theme font choice into a style's theme tokens. */
class ClockTypography {
    private var cachedScopeId: String? = null
    private var cachedFamily: String? = null
    private var cachedWeight = 0
    private var cachedSource: ClockThemeTokens? = null
    private var cachedResult: ClockThemeTokens? = null

    fun apply(context: Context, tokens: ClockThemeTokens, scopeId: String?): ClockThemeTokens {
        if (context == null || tokens == null) return tokens
        val preferences = ClockPreferences(context)
        return apply(
            context,
            tokens,
            scopeId,
            preferences.getFontFamily(scopeId),
            preferences.getFontWeight(scopeId),
        )
    }

    fun apply(
        context: Context,
        tokens: ClockThemeTokens,
        scopeId: String?,
        family: String?,
        weight: Int,
    ): ClockThemeTokens {
        if (context == null || tokens == null) return tokens
        cachedResult?.let { result ->
            if (tokens === cachedSource && weight == cachedWeight &&
                scopeId == cachedScopeId && family == cachedFamily
            ) {
                return result
            }
        }

        val face = ClockTypefaceResolver.resolve(context, family, weight)
        val result = if (face == null) tokens else tokens.toBuilder().typefaces(face, face).build()
        cachedScopeId = scopeId
        cachedFamily = family
        cachedWeight = weight
        cachedSource = tokens
        cachedResult = result
        return result
    }

    /** Drops the memoised tokens, so the next draw re-reads the user's choice. */
    fun invalidate() {
        cachedResult = null
        cachedSource = null
    }
}
