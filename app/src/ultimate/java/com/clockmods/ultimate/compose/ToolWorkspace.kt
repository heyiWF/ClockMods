package com.clockmods.ultimate.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R
import com.clockmods.ui.compose.TimeText

/** Width and height constraints, rather than physical pixels, decide the two-pane layout. */
@Composable
internal fun ToolWorkspace(
    modifier: Modifier,
    time: String,
    phase: String? = null,
    expandedDetails: Boolean = false,
    rollDigits: Boolean = false,
    details: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(24.dp)) {
        val wide = maxWidth >= 472.dp && maxWidth > maxHeight
        val face: @Composable (Modifier) -> Unit = { faceModifier ->
            Column(faceModifier) {
                if (phase != null) Text(phase, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    val size = (maxWidth.value / (time.length * .59f) / LocalDensity.current.fontScale)
                        .coerceAtMost(96f).coerceAtLeast(24f)
                    if (rollDigits) RollingTime(time, size) else
                        TimeText(time, Modifier.fillMaxWidth(), fontSize = size.sp,
                            fontWeight = FontWeight.Light, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        if (wide) {
            Row(Modifier.widthIn(max = 1100.dp).fillMaxSize().align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                face(Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    details()
                    actions()
                }
            }
        } else {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                face(Modifier.weight(1f).fillMaxWidth())
                Column(if (expandedDetails) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()) {
                    details()
                    actions()
                }
            }
        }
    }
}

@Composable
internal fun ToolActions(running: Boolean, enabled: Boolean = true, showSecondary: Boolean = true,
    onToggle: () -> Unit, onReset: () -> Unit, extra: ImageVector? = null,
    extraLabel: String = "", onExtra: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 20.dp else 36.dp, tween(if (pressed) 150 else 220), label = "button shape")
    val background by animateColorAsState(if (running) Color(0xFFF4C95D) else MaterialTheme.colorScheme.primary, tween(240), label = "running color")
    val foreground by animateColorAsState(if (running) Color(0xFF302500) else MaterialTheme.colorScheme.onPrimary, tween(240), label = "running text")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(showSecondary, enter = expandHorizontally(tween(300)) + fadeIn(tween(200)), exit = shrinkHorizontally(tween(240)) + fadeOut(tween(180))) {
            ToolIconButton(Icons.Default.RestartAlt, stringResource(R.string.timer_reset), onReset)
        }
        Button(onClick = onToggle, enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = 72.dp), contentPadding = PaddingValues(12.dp),
            interactionSource = interaction, shape = RoundedCornerShape(corner),
            colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = foreground)) {
            Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (running) R.string.timer_pause else R.string.timer_start),
                style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
        if (extra != null) AnimatedVisibility(showSecondary,
            enter = expandHorizontally(tween(300)) + fadeIn(tween(200)), exit = shrinkHorizontally(tween(240)) + fadeOut(tween(180))) {
            ToolIconButton(extra, extraLabel, onExtra)
        }
    }
}

@Composable
private fun ToolIconButton(icon: ImageVector, label: String, action: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 20.dp else 36.dp, tween(180), label = "secondary shape")
    FilledTonalIconButton(action, Modifier.size(width = 64.dp, height = 72.dp),
        shape = RoundedCornerShape(corner), interactionSource = interaction) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun StopwatchLaps(laps: List<Long>, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(stringResource(R.string.stopwatch_lap), Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.stopwatch_split), Modifier.weight(1f), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.stopwatch_total), Modifier.weight(1f), textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(laps, key = { index, _ -> laps.size - index }) { index, lap ->
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { entered = true }
                val entrance by animateFloatAsState(if (entered) 1f else 0f, tween(260), label = "lap entrance")
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).animateItem()
                    .graphicsLayer { alpha = entrance; translationY = -24.dp.toPx() * (1f - entrance) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text((laps.size - index).toString(), Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)
                    TimeText(formatStopwatch((lap - (laps.getOrNull(index + 1) ?: 0)).coerceAtLeast(0)),
                        Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    TimeText(formatStopwatch(lap), Modifier.weight(1f), textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RollingTime(time: String, size: Float) {
    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = time },
        verticalAlignment = Alignment.CenterVertically) {
        time.forEach { character ->
            if (character.isDigit()) AnimatedContent(character,
                modifier = Modifier.clipToBounds().clearAndSetSemantics {},
                transitionSpec = {
                    (slideInVertically(tween(240)) { it } + fadeIn(tween(160))) togetherWith
                        (slideOutVertically(tween(240)) { -it } + fadeOut(tween(160)))
                }, label = "timer digit") { digit ->
                TimeText(digit.toString(), fontSize = size.sp, fontWeight = FontWeight.Light, maxLines = 1)
            } else TimeText(character.toString(), Modifier.clearAndSetSemantics {},
                fontSize = size.sp, fontWeight = FontWeight.Light, maxLines = 1)
        }
    }
}
