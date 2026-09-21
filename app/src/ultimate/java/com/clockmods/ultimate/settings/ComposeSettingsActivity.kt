package com.clockmods.ultimate.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clockmods.LocaleManager
import com.clockmods.background.ClockPreferences
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleMetadata
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles

/** Compose-native settings surface; the class name preserves the original manifest/API contract. */
open class UltimateSettingsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase) ?: newBase)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setLocale(LocaleManager.resolveLocale(this))
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockModsTheme {
                SettingsScreen(
                    initialPage = resolvePageId(
                        intent.getStringExtra(EXTRA_PAGE_ID),
                        intent.getStringExtra(LEGACY_EXTRA_PAGE_ID),
                    ),
                    onDone = ::finish,
                    onLanguageChanged = ::recreate,
                )
            }
        }
    }

    companion object {
        const val ACTION_OPEN = "com.clockmods.ultimate.action.OPEN_SETTINGS"
        const val ACTION_OPEN_SUBPAGE = "com.clockmods.ultimate.action.OPEN_SETTINGS_SUBPAGE"
        const val EXTRA_SETTINGS_CHANGED = "com.clockmods.ultimate.settings.extra.CHANGED"
        const val EXTRA_STYLE_ID = "com.clockmods.ultimate.settings.extra.STYLE_ID"
        const val EXTRA_CHANGE_SOURCE = "com.clockmods.ultimate.settings.extra.CHANGE_SOURCE"
        const val EXTRA_PAGE_ID = "page"
        const val LEGACY_EXTRA_PAGE_ID = "com.clockmods.ultimate.settings.extra.PAGE_ID"

        @JvmField val STYLE_PREFERENCES = UltimateClockPreferences.PREFERENCES_NAME
        @JvmField val KEY_STYLE_ID = UltimateClockPreferences.KEY_STYLE_ID
        @JvmField val KEY_SECOND_MOTION = UltimateClockPreferences.KEY_SECOND_MOTION
        const val STYLE_GLASS_ATELIER = UltimateClockStyles.STYLE_GLASS_ATELIER
        const val STYLE_PRO_CLASSIC = UltimateClockStyles.STYLE_PRO_CLASSIC
        const val STYLE_NOIR_INSTRUMENT = UltimateClockStyles.STYLE_NOIR_INSTRUMENT
        const val STYLE_PAPER_STATION = UltimateClockStyles.STYLE_PAPER_STATION
        const val STYLE_ORBIT_NEON = UltimateClockStyles.STYLE_ORBIT_NEON
        const val STYLE_DIGITAL_GRID = UltimateClockStyles.STYLE_DIGITAL_GRID
        const val STYLE_TYPOGRAPHIC = UltimateClockStyles.STYLE_TYPOGRAPHIC
        const val MOTION_SMOOTH = "smooth"
        const val MOTION_TICK = "tick"
        const val MOTION_OFF = "off"

        @JvmStatic
        fun createIntent(context: Context): Intent =
            Intent(context, UltimateSettingsActivity::class.java).setAction(ACTION_OPEN)

        @JvmStatic
        fun createSubpageIntent(context: Context, pageId: String): Intent =
            Intent(context, UltimateSubSettingsActivity::class.java)
                .setAction(ACTION_OPEN_SUBPAGE)
                .putExtra(EXTRA_PAGE_ID, pageId)
                .putExtra(LEGACY_EXTRA_PAGE_ID, pageId)

        @JvmStatic
        fun createDefaultSubpageIntent(context: Context): Intent =
            createSubpageIntent(context, "style")

        @JvmStatic
        fun resolvePageId(current: String?, legacy: String?): String? =
            current?.takeIf(String::isNotBlank) ?: legacy?.takeIf(String::isNotBlank)

        @JvmStatic
        fun styleGalleryTargetScrollX(
            previousScrollX: Int,
            cardLeft: Int,
            cardWidth: Int,
            viewportWidth: Int,
        ): Int = if (previousScrollX >= 0) previousScrollX else
            maxOf(0, cardLeft - maxOf(0, viewportWidth - cardWidth) / 2)

        @JvmStatic
        fun clockPreviewViewport(windowWidth: Int, windowHeight: Int, orientation: Int): IntArray {
            var width = maxOf(1, windowWidth)
            var height = maxOf(1, windowHeight)
            val portrait = orientation == ClockPreferences.ORIENTATION_PORTRAIT
            val landscape = orientation == ClockPreferences.ORIENTATION_LANDSCAPE
            if ((portrait && width > height) || (landscape && width < height)) {
                val swap = width
                width = height
                height = swap
            }
            return intArrayOf(width, height)
        }

        @JvmStatic
        fun fitClockPreviewSize(
            maxWidth: Int,
            maxHeight: Int,
            viewportWidth: Int,
            viewportHeight: Int,
        ): IntArray {
            val widthLimit = maxOf(1, maxWidth)
            val heightLimit = maxOf(1, maxHeight)
            val width = maxOf(1, viewportWidth)
            val height = maxOf(1, viewportHeight)
            val widthAtHeightLimit = maxOf(1, kotlin.math.round(heightLimit.toDouble() * width / height).toInt())
            if (widthAtHeightLimit <= widthLimit) return intArrayOf(widthAtHeightLimit, heightLimit)
            val heightAtWidthLimit = maxOf(1, kotlin.math.round(widthLimit.toDouble() * height / width).toInt())
            return intArrayOf(widthLimit, minOf(heightLimit, heightAtWidthLimit))
        }

        @JvmStatic
        fun shouldShowSecondMotionControls(style: ClockStyle?): Boolean =
            style != null && style.getMetadata().getKind() != ClockStyleMetadata.Kind.DIGITAL &&
                style.getMetadata().getCapabilities().supports(ClockStyleCapabilities.Capability.SECONDS)

        /**
         * Window width (dp) at which the settings surface switches to the side-by-side rail + page
         * layout. Must stay in sync with the breakpoint used to pick the layout in `SettingsScreen`.
         */
        const val TWO_PANE_MIN_WIDTH_DP = 840

        @JvmStatic
        fun isTwoPaneWidth(widthDp: Int): Boolean = widthDp >= TWO_PANE_MIN_WIDTH_DP

        /**
         * True when a back press should leave settings outright. In two-pane mode the page shows
         * next to the rail, so it is not a deeper screen: one back press always exits. In compact
         * mode a selected page is a real second level, so the first back returns to the rail.
         */
        @JvmStatic
        fun shouldCloseSettingsOnBack(twoPane: Boolean, pageSelected: Boolean): Boolean =
            twoPane || !pageSelected
    }
}

/** Retains the detail Activity component used by existing widget PendingIntents. */
class UltimateSubSettingsActivity : UltimateSettingsActivity()

/** Source-compatible name for callers that explicitly select the Compose settings surface. */
typealias ComposeSettingsActivity = UltimateSettingsActivity
