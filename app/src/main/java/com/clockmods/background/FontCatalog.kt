package com.clockmods.background

import android.content.Context
import com.clockmods.R
import java.util.Collections

object FontCatalog {
    @JvmField val WEIGHT_STOPS = intArrayOf(100, 200, 300, 400, 500, 600, 700, 800, 900)
    const val DEFAULT_WEIGHT = 400

    class FontOption private constructor(
        @JvmField val id: String,
        @JvmField val displayName: String,
        @JvmField val variableAsset: String?,
        @JvmField val variableAxes: String?,
        private val weights: IntArray,
        private val staticAssets: Array<String>?,
    ) {
        fun isSystem(): Boolean = variableAsset == null && staticAssets == null
        fun isVariable(): Boolean = variableAsset != null
        fun availableWeights(): IntArray = weights.clone()
        fun weightCount(): Int = weights.size
        fun weightAt(index: Int): Int = when { index <= 0 -> weights[0]; index >= weights.size -> weights.last(); else -> weights[index] }
        fun emphasizedWeight(baseWeight: Int): Int {
            val base = weightAt(indexOfNearestWeight(baseWeight))
            return weights.firstOrNull { it >= base + 200 } ?: weights.last()
        }
        fun indexOfNearestWeight(weight: Int): Int {
            var best = 0
            var distance = Int.MAX_VALUE
            weights.forEachIndexed { index, item ->
                val current = kotlin.math.abs(item - weight)
                if (current < distance) { distance = current; best = index }
            }
            return best
        }
        fun nearestWeight(weight: Int): Int = weights[indexOfNearestWeight(weight)]
        fun staticAssetFor(weight: Int): String? = staticAssets?.get(indexOfNearestWeight(weight))

        companion object {
            @JvmStatic fun system(id: String, displayName: String) = FontOption(id, displayName, null, null, intArrayOf(100, 300, 400, 500, 900), null)
            @JvmStatic fun variable(id: String, displayName: String, asset: String, axes: String?, minWeight: Int, maxWeight: Int): FontOption = FontOption(id, displayName, asset, axes, WEIGHT_STOPS.filter { it in minWeight..maxWeight }.toIntArray(), null)
            @JvmStatic fun statics(id: String, displayName: String, weights: IntArray, assets: Array<String>) = FontOption(id, displayName, null, null, weights, assets)
        }
    }

    private val OPTIONS: List<FontOption> = Collections.unmodifiableList(buildList {
        add(FontOption.system(ClockPreferences.FONT_SYSTEM, "系统字体"))
        add(FontOption.variable(ClockPreferences.FONT_ROBOTO, "Roboto", "fonts/Roboto-Variable.ttf", null, 100, 900))
        add(FontOption.statics(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY, "Google Sans Display", intArrayOf(400, 500, 700), arrayOf("fonts/GoogleSansDisplay-Regular.ttf", "fonts/GoogleSansDisplay-Medium.ttf", "fonts/GoogleSansDisplay-Bold.ttf")))
        add(FontOption.statics(ClockPreferences.FONT_GOOGLE_SANS_TEXT, "Google Sans Text", intArrayOf(400, 500, 700), arrayOf("fonts/GoogleSansText-Regular.ttf", "fonts/GoogleSansText-Medium.ttf", "fonts/GoogleSansText-Bold.ttf")))
        add(FontOption.variable(ClockPreferences.FONT_SF_PRO_DISPLAY, "SF Pro Display", "fonts/SFPro-Variable.ttf", "'opsz' 28", 100, 900))
        add(FontOption.statics(ClockPreferences.FONT_SF_PRO_ROUNDED, "SF Pro Rounded", intArrayOf(300, 400, 500, 600, 700), arrayOf("fonts/SFProRounded-Light.otf", "fonts/SFProRounded-Regular.otf", "fonts/SFProRounded-Medium.otf", "fonts/SFProRounded-Semibold.otf", "fonts/SFProRounded-Bold.otf")))
        add(FontOption.variable(ClockPreferences.FONT_INTER, "Inter", "fonts/Inter-Variable.ttf", null, 100, 900))
        add(FontOption.statics(ClockPreferences.FONT_LATO, "Lato", intArrayOf(100, 300, 400, 700, 900), arrayOf("fonts/Lato-Thin.ttf", "fonts/Lato-Light.ttf", "fonts/Lato-Regular.ttf", "fonts/Lato-Bold.ttf", "fonts/Lato-Black.ttf")))
        add(FontOption.variable(ClockPreferences.FONT_LORA, "Lora", "fonts/Lora-Variable.ttf", null, 400, 700))
        add(FontOption.variable(ClockPreferences.FONT_NOTO_SANS, "Noto Sans", "fonts/NotoSans-Variable.ttf", null, 100, 900))
        add(FontOption.variable(ClockPreferences.FONT_BITCOUNT, "Bitcount Grid Double", "fonts/BitcountGridDouble-Variable.ttf", null, 100, 900))
    })

    @JvmStatic fun options(): List<FontOption> = OPTIONS
    @JvmStatic fun optionFor(id: String?): FontOption = OPTIONS.firstOrNull { it.id == id } ?: OPTIONS.first()
    @JvmStatic fun indexOf(id: String?): Int = OPTIONS.indexOfFirst { it.id == id }.let { if (it < 0) 0 else it }
    @JvmStatic fun idForIndex(index: Int): String = if (index in OPTIONS.indices) OPTIONS[index].id else ClockPreferences.FONT_SYSTEM
    @JvmStatic fun isAvailable(id: String?): Boolean = OPTIONS.any { it.id == id }
    @JvmStatic fun displayNames(): Array<String> = OPTIONS.map { it.displayName }.toTypedArray()
    @JvmStatic fun displayNames(context: Context): Array<String> = displayNames().also { names -> OPTIONS.forEachIndexed { index, option -> if (option.isSystem()) names[index] = context.getString(R.string.font_system) } }
}
