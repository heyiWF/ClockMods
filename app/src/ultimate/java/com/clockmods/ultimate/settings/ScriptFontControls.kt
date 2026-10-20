package com.clockmods.ultimate.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.background.FontCatalog
import com.clockmods.background.ScriptFontPreferences
import kotlin.math.roundToInt

@Composable
internal fun ScriptFontControls(preferences: ClockPreferences, scope: String, generation: Int) {
    val language = preferences.getClockLanguage()
    if (!ScriptFontPreferences.hasSeparateScript(language)) return
    val context = LocalContext.current
    var family by remember(scope, language, generation) { mutableStateOf(preferences.getScriptFontFamily(scope)) }
    var weight by remember(scope, language, generation) { mutableIntStateOf(preferences.getScriptFontWeight(scope)) }
    Text(stringResource(when (language) {
        "ja" -> R.string.font_script_japanese
        "ko" -> R.string.font_script_korean
        else -> R.string.font_script_chinese
    }))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScriptFontPreferences.options(language).forEach { option ->
            FilterChip(selected = family == option.id, onClick = {
                family = option.id
                preferences.setScriptFontFamily(scope, family)
                weight = option.nearestWeight(weight)
                preferences.setScriptFontWeight(scope, weight)
            }, label = { Text(FontCatalog.displayName(context, option.id)) })
        }
    }
    val font = FontCatalog.optionForLanguage(family, language)
    Text("${stringResource(R.string.ultimate_font_weight)} $weight")
    Slider(value = font.indexOfNearestWeight(weight).toFloat(), onValueChange = {
        weight = font.weightAt(it.roundToInt())
        preferences.setScriptFontWeight(scope, weight)
    }, valueRange = 0f..(font.weightCount() - 1).coerceAtLeast(1).toFloat(),
        steps = (font.weightCount() - 2).coerceAtLeast(0), enabled = font.weightCount() > 1)
}
