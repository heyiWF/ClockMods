package com.clockmods.calendar

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clockmods.background.ClockPreferences
import com.clockmods.ultimate.ComposeMainActivity
import com.clockmods.weather.WeatherModels
import com.clockmods.weather.WeatherRepository
import com.clockmods.weather.DailyForecastRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** adb shell am instrument -w -e calendar true .../WidgetAcceptanceInstrumentation */
object CalendarAcceptance {
    fun run(instrumentation: Instrumentation, weatherEnabled: Boolean = false, dashboardOnly: Boolean = false) {
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)
        val original = prefs.all.toMap()
        val caches = listOf("weather_cache", "daily_forecast_cache").associateWith {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).getString("data", null)
        }
        val results = mutableListOf<String>()
        var activity: Activity? = null
        var resultCode = Activity.RESULT_OK
        val report = Bundle()
        try {
            val out = File(context.getExternalFilesDir(null), "calendar-acceptance").apply { mkdirs() }
            if (weatherEnabled) {
                val now = System.currentTimeMillis()
                WeatherRepository(context).save(WeatherModels.WeatherDisplayData("101010100", "北京", "北京", "晴", "100", "26", now,
                    WeatherModels.WeatherDetail("27", "45", "东风", "2", "0", null, "35", "优")), "manual")
                val day = Calendar.getInstance()
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val entries = (0..2).map { index ->
                    WeatherModels.DailyForecast(format.format(day.time), "18", "28", "100", "晴", "东风", "2", "45")
                        .also { day.add(Calendar.DAY_OF_MONTH, 1) }
                }
                DailyForecastRepository(context).save(WeatherModels.DailyForecastData("101010100", "北京", "北京", now, entries), "manual")
            }
            val themes = when {
                dashboardOnly -> listOf("graphite", "carbon")
                weatherEnabled -> listOf("graphite", "carbon", "agenda")
                else -> listOf("graphite", "carbon", "paper", "poster", "agenda")
            }
            for (id in themes) {
                for (orientation in listOf(1, 2)) {
                    instrumentation.runOnMainSync {
                        activity?.finish()
                        prefs.edit().putString("calendar_theme", "calendar.$id")
                            .putInt("screen_orientation", orientation).putBoolean("weather_enabled", weatherEnabled)
                            .putBoolean("show_seconds", true).putBoolean("show_status_icons", false)
                            .putString("weather_location_mode", "manual").putString("weather_location_id", "101010100").commit()
                    }
                    // Finish the previous configuration before rotating; otherwise its recreation
                    // can satisfy the monitor intended for the next theme's Activity.
                    SystemClock.sleep(300)
                    instrumentation.uiAutomation.setRotation(orientation - 1)
                    SystemClock.sleep(700)
                    val monitor = instrumentation.addMonitor("com.clockmods.ultimate.UltimateMainActivity", null, false)
                    context.startActivity(Intent().setClassName(context.packageName,
                        "com.clockmods.ultimate.UltimateMainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(ComposeMainActivity.EXTRA_DESTINATION, ComposeMainActivity.DESTINATION_CALENDAR))
                    activity = monitor.waitForActivityWithTimeout(10_000) ?: error("Activity did not start")
                    instrumentation.removeMonitor(monitor)
                    await("$id calendar attached") { nodes(instrumentation).any { it.viewIdResourceName == "calendar:calendar.$id" } }
                    await("$id orientation") {
                        context.resources.configuration.orientation == if (orientation == 1) Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                    }
                    SystemClock.sleep(1000)
                    val tree = nodes(instrumentation)
                    val dayNodes = tree.filter { it.viewIdResourceName?.startsWith(if (id == "agenda") "week-day:" else "day:") == true }
                    check(dayNodes.size == if (id == "agenda") 7 else 42) { "$id: day count ${dayNodes.size}" }
                    check(tree.any { it.viewIdResourceName == "dashboard-readings" } == (id in listOf("graphite", "carbon"))) { "$id: clock capability" }
                    if (id == "graphite") {
                        val dashboard = tree.first { it.viewIdResourceName == "dashboard-readings" }
                        val clockCard = Rect().also { dashboard.getChild(0).getBoundsInScreen(it) }
                        val timeNode = tree.first { it.text?.toString()?.matches(Regex("\\d{2}:\\d{2}")) == true }
                        val timeBounds = Rect().also { timeNode.getBoundsInScreen(it) }
                        check(kotlin.math.abs(timeBounds.exactCenterY() - clockCard.exactCenterY()) < clockCard.height() * .08f) {
                            "dashboard clock is not vertically centered: time=$timeBounds card=$clockCard"
                        }
                        val seconds = tree.first { it.text?.toString()?.matches(Regex(":\\d{2}")) == true }.text.toString()
                        await("dashboard clock advances") {
                            nodes(instrumentation).any {
                                it.text?.toString()?.matches(Regex(":\\d{2}")) == true && it.text.toString() != seconds
                            }
                        }
                    }
                    check(tree.any { it.viewIdResourceName == "month-panel" } == (id in listOf("graphite", "carbon", "paper"))) { "$id: month panel capability" }
                    check(tree.none { it.viewIdResourceName == "agenda-card" } || id == "agenda")
                    if (weatherEnabled) check(tree.any { it.text?.contains("26℃") == true }) { "$id: cached weather not displayed" }
                    val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot failed")
                    check((bitmap.width < bitmap.height) == (orientation == 1)) { "Screenshot orientation mismatch" }
                    File(out, "$id-$orientation${if (weatherEnabled) "-weather" else ""}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                    // Select another visible date and ensure selection state reaches accessibility.
                    val target = dayNodes.first { !it.isChecked }
                    val tag = target.viewIdResourceName
                    click(target)
                    await("$id day selection $tag") { nodes(instrumentation).any { it.viewIdResourceName == tag && it.isChecked } }
                    val todayLabel = context.getString(com.clockmods.R.string.calendar_today)
                    val todayButton = nodes(instrumentation).first { it.contentDescription?.toString() == todayLabel || (it.isClickable && it.text?.toString() == todayLabel) }
                    click(todayButton)
                    val initialSelection = dayNodes.first { it.isChecked }.viewIdResourceName
                    await("$id return to today") { nodes(instrumentation).any { it.viewIdResourceName == initialSelection && it.isChecked } }
                    val prefix = if (id == "agenda") "week-day:" else "day:"
                    fun dates() = nodes(instrumentation).mapNotNull { it.viewIdResourceName }.filter { it.startsWith(prefix) }.toSet()
                    val before = dates()
                    val grid = nodes(instrumentation).first { it.viewIdResourceName == if (id == "agenda") "week-strip" else "month-grid" }
                    swipe(instrumentation, grid)
                    await("$id page swipe") { dates() != before }
                    click(nodes(instrumentation).first { it.contentDescription?.toString() == todayLabel || (it.isClickable && it.text?.toString() == todayLabel) })
                    await("$id today after swipe") { dates() == before }
                    if (id != "poster") {
                        click(nodes(instrumentation).first { it.viewIdResourceName == "month-title" })
                        await("month picker") { nodes(instrumentation).any { it.text?.toString() == context.getString(com.clockmods.R.string.calendar_month_picker_title) } }
                        click(nodes(instrumentation).first { it.text?.toString() == context.getString(com.clockmods.R.string.ultimate_cancel) })
                    }
                    results += "$id/$orientation: layout, date count, selection, today, swipe, picker and screenshot passed"
                }
            }
            File(out, if (weatherEnabled) "weather-results.txt" else "results.txt").writeText(results.joinToString("\n"))
            report.putString("stream", results.joinToString("\n"))
        } catch (error: Throwable) {
            resultCode = Activity.RESULT_CANCELED
            report.putString("stream", error.stackTraceToString() + "\n" + nodes(instrumentation).filter { it.viewIdResourceName != null }
                .joinToString("\n") { "${it.viewIdResourceName}: checked=${it.isChecked}, selected=${it.isSelected}, ${it.text}" })
        } finally {
            instrumentation.runOnMainSync {
                activity?.finish()
                val editor = prefs.edit().clear()
                original.forEach { (key, value) -> when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                } }
                editor.commit()
                caches.forEach { (name, value) -> context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().putString("data", value).commit() }
            }
            instrumentation.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
        }
        instrumentation.finish(resultCode, report)
    }

    private fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) { check(SystemClock.uptimeMillis() < deadline) { "Timed out: $label" }; SystemClock.sleep(200) }
    }

    private fun click(node: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) target = target.parent
        check(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "No clickable ancestor: ${node.text ?: node.contentDescription}" }
    }

    private fun swipe(instrumentation: Instrumentation, node: AccessibilityNodeInfo) {
        val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
        val start = SystemClock.uptimeMillis()
        for (step in 0..12) {
            val action = when (step) { 0 -> MotionEvent.ACTION_DOWN; 12 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
            val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action,
                bounds.left + bounds.width() * (.85f - .7f * step / 12), bounds.exactCenterY(), 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            check(instrumentation.uiAutomation.injectInputEvent(event, true))
            event.recycle()
            SystemClock.sleep(20)
        }
    }

    private fun nodes(instrumentation: Instrumentation): List<AccessibilityNodeInfo> {
        instrumentation.uiAutomation.clearCache()
        val output = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            output += node
            repeat(node.childCount) { node.getChild(it)?.let(::visit) }
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        return output
    }
}
