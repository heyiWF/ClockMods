package com.clockmods.widget.render

import android.content.Context
import com.clockmods.R
import com.clockmods.background.FontCatalog
import com.clockmods.widget.model.WidgetConfig

object WidgetFontRegistry {
    const val THEME: String = WidgetConfig.FONT_THEME
    private const val STRUCTURAL_COLUMNS = 3
    private val bundled = FontCatalog.options().filterNot { it.isSystem() }
    @JvmField val COLUMNS: Int = STRUCTURAL_COLUMNS + bundled.size

    @JvmStatic fun ids(): List<String> = WidgetConfig.FONT_IDS.toList()

    @JvmStatic fun idAt(index: Int): String = WidgetConfig.FONT_IDS.getOrNull(index) ?: THEME

    @JvmStatic fun indexOf(id: String?): Int = WidgetConfig.FONT_IDS.indexOf(id).takeIf { it >= 0 } ?: 0

    @JvmStatic fun isKnown(id: String?): Boolean = WidgetConfig.FONT_IDS.contains(id)

    @JvmStatic fun labels(context: Context): Array<String> {
        val families = FontCatalog.displayNames(context)
        return arrayOf(context.getString(R.string.widget_font_theme), *families)
    }

    @JvmStatic fun columnOf(fontId: String?, themeColumn: Int): Int {
        if (THEME == fontId) return themeColumn
        if (FontCatalog.optionFor(fontId).isSystem()) return 0
        val index = bundled.indexOfFirst { it.id == fontId }
        return if (index >= 0) STRUCTURAL_COLUMNS + index else themeColumn
    }
}
