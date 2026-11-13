package com.clockmods.pro.alarm

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.ui.compose.TemperatureLabel

/** Actual Compose text rendering catches span ordering that Paint-only geometry cannot. */
object TemperatureSamples {
    @JvmStatic fun show(activity: AlarmUi) {
        activity.setContent {
            Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp, 72.dp)) {
                listOf(FontFamily.SansSerif, FontFamily.Serif, FontFamily.Monospace).forEachIndexed { index, family ->
                    TemperatureLabel("26℃", Modifier, Color.White,
                        TextStyle(fontSize = 56.sp, fontFamily = family,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal), TextOverflow.Clip)
                }
            }
        }
    }
}
