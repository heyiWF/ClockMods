package com.clockmods.ultimate.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.toArgb
import com.clockmods.ui.StatusIconStyle
import com.clockmods.ui.StatusSymbolRenderer

/** Uses the original vectors for the default look and the variable font for custom axes. */
@Composable
internal fun StatusSymbolIcon(
    codePoint: Int,
    style: StatusIconStyle,
    color: Color,
    modifier: Modifier,
    fallbackDrawable: Int? = null,
    fallbackVector: ImageVector? = null,
) {
    val context = LocalContext.current
    val customAvailable = remember(context, style, codePoint) {
        !style.isDefault && StatusSymbolRenderer.isAvailable(context, style, codePoint)
    }
    if (!customAvailable) {
        if (fallbackDrawable != null) {
            Icon(painterResource(fallbackDrawable), null, modifier, tint = color)
        } else if (fallbackVector != null) {
            Icon(fallbackVector, null, modifier, tint = color)
        }
        return
    }
    val paint = remember(color) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
    }
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            StatusSymbolRenderer.draw(
                canvas.nativeCanvas,
                context,
                codePoint,
                style,
                0f,
                0f,
                size.minDimension,
                paint,
            )
        }
    }
}
