package com.clockmods.ultimate.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.background.AutoStartManager
import com.clockmods.background.ClockPreferences
import com.clockmods.ultimate.AntiBurnPreferences
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.WorldClockRepository

internal enum class SettingsPage(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val icon: ImageVector,
) {
    STYLE(
        R.string.ultimate_category_clock_style,
        R.string.ultimate_category_clock_style_summary,
        Icons.Default.Palette,
    ),
    BACKGROUND(
        R.string.ultimate_category_background,
        R.string.ultimate_category_background_summary,
        Icons.Default.Wallpaper,
    ),
    TIME_DATE(
        R.string.ultimate_category_time_date,
        R.string.ultimate_category_time_date_summary,
        Icons.Default.Schedule,
    ),
    WEATHER(
        R.string.ultimate_category_weather,
        R.string.ultimate_category_weather_summary,
        Icons.Default.Cloud,
    ),
    CALENDAR(
        R.string.ultimate_category_calendar,
        R.string.ultimate_category_calendar_summary,
        Icons.Default.CalendarMonth,
    ),
    CHIME(
        R.string.ultimate_category_chime,
        R.string.ultimate_category_chime_summary,
        Icons.Default.Notifications,
    ),
    SYSTEM(
        R.string.ultimate_category_system,
        R.string.ultimate_category_system_summary,
        Icons.Default.Language,
    ),
}

private fun pageFromId(pageId: String?): SettingsPage? = when (pageId) {
    "style", "clock_style" -> SettingsPage.STYLE
    "background", "display" -> SettingsPage.BACKGROUND
    "time", "time_date" -> SettingsPage.TIME_DATE
    "weather" -> SettingsPage.WEATHER
    "calendar", "calendar_style" -> SettingsPage.CALENDAR
    "chime" -> SettingsPage.CHIME
    "system", "language" -> SettingsPage.SYSTEM
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    initialPage: String?,
    onDone: () -> Unit,
    onLanguageChanged: () -> Unit,
) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(pageFromId(initialPage)) }
    var generation by rememberSaveable { mutableIntStateOf(0) }
    var confirmRestore by remember { mutableStateOf(false) }
    BackHandler {
        if (page == null) onDone() else page = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        page?.let { stringResource(it.title) }
                            ?: stringResource(R.string.ultimate_settings_pane_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (page == null) onDone() else page = null }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ultimate_back_to_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val expanded = maxWidth >= 840.dp
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    SettingsHome(
                        selected = page ?: SettingsPage.STYLE,
                        onSelect = { page = it },
                        onRestore = { confirmRestore = true },
                        onDone = onDone,
                        modifier = Modifier.width(340.dp),
                    )
                    VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
                    SettingsPageContent(
                        page = page ?: SettingsPage.STYLE,
                        generation = generation,
                        onLanguageChanged = onLanguageChanged,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (page == null) {
                SettingsHome(
                    selected = null,
                    onSelect = { page = it },
                    onRestore = { confirmRestore = true },
                    onDone = onDone,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SettingsPageContent(
                    page = page!!,
                    generation = generation,
                    onLanguageChanged = onLanguageChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.ultimate_restore_default)) },
            text = { Text(stringResource(R.string.reset_default)) },
            confirmButton = {
                TextButton(onClick = {
                    ClockPreferences(context).restoreDefaults()
                    UltimateClockPreferences(context).restoreDefaults()
                    AntiBurnPreferences(context).restoreDefaults()
                    WorldClockRepository(context).restoreDefaults()
                    AutoStartManager.setEnabled(context, false)
                    generation++
                    confirmRestore = false
                }) { Text(stringResource(R.string.ultimate_restore_default)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(stringResource(R.string.ultimate_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsHome(
    selected: SettingsPage?,
    onSelect: (SettingsPage) -> Unit,
    onRestore: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsPage.entries.forEach { item ->
            Card(onClick = { onSelect(item) }, Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (selected == item) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(item.summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        Spacer(Modifier.padding(2.dp))
        OutlinedButton(onClick = onRestore, Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ultimate_restore_default))
        }
        Button(onClick = onDone, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ultimate_close_settings))
        }
    }
}

@Composable
private fun SettingsPageContent(
    page: SettingsPage,
    generation: Int,
    onLanguageChanged: () -> Unit,
    modifier: Modifier,
) {
    when (page) {
        SettingsPage.STYLE -> StyleSettingsPage(modifier, generation)
        SettingsPage.BACKGROUND -> BackgroundSettingsPage(modifier, generation)
        SettingsPage.TIME_DATE -> TimeDateSettingsPage(modifier, generation)
        SettingsPage.WEATHER -> WeatherSettingsPage(modifier, generation)
        SettingsPage.CALENDAR -> CalendarSettingsPage(modifier, generation)
        SettingsPage.CHIME -> ChimeSettingsPage(modifier, generation)
        SettingsPage.SYSTEM -> SystemSettingsPage(modifier, generation, onLanguageChanged)
    }
}

@Composable
internal fun SettingsColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            content()
        }
    }
}

@Composable
internal fun SettingSwitch(
    label: String,
    value: Boolean,
    summary: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked = value, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
internal fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueLabel, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
