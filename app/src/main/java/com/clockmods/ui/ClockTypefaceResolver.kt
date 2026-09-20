package com.clockmods.ui

import android.content.Context
import android.graphics.Typeface
import com.clockmods.background.ClockPreferences
import com.clockmods.background.FontCatalog

object ClockTypefaceResolver {
    private val cache = HashMap<String, Typeface>()

    @JvmStatic
    fun resolve(context: Context, family: String?, weight: Int): Typeface {
        val normalized = ClockPreferences.normalizeFontFamily(family)
        val option = FontCatalog.optionFor(normalized)
        val resolvedWeight = option.nearestWeight(weight)
        return if (option.isSystem()) systemTypeface(resolvedWeight)
        else loadOption(context, option, resolvedWeight)
    }

    @JvmStatic
    fun resolveTime(context: Context, family: String?, bold: Boolean): Typeface =
        resolve(context, family, if (bold) 700 else FontCatalog.DEFAULT_WEIGHT)

    @JvmStatic
    fun resolveForTheme(
        context: Context,
        preferences: ClockPreferences,
        scopeId: String,
        emphasized: Boolean,
    ): Typeface {
        val family = preferences.getFontFamily(scopeId)
        var weight = preferences.getFontWeight(scopeId)
        if (emphasized) weight = FontCatalog.optionFor(family).emphasizedWeight(weight)
        return resolve(context, family, weight)
    }

    @JvmStatic
    fun resolveSupporting(
        context: Context,
        family: String?,
        weight: Int,
        containsChinese: Boolean,
    ): Typeface = if (containsChinese) systemTypeface(weight) else resolve(context, family, weight)

    @JvmStatic
    fun resolveSupporting(
        context: Context,
        family: String?,
        bold: Boolean,
        containsChinese: Boolean,
    ): Typeface = resolveSupporting(
        context,
        family,
        if (bold) 700 else FontCatalog.DEFAULT_WEIGHT,
        containsChinese,
    )

    @JvmStatic
    fun resolveSupportingForCodePoint(
        context: Context,
        family: String?,
        weight: Int,
        codePoint: Int,
    ): Typeface = resolveSupporting(context, family, weight, isChinese(codePoint))

    @JvmStatic
    fun resolveSupportingForCodePoint(
        context: Context,
        family: String?,
        bold: Boolean,
        codePoint: Int,
    ): Typeface = resolveSupporting(context, family, bold, isChinese(codePoint))

    @JvmStatic
    fun isChinese(codePoint: Int): Boolean {
        val block = Character.UnicodeBlock.of(codePoint)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
    }

    private fun systemTypeface(weight: Int): Typeface {
        val family = when {
            weight <= 100 -> "sans-serif-thin"
            weight <= 300 -> "sans-serif-light"
            weight <= 400 -> "sans-serif"
            weight <= 500 -> "sans-serif-medium"
            else -> "sans-serif-black"
        }
        return Typeface.create(family, Typeface.NORMAL)
    }

    @Synchronized
    private fun loadOption(
        context: Context,
        option: FontCatalog.FontOption,
        weight: Int,
    ): Typeface {
        val cacheKey = option.id + '#' + weight
        cache[cacheKey]?.let { return it }
        val loaded = if (option.isVariable()) loadVariable(context, option, weight)
        else load(context, option.staticAssetFor(weight), systemTypeface(weight))
        cache[cacheKey] = loaded
        return loaded
    }

    private fun loadVariable(
        context: Context,
        option: FontCatalog.FontOption,
        weight: Int,
    ): Typeface {
        val asset = option.variableAsset ?: return systemTypeface(weight)
        val settings = "'wght' $weight" + (option.variableAxes?.let { ", $it" } ?: "")
        return try {
            Typeface.Builder(context.assets, asset)
                .setFontVariationSettings(settings)
                .setWeight(FontCatalog.DEFAULT_WEIGHT)
                .build()
        } catch (_: RuntimeException) {
            load(context, option.variableAsset, systemTypeface(weight))
        }
    }

    private fun load(context: Context, path: String?, fallback: Typeface): Typeface {
        if (path == null) return fallback
        return try {
            Typeface.Builder(context.assets, path)
                .setWeight(FontCatalog.DEFAULT_WEIGHT)
                .build()
        } catch (_: RuntimeException) {
            fallback
        }
    }
}
