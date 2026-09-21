package com.clockmods.ultimate.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.LocaleManager
import com.clockmods.background.ClockPreferences
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.FontCatalog
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.WorldClockEntry
import com.clockmods.ultimate.clock.ClockPalette
import com.clockmods.ultimate.compose.ClockPreviewCanvas
import com.clockmods.ultimate.compose.ClockStyleThumbnail
import com.clockmods.ultimate.compose.previewClockBackground
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.ultimate.clock.WorldClockCatalog
import com.clockmods.ultimate.clock.WorldClockRepository
import java.util.Locale

private enum class StyleDialog { TIME_COLOR, DATE_COLOR }

/** Every gallery tile shares one size so the row reads as an even grid. */
private val STYLE_CARD_WIDTH = 172.dp
private val STYLE_CARD_HEIGHT = 136.dp

/** Height of the live theme preview shown above the palette swatches. */
private val PALETTE_PREVIEW_HEIGHT = 168.dp

/**
 * The one colour ramp offered by every picker, ordered dark to light so the swatches read as a
 * gradient rather than a set of competing hues.
 *
 * Every entry stays muted: the saturated primaries that used to sit here (a bright red, a vivid
 * purple, a strong teal) shouted over the clock face and clashed with the themes' own colourways.
 * The three `ClockPalette.DEFAULT` values are included so a fresh install still shows a selection.
 */
internal val CLOCK_COLOR_PRESETS = listOf(
    0xFF000000.toInt(), // black
    0xFF171918.toInt(), // ink
    0xFF154974.toInt(), // deep navy (palette default background)
    0xFF23557F.toInt(), // panel blue (palette default panel)
    0xFF55697A.toInt(), // slate
    0xFF78838C.toInt(), // stone
    0xFF5C6B57.toInt(), // sage
    0xFF6E6252.toInt(), // umber
    0xFF9ECAFC.toInt(), // pastel blue (palette default accent)
    0xFFE7E2D6.toInt(), // sand
    0xFFF5F1E6.toInt(), // cream
    0xFFFFFFFF.toInt(), // white
)

@Composable
internal fun StyleSettingsPage(modifier: Modifier, generation: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context, generation) { ClockPreferences(context) }
    val stylePreferences = remember(context, generation) { UltimateClockPreferences(context) }
    val worldRepository = remember(context, generation) { WorldClockRepository(context) }
    var styleId by remember(generation) { mutableStateOf(stylePreferences.getStyleId()) }
    var motion by remember(generation, styleId) {
        mutableStateOf(stylePreferences.getSecondHandMotion())
    }
    var family by remember(generation, styleId) {
        mutableStateOf(preferences.getFontFamily(styleId))
    }
    var weight by remember(generation, styleId) {
        mutableIntStateOf(preferences.getFontWeight(styleId))
    }
    var timeScale by remember(generation, styleId) {
        mutableFloatStateOf(
            if (styleId == UltimateClockStyles.STYLE_PRO_CLASSIC) {
                preferences.getTimeFontScale()
            } else {
                preferences.getTimeFontScale(styleId)
            },
        )
    }
    var dateScale by remember(generation, styleId) {
        mutableFloatStateOf(
            if (styleId == UltimateClockStyles.STYLE_PRO_CLASSIC) {
                preferences.getDateFontScale()
            } else {
                preferences.getDateFontScale(styleId)
            },
        )
    }
    var supportingScale by remember(generation, styleId) {
        mutableFloatStateOf(preferences.getSupportingFontScale(styleId))
    }
    var blinkColon by remember(generation) { mutableStateOf(preferences.isBlinkColon()) }
    var animateDigits by remember(generation) { mutableStateOf(preferences.isAnimateTimeChanges()) }
    var transition by remember(generation) { mutableStateOf(preferences.getTimeTransition()) }
    var smallSeconds by remember(generation) { mutableStateOf(preferences.isSmallSeconds()) }
    var portraitStacked by remember(generation) { mutableStateOf(preferences.isPortraitStacked()) }
    var dualLine by remember(generation) { mutableStateOf(preferences.isDateLunarDualLine()) }
    var timeColor by remember(generation) { mutableIntStateOf(preferences.getTimeColor()) }
    var dateColor by remember(generation) { mutableIntStateOf(preferences.getDateColor()) }
    var worldEnabled by remember(generation) { mutableStateOf(worldRepository.isEnabled()) }
    var selectedCities by remember(generation) { mutableStateOf(worldRepository.getSelected()) }
    var showWorldEditor by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<StyleDialog?>(null) }
    var palette by remember(generation, styleId) {
        mutableStateOf(stylePreferences.getPalette(styleId))
    }
    val backgroundRepository = remember(context, generation) { BackgroundRepository(context) }
    val activeStyle = remember(styleId) {
        UltimateClockStyles.sharedRegistry().resolveForApi(styleId, android.os.Build.VERSION.SDK_INT)
    }
    val capabilities = activeStyle.getMetadata().getCapabilities()
    val proClassic = styleId == UltimateClockStyles.STYLE_PRO_CLASSIC
    val supportsTimeScale = proClassic || ClockPalette.supports(styleId)
    val supportsDateScale = capabilities.supports(ClockStyleCapabilities.Capability.DATE)
    val supportsSupportingScale = capabilities.supports(ClockStyleCapabilities.Capability.WEATHER) ||
        capabilities.supports(ClockStyleCapabilities.Capability.STATUS)
    val supportsWorldClock = capabilities.supports(ClockStyleCapabilities.Capability.WORLD_CLOCK)

    SettingsColumn(modifier) {
        SettingSection(stringResource(R.string.ultimate_style_gallery)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(UltimateClockStyles.builtIns(), key = { it.getMetadata().getId() }) { style ->
                    val metadata = style.getMetadata()
                    val selected = metadata.getId() == styleId
                    Card(
                        onClick = {
                            styleId = metadata.getId()
                            stylePreferences.setStyleId(styleId)
                            family = preferences.getFontFamily(styleId)
                            weight = preferences.getFontWeight(styleId)
                            timeScale = if (metadata.getId() == UltimateClockStyles.STYLE_PRO_CLASSIC) {
                                preferences.getTimeFontScale()
                            } else {
                                preferences.getTimeFontScale(styleId)
                            }
                            dateScale = if (metadata.getId() == UltimateClockStyles.STYLE_PRO_CLASSIC) {
                                preferences.getDateFontScale()
                            } else {
                                preferences.getDateFontScale(styleId)
                            }
                            supportingScale = preferences.getSupportingFontScale(styleId)
                            palette = stylePreferences.getPalette(styleId)
                        },
                        // Fixed card size keeps every tile in the gallery aligned regardless of
                        // how long the localized name or summary happens to be.
                        modifier = Modifier.width(STYLE_CARD_WIDTH).height(STYLE_CARD_HEIGHT),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // A live thumbnail of the face, painted by the same renderer the clock
                            // uses, so the gallery reads at a glance instead of describing styles
                            // in words.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(MaterialTheme.shapes.small),
                            ) {
                                ClockStyleThumbnail(
                                    styleId = metadata.getId(),
                                    palette = stylePreferences.getPalette(metadata.getId()),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                stringResource(UltimateClockStyles.styleNameRes(metadata.getId())),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(UltimateClockStyles.styleSummaryRes(metadata.getId())),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (UltimateSettingsActivity.shouldShowSecondMotionControls(activeStyle)) {
            SettingSection(stringResource(R.string.ultimate_style_second_motion)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    buildList {
                        if (capabilities.supports(ClockStyleCapabilities.Capability.SMOOTH_SECONDS)) {
                            add(ClockState.SecondHandMotion.SWEEP to R.string.ultimate_motion_smooth)
                        }
                        add(ClockState.SecondHandMotion.TICK to R.string.ultimate_motion_tick)
                        add(ClockState.SecondHandMotion.OFF to R.string.ultimate_motion_off)
                    }.forEach { (value, label) ->
                        FilterChip(
                            selected = motion == value,
                            onClick = {
                                motion = value
                                stylePreferences.setSecondHandMotion(value)
                            },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
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
                            preferences.setFontFamily(styleId, family)
                            weight = option.nearestWeight(weight)
                            preferences.setFontWeight(styleId, weight)
                        },
                        label = { Text(FontCatalog.displayName(context, option.id)) },
                    )
                }
            }
            val font = FontCatalog.optionFor(family)
            val weights = font.availableWeights()
            Text("${stringResource(R.string.ultimate_font_weight)} $weight")
            Slider(
                value = font.indexOfNearestWeight(weight).toFloat(),
                onValueChange = {
                    weight = font.weightAt(it.toInt())
                    preferences.setFontWeight(styleId, weight)
                },
                valueRange = 0f..(weights.size - 1).coerceAtLeast(1).toFloat(),
                steps = (weights.size - 2).coerceAtLeast(0),
            )
            if (supportsTimeScale) {
                SettingSlider(
                    stringResource(R.string.ultimate_time_size),
                    timeScale,
                    ClockPreferences.MIN_FONT_SCALE..ClockPreferences.MAX_FONT_SCALE,
                    stringResource(R.string.ultimate_percent_value, (timeScale * 100).toInt()),
                ) {
                    timeScale = it
                    if (proClassic) preferences.setTimeFontScale(it)
                    else preferences.setTimeFontScale(styleId, it)
                }
            }
            if (supportsDateScale) {
                SettingSlider(
                    stringResource(R.string.ultimate_date_size),
                    dateScale,
                    ClockPreferences.MIN_FONT_SCALE..ClockPreferences.MAX_FONT_SCALE,
                    stringResource(R.string.ultimate_percent_value, (dateScale * 100).toInt()),
                ) {
                    dateScale = it
                    if (proClassic) preferences.setDateFontScale(it)
                    else preferences.setDateFontScale(styleId, it)
                }
            }
            if (supportsSupportingScale) {
                SettingSlider(
                    stringResource(R.string.ultimate_supporting_text_size),
                    supportingScale,
                    ClockPreferences.MIN_SUPPORTING_FONT_SCALE..ClockPreferences.MAX_SUPPORTING_FONT_SCALE,
                    stringResource(R.string.ultimate_percent_value, (supportingScale * 100).toInt()),
                ) {
                    supportingScale = it
                    preferences.setSupportingFontScale(styleId, it)
                }
            }
        }

        if (proClassic) {
            SettingSection(stringResource(R.string.ultimate_time_appearance_section)) {
                StyleColorSetting(
                    label = stringResource(R.string.ultimate_time_color),
                    color = timeColor,
                    onClick = { dialog = StyleDialog.TIME_COLOR },
                )
                SettingSwitch(stringResource(R.string.ultimate_blink_colon), blinkColon) {
                    blinkColon = it
                    preferences.setBlinkColon(it)
                }
                SettingSwitch(stringResource(R.string.ultimate_animate_time_changes), animateDigits) {
                    animateDigits = it
                    preferences.setAnimateTimeChanges(it)
                }
                Text(stringResource(R.string.ultimate_time_transition))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        ClockPreferences.TRANSITION_FADE to R.string.ultimate_transition_fade,
                        ClockPreferences.TRANSITION_SLIDE_UP to R.string.ultimate_transition_slide_up,
                        ClockPreferences.TRANSITION_SLIDE_DOWN to R.string.ultimate_transition_slide_down,
                        ClockPreferences.TRANSITION_SCALE to R.string.ultimate_transition_scale,
                        ClockPreferences.TRANSITION_FLIP to R.string.ultimate_transition_flip,
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = transition == value,
                            onClick = {
                                transition = value
                                preferences.setTimeTransition(value)
                            },
                            enabled = animateDigits,
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                SettingSwitch(
                    stringResource(R.string.ultimate_small_seconds),
                    smallSeconds,
                    enabled = preferences.isShowSeconds(),
                ) {
                    smallSeconds = it
                    preferences.setSmallSeconds(it)
                }
                SettingSwitch(stringResource(R.string.ultimate_portrait_stacked), portraitStacked) {
                    portraitStacked = it
                    preferences.setPortraitStacked(it)
                }
                StyleColorSetting(
                    label = stringResource(R.string.ultimate_date_color),
                    color = dateColor,
                    onClick = { dialog = StyleDialog.DATE_COLOR },
                )
                SettingSwitch(
                    stringResource(R.string.ultimate_date_lunar_dual_line),
                    dualLine,
                    enabled = preferences.isShowLunar(),
                ) {
                    dualLine = it
                    preferences.setDateLunarDualLine(it)
                }
            }
        }

        if (ClockPalette.supports(styleId)) {
            SettingSection(stringResource(R.string.ultimate_style_palette)) {
                Text(
                    stringResource(R.string.ultimate_palette_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PaletteEditor(
                    styleId = styleId,
                    palette = palette,
                    repository = backgroundRepository,
                    appearance = stylePreferences,
                    generation = generation,
                    onChange = {
                        palette = it
                        stylePreferences.setPalette(styleId, it)
                    },
                )
            }
        }

        if (supportsWorldClock) {
            SettingSection(stringResource(R.string.ultimate_world_clock_section)) {
                SettingSwitch(
                    stringResource(R.string.ultimate_world_clock_enabled),
                    worldEnabled,
                    stringResource(R.string.ultimate_world_clock_summary),
                ) {
                    worldEnabled = it
                    worldRepository.setEnabled(it)
                    if (it && worldRepository.getSelected().isEmpty()) {
                        selectedCities = WorldClockCatalog.defaults()
                        worldRepository.save(selectedCities)
                    }
                }
                AssistChip(
                    onClick = { showWorldEditor = true },
                    label = {
                        Text(stringResource(R.string.ultimate_world_clock_count, selectedCities.size))
                    },
                )
            }
        }
    }

    if (showWorldEditor) {
        WorldClockDialog(
            initial = selectedCities,
            onDismiss = { showWorldEditor = false },
            onSave = {
                selectedCities = it
                worldRepository.save(it)
                showWorldEditor = false
            },
        )
    }

    when (dialog) {
        StyleDialog.TIME_COLOR -> StyleColorDialog(
            title = stringResource(R.string.ultimate_time_color),
            pickerLabel = stringResource(R.string.ultimate_time_color_picker),
            initialColor = timeColor,
            onDismiss = { dialog = null },
            onConfirm = {
                timeColor = it
                preferences.setTimeColor(it)
                dialog = null
            },
        )
        StyleDialog.DATE_COLOR -> StyleColorDialog(
            title = stringResource(R.string.ultimate_date_color),
            pickerLabel = stringResource(R.string.ultimate_date_color_picker),
            initialColor = dateColor,
            onDismiss = { dialog = null },
            onConfirm = {
                dateColor = it
                preferences.setDateColor(it)
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun StyleColorSetting(label: String, color: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Text(styleColorSummary(color), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier.size(28.dp).clip(MaterialTheme.shapes.small).background(Color(color)),
        )
    }
}

@Composable
private fun StyleColorDialog(
    title: String,
    pickerLabel: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var draft by rememberSaveable(initialColor) { mutableStateOf(styleColorSummary(initialColor)) }
    val parsed = remember(draft) { parseStyleColor(draft) }
    val presets = CLOCK_COLOR_PRESETS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    presets.forEach { color ->
                        Box(
                            Modifier
                                .size(if (parsed == color) 42.dp else 36.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color(color))
                                .clickable { draft = styleColorSummary(color) },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(9) },
                    label = { Text(pickerLabel) },
                    supportingText = parsed?.let { { Text(styleColorSummary(it)) } },
                    isError = draft.isNotBlank() && parsed == null,
                    singleLine = true,
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

private fun styleColorSummary(color: Int): String = String.format(Locale.ROOT, "#%08X", color)

private fun parseStyleColor(value: String): Int? {
    val raw = value.trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    return runCatching {
        val argb = if (raw.length == 6) "FF$raw" else raw
        argb.toLong(16).toInt()
    }.getOrNull()
}

@Composable
private fun PaletteEditor(
    styleId: String,
    palette: ClockPalette,
    repository: BackgroundRepository,
    appearance: UltimateClockPreferences,
    generation: Int,
    onChange: (ClockPalette) -> Unit,
) {
    // The preview renders through the style's real renderer over the user's real background, so the
    // swatch rows below are a live readout of the theme rather than a blind colour picker.
    val background = previewClockBackground(repository, appearance, generation)
    Box(
        Modifier
            .fillMaxWidth()
            .height(PALETTE_PREVIEW_HEIGHT)
            .clip(MaterialTheme.shapes.medium),
    ) {
        ClockPreviewCanvas(
            styleId = styleId,
            palette = palette,
            background = background,
            modifier = Modifier.fillMaxSize(),
        )
    }
    listOf(
        0 to R.string.ultimate_palette_background,
        1 to R.string.ultimate_palette_panel,
        2 to R.string.ultimate_palette_accent,
    ).forEach { (role, label) ->
        Text(stringResource(label))
        // A palette saved by an older build can hold a colour this ramp no longer offers, so keep
        // it in the row as the selected swatch instead of silently dropping the highlight.
        val current = palette.color(role)
        val swatches = if (current in CLOCK_COLOR_PRESETS) {
            CLOCK_COLOR_PRESETS
        } else {
            listOf(current) + CLOCK_COLOR_PRESETS
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            swatches.forEach { color ->
                val selected = current == color
                Box(
                    Modifier
                        .size(if (selected) 38.dp else 34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(color))
                        .clickable { onChange(palette.withColor(role, color)) },
                )
            }
        }
    }
    SettingSwitch(
        stringResource(R.string.ultimate_palette_gaussian_blur),
        palette.gaussianBlur,
        stringResource(R.string.ultimate_palette_gaussian_blur_summary),
    ) { onChange(palette.withGaussianBlur(it)) }
    SettingSlider(
        stringResource(R.string.ultimate_palette_blur_strength),
        palette.blurStrength.toFloat(),
        0f..100f,
        "${palette.blurStrength}%",
    ) { onChange(palette.withBlurStrength(it.toInt())) }
    SettingSlider(
        stringResource(R.string.ultimate_palette_blur_brightness),
        palette.blurBrightness.toFloat(),
        0f..100f,
        "${palette.blurBrightness}%",
    ) { onChange(palette.withBlurBrightness(it.toInt())) }
    SettingSwitch(
        stringResource(R.string.ultimate_palette_card_shadow),
        palette.cardShadow,
        stringResource(R.string.ultimate_palette_card_shadow_summary),
    ) { onChange(palette.withCardShadow(it)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorldClockDialog(
    initial: List<WorldClockEntry>,
    onDismiss: () -> Unit,
    onSave: (List<WorldClockEntry>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(initial) }
    val context = LocalContext.current
    val language = remember(context) {
        WorldClockCatalog.languageOf(LocaleManager.resolveLocale(context))
    }
    val results = remember(query) { WorldClockCatalog.search(query).take(100) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ultimate_world_clock_title)) },
        text = {
            Column(Modifier.fillMaxHeight(.78f)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.ultimate_world_clock_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.ultimate_world_clock_counter, selected.size),
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it.getId() }) { entry ->
                        val index = selected.indexOf(entry)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.getFlagEmoji(), Modifier.padding(end = 8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(WorldClockCatalog.displayCity(entry, language))
                                Text(
                                    WorldClockCatalog.displayCountry(entry, language) +
                                        " · " + entry.getZoneId(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (index >= 0) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) selected = selected.toMutableList().also {
                                            val value = it.removeAt(index)
                                            it.add(index - 1, value)
                                        }
                                    },
                                    enabled = index > 0,
                                ) { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                                IconButton(
                                    onClick = {
                                        if (index < selected.lastIndex) {
                                            selected = selected.toMutableList().also {
                                                val value = it.removeAt(index)
                                                it.add(index + 1, value)
                                            }
                                        }
                                    },
                                    enabled = index < selected.lastIndex,
                                ) { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                                IconButton(onClick = { selected = selected - entry }) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                }
                            } else {
                                IconButton(
                                    onClick = { selected = selected + entry },
                                    enabled = selected.size < WorldClockRepository.MAX_SELECTED,
                                ) { Icon(Icons.Default.Add, contentDescription = null) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) {
                Text(stringResource(R.string.ultimate_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ultimate_cancel))
            }
        },
    )
}
