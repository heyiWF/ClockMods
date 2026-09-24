package com.clockmods.ultimate.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.clockmods.background.ClockPreferences
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val cyan = Color(0xFF4DE4F2)
private val violet = Color(0xFFA78BFA)
private val rose = Color(0xFFFF7FB0)

/** Luminous cues that let the active clock face show through. All motion stays inside five seconds. */
internal fun DrawScope.drawAlternativeChimeEffect(animation: String, progress: Float, fade: Float) {
    when (animation) {
        ClockPreferences.CHIME_AURORA -> drawAurora(progress, fade)
        ClockPreferences.CHIME_ORBIT -> drawOrbit(progress, fade)
        ClockPreferences.CHIME_COMET -> drawComet(progress, fade)
    }
}

private fun DrawScope.drawAurora(progress: Float, fade: Float) {
    val enter = chimeEase((progress / .27f).coerceIn(0f, 1f))
    val glow = enter * fade
    drawRect(Color(0xFF070D20).copy(alpha = .38f * glow))
    val scale = min(size.width, size.height)
    val drift = chimeEase(progress) * size.width * .22f
    listOf(
        Triple(cyan, Offset(size.width * .22f + drift, size.height * .42f), scale * .78f),
        Triple(violet, Offset(size.width * .76f - drift * .6f, size.height * .57f), scale * .83f),
        Triple(rose, Offset(size.width * .52f + drift * .35f, size.height * .25f), scale * .56f),
    ).forEach { (color, origin, radius) ->
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = .43f * glow), color.copy(alpha = .14f * glow),
                    Color.Transparent),
                center = origin,
                radius = radius,
            ),
            radius = radius,
            center = origin,
        )
    }
    repeat(22) { index ->
        val x = size.width * ((index * .6180339f + .11f) % 1f)
        val y = size.height * ((index * .381966f + .19f) % 1f)
        val rise = chimeEase((progress * 1.3f - index * .019f).coerceIn(0f, 1f))
        drawCircle(
            color = if (index % 2 == 0) cyan else rose,
            radius = (1.4f + index % 3) * density,
            center = Offset(x, y - rise * size.height * .12f),
            alpha = (.25f + .35f * rise) * glow,
        )
    }
}

private fun DrawScope.drawOrbit(progress: Float, fade: Float) {
    val enter = chimeEase((progress / .24f).coerceIn(0f, 1f))
    val strength = enter * fade
    val unit = min(size.width, size.height)
    val radius = unit * (.34f + .16f * enter)
    val stroke = maxOf(1.4f * density, unit * .002f)
    drawCircle(cyan.copy(alpha = .09f * strength), radius * 1.22f, center)
    repeat(3) { index ->
        val ringRadius = radius * (1f + index * .18f)
        drawCircle(
            color = listOf(cyan, violet, rose)[index].copy(alpha = (.54f - index * .10f) * strength),
            radius = ringRadius,
            center = center,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = listOf(rose, cyan, violet)[index].copy(alpha = .86f * strength),
            startAngle = -105f + chimeEase(progress) * (185f + index * 76f) + index * 115f,
            sweepAngle = 42f + index * 12f,
            useCenter = false,
            topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
            size = Size(ringRadius * 2f, ringRadius * 2f),
            style = Stroke(width = stroke * (2.6f - index * .35f)),
        )
    }
    repeat(12) { index ->
        val angle = (index * 30.0 + 105.0 + chimeEase(progress) * 100.0) * PI / 180.0
        val distance = radius * (1f + (index % 3) * .18f)
        val point = Offset(
            center.x + cos(angle).toFloat() * distance,
            center.y + sin(angle).toFloat() * distance,
        )
        drawCircle(
            color = if (index % 2 == 0) cyan else violet,
            radius = (2.5f + index % 3) * density,
            center = point,
            alpha = strength,
        )
    }
}

private fun DrawScope.drawComet(progress: Float, fade: Float) {
    val enter = chimeEase((progress / .20f).coerceIn(0f, 1f))
    val strength = enter * fade
    val travel = progress * progress * (3f - 2f * progress)
    val diagonal = size.width + size.height
    repeat(9) { index ->
        val lane = (index + .5f) / 9f
        val x = -size.width * .25f + travel * diagonal * (1.06f + index * .012f) +
            index * size.width * .057f
        val y = size.height * lane - travel * size.height * .42f
        val head = Offset(x, y)
        val tail = Offset(x - size.width * (.13f + index % 3 * .035f), y + size.height * .11f)
        drawLine(
            brush = Brush.linearGradient(
                0f to Color.Transparent,
                .7f to (if (index % 2 == 0) cyan else violet).copy(alpha = .36f * strength),
                1f to Color.White.copy(alpha = .85f * strength),
                start = tail,
                end = head,
            ),
            start = tail,
            end = head,
            strokeWidth = (2.1f + index % 3) * density,
        )
        drawCircle(
            color = Color.White,
            radius = (2f + index % 2) * density,
            center = head,
            alpha = .8f * strength,
        )
    }
    val sweep = center.x - size.width * .6f + travel * size.width * 1.2f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(rose.copy(alpha = .18f * strength), Color.Transparent),
            center = Offset(sweep, center.y),
            radius = min(size.width, size.height) * .43f,
        ),
        radius = min(size.width, size.height) * .43f,
        center = Offset(sweep, center.y),
    )
}

/** Cubic ease-out keeps the entrance lively without an abrupt stop. */
internal fun chimeEase(value: Float): Float {
    val remaining = 1f - value.coerceIn(0f, 1f)
    return 1f - remaining * remaining * remaining
}
