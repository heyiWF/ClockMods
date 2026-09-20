package com.clockmods.ui

import android.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Parses SVG and VectorDrawable path data used by bundled icon assets. */
object SvgPath {
    @JvmStatic
    fun parse(data: String?): Path {
        val path = Path()
        if (data.isNullOrEmpty()) return path

        var index = 0
        var command = '\u0000'
        var currentX = 0f
        var currentY = 0f
        var startX = 0f
        var startY = 0f
        var lastControlX = 0f
        var lastControlY = 0f
        var lastCommand = '\u0000'
        val args = FloatArray(8)

        while (index < data.length) {
            val next = data[index]
            if (isWhitespaceOrComma(next)) {
                index++
                continue
            }
            if (isCommand(next)) {
                command = next
                index++
            } else if (command == '\u0000') {
                index++
                continue
            }

            val argCount = argCountFor(command)
            var parsed = 0
            while (parsed < argCount) {
                index = skipSeparators(data, index)
                if (index >= data.length) break
                val result = readNumber(data, index)
                args[parsed] = result.value
                index = result.end
                parsed++
            }
            if (parsed < argCount) break

            val relative = command.isLowerCase()
            val upper = command.uppercaseChar()
            when (upper) {
                'M' -> {
                    currentX = if (relative) currentX + args[0] else args[0]
                    currentY = if (relative) currentY + args[1] else args[1]
                    path.moveTo(currentX, currentY)
                    startX = currentX
                    startY = currentY
                    command = if (relative) 'l' else 'L'
                }
                'L' -> {
                    currentX = if (relative) currentX + args[0] else args[0]
                    currentY = if (relative) currentY + args[1] else args[1]
                    path.lineTo(currentX, currentY)
                }
                'H' -> {
                    currentX = if (relative) currentX + args[0] else args[0]
                    path.lineTo(currentX, currentY)
                }
                'V' -> {
                    currentY = if (relative) currentY + args[0] else args[0]
                    path.lineTo(currentX, currentY)
                }
                'C' -> {
                    val x1 = if (relative) currentX + args[0] else args[0]
                    val y1 = if (relative) currentY + args[1] else args[1]
                    val x2 = if (relative) currentX + args[2] else args[2]
                    val y2 = if (relative) currentY + args[3] else args[3]
                    val x = if (relative) currentX + args[4] else args[4]
                    val y = if (relative) currentY + args[5] else args[5]
                    path.cubicTo(x1, y1, x2, y2, x, y)
                    lastControlX = x2
                    lastControlY = y2
                    currentX = x
                    currentY = y
                }
                'S' -> {
                    val reflect = lastCommand == 'C' || lastCommand == 'S'
                    val x1 = if (reflect) 2 * currentX - lastControlX else currentX
                    val y1 = if (reflect) 2 * currentY - lastControlY else currentY
                    val x2 = if (relative) currentX + args[0] else args[0]
                    val y2 = if (relative) currentY + args[1] else args[1]
                    val x = if (relative) currentX + args[2] else args[2]
                    val y = if (relative) currentY + args[3] else args[3]
                    path.cubicTo(x1, y1, x2, y2, x, y)
                    lastControlX = x2
                    lastControlY = y2
                    currentX = x
                    currentY = y
                }
                'Q' -> {
                    val x1 = if (relative) currentX + args[0] else args[0]
                    val y1 = if (relative) currentY + args[1] else args[1]
                    val x = if (relative) currentX + args[2] else args[2]
                    val y = if (relative) currentY + args[3] else args[3]
                    path.quadTo(x1, y1, x, y)
                    lastControlX = x1
                    lastControlY = y1
                    currentX = x
                    currentY = y
                }
                'T' -> {
                    val reflect = lastCommand == 'Q' || lastCommand == 'T'
                    val x1 = if (reflect) 2 * currentX - lastControlX else currentX
                    val y1 = if (reflect) 2 * currentY - lastControlY else currentY
                    val x = if (relative) currentX + args[0] else args[0]
                    val y = if (relative) currentY + args[1] else args[1]
                    path.quadTo(x1, y1, x, y)
                    lastControlX = x1
                    lastControlY = y1
                    currentX = x
                    currentY = y
                }
                'A' -> {
                    val x = if (relative) currentX + args[5] else args[5]
                    val y = if (relative) currentY + args[6] else args[6]
                    arcTo(
                        path,
                        currentX,
                        currentY,
                        args[0],
                        args[1],
                        args[2],
                        args[3] != 0f,
                        args[4] != 0f,
                        x,
                        y,
                    )
                    currentX = x
                    currentY = y
                }
                'Z' -> {
                    path.close()
                    currentX = startX
                    currentY = startY
                }
            }
            lastCommand = upper
        }
        return path
    }

    private fun argCountFor(command: Char): Int = when (command.uppercaseChar()) {
        'M', 'L', 'T' -> 2
        'H', 'V' -> 1
        'C' -> 6
        'S', 'Q' -> 4
        'A' -> 7
        else -> 0
    }

    private fun isCommand(character: Char): Boolean =
        character.uppercaseChar() in charArrayOf('M', 'L', 'H', 'V', 'C', 'S', 'Q', 'T', 'A', 'Z')

    private fun isWhitespaceOrComma(character: Char): Boolean =
        character == ' ' || character == ',' || character == '\t' || character == '\n' || character == '\r'

    private fun skipSeparators(data: String, start: Int): Int {
        var index = start
        while (index < data.length && isWhitespaceOrComma(data[index])) index++
        return index
    }

    private class NumberResult(val value: Float, val end: Int)

    private fun readNumber(data: String, start: Int): NumberResult {
        var index = start
        var seenDigit = false
        var seenDot = false
        var seenExponent = false
        if (index < data.length && (data[index] == '+' || data[index] == '-')) index++
        while (index < data.length) {
            val character = data[index]
            when {
                character in '0'..'9' -> {
                    seenDigit = true
                    index++
                }
                character == '.' && !seenDot && !seenExponent -> {
                    seenDot = true
                    index++
                }
                (character == 'e' || character == 'E') && seenDigit && !seenExponent -> {
                    seenExponent = true
                    index++
                    if (index < data.length && (data[index] == '+' || data[index] == '-')) index++
                }
                else -> break
            }
        }
        if (index == start) return NumberResult(0f, index)
        return NumberResult(data.substring(start, index).toFloatOrNull() ?: 0f, index)
    }

    private fun arcTo(
        path: Path,
        x0: Float,
        y0: Float,
        initialRx: Float,
        initialRy: Float,
        rotationDegrees: Float,
        largeArc: Boolean,
        sweep: Boolean,
        x: Float,
        y: Float,
    ) {
        if (initialRx == 0f || initialRy == 0f) {
            path.lineTo(x, y)
            return
        }
        var rx = abs(initialRx.toDouble())
        var ry = abs(initialRy.toDouble())
        val phi = Math.toRadians(rotationDegrees % 360.0)
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val dx = (x0 - x) / 2.0
        val dy = (y0 - y) / 2.0
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy
        var rxSquared = rx * rx
        var rySquared = ry * ry
        val x1pSquared = x1p * x1p
        val y1pSquared = y1p * y1p

        val lambda = x1pSquared / rxSquared + y1pSquared / rySquared
        if (lambda > 1.0) {
            val scale = sqrt(lambda)
            rx *= scale
            ry *= scale
            rxSquared = rx * rx
            rySquared = ry * ry
        }

        val sign = if (largeArc != sweep) 1.0 else -1.0
        val numerator = rxSquared * rySquared - rxSquared * y1pSquared - rySquared * x1pSquared
        val denominator = rxSquared * y1pSquared + rySquared * x1pSquared
        val coefficient = sign * sqrt(max(0.0, numerator / denominator))
        val cxp = coefficient * (rx * y1p / ry)
        val cyp = coefficient * -(ry * x1p / rx)
        val cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2.0
        val cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2.0

        val startAngle = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry)
        var deltaAngle = angle(
            (x1p - cxp) / rx,
            (y1p - cyp) / ry,
            (-x1p - cxp) / rx,
            (-y1p - cyp) / ry,
        )
        if (!sweep && deltaAngle > 0) deltaAngle -= 2 * PI
        else if (sweep && deltaAngle < 0) deltaAngle += 2 * PI

        val segments = ceil(abs(deltaAngle) / (PI / 2.0)).toInt()
        val delta = deltaAngle / segments
        val tangent = (4.0 / 3.0) * tan(delta / 4.0)
        var angle1 = startAngle
        repeat(segments) {
            val angle2 = angle1 + delta
            val cos1 = cos(angle1)
            val sin1 = sin(angle1)
            val cos2 = cos(angle2)
            val sin2 = sin(angle2)
            val end1X = cx + rx * cosPhi * cos1 - ry * sinPhi * sin1
            val end1Y = cy + rx * sinPhi * cos1 + ry * cosPhi * sin1
            val end2X = cx + rx * cosPhi * cos2 - ry * sinPhi * sin2
            val end2Y = cy + rx * sinPhi * cos2 + ry * cosPhi * sin2
            val derivative1X = -rx * cosPhi * sin1 - ry * sinPhi * cos1
            val derivative1Y = -rx * sinPhi * sin1 + ry * cosPhi * cos1
            val derivative2X = -rx * cosPhi * sin2 - ry * sinPhi * cos2
            val derivative2Y = -rx * sinPhi * sin2 + ry * cosPhi * cos2
            path.cubicTo(
                (end1X + tangent * derivative1X).toFloat(),
                (end1Y + tangent * derivative1Y).toFloat(),
                (end2X - tangent * derivative2X).toFloat(),
                (end2Y - tangent * derivative2Y).toFloat(),
                end2X.toFloat(),
                end2Y.toFloat(),
            )
            angle1 = angle2
        }
    }

    private fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        val dot = ux * vx + uy * vy
        val length = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        val value = (dot / length).coerceIn(-1.0, 1.0)
        val result = acos(value)
        return if (ux * vy - uy * vx < 0) -result else result
    }
}
