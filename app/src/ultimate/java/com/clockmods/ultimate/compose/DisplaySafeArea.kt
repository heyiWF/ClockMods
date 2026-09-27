package com.clockmods.ultimate.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max

/** Insets in physical pixels, since the clock renderers and status capsule use pixel bounds. */
internal data class DisplaySafeArea(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    fun contentLeft(width: Int) = left.coerceIn(0, width)
    fun contentTop(height: Int) = top.coerceIn(0, height)
    fun contentRight(width: Int) = (width - right).coerceAtLeast(contentLeft(width))
    fun contentBottom(height: Int) = (height - bottom).coerceAtLeast(contentTop(height))
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun displaySafeArea(enabled: Boolean, immersive: Boolean): DisplaySafeArea {
    if (!enabled || !immersive) return DisplaySafeArea()
    val density = LocalDensity.current
    val cutout = WindowInsets.displayCutout
    val status = WindowInsets.statusBarsIgnoringVisibility
    val navigation = WindowInsets.navigationBarsIgnoringVisibility
    return DisplaySafeArea(
        left = max(cutout.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr),
            status.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr)),
        top = max(cutout.getTop(density), status.getTop(density)),
        right = max(cutout.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr),
            status.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr)),
        bottom = max(cutout.getBottom(density), navigation.getBottom(density)),
    )
}
