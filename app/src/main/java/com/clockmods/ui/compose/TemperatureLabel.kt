package com.clockmods.ui.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import com.clockmods.ui.TemperatureText

@Composable
fun TemperatureLabel(text: String, modifier: Modifier, color: Color, style: TextStyle, overflow: TextOverflow) {
    val resolved by LocalFontFamilyResolver.current.resolve(style.fontFamily,
        style.fontWeight ?: FontWeight.Normal, style.fontStyle ?: FontStyle.Normal,
        style.fontSynthesis ?: FontSynthesis.All)
    val size = with(LocalDensity.current) { style.fontSize.toPx() }
    val formatted = remember(text, resolved, size, style.fontSize) {
        buildAnnotatedString {
            append(text)
            val unit = listOf("℃", "℉", "°C", "°F").firstOrNull { text.endsWith(it) }
            if (unit != null) {
                val digits = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = resolved as Typeface; textSize = size }
                val scale = TemperatureText.unitScale(digits, unit)
                val small = Paint(digits).apply { textSize = size * scale }
                val shift = if (small.ascent() == 0f) 0f else TemperatureText.topOffset(digits, small, unit) / small.ascent()
                addStyle(SpanStyle(fontSize = style.fontSize * scale, baselineShift = BaselineShift(shift)), text.length - unit.length, text.length)
            }
        }
    }
    Text(formatted, modifier, color = color, style = style, maxLines = 1, overflow = overflow)
}
