package com.clockmods.widget.config

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.clockmods.R
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.render.WidgetRemoteViewsFactory
import com.clockmods.widget.render.WidgetSizeClassResolver
import com.clockmods.widget.update.WidgetUpdateCoordinator
import kotlin.math.roundToInt

/** Compose host for the real RemoteViews preview used by the launcher. */
@Composable
internal fun WidgetPreview(
    config: WidgetConfig,
    modifier: Modifier = Modifier,
) {
    val desiredWidth: Dp
    val desiredHeight: Dp
    when (config.kind) {
        WidgetKind.DIGITAL -> {
            desiredWidth = 340.dp
            desiredHeight = 96.dp
        }
        WidgetKind.WEATHER -> {
            desiredWidth = 340.dp
            desiredHeight = 156.dp
        }
        else -> {
            desiredWidth = 176.dp
            desiredHeight = 176.dp
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val previewWidth = minOf(maxWidth, desiredWidth)
        val previewHeight = desiredHeight
        AndroidView(
            factory = { context ->
                WidgetPreviewView(context).apply { id = R.id.widget_config_preview }
            },
            update = { preview ->
                preview.show(config, previewWidth.value, previewHeight.value)
            },
            modifier = Modifier.width(previewWidth).height(previewHeight),
        )
    }
}

/** Bridges RemoteViews into Compose; no configuration controls live in this View. */
class WidgetPreviewView(context: Context) : FrameLayout(context) {
    private var generation = 0
    private var lastConfig: WidgetConfig? = null
    private var lastWidthDp = Float.NaN
    private var lastHeightDp = Float.NaN

    fun show(config: WidgetConfig) {
        val density = resources.displayMetrics.density
        show(
            config,
            measuredPx(horizontal = true) / density,
            measuredPx(horizontal = false) / density,
        )
    }

    internal fun show(config: WidgetConfig, widthDp: Float, heightDp: Float) {
        if (lastConfig === config && lastWidthDp == widthDp && lastHeightDp == heightDp) return
        lastConfig = config
        lastWidthDp = widthDp
        lastHeightDp = heightDp
        val current = ++generation
        val inflationContext = context.applicationContext
        WidgetUpdateCoordinator.execute {
            val views = WidgetRemoteViewsFactory.create(
                context,
                config,
                WidgetSizeClassResolver.resolve(widthDp, heightDp),
            )
            post {
                if (current != generation) return@post
                removeAllViews()
                addView(
                    views.apply(inflationContext, this),
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                )
                disableClicks(this)
            }
        }
    }

    private fun measuredPx(horizontal: Boolean): Int {
        var size = if (horizontal) width else height
        if (size > 0) return size
        layoutParams?.let { params ->
            size = if (horizontal) params.width else params.height
            if (size > 0) return size
        }
        return ((if (horizontal) 340 else 156) * resources.displayMetrics.density).roundToInt()
    }

    private fun disableClicks(view: View) {
        view.setOnClickListener(null)
        view.isClickable = false
        if (view is ViewGroup) {
            repeat(view.childCount) { disableClicks(view.getChildAt(it)) }
        }
    }

    override fun onDetachedFromWindow() {
        generation++
        lastConfig = null
        super.onDetachedFromWindow()
    }
}
