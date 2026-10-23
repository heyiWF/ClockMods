package com.clockmods.ultimate.compose

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*

/** Font changes invalidate preview typography without recreating background/image state. */
@Composable
internal fun rememberPreviewFontRevision(context: Context): Int {
    var revision by remember(context) { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val preferences = context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith("font_") || key.startsWith("script_font__") ||
                key == "bold_text" || key == "clock_language" || key == "show_chinese_calendar" ||
                key == "show_lunar" || key.startsWith("date_pattern_")) revision++
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return revision
}
