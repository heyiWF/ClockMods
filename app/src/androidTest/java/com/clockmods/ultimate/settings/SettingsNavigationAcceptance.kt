package com.clockmods.ultimate.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.clockmods.R
import java.io.File

/** Exercises real accessibility selection and navigation in the installed settings activity. */
class SettingsNavigationAcceptance : Instrumentation() {
    private var expanded = false
    private var label = "settings"
    private var checks = 0

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        expanded = arguments?.getString("expanded") == "true"
        label = arguments?.getString("label") ?: label
        start()
    }

    private fun verify(value: Boolean, message: String) {
        check(value) { message }
        checks++
    }

    private fun root(): AccessibilityNodeInfo? {
        if (android.os.Build.VERSION.SDK_INT >= 33) uiAutomation.clearCache()
        return uiAutomation.rootInActiveWindow
    }

    private fun nodes(node: AccessibilityNodeInfo? = root()): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) +
            (0 until node.childCount).flatMap { nodes(node.getChild(it)) }

    private fun settle() { waitForIdleSync(); SystemClock.sleep(450) }

    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var current = node
        while (!current.isClickable) current = current.parent ?: error("Missing clickable parent")
        return current
    }

    private fun row(title: String): AccessibilityNodeInfo? {
        var match = nodes().firstOrNull {
            it.text?.toString()?.lineSequence()?.firstOrNull() == title
        } ?: return null
        while (!match.isClickable && !match.isSelected) match = match.parent ?: return null
        return match
    }

    private fun findRow(title: String): AccessibilityNodeInfo {
        repeat(4) {
            row(title)?.let { return it }
            settle()
        }
        repeat(10) {
            row(title)?.let { return it }
            val scroll = nodes().firstOrNull { it.isScrollable }
            scroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            settle()
        }
        error("Missing navigation row: $title; nodes=${nodes().map { "${it.text}|${it.contentDescription}|${it.isClickable}" }}")
    }

    private fun screenshot(name: String): Bitmap {
        val bitmap = checkNotNull(uiAutomation.takeScreenshot())
        val dir = File(targetContext.getExternalFilesDir(null), "settings-navigation").apply { mkdirs() }
        File(dir, "$label-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    override fun onStart() {
        val report = Bundle()
        var activity: Activity? = null
        try {
            uiAutomation.serviceInfo = uiAutomation.serviceInfo
            activity = startActivitySync(UltimateSettingsActivity.createIntent(targetContext)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            settle()
            SystemClock.sleep(1000)
            val context = activity
            val titles = listOf(R.string.ultimate_category_clock_style,
                R.string.ultimate_category_calendar, R.string.ultimate_category_background,
                R.string.ultimate_category_time_date, R.string.ultimate_category_weather,
                R.string.ultimate_category_chime, R.string.ultimate_category_system).map(context::getString)
            verify(nodes().none { it.text?.toString() == context.getString(R.string.ultimate_close_settings) },
                "Redundant close button is still present")
            val initial = screenshot("initial")
            val first = findRow(titles.first())
            verify(first.isSelected == expanded, "Initial selection does not match pane mode")
            val firstBounds = Rect().also(first::getBoundsInScreen)
            for ((index, title) in titles.withIndex()) {
                val target = findRow(title)
                if (!target.isSelected) verify(target.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Cannot click $title")
                settle()
                if (expanded) {
                    verify(findRow(title).isSelected, "$title is not marked selected")
                    verify(nodes().filter {
                        it.isSelected &&
                            Rect().also(it::getBoundsInScreen).right <= firstBounds.right
                    }.size == 1,
                        "Navigation must expose exactly one selected row")
                    if (index == 1) {
                        verify(!findRow(titles.first()).isSelected, "Previous row remains selected")
                        val changed = screenshot("calendar")
                        // Sample away from text: full-row fill must change, not only the icon.
                        val x = firstBounds.right - firstBounds.height() / 2
                        val y = firstBounds.centerY()
                        verify(initial.getPixel(x, y) != changed.getPixel(x, y),
                            "Previous row background did not restore")
                        changed.recycle()
                    }
                } else {
                    val back = nodes().first { it.contentDescription?.toString() == context.getString(R.string.ultimate_back_to_settings) }
                    verify(clickable(back).performAction(AccessibilityNodeInfo.ACTION_CLICK), "Detail back arrow failed")
                    settle()
                    verify(nodes().none { it.isSelected }, "Compact home retains selection")
                }
            }
            screenshot("last").recycle()
            initial.recycle()
            val close = nodes().first { it.contentDescription?.toString() == context.getString(R.string.ultimate_close_settings) }
            verify(clickable(close).performAction(AccessibilityNodeInfo.ACTION_CLICK), "Home back arrow failed")
            settle()
            verify(activity.isFinishing || activity.isDestroyed, "Back arrow did not close settings")
            report.putString("stream", "PASS $label: $checks checks\n")
            finish(Activity.RESULT_OK, report)
        } catch (failure: Throwable) {
            screenshot("failure").recycle()
            report.putString("stream", "FAIL $label: ${failure.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, report)
        } finally {
            activity?.let { runOnMainSync { it.finish() } }
        }
    }
}
