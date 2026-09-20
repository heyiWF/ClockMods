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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles

/** Compose-native, three-step first-run setup flow. */
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
) {
    fun applyTo(context: Context) {
        ClockPreferences(context).apply {
            setClockLanguage(language)
            setWeatherEnabled(weatherEnabled)
            if (getWeatherLocationMode() != ClockPreferences.WEATHER_LOCATION_MANUAL) {
                setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_AUTOMATIC)
            }
            setWeatherTemperatureUnit(temperatureUnit)
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
    val appearance = UltimateClockPreferences(baseContext)
    var step by rememberSaveable { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableStateOf(preferences.getClockLanguage()) }
    var weatherEnabled by rememberSaveable { mutableStateOf(preferences.isWeatherEnabled()) }
    var temperatureUnit by rememberSaveable {
        mutableStateOf(preferences.getWeatherTemperatureUnit())
    }
    var styleId by rememberSaveable { mutableStateOf(appearance.getStyleId()) }

    val currentConfiguration = LocalConfiguration.current
    val configuration = Configuration(currentConfiguration).apply {
        setLocale(LocaleManager.resolveLocale(language))
    }
    val localizedContext = baseContext.createConfigurationContext(configuration)

    fun finish() = onFinish(
        SetupDraft(language, weatherEnabled, temperatureUnit, styleId),
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
                            else -> StyleStep(styleId, onSelected = { styleId = it })
                        }
                    }
                    HorizontalDivider()
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
private fun LanguageStep(selected: String, onSelected: (String) -> Unit) {
    StepHeading(
        icon = { Icon(Icons.Default.Language, contentDescription = null) },
        title = R.string.setup_wizard_language_title,
        summary = R.string.setup_wizard_language_summary,
    )
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

@Composable
private fun WeatherStep(
    enabled: Boolean,
    unit: String,
    onEnabledChanged: (Boolean) -> Unit,
    onUnitSelected: (String) -> Unit,
) {
    StepHeading(
        icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
        title = R.string.setup_wizard_weather_title,
        summary = R.string.setup_wizard_weather_summary,
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
    Text(
        stringResource(R.string.setup_wizard_temperature_title),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        fontWeight = FontWeight.SemiBold,
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceRow(
            label = stringResource(R.string.ultimate_weather_celsius),
            selected = unit == ClockPreferences.WEATHER_UNIT_CELSIUS,
            enabled = enabled,
            onClick = { onUnitSelected(ClockPreferences.WEATHER_UNIT_CELSIUS) },
        )
        ChoiceRow(
            label = stringResource(R.string.ultimate_weather_fahrenheit),
            selected = unit == ClockPreferences.WEATHER_UNIT_FAHRENHEIT,
            enabled = enabled,
            onClick = { onUnitSelected(ClockPreferences.WEATHER_UNIT_FAHRENHEIT) },
        )
    }
}

@Composable
private fun StyleStep(selected: String, onSelected: (String) -> Unit) {
    StepHeading(
        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = R.string.setup_wizard_style_title,
        summary = R.string.setup_wizard_style_summary,
    )
    val styles = listOf(
        Triple(
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            R.string.ultimate_style_glass_name,
            R.string.ultimate_style_glass_summary,
        ),
        Triple(
            UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
            R.string.ultimate_style_noir_name,
            R.string.ultimate_style_noir_summary,
        ),
        Triple(
            UltimateClockStyles.STYLE_PAPER_STATION,
            R.string.ultimate_style_paper_name,
            R.string.ultimate_style_paper_summary,
        ),
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        styles.forEach { (id, title, summary) ->
            val isSelected = selected == id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(id) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (isSelected) {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
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
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = onClick)
        Text(
            label,
            modifier = Modifier.padding(start = 8.dp),
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
    }
}

private const val STEP_COUNT = 3
