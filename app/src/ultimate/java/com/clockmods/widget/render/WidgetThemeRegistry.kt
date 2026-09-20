package com.clockmods.widget.render

import android.content.Context
import android.content.res.Configuration
import com.clockmods.R
import com.clockmods.widget.model.WidgetThemeSpec
import com.clockmods.widget.model.WidgetThemeSpec.Font.MONOSPACE
import com.clockmods.widget.model.WidgetThemeSpec.Font.SANS
import com.clockmods.widget.model.WidgetThemeSpec.Font.SERIF

object WidgetThemeRegistry {
    @JvmStatic fun all(context: Context): List<WidgetThemeSpec> {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val primary = context.getColor(if (night) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900)
        val secondary = context.getColor(if (night) android.R.color.system_neutral2_200 else android.R.color.system_neutral2_700)
        val accent = context.getColor(if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_700)
        return listOf(
            WidgetThemeSpec("system.dynamic", R.string.widget_theme_dynamic, R.drawable.widget_bg_dynamic, primary, secondary, accent, SANS, 255),
            WidgetThemeSpec("glass.light", R.string.widget_theme_glass, R.drawable.widget_bg_glass, 0xff152a38.toInt(), 0xff304657.toInt(), 0xff125e70.toInt(), SANS, 235),
            WidgetThemeSpec("instrument.dark", R.string.widget_theme_instrument, R.drawable.widget_bg_instrument, 0xfff4f6f9.toInt(), 0xffacbac7.toInt(), 0xffa8dcfa.toInt(), MONOSPACE, 255),
            WidgetThemeSpec("paper.warm", R.string.widget_theme_paper, R.drawable.widget_bg_paper, 0xff30281f.toInt(), 0xff675541.toInt(), 0xff9e362c.toInt(), SERIF, 255),
            WidgetThemeSpec("neon.night", R.string.widget_theme_neon, R.drawable.widget_bg_neon, 0xffecfaff.toInt(), 0xffacbde0.toInt(), 0xff5fffe0.toInt(), MONOSPACE, 255),
            WidgetThemeSpec("transparent.clean", R.string.widget_theme_transparent, R.drawable.widget_bg_transparent, 0xfffafafa.toInt(), 0xfffafafa.toInt(), 0xfffafafa.toInt(), SANS, 0),
        )
    }

    @JvmStatic fun resolve(context: Context, id: String?): WidgetThemeSpec =
        all(context).firstOrNull { it.id == id } ?: all(context).first()
}
