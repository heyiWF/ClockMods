package com.clockmods.ultimate.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.AutoStartManager
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ClockPreferences
import com.clockmods.background.FontCatalog
import com.clockmods.time.RegionTimeZones
import com.clockmods.ui.DateFormatter
import com.clockmods.ui.StatusIconStyle
import com.clockmods.ui.StatusSymbolRenderer
import com.clockmods.ultimate.AntiBurnPreferences
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.compose.CalendarThemeCatalog
import com.clockmods.ultimate.compose.CalendarThemeThumbnail
import com.clockmods.ultimate.compose.StatusSymbolIcon
import com.clockmods.weather.WeatherLocationCatalog
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class SettingOption<T>(val value: T, val label: String)

@Composable
private fun <T> SettingChoices(
    options: List<SettingOption<T>>,
    selected: T,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                enabled = enabled,
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
private fun SettingAction(
    label: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    enabled: Boolean,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f).padding(vertical = 5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueLabel, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            enabled = enabled,
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<SettingOption<T>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(options, key = { it.value.toString() }) { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option.value == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option.value) },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.value == selected,
                            onClick = null,
                        )
                        Text(option.label, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTimeDialog(
    @StringRes title: Int,
    initialMinutes: Int,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val normalized = Math.floorMod(initialMinutes, 24 * 60)
    val state = rememberTimePickerState(
        initialHour = normalized / 60,
        initialMinute = normalized % 60,
        is24Hour = use24Hour,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
            ) {
                TimeInput(state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.ultimate_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
        },
    )
}

@Composable
private fun ColorDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var draft by rememberSaveable(initialColor) { mutableStateOf(colorSummary(initialColor)) }
    val parsed = remember(draft) { parseColor(draft) }
    val presets = CLOCK_COLOR_PRESETS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ultimate_background_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    presets.forEach { color ->
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { draft = colorSummary(color) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (parsed == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (Color(color).luminance() > .5f) Color.Black else Color.White,
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(9) },
                    label = { Text(stringResource(R.string.ultimate_background_color_picker)) },
                    supportingText = parsed?.let { { Text(colorSummary(it)) } },
                    isError = draft.isNotBlank() && parsed == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.ultimate_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
        },
    )
}

private enum class BackgroundTime { DIM_START, DIM_END }

@Composable
internal fun BackgroundSettingsPage(modifier: Modifier, generation: Int) {
    val context = LocalContext.current
    val repository = remember(context, generation) { BackgroundRepository(context) }
    val stylePreferences = remember(context, generation) { UltimateClockPreferences(context) }
    val initialMode = remember(repository, stylePreferences) {
        val stored = stylePreferences.getBackgroundMode()
        if (stored == UltimateClockPreferences.BACKGROUND_MODE_IMAGE && !repository.hasImage()) {
            UltimateClockPreferences.BACKGROUND_MODE_THEME
        } else {
            stored
        }
    }
    var mode by remember(generation) { mutableStateOf(initialMode) }
    var hasImage by remember(generation) { mutableStateOf(repository.hasImage()) }
    var color by remember(generation) { mutableIntStateOf(repository.getCurrentColor()) }
    var showStatus by remember(generation) { mutableStateOf(repository.isShowStatusIcons()) }
    var statusScale by remember(generation) { mutableFloatStateOf(repository.getStatusIconScale()) }
    var statusStyle by remember(generation) { mutableStateOf(StatusIconStyle.read(context)) }
    val updateStatusStyle: (StatusIconStyle) -> Unit = { updated ->
        statusStyle = updated
        updated.save(context)
    }
    var dim by remember(generation) { mutableStateOf(repository.isDimBackground()) }
    var scheduleDim by remember(generation) {
        mutableStateOf(repository.isScheduleDimBackground())
    }
    var dimStart by remember(generation) { mutableIntStateOf(repository.getDimStartMinutes()) }
    var dimEnd by remember(generation) { mutableIntStateOf(repository.getDimEndMinutes()) }
    var editingColor by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf<BackgroundTime?>(null) }
    val scope = rememberCoroutineScope()
    val windowSize = LocalWindowInfo.current.containerSize

    LaunchedEffect(initialMode) {
        if (stylePreferences.getBackgroundMode() != initialMode) {
            stylePreferences.setBackgroundMode(initialMode)
            repository.useColor()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val longSide = maxOf(
                windowSize.width,
                windowSize.height,
            )
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        repository.saveImage(it, longSide)
                    } ?: error("Unable to open selected image")
                }.isSuccess
            }
            if (imported) {
                hasImage = true
                mode = UltimateClockPreferences.BACKGROUND_MODE_IMAGE
                stylePreferences.setBackgroundMode(mode)
                repository.useImage()
            }
            Toast.makeText(
                context,
                if (imported) R.string.ultimate_background_imported
                else R.string.ultimate_background_import_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val launchImagePicker = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_background_source_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        UltimateClockPreferences.BACKGROUND_MODE_THEME,
                        stringResource(R.string.ultimate_background_theme),
                    ),
                    SettingOption(
                        UltimateClockPreferences.BACKGROUND_MODE_COLOR,
                        stringResource(R.string.ultimate_background_solid_color),
                    ),
                    SettingOption(
                        UltimateClockPreferences.BACKGROUND_MODE_IMAGE,
                        stringResource(R.string.ultimate_background_image),
                    ),
                ),
                selected = mode,
            ) { selected ->
                if (selected == UltimateClockPreferences.BACKGROUND_MODE_IMAGE && !hasImage) {
                    launchImagePicker()
                } else {
                    mode = selected
                    stylePreferences.setBackgroundMode(selected)
                    if (selected == UltimateClockPreferences.BACKGROUND_MODE_IMAGE) {
                        repository.useImage()
                    } else {
                        repository.useColor()
                    }
                }
            }
            SettingAction(
                stringResource(R.string.ultimate_background_color),
                colorSummary(color),
            ) { editingColor = true }
            SettingAction(
                stringResource(R.string.ultimate_background_import),
                stringResource(
                    if (hasImage) R.string.ultimate_background_replace_summary
                    else R.string.ultimate_background_import_summary,
                ),
                onClick = launchImagePicker,
            )
        }

        SettingSection(stringResource(R.string.ultimate_display_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_show_status_icons),
                showStatus,
                stringResource(R.string.ultimate_show_status_icons_summary),
            ) {
                showStatus = it
                repository.setShowStatusIcons(it)
            }
            SettingSlider(
                label = stringResource(R.string.ultimate_status_icon_size),
                value = statusScale,
                range = ClockPreferences.MIN_STATUS_ICON_SCALE..ClockPreferences.MAX_STATUS_ICON_SCALE,
                valueLabel = stringResource(
                    R.string.ultimate_percent_value,
                    (statusScale * 100).roundToInt(),
                ),
                enabled = showStatus,
            ) {
                statusScale = it
                repository.setStatusIconScale(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_status_icon_style)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.ultimate_status_icon_preview),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val previewColor = MaterialTheme.colorScheme.primary
                StatusSymbolIcon(StatusSymbolRenderer.WIFI_FULL, statusStyle, previewColor,
                    Modifier.size(28.dp), fallbackDrawable = R.drawable.ic_signal_wifi_4_bar)
                StatusSymbolIcon(StatusSymbolRenderer.BATTERY_BOLT, statusStyle, previewColor,
                    Modifier.size(28.dp), fallbackDrawable = R.drawable.ic_battery_android_bolt)
            }
            Text(stringResource(R.string.ultimate_status_icon_shape),
                style = MaterialTheme.typography.titleSmall)
            SettingChoices(
                options = listOf(
                    SettingOption(StatusIconStyle.OUTLINED, stringResource(R.string.ultimate_status_icon_outlined)),
                    SettingOption(StatusIconStyle.ROUNDED, stringResource(R.string.ultimate_status_icon_rounded)),
                    SettingOption(StatusIconStyle.SHARP, stringResource(R.string.ultimate_status_icon_sharp)),
                ),
                selected = statusStyle.family,
                enabled = showStatus,
            ) { updateStatusStyle(statusStyle.withFamily(it)) }
            Text(stringResource(R.string.ultimate_status_icon_fill),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp))
            SettingChoices(
                options = listOf(
                    SettingOption(StatusIconStyle.FILL_AUTO, stringResource(R.string.ultimate_status_icon_fill_auto)),
                    SettingOption(StatusIconStyle.FILL_OUTLINE, stringResource(R.string.ultimate_status_icon_outlined)),
                    SettingOption(StatusIconStyle.FILL_SOLID, stringResource(R.string.ultimate_status_icon_solid)),
                ),
                selected = statusStyle.fill,
                enabled = showStatus,
            ) { updateStatusStyle(statusStyle.withFill(it)) }
            SettingSlider(
                label = stringResource(R.string.ultimate_status_icon_weight),
                value = statusStyle.weight.toFloat(),
                range = 100f..700f,
                valueLabel = statusStyle.weight.toString(),
                enabled = showStatus,
                steps = 5,
            ) { updateStatusStyle(statusStyle.withWeight((it / 100f).roundToInt() * 100)) }
            SettingSlider(
                label = stringResource(R.string.ultimate_status_icon_grade),
                value = statusStyle.grade.toFloat(),
                range = -50f..200f,
                valueLabel = statusStyle.grade.toString(),
                enabled = showStatus,
                steps = 9,
            ) { updateStatusStyle(statusStyle.withGrade(((it + 50f) / 25f).roundToInt() * 25 - 50)) }
            SettingSlider(
                label = stringResource(R.string.ultimate_status_icon_optical_size),
                value = statusStyle.opticalSize.toFloat(),
                range = 20f..48f,
                valueLabel = statusStyle.opticalSize.toString(),
                enabled = showStatus,
                steps = 6,
            ) { updateStatusStyle(statusStyle.withOpticalSize(((it - 20f) / 4f).roundToInt() * 4 + 20)) }
            Text(stringResource(R.string.ultimate_status_icon_optical_size_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            SettingAction(
                stringResource(R.string.ultimate_status_icon_reset),
                enabled = showStatus,
            ) { updateStatusStyle(StatusIconStyle.defaults()) }
        }

        SettingSection(stringResource(R.string.ultimate_dimming_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_dim_background),
                dim,
                stringResource(R.string.ultimate_dim_background_summary),
            ) {
                dim = it
                repository.setDimBackground(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_schedule_dim_background),
                scheduleDim,
                stringResource(R.string.ultimate_schedule_dim_background_summary),
            ) {
                scheduleDim = it
                repository.setScheduleDimBackground(it)
            }
            SettingAction(
                stringResource(R.string.ultimate_dim_start),
                formatMinutes(dimStart),
                enabled = scheduleDim,
            ) { editingTime = BackgroundTime.DIM_START }
            SettingAction(
                stringResource(R.string.ultimate_dim_end),
                formatMinutes(dimEnd),
                enabled = scheduleDim,
            ) { editingTime = BackgroundTime.DIM_END }
        }
    }

    if (editingColor) {
        ColorDialog(
            initialColor = color,
            onDismiss = { editingColor = false },
            onConfirm = {
                color = it
                mode = UltimateClockPreferences.BACKGROUND_MODE_COLOR
                stylePreferences.setBackgroundMode(mode)
                repository.setCurrentColor(it)
                editingColor = false
            },
        )
    }
    editingTime?.let { field ->
        SettingsTimeDialog(
            title = if (field == BackgroundTime.DIM_START) {
                R.string.ultimate_dim_start
            } else {
                R.string.ultimate_dim_end
            },
            initialMinutes = if (field == BackgroundTime.DIM_START) dimStart else dimEnd,
            use24Hour = ClockPreferences(context).isUse24Hour(),
            onDismiss = { editingTime = null },
            onConfirm = { minutes ->
                if (field == BackgroundTime.DIM_START) {
                    dimStart = minutes
                    repository.setDimStartMinutes(minutes)
                } else {
                    dimEnd = minutes
                    repository.setDimEndMinutes(minutes)
                }
                editingTime = null
            },
        )
    }
}

private enum class TimeDateDialog { SYNC_INTERVAL, TIME_ZONE, DATE_FORMAT }

@Composable
internal fun TimeDateSettingsPage(modifier: Modifier, generation: Int) {
    val context = LocalContext.current
    val repository = remember(context, generation) { BackgroundRepository(context) }
    var use24Hour by remember(generation) { mutableStateOf(repository.isUse24Hour()) }
    var showSeconds by remember(generation) { mutableStateOf(repository.isShowSeconds()) }
    var useNetworkTime by remember(generation) { mutableStateOf(repository.isUseNetworkTime()) }
    var syncInterval by remember(generation) {
        mutableIntStateOf(repository.getSyncIntervalMinutes())
    }
    var timeZone by remember(generation) { mutableStateOf(repository.getTimeZoneId()) }
    var showLunar by remember(generation) { mutableStateOf(repository.isShowLunar()) }
    var datePattern by remember(generation) {
        mutableStateOf(
            if (repository.getClockLanguage() == ClockPreferences.LANGUAGE_ENGLISH) {
                repository.getDatePatternEn()
            } else {
                repository.getDatePatternCn()
            },
        )
    }
    var dialog by rememberSaveable { mutableStateOf<TimeDateDialog?>(null) }
    val syncOptions = listOf(
        SettingOption(30, stringResource(R.string.ultimate_sync_30_minutes)),
        SettingOption(60, stringResource(R.string.ultimate_sync_1_hour)),
        SettingOption(360, stringResource(R.string.ultimate_sync_6_hours)),
        SettingOption(1440, stringResource(R.string.ultimate_sync_1_day)),
    )
    val regionNames = stringArrayResource(R.array.region_names)
    val timeZoneOptions = remember(regionNames) {
        RegionTimeZones.ZONE_IDS.mapIndexed { index, zoneId ->
            SettingOption(zoneId, regionNames.getOrElse(index) { zoneId })
        }
    }

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_time_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_use_24_hour),
                use24Hour,
                stringResource(R.string.ultimate_use_24_hour_summary),
            ) {
                use24Hour = it
                repository.setUse24Hour(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_show_seconds),
                showSeconds,
                stringResource(R.string.ultimate_show_seconds_summary),
            ) {
                showSeconds = it
                repository.setShowSeconds(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_time_sync_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_network_time),
                useNetworkTime,
                stringResource(R.string.ultimate_network_time_summary),
            ) {
                useNetworkTime = it
                repository.setUseNetworkTime(it)
            }
            SettingAction(
                label = stringResource(R.string.ultimate_sync_interval),
                summary = syncOptions.firstOrNull { it.value == syncInterval }?.label
                    ?: syncOptions.first().label,
                enabled = useNetworkTime,
            ) { dialog = TimeDateDialog.SYNC_INTERVAL }
            val zoneIndex = RegionTimeZones.indexOfZoneId(timeZone)
            SettingAction(
                stringResource(R.string.ultimate_time_zone),
                regionNames.getOrElse(zoneIndex) { regionNames.firstOrNull().orEmpty() },
            ) { dialog = TimeDateDialog.TIME_ZONE }
        }

        SettingSection(stringResource(R.string.ultimate_date_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_show_lunar),
                showLunar,
                stringResource(R.string.ultimate_show_lunar_summary),
            ) {
                showLunar = it
                repository.setShowLunar(it)
            }
            SettingAction(
                stringResource(R.string.ultimate_date_format),
                datePattern,
            ) { dialog = TimeDateDialog.DATE_FORMAT }
        }
    }

    when (dialog) {
        TimeDateDialog.SYNC_INTERVAL -> SingleChoiceDialog(
            title = stringResource(R.string.ultimate_sync_interval),
            options = syncOptions,
            selected = syncInterval,
            onDismiss = { dialog = null },
            onSelect = {
                syncInterval = it
                repository.setSyncIntervalMinutes(it)
                dialog = null
            },
        )
        TimeDateDialog.TIME_ZONE -> SingleChoiceDialog(
            title = stringResource(R.string.ultimate_time_zone),
            options = timeZoneOptions,
            selected = timeZone,
            onDismiss = { dialog = null },
            onSelect = {
                timeZone = it
                repository.setTimeZoneId(it)
                dialog = null
            },
        )
        TimeDateDialog.DATE_FORMAT -> DatePatternDialog(
            repository = repository,
            onDismiss = { dialog = null },
            onSave = {
                datePattern = it
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun DatePatternDialog(
    repository: BackgroundRepository,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val language = repository.getClockLanguage()
    val english = language == ClockPreferences.LANGUAGE_ENGLISH
    val fallback = if (english) {
        ClockPreferences.DEFAULT_DATE_PATTERN_EN
    } else {
        ClockPreferences.DEFAULT_DATE_PATTERN_CN
    }
    val current = if (english) repository.getDatePatternEn() else repository.getDatePatternCn()
    var draft by rememberSaveable(current) { mutableStateOf(current) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val valid = DateFormatter.isValidPattern(draft.trim())
    val preview = remember(draft, language) {
        if (valid) {
            DateFormatter.preview(draft.trim(), LocaleManager.dateLang(language))
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ultimate_date_format)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.ultimate_date_format_help),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        if (it.length <= DateFormatter.MAX_PATTERN_LENGTH) draft = it
                    },
                    isError = !valid,
                    supportingText = {
                        Text(
                            preview?.let { stringResource(R.string.date_format_preview, it) }
                                ?: stringResource(R.string.ultimate_date_format_invalid),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showHelp = true }) {
                    Text(stringResource(R.string.date_format_help_button))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val value = draft.trim()
                    if (english) repository.setDatePatternEn(value)
                    else repository.setDatePatternCn(value)
                    repository.setDateFormatState(english, "", "", true, value)
                    onSave(value)
                },
            ) { Text(stringResource(R.string.ultimate_apply)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { draft = fallback }) {
                    Text(stringResource(R.string.ultimate_restore_default))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ultimate_cancel))
                }
            }
        },
    )

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.date_format_help_title)) },
            text = {
                Text(
                    stringResource(
                        if (english) R.string.date_format_help_body_en
                        else R.string.date_format_help_body_cn,
                    ),
                    Modifier.fillMaxHeight(.7f).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.ultimate_got_it))
                }
            },
        )
    }
}

private enum class WeatherDialog { MESSAGE, INTERVAL, LOCATION }

@Composable
internal fun WeatherSettingsPage(modifier: Modifier, generation: Int) {
    val context = LocalContext.current
    val repository = remember(context, generation) { BackgroundRepository(context) }
    var enabled by remember(generation) { mutableStateOf(repository.isWeatherEnabled()) }
    var detailed by remember(generation) { mutableStateOf(repository.isWeatherDetailed()) }
    var message by remember(generation) { mutableStateOf(repository.getCustomMessage()) }
    var manualLocation by remember(generation) {
        mutableStateOf(
            repository.getWeatherLocationMode() == ClockPreferences.WEATHER_LOCATION_MANUAL,
        )
    }
    var locationSummary by remember(generation) {
        mutableStateOf(weatherLocationSummary(repository))
    }
    var temperatureUnit by remember(generation) {
        mutableStateOf(repository.getWeatherTemperatureUnit())
    }
    var interval by remember(generation) {
        mutableIntStateOf(repository.getWeatherIntervalMinutes())
    }
    var iconFill by remember(generation) { mutableStateOf(repository.isWeatherIconFill()) }
    var dynamicColor by remember(generation) {
        mutableStateOf(repository.isWeatherIconDynamicColor())
    }
    var dialog by rememberSaveable { mutableStateOf<WeatherDialog?>(null) }
    val intervalOptions = listOf(
        SettingOption(10, stringResource(R.string.ultimate_weather_10_minutes)),
        SettingOption(30, stringResource(R.string.ultimate_weather_30_minutes)),
        SettingOption(60, stringResource(R.string.ultimate_weather_1_hour)),
        SettingOption(180, stringResource(R.string.ultimate_weather_3_hours)),
        SettingOption(360, stringResource(R.string.ultimate_weather_6_hours)),
        SettingOption(720, stringResource(R.string.ultimate_weather_12_hours)),
    )

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_weather_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_weather_enabled),
                enabled,
                stringResource(R.string.ultimate_weather_enabled_summary),
            ) {
                enabled = it
                repository.setWeatherEnabled(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_weather_detailed),
                detailed,
                stringResource(R.string.ultimate_weather_detailed_summary),
            ) {
                detailed = it
                repository.setWeatherDetailed(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_custom_message_section)) {
            SettingAction(
                stringResource(R.string.ultimate_custom_message),
                message.ifBlank { stringResource(R.string.ultimate_custom_message_empty) },
            ) { dialog = WeatherDialog.MESSAGE }
        }

        SettingSection(stringResource(R.string.ultimate_weather_location_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        false,
                        stringResource(R.string.ultimate_weather_location_auto),
                    ),
                    SettingOption(
                        true,
                        stringResource(R.string.ultimate_weather_location_manual),
                    ),
                ),
                selected = manualLocation,
                enabled = enabled,
            ) { manual ->
                manualLocation = manual
                repository.setWeatherLocationMode(
                    if (manual) ClockPreferences.WEATHER_LOCATION_MANUAL
                    else ClockPreferences.WEATHER_LOCATION_AUTOMATIC,
                )
                if (manual && repository.getWeatherLocationId().isBlank()) {
                    dialog = WeatherDialog.LOCATION
                }
            }
            SettingAction(
                stringResource(R.string.ultimate_weather_choose_location),
                locationSummary.ifBlank {
                    stringResource(R.string.ultimate_weather_location_not_set)
                },
                enabled = enabled && manualLocation,
            ) { dialog = WeatherDialog.LOCATION }
        }

        SettingSection(stringResource(R.string.ultimate_weather_temperature_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        ClockPreferences.WEATHER_UNIT_CELSIUS,
                        stringResource(R.string.ultimate_weather_celsius),
                    ),
                    SettingOption(
                        ClockPreferences.WEATHER_UNIT_FAHRENHEIT,
                        stringResource(R.string.ultimate_weather_fahrenheit),
                    ),
                ),
                selected = temperatureUnit,
                enabled = enabled,
            ) {
                temperatureUnit = it
                repository.setWeatherTemperatureUnit(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_weather_refresh_section)) {
            SettingAction(
                label = stringResource(R.string.ultimate_weather_refresh_interval),
                summary = intervalOptions.firstOrNull { it.value == interval }?.label
                    ?: intervalOptions.first().label,
                enabled = enabled,
            ) { dialog = WeatherDialog.INTERVAL }
        }

        SettingSection(stringResource(R.string.ultimate_weather_icon_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_weather_icon_fill),
                iconFill,
                stringResource(R.string.ultimate_weather_icon_fill_summary),
            ) {
                iconFill = it
                repository.setWeatherIconFill(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_weather_dynamic_color),
                dynamicColor,
                stringResource(R.string.ultimate_weather_dynamic_color_summary),
            ) {
                dynamicColor = it
                repository.setWeatherIconDynamicColor(it)
            }
        }
    }

    when (dialog) {
        WeatherDialog.MESSAGE -> CustomMessageDialog(
            initial = message,
            onDismiss = { dialog = null },
            onSave = {
                message = it
                repository.setCustomMessage(it)
                dialog = null
            },
        )
        WeatherDialog.INTERVAL -> SingleChoiceDialog(
            title = stringResource(R.string.ultimate_weather_refresh_interval),
            options = intervalOptions,
            selected = interval,
            onDismiss = { dialog = null },
            onSelect = {
                interval = it
                repository.setWeatherIntervalMinutes(it)
                dialog = null
            },
        )
        WeatherDialog.LOCATION -> WeatherLocationDialog(
            initialProvince = repository.getWeatherProvince(),
            initialCity = repository.getWeatherCity(),
            initialLocationId = repository.getWeatherLocationId(),
            english = repository.getClockLanguage() == ClockPreferences.LANGUAGE_ENGLISH,
            onDismiss = { dialog = null },
            onSelect = { location ->
                repository.setManualWeatherLocation(
                    location.locationId,
                    location.province,
                    location.city,
                    location.district,
                    location.latitude,
                    location.longitude,
                )
                repository.setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_MANUAL)
                manualLocation = true
                locationSummary = weatherLocationSummary(repository)
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun CustomMessageDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ultimate_custom_message)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(ClockPreferences.MAX_CUSTOM_MESSAGE_LENGTH) },
                label = { Text(stringResource(R.string.ultimate_custom_message_hint)) },
                supportingText = {
                    Text("${value.length}/${ClockPreferences.MAX_CUSTOM_MESSAGE_LENGTH}")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) {
                Text(stringResource(R.string.ultimate_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
        },
    )
}

private enum class LocationStage { PROVINCE, CITY, DISTRICT }

@Composable
private fun WeatherLocationDialog(
    initialProvince: String,
    initialCity: String,
    initialLocationId: String,
    english: Boolean,
    onDismiss: () -> Unit,
    onSelect: (WeatherLocationCatalog.LocationEntry) -> Unit,
) {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<WeatherLocationCatalog?>(null) }
    var failed by remember { mutableStateOf(false) }
    var province by rememberSaveable(initialProvince) { mutableStateOf(initialProvince) }
    var city by rememberSaveable(initialCity) { mutableStateOf(initialCity) }
    var stage by rememberSaveable {
        mutableStateOf(
            if (initialProvince.isNotBlank() && initialCity.isNotBlank()) {
                LocationStage.DISTRICT
            } else {
                LocationStage.PROVINCE
            },
        )
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { WeatherLocationCatalog.load(context) }
        }.onSuccess { catalog = it }.onFailure { failed = true }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stage != LocationStage.PROVINCE) {
                    IconButton(
                        onClick = {
                            stage = if (stage == LocationStage.DISTRICT) {
                                LocationStage.CITY
                            } else {
                                LocationStage.PROVINCE
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ultimate_back_to_settings),
                        )
                    }
                }
                Text(stringResource(R.string.ultimate_weather_choose_location))
            }
        },
        text = {
            when {
                failed -> Text(stringResource(R.string.weather_location_list_error))
                catalog == null -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                else -> {
                    val loaded = catalog!!
                    Column {
                        if (province.isNotBlank()) {
                            Text(
                                listOf(province, city).filter { it.isNotBlank() }
                                    .joinToString(" / "),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                            when (stage) {
                                LocationStage.PROVINCE -> {
                                    val values = loaded.provinces()
                                    val labels = loaded.provinceLabels(english)
                                    items(values.indices.toList(), key = { values[it] }) { index ->
                                        LocationChoiceRow(
                                            label = labels.getOrElse(index) { values[index] },
                                            selected = values[index] == province,
                                        ) {
                                            province = values[index]
                                            city = ""
                                            stage = LocationStage.CITY
                                        }
                                    }
                                }
                                LocationStage.CITY -> {
                                    val values = loaded.cities(province)
                                    val labels = loaded.cityLabels(province, english)
                                    items(values.indices.toList(), key = { values[it] }) { index ->
                                        LocationChoiceRow(
                                            label = labels.getOrElse(index) { values[index] },
                                            selected = values[index] == city,
                                        ) {
                                            city = values[index]
                                            stage = LocationStage.DISTRICT
                                        }
                                    }
                                }
                                LocationStage.DISTRICT -> {
                                    val values = loaded.districts(province, city)
                                    items(values, key = { it.locationId }) { entry ->
                                        LocationChoiceRow(
                                            label = entry.displayDistrict(english),
                                            selected = entry.locationId == initialLocationId,
                                        ) { onSelect(entry) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ultimate_cancel)) }
        },
    )
}

@Composable
private fun LocationChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}

@Composable
internal fun CalendarSettingsPage(modifier: Modifier, generation: Int) {
    val context = LocalContext.current
    val preferences = remember(context, generation) { ClockPreferences(context) }
    val styles = remember { CalendarThemeCatalog.presets() }
    var weekStart by remember(generation) { mutableIntStateOf(preferences.getCalendarWeekStart()) }
    var highlightWeekends by remember(generation) {
        mutableStateOf(preferences.isCalendarHighlightWeekends())
    }
    var themeId by remember(generation) {
        mutableStateOf(CalendarThemeCatalog.resolve(preferences.getCalendarTheme()).id)
    }
    val typographyScope = ClockPreferences.calendarScope(themeId)
    var family by remember(generation, themeId) {
        mutableStateOf(preferences.getFontFamily(typographyScope))
    }
    var weight by remember(generation, themeId) {
        mutableIntStateOf(preferences.getFontWeight(typographyScope))
    }
    var timeScale by remember(generation, themeId) {
        mutableFloatStateOf(preferences.getTimeFontScale(typographyScope))
    }
    var dateScale by remember(generation, themeId) {
        mutableFloatStateOf(preferences.getDateFontScale(typographyScope))
    }
    var supportingScale by remember(generation, themeId) {
        mutableFloatStateOf(preferences.getSupportingFontScale(typographyScope))
    }

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_style_gallery)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(styles, key = { it.id }) { style ->
                    val selected = style.id == themeId
                    Card(
                        onClick = {
                            themeId = style.id
                            preferences.setCalendarTheme(themeId)
                        },
                        modifier = Modifier.width(190.dp).height(172.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.fillMaxWidth().height(88.dp).clip(MaterialTheme.shapes.small)) {
                                CalendarThemeThumbnail(theme = style, modifier = Modifier.fillMaxSize())
                            }
                            Text(stringResource(style.nameRes), maxLines = 1,
                                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(style.summaryRes), maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        SettingSection(stringResource(R.string.ultimate_calendar_week_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        ClockPreferences.CALENDAR_WEEK_START_SUNDAY,
                        stringResource(R.string.ultimate_calendar_sunday),
                    ),
                    SettingOption(
                        ClockPreferences.CALENDAR_WEEK_START_MONDAY,
                        stringResource(R.string.ultimate_calendar_monday),
                    ),
                ),
                selected = weekStart,
            ) {
                weekStart = it
                preferences.setCalendarWeekStart(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_calendar_highlight_weekends),
                highlightWeekends,
                stringResource(R.string.ultimate_calendar_highlight_weekends_summary),
            ) {
                highlightWeekends = it
                preferences.setCalendarHighlightWeekends(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_typography_section)) {
            Text(stringResource(R.string.ultimate_font_family))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FontCatalog.options().forEach { option ->
                    FilterChip(
                        selected = family == option.id,
                        onClick = {
                            family = option.id
                            preferences.setFontFamily(typographyScope, family)
                            weight = option.nearestWeight(weight)
                            preferences.setFontWeight(typographyScope, weight)
                        },
                        label = { Text(FontCatalog.displayName(context, option.id)) },
                    )
                }
            }
            val font = FontCatalog.optionFor(family)
            val weights = font.availableWeights()
            SettingSlider(
                label = stringResource(R.string.ultimate_font_weight),
                value = font.indexOfNearestWeight(weight).toFloat(),
                range = 0f..(weights.size - 1).coerceAtLeast(1).toFloat(),
                valueLabel = weight.toString(),
                enabled = true,
                steps = (weights.size - 2).coerceAtLeast(0),
            ) {
                weight = font.weightAt(it.roundToInt())
                preferences.setFontWeight(typographyScope, weight)
            }
            SettingSlider(
                label = stringResource(R.string.ultimate_time_size),
                value = timeScale,
                range = ClockPreferences.MIN_FONT_SCALE..ClockPreferences.MAX_FONT_SCALE,
                valueLabel = stringResource(
                    R.string.ultimate_percent_value,
                    (timeScale * 100).roundToInt(),
                ),
                enabled = true,
            ) {
                timeScale = it
                preferences.setTimeFontScale(typographyScope, it)
            }
            SettingSlider(
                label = stringResource(R.string.ultimate_date_size),
                value = dateScale,
                range = ClockPreferences.MIN_FONT_SCALE..ClockPreferences.MAX_DATE_FONT_SCALE,
                valueLabel = stringResource(
                    R.string.ultimate_percent_value,
                    (dateScale * 100).roundToInt(),
                ),
                enabled = true,
            ) {
                dateScale = it
                preferences.setDateFontScale(typographyScope, it)
            }
            SettingSlider(
                label = stringResource(R.string.ultimate_supporting_text_size),
                value = supportingScale,
                range = ClockPreferences.MIN_SUPPORTING_FONT_SCALE..
                    ClockPreferences.MAX_SUPPORTING_FONT_SCALE,
                valueLabel = stringResource(
                    R.string.ultimate_percent_value,
                    (supportingScale * 100).roundToInt(),
                ),
                enabled = true,
            ) {
                supportingScale = it
                preferences.setSupportingFontScale(typographyScope, it)
            }
        }
    }
}

private enum class ChimeTime { QUIET_START, QUIET_END }

@Composable
internal fun ChimeSettingsPage(modifier: Modifier, generation: Int) {
    val context = LocalContext.current
    val repository = remember(context, generation) { BackgroundRepository(context) }
    var hourly by remember(generation) { mutableStateOf(repository.isHourlyChimeEnabled()) }
    var halfHour by remember(generation) {
        mutableStateOf(repository.isHalfHourChimeEnabled())
    }
    var chimeAnimation by remember(generation) {
        mutableStateOf(repository.getChimeAnimation())
    }
    var quietHours by remember(generation) {
        mutableStateOf(repository.isHourlyChimeQuietEnabled())
    }
    var quietStart by remember(generation) {
        mutableIntStateOf(repository.getHourlyChimeQuietStart())
    }
    var quietEnd by remember(generation) {
        mutableIntStateOf(repository.getHourlyChimeQuietEnd())
    }
    var editingTime by rememberSaveable { mutableStateOf<ChimeTime?>(null) }

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_chime_cues_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_hourly_chime),
                hourly,
                stringResource(R.string.ultimate_hourly_chime_summary),
            ) {
                hourly = it
                repository.setHourlyChimeEnabled(it)
            }
            SettingSwitch(
                stringResource(R.string.ultimate_half_hour_chime),
                halfHour,
                stringResource(R.string.ultimate_half_hour_chime_summary),
            ) {
                halfHour = it
                repository.setHalfHourChimeEnabled(it)
            }
            Text(stringResource(R.string.ultimate_chime_animation))
            SettingChoices(
                options = listOf(
                    SettingOption(ClockPreferences.CHIME_RADIAL, stringResource(R.string.ultimate_chime_radial)),
                    SettingOption(ClockPreferences.CHIME_RIPPLE, stringResource(R.string.ultimate_chime_ripple)),
                    SettingOption(ClockPreferences.CHIME_PULSE, stringResource(R.string.ultimate_chime_pulse)),
                    SettingOption(ClockPreferences.CHIME_AURORA, stringResource(R.string.ultimate_chime_aurora)),
                    SettingOption(ClockPreferences.CHIME_ORBIT, stringResource(R.string.ultimate_chime_orbit)),
                    SettingOption(ClockPreferences.CHIME_COMET, stringResource(R.string.ultimate_chime_comet)),
                ),
                selected = chimeAnimation,
                enabled = hourly || halfHour,
            ) {
                chimeAnimation = it
                repository.setChimeAnimation(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_chime_quiet_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_chime_quiet_hours),
                quietHours,
                stringResource(R.string.ultimate_chime_quiet_hours_summary),
            ) {
                quietHours = it
                repository.setHourlyChimeQuietEnabled(it)
            }
            SettingAction(
                stringResource(R.string.ultimate_chime_quiet_start),
                formatMinutes(quietStart),
                enabled = quietHours,
            ) { editingTime = ChimeTime.QUIET_START }
            SettingAction(
                stringResource(R.string.ultimate_chime_quiet_end),
                formatMinutes(quietEnd),
                enabled = quietHours,
            ) { editingTime = ChimeTime.QUIET_END }
        }
    }

    editingTime?.let { field ->
        SettingsTimeDialog(
            title = if (field == ChimeTime.QUIET_START) {
                R.string.ultimate_chime_quiet_start
            } else {
                R.string.ultimate_chime_quiet_end
            },
            initialMinutes = if (field == ChimeTime.QUIET_START) quietStart else quietEnd,
            use24Hour = repository.isUse24Hour(),
            onDismiss = { editingTime = null },
            onConfirm = { minutes ->
                if (field == ChimeTime.QUIET_START) {
                    quietStart = minutes
                    repository.setHourlyChimeQuietStart(minutes)
                } else {
                    quietEnd = minutes
                    repository.setHourlyChimeQuietEnd(minutes)
                }
                editingTime = null
            },
        )
    }
}

private enum class SystemDialog { ANTI_BURN_PERIOD }

@Composable
internal fun SystemSettingsPage(
    modifier: Modifier,
    generation: Int,
    onLanguageChanged: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context, generation) { ClockPreferences(context) }
    val antiBurn = remember(context, generation) { AntiBurnPreferences(context) }
    var antiBurnEnabled by remember(generation) { mutableStateOf(antiBurn.isEnabled()) }
    var antiBurnPeriod by remember(generation) { mutableIntStateOf(antiBurn.getPeriodMinutes()) }
    var antiBurnAmplitude by remember(generation) {
        mutableFloatStateOf(antiBurn.getAmplitudeDp())
    }
    var antiBurnAutoDim by remember(generation) { mutableStateOf(antiBurn.isAutoDim()) }
    var orientation by remember(generation) {
        mutableIntStateOf(preferences.getScreenOrientation())
    }
    var language by remember(generation) { mutableStateOf(preferences.getClockLanguage()) }
    var autoStart by remember(generation) { mutableStateOf(preferences.isAutoStart()) }
    var dialog by rememberSaveable { mutableStateOf<SystemDialog?>(null) }
    val periodOptions = listOf(1, 10, 30, 60).map {
        SettingOption(it, stringResource(R.string.ultimate_minutes_value, it))
    }

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_anti_burn_section)) {
            SettingSwitch(
                stringResource(R.string.ultimate_anti_burn_enabled),
                antiBurnEnabled,
                stringResource(R.string.ultimate_anti_burn_summary),
            ) {
                antiBurnEnabled = it
                antiBurn.setEnabled(it)
            }
            SettingAction(
                stringResource(R.string.ultimate_anti_burn_period),
                stringResource(R.string.ultimate_minutes_value, antiBurnPeriod),
                enabled = antiBurnEnabled,
            ) { dialog = SystemDialog.ANTI_BURN_PERIOD }
            SettingSlider(
                label = stringResource(R.string.ultimate_anti_burn_amplitude),
                value = antiBurnAmplitude,
                range = 0f..12f,
                valueLabel = stringResource(
                    R.string.ultimate_dp_value,
                    antiBurnAmplitude.roundToInt(),
                ),
                enabled = antiBurnEnabled,
                steps = 11,
            ) {
                antiBurnAmplitude = it
                antiBurn.setAmplitudeDp(it)
            }
            SettingSwitch(
                label = stringResource(R.string.ultimate_anti_burn_auto_dim),
                value = antiBurnAutoDim,
                summary = stringResource(R.string.ultimate_anti_burn_auto_dim_summary),
                enabled = antiBurnEnabled,
            ) {
                antiBurnAutoDim = it
                antiBurn.setAutoDim(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_orientation_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        ClockPreferences.ORIENTATION_FOLLOW_SYSTEM,
                        stringResource(R.string.ultimate_orientation_follow),
                    ),
                    SettingOption(
                        ClockPreferences.ORIENTATION_PORTRAIT,
                        stringResource(R.string.ultimate_orientation_portrait),
                    ),
                    SettingOption(
                        ClockPreferences.ORIENTATION_LANDSCAPE,
                        stringResource(R.string.ultimate_orientation_landscape),
                    ),
                ),
                selected = orientation,
            ) {
                orientation = it
                preferences.setScreenOrientation(it)
            }
        }

        SettingSection(stringResource(R.string.ultimate_language_section)) {
            SettingChoices(
                options = listOf(
                    SettingOption(
                        ClockPreferences.LANGUAGE_SIMPLIFIED,
                        stringResource(R.string.ultimate_language_simplified),
                    ),
                    SettingOption(
                        ClockPreferences.LANGUAGE_TRADITIONAL,
                        stringResource(R.string.ultimate_language_traditional),
                    ),
                    SettingOption(
                        ClockPreferences.LANGUAGE_ENGLISH,
                        stringResource(R.string.ultimate_language_english),
                    ),
                ),
                selected = language,
            ) {
                language = it
                preferences.setClockLanguage(it)
                onLanguageChanged()
            }
            SettingSwitch(
                stringResource(R.string.ultimate_auto_start),
                autoStart,
                stringResource(R.string.ultimate_auto_start_summary),
            ) {
                autoStart = it
                preferences.setAutoStart(it)
                AutoStartManager.setEnabled(context, it)
                if (it) AutoStartManager.requestHomeRole(context)
            }
        }
    }

    if (dialog == SystemDialog.ANTI_BURN_PERIOD) {
        SingleChoiceDialog(
            title = stringResource(R.string.ultimate_anti_burn_period),
            options = periodOptions,
            selected = antiBurnPeriod,
            onDismiss = { dialog = null },
            onSelect = {
                antiBurnPeriod = it
                antiBurn.setPeriodMinutes(it)
                dialog = null
            },
        )
    }
}

private fun colorSummary(color: Int): String = String.format(Locale.ROOT, "#%08X", color)

private fun parseColor(value: String): Int? {
    val raw = value.trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    return runCatching {
        val argb = if (raw.length == 6) "FF$raw" else raw
        argb.toLong(16).toInt()
    }.getOrNull()
}

private fun formatMinutes(minutes: Int): String {
    val normalized = Math.floorMod(minutes, 24 * 60)
    return String.format(Locale.getDefault(), "%02d:%02d", normalized / 60, normalized % 60)
}

private fun weatherLocationSummary(repository: BackgroundRepository): String =
    listOf(
        repository.getWeatherProvince(),
        repository.getWeatherCity(),
        repository.getWeatherDistrict(),
    ).filter { it.isNotBlank() }.joinToString(" / ")
