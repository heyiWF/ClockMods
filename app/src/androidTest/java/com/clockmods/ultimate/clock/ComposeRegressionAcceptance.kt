package com.clockmods.ultimate.clock

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockState
import com.clockmods.weather.QWeatherClient
import com.clockmods.weather.QWeatherConfig
import java.io.File
import java.util.Locale

/** Real font metrics and TLS requests; run only in an isolated audit package. */
class ComposeRegressionAcceptance : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val report = Bundle()
        try {
            check(targetContext.packageName.contains("predictivecompose"))
            val style = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_DUAL_BLOCKS)!!
            val directory = File(targetContext.getExternalFilesDir(null), "compose-regression").apply { mkdirs() }
            var checks = 0
            for ((width, height) in listOf(2027 to 1139, 900 to 1600)) {
                for (dualLine in listOf(false, true)) {
                    for (scale in listOf(.5f, 1f)) {
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val bounds = mutableListOf<Pair<String, Float>>()
                        val canvas = object : Canvas(bitmap) {
                            override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                                if (text.contains("年")) bounds += text to (y + paint.fontMetrics.ascent)
                                super.drawText(text, x, y, paint)
                            }
                        }
                        val state = ClockState.builder(System.currentTimeMillis())
                            .dateText("2027 年 1 月 28 日 星期四 | 丙午[马]年腊月廿一")
                            .locale(Locale.SIMPLIFIED_CHINESE).dateLunarDualLine(dualLine)
                            .dateScale(scale).timeScale(1f).supportingScale(1f).build()
                        style.getRenderer().render(canvas,
                            ClockRenderContext(0f, 0f, width.toFloat(), height.toFloat(), 2f, 2f, state.getTimeMillis()),
                            state, style.getThemeTokens())
                        check(bounds.isNotEmpty()) { "Date was not drawn" }
                        check(bounds.all { it.second >= height * .037f + 19f }) { "Date escaped card: $bounds" }
                        if (dualLine && scale == 1f) File(directory, "dual-$width.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap.recycle()
                        checks++
                    }
                }
            }
            check(QWeatherConfig.isConfigured()) { "Weather credentials missing from audit build" }
            val weather = QWeatherClient(targetContext, QWeatherConfig.apiHost())
                .fetchLocation("101010100", "北京", "北京")
            report.putString("weather", "HTTPS request succeeded after automatic time was restored")
            report.putString("result", "PASS $checks date layouts and live weather")
            finish(Activity.RESULT_OK, report)
        } catch (error: Throwable) {
            report.putString("failure", android.util.Log.getStackTraceString(error))
            finish(Activity.RESULT_CANCELED, report)
        }
    }
}
