package com.clockmods.ui.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.clockmods.background.ClockPreferences
import com.clockmods.background.UserFontStore
import com.clockmods.ui.ClockTypefaceResolver
import com.clockmods.ui.ClockTimeText

/** Measure the actual resolved face, including imported fonts and Compose's font synthesis. */
@Composable
fun TimeText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val actual = timeTextStyle(style, fontSize, fontWeight)
    val shift = if (':' in text) {
        val paint = rememberTimePaint(actual)
        remember(paint) {
            if (paint.ascent() == 0f) 0f else ClockTimeText.colonBaselineOffset(paint) / paint.ascent()
        }
    } else 0f
    val aligned = remember(text, shift) {
        buildAnnotatedString {
            append(text)
            text.forEachIndexed { index, character ->
                if (character == ':') {
                    addStyle(SpanStyle(baselineShift = BaselineShift(shift)), index, index + 1)
                }
            }
        }
    }
    Text(aligned, modifier, color = color, maxLines = maxLines, textAlign = textAlign, style = actual)
}

/** Draw an independently laid-out separator without changing its line height. */
@Composable
internal fun TimeColon(modifier: Modifier, fontSize: TextUnit, fontWeight: FontWeight) {
    val style = timeTextStyle(LocalTextStyle.current, fontSize, fontWeight)
    val paint = rememberTimePaint(style)
    val offset = remember(paint) { ClockTimeText.colonBaselineOffset(paint) }
    Text(":", modifier.graphicsLayer { translationY = offset }, maxLines = 1, style = style)
}

@Composable
private fun timeTextStyle(style: TextStyle, fontSize: TextUnit, fontWeight: FontWeight?): TextStyle {
    val context = LocalContext.current
    val preferences = remember(context) { ClockPreferences(context) }
    val family = preferences.getRenderFontFamily()
    val revision = UserFontStore.revision()
    val weight = fontWeight ?: style.fontWeight ?: FontWeight(if (preferences.isBoldText()) 700 else 400)
    val face = remember(context, family, weight, revision) { ClockTypefaceResolver.resolve(context, family, weight.weight) }
    return LocalTextStyle.current.merge(style).merge(TextStyle(
        fontFamily = style.fontFamily ?: FontFamily(face), fontSize = fontSize, fontWeight = weight,
    ))
}

@Composable
private fun rememberTimePaint(style: TextStyle): Paint {
    val resolved by LocalFontFamilyResolver.current.resolve(style.fontFamily, checkNotNull(style.fontWeight),
        style.fontStyle ?: FontStyle.Normal, style.fontSynthesis ?: FontSynthesis.All)
    val density = LocalDensity.current
    val size = with(density) { (if (style.fontSize == TextUnit.Unspecified) 16.sp else style.fontSize).toPx() }
    return remember(resolved, size) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = resolved as Typeface
            textSize = size
        }
    }
}
