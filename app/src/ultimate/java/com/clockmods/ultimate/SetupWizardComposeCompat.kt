package com.clockmods.ultimate

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.background.BackgroundRepository
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.ultimate.compose.CalendarThemeCatalog
import com.clockmods.ultimate.compose.CalendarThemeThumbnail
import com.clockmods.ultimate.compose.ClockStyleThumbnail
import com.clockmods.ultimate.compose.previewClockBackground

/** Compose-native first-run setup flow. */
class SetupWizardActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase) ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockModsTheme {
                SetupWizard(
                    onFinish = { draft ->
                        draft.applyTo(this)
                        markCompleted(this)
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onSkip = {
                        markCompleted(this)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val PREFS = "clockmods_onboarding"
        private const val KEY_COMPLETED = "completed"

        @JvmStatic
        fun isCompleted(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPLETED, false)

        @JvmStatic
        fun markCompleted(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COMPLETED, true).apply()
        }
    }
}

private data class SetupDraft(
    val language: String,
    val weatherEnabled: Boolean,
    val temperatureUnit: String,
    val styleId: String,
    val calendarThemeId: String,
) {
    fun applyTo(context: Context) {
        ClockPreferences(context).apply {
            setClockLanguage(language)
            setWeatherEnabled(weatherEnabled)
            if (getWeatherLocationMode() != ClockPreferences.WEATHER_LOCATION_MANUAL) {
                setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_AUTOMATIC)
            }
            setWeatherTemperatureUnit(temperatureUnit)
            setCalendarTheme(calendarThemeId)
        }
        UltimateClockPreferences(context).setStyleId(styleId)
    }
}

@Composable
private fun SetupWizard(
    onFinish: (SetupDraft) -> Unit,
    onSkip: () -> Unit,
) {
    val baseContext = LocalContext.current
    val preferences = ClockPreferences(baseContext)
    var step by rememberSaveable { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableStateOf<String?>(null) }
    var weatherEnabled by rememberSaveable { mutableStateOf(preferences.isWeatherEnabled()) }
    var temperatureUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var styleId by rememberSaveable { mutableStateOf<String?>(null) }
    var calendarThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    val canContinue = canContinueSetupStep(
        step, language, weatherEnabled, temperatureUnit, styleId, calendarThemeId,
    )

    val currentConfiguration = LocalConfiguration.current
    val configuration = Configuration(currentConfiguration).apply {
        setLocale(LocaleManager.resolveLocale(language ?: preferences.getClockLanguage()))
    }
    val localizedContext = baseContext.createConfigurationContext(configuration)

    fun finish() = onFinish(
        SetupDraft(
            checkNotNull(language),
            weatherEnabled,
            temperatureUnit ?: preferences.getWeatherTemperatureUnit(),
            checkNotNull(styleId),
            checkNotNull(calendarThemeId),
        ),
    )
    fun goBack() {
        if (step > 0) step-- else onSkip()
    }

    BackHandler(onBack = ::goBack)
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
    ) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val horizontalPadding = if (maxWidth >= 600.dp) 32.dp else 20.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                ) {
                    Text("ClockMods Ultimate", style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.setup_wizard_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (step + 1) / STEP_COUNT.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.setup_wizard_step, step + 1, STEP_COUNT),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    key(step) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 20.dp),
                        ) {
                            when (step) {
                                0 -> LanguageStep(language, onSelected = { language = it })
                                1 -> WeatherStep(
                                    enabled = weatherEnabled,
                                    unit = temperatureUnit,
                                    onEnabledChanged = { weatherEnabled = it },
                                    onUnitSelected = { temperatureUnit = it },
                                )
                                2 -> StyleStep(styleId, onSelected = { styleId = it })
                                else -> CalendarStyleStep(calendarThemeId,
                                    onSelected = { calendarThemeId = it })
                            }
                        }
                    }
                    HorizontalDivider()
                    if (!canContinue) {
                        Text(
                            stringResource(R.string.setup_wizard_choose_option),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = ::goBack, modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(
                                    if (step == 0) R.string.setup_wizard_skip
                                    else R.string.setup_wizard_back,
                                ),
                            )
                        }
                        Button(
                            onClick = { if (step < STEP_COUNT - 1) step++ else finish() },
                            modifier = Modifier.weight(1f),
                            enabled = canContinue,
                        ) {
                            Text(
                                stringResource(
                                    if (step == STEP_COUNT - 1) R.string.setup_wizard_done
                                    else R.string.setup_wizard_next,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageStep(selected: String?, onSelected: (String) -> Unit) {
    StepHeading(
        icon = { Icon(Icons.Default.Language, contentDescription = null) },
        title = R.string.setup_wizard_language_title,
        summary = R.string.setup_wizard_language_summary,
    )
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        listOf(
            ClockPreferences.LANGUAGE_SIMPLIFIED to R.string.ultimate_language_simplified,
            ClockPreferences.LANGUAGE_TRADITIONAL to R.string.ultimate_language_traditional,
            ClockPreferences.LANGUAGE_ENGLISH to R.string.ultimate_language_english,
        ).forEach { (value, label) ->
            ChoiceRow(
                label = stringResource(label),
                selected = selected == value,
                onClick = { onSelected(value) },
            )
        }
    }
}

@Composable
private fun WeatherStep(
    enabled: Boolean,
    unit: String?,
    onEnabledChanged: (Boolean) -> Unit,
    onUnitSelected: (String) -> Unit,
) {
    StepHeading(
        icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
        title = R.string.setup_wizard_weather_title,
        summary = R.string.setup_wizard_weather_summary,
    )
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChanged)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.setup_wizard_weather_enabled))
            Text(
                stringResource(R.string.setup_wizard_weather_enabled_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
    if (enabled) {
        Text(
            stringResource(R.string.setup_wizard_temperature_title),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            fontWeight = FontWeight.SemiBold,
        )
        Column(Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceRow(
                label = stringResource(R.string.ultimate_weather_celsius),
                selected = unit == ClockPreferences.WEATHER_UNIT_CELSIUS,
                onClick = { onUnitSelected(ClockPreferences.WEATHER_UNIT_CELSIUS) },
            )
            ChoiceRow(
                label = stringResource(R.string.ultimate_weather_fahrenheit),
                selected = unit == ClockPreferences.WEATHER_UNIT_FAHRENHEIT,
                onClick = { onUnitSelected(ClockPreferences.WEATHER_UNIT_FAHRENHEIT) },
            )
        }
    }
}

@Composable
private fun StyleStep(selected: String?, onSelected: (String) -> Unit) {
    StepHeading(
        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = R.string.setup_wizard_style_title,
        summary = R.string.setup_wizard_style_summary,
    )
    val context = LocalContext.current
    val styles = remember { UltimateClockStyles.builtIns() }
    val repository = remember(context) { BackgroundRepository(context) }
    val appearance = remember(context) { UltimateClockPreferences(context) }
    val background = previewClockBackground(repository, appearance, 0)
    Column(Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        styles.forEach { style ->
            val id = style.getMetadata().getId()
            ThemeChoiceCard(
                title = stringResource(UltimateClockStyles.styleNameRes(id)),
                summary = stringResource(UltimateClockStyles.styleSummaryRes(id)),
                selected = selected == id,
                onClick = { onSelected(id) },
            ) {
                ClockStyleThumbnail(
                    styleId = id,
                    palette = appearance.getPalette(id),
                    background = background,
                    repository = repository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CalendarStyleStep(selected: String?, onSelected: (String) -> Unit) {
    StepHeading(
        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = R.string.setup_wizard_calendar_style_title,
        summary = R.string.setup_wizard_calendar_style_summary,
    )
    val themes = remember { CalendarThemeCatalog.presets() }
    Column(Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        themes.forEach { theme ->
            ThemeChoiceCard(
                title = stringResource(theme.nameRes),
                summary = stringResource(theme.summaryRes),
                selected = selected == theme.id,
                onClick = { onSelected(theme.id) },
            ) {
                CalendarThemeThumbnail(theme, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected, role = Role.RadioButton, onClick = onClick,
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 104.dp, height = 76.dp)
                .clip(RoundedCornerShape(6.dp))) { preview() }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            if (selected) Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}

@Composable
private fun StepHeading(
    icon: @Composable () -> Unit,
    @StringRes title: Int,
    @StringRes summary: Int,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(Modifier.padding(12.dp)) { icon() }
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(summary),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            label,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

internal fun canContinueSetupStep(
    step: Int,
    language: String?,
    weatherEnabled: Boolean,
    temperatureUnit: String?,
    styleId: String?,
    calendarThemeId: String?,
): Boolean = when (step) {
    0 -> language != null
    1 -> !weatherEnabled || temperatureUnit != null
    2 -> styleId != null
    3 -> calendarThemeId != null
    else -> false
}

private const val STEP_COUNT = 4
