package com.clockmods.ultimate.clock

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.compose.CalendarThemeCatalog
import com.clockmods.ultimate.compose.CalendarThemeThumbnail
import com.clockmods.ultimate.compose.ClockStyleThumbnail
import com.clockmods.ultimate.compose.previewClockBackground
import com.clockmods.ultimate.settings.FontManagerActivity
import java.io.File

/** Observe actual thumbnail pixels after preference changes while the screen remains open. */
object FontPreviewAcceptance {
    @JvmStatic fun run(test: Instrumentation, western: String, script: String): Int {
        val context = test.targetContext
        val preferences = ClockPreferences(context)
        val style = "typographic.poster"
        val calendar = ClockPreferences.calendarScope("calendar.graphite")
        for (scope in listOf(style, calendar)) {
            preferences.setFontFamily(scope, ClockPreferences.FONT_SYSTEM)
            preferences.setScriptFontFamily(scope, ClockPreferences.FONT_SYSTEM)
        }
        val activity = test.startActivitySync(Intent(context, FontManagerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        lateinit var clockView: ComposeView
        lateinit var calendarView: ComposeView
        try {
            test.runOnMainSync {
                val layout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                clockView = ComposeView(activity)
                calendarView = ComposeView(activity)
                layout.addView(clockView, LinearLayout.LayoutParams(720, 480))
                layout.addView(calendarView, LinearLayout.LayoutParams(720, 480))
                activity.setContentView(layout)
                clockView.setContent {
                    ClockModsTheme {
                        val repository = BackgroundRepository(activity)
                        val appearance = UltimateClockPreferences(activity)
                        ClockStyleThumbnail(style, appearance.getPalette(style),
                            previewClockBackground(repository, appearance, 0), repository, Modifier.fillMaxSize())
                    }
                }
                calendarView.setContent {
                    ClockModsTheme { CalendarThemeThumbnail(CalendarThemeCatalog.resolve("calendar.graphite"), Modifier.fillMaxSize()) }
                }
            }
            SystemClock.sleep(900)
            fun frame(view: ComposeView): Bitmap {
                lateinit var bitmap: Bitmap
                test.runOnMainSync {
                    check(view.width > 0 && view.height > 0)
                    bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                }
                return bitmap
            }
            var beforeClock = frame(clockView)
            var beforeCalendar = frame(calendarView)
            var checks = 0
            for (eastAsian in listOf(false, true)) {
                test.runOnMainSync {
                    for (scope in listOf(style, calendar)) {
                        if (eastAsian) preferences.setScriptFontFamily(scope, script)
                        else preferences.setFontFamily(scope, western)
                    }
                }
                SystemClock.sleep(500)
                val afterClock = frame(clockView)
                val afterCalendar = frame(calendarView)
                check(!beforeClock.sameAs(afterClock)) { "Clock preview did not update ${if (eastAsian) "East Asian" else "Western"} font" }
                check(!beforeCalendar.sameAs(afterCalendar)) { "Calendar preview did not update ${if (eastAsian) "East Asian" else "Western"} font" }
                checks += 2
                beforeClock.recycle(); beforeCalendar.recycle()
                beforeClock = afterClock; beforeCalendar = afterCalendar
            }
            val directory = File(context.getExternalFilesDir(null), "font-import-acceptance")
            for ((name, bitmap) in listOf("clock-font-preview" to beforeClock, "calendar-font-preview" to beforeCalendar)) {
                File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            return checks
        } finally { test.runOnMainSync { activity.finish() } }
    }
}
