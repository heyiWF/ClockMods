package com.clockmods.widget.config

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clockmods.R
import com.clockmods.LocaleManager
import com.clockmods.ui.compose.ClockModsTheme
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.render.WidgetFontRegistry
import com.clockmods.widget.render.WidgetThemeRegistry
import com.clockmods.widget.store.WidgetConfigStore
import com.clockmods.widget.update.WidgetUpdateCoordinator
import java.util.TimeZone
import kotlin.math.roundToInt

/** Shared launcher configuration flow for every widget provider. */
class WidgetConfigActivity : ComponentActivity() {
    private lateinit var draft: WidgetConfig
    private var loadedConfig by mutableStateOf<WidgetConfig?>(null)
    private var dateToggleForInstrumentation: (() -> Unit)? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase) ?: newBase)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setLocale(LocaleManager.resolveLocale(this))
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val kind = WidgetUpdateCoordinator.kindFor(this, id)
        if (kind == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            ClockModsTheme {
                val loaded = loadedConfig
                if (loaded == null) {
                    WidgetConfigLoading(onCancel = ::finish)
                } else {
                    WidgetConfigScreen(
                        initial = loaded,
                        onDraftChanged = { draft = it },
                        onCancel = ::finish,
                        onDone = ::commit,
                        onDateToggleReady = { callback ->
                            dateToggleForInstrumentation = callback
                        },
                    )
                }
            }
        }
        WidgetUpdateCoordinator.execute {
            val loaded = if (state == null) {
                WidgetConfigStore(this).getOrDefault(id, kind)
            } else {
                WidgetConfigStore.decode(state.getString(STATE_DRAFT), id, kind)
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                draft = loaded
                loadedConfig = loaded
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::draft.isInitialized) outState.putString(STATE_DRAFT, WidgetConfigStore.encode(draft))
    }

    private fun commit(value: WidgetConfig) {
        val saved = value.toBuilder().updatedAt(System.currentTimeMillis()).build()
        draft = saved
        WidgetUpdateCoordinator.execute {
            if (WidgetUpdateCoordinator.kindFor(this, saved.appWidgetId) == null) {
                runOnUiThread { finish() }
                return@execute
            }
            WidgetConfigStore(this).save(saved)
            WidgetUpdateCoordinator.updateOne(this, saved.appWidgetId)
            WidgetUpdateCoordinator.reconcile(this)
            runOnUiThread {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, saved.appWidgetId),
                )
                finish()
            }
        }
    }

    /**
     * Lets the device-level widget contract test exercise a Compose-only control.
     * The callback exists only while the configuration screen is composed and never writes data.
     */
    fun toggleDateForInstrumentation(): Boolean {
        val toggle = dateToggleForInstrumentation ?: return false
        toggle()
        return true
    }

    private companion object {
        const val STATE_DRAFT = "draft"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigLoading(onCancel: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_configure)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.widget_close),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    initial: WidgetConfig,
    onDraftChanged: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
    onDone: (WidgetConfig) -> Unit,
    onDateToggleReady: (((() -> Unit)?) -> Unit),
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(initial) }
    var saving by remember { mutableStateOf(false) }
    fun update(value: WidgetConfig) {
        draft = value
        onDraftChanged(value)
    }

    DisposableEffect(Unit) {
        onDateToggleReady {
            update(draft.toBuilder().showDate(!draft.showDate).build())
        }
        onDispose { onDateToggleReady(null) }
    }

    val themes = remember(context) { WidgetThemeRegistry.all(context) }
    val fontLabels = remember(context) { WidgetFontRegistry.labels(context).toList() }
    val timeLabels = listOf(
        stringResource(R.string.widget_system),
        stringResource(R.string.widget_24h),
        stringResource(R.string.widget_12h),
    )
    val systemZoneLabel = stringResource(R.string.widget_system)
    val zones = remember(systemZoneLabel) {
        TimeZone.getAvailableIDs().sorted().toMutableList().apply {
            add(0, systemZoneLabel)
        }
    }
    val tapLabels = listOf(
        stringResource(R.string.widget_open_clock),
        stringResource(R.string.widget_open_calendar),
        stringResource(R.string.widget_open_weather),
        stringResource(R.string.widget_open_config),
    )
    val isClock = draft.kind == WidgetKind.DIGITAL || draft.kind == WidgetKind.WEATHER

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_configure)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.widget_close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    modifier = Modifier.testTag("widget_config_reset"),
                    onClick = {
                        update(WidgetConfig.builder(draft.appWidgetId, draft.kind).build())
                    },
                    enabled = !saving,
                ) { Text(stringResource(R.string.widget_reset)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    modifier = Modifier.testTag("widget_config_done"),
                    onClick = {
                        saving = true
                        onDone(draft)
                    },
                    enabled = !saving,
                ) { Text(stringResource(R.string.widget_done)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            SectionTitle(R.string.widget_preview)
            WidgetPreview(draft)
            SectionTitle(R.string.widget_theme)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themes.forEach { theme ->
                    FilterChip(
                        selected = theme.id == draft.themeId,
                        onClick = {
                            update(
                                draft.toBuilder()
                                    .themeId(theme.id)
                                    .backgroundAlpha(theme.defaultBackgroundAlpha)
                                    .build(),
                            )
                        },
                        label = { Text(stringResource(theme.displayNameRes)) },
                    )
                }
            }
            ConfigSwitch(
                R.string.widget_dark_text,
                draft.darkText,
                { update(draft.toBuilder().darkText(it).build()) },
                visible = draft.themeId == "transparent.clean",
            )
            ConfigDropdown(
                R.string.widget_font,
                fontLabels,
                WidgetFontRegistry.indexOf(draft.fontId),
            ) { update(draft.toBuilder().fontId(WidgetFontRegistry.idAt(it)).build()) }

            SectionTitle(R.string.widget_content)
            ConfigSwitch(
                R.string.widget_date,
                draft.showDate,
                { update(draft.toBuilder().showDate(it).build()) },
                modifier = Modifier.testTag("widget_config_date"),
            )
            ConfigSwitch(R.string.widget_weekday, draft.showWeekday, onChanged = {
                update(draft.toBuilder().showWeekday(it).build())
            })
            ConfigSwitch(
                R.string.widget_lunar,
                draft.showLunar,
                { update(draft.toBuilder().showLunar(it).build()) },
                visible = draft.kind != WidgetKind.ANALOG,
            )
            ConfigSwitch(R.string.widget_location, draft.showLocation, onChanged = {
                update(draft.toBuilder().showLocation(it).build())
            })
            ConfigSwitch(
                R.string.widget_description,
                draft.showWeatherDescription,
                { update(draft.toBuilder().showWeatherDescription(it).build()) },
                visible = draft.kind == WidgetKind.WEATHER,
            )
            ConfigSwitch(
                R.string.widget_seconds,
                draft.showSeconds,
                { update(draft.toBuilder().showSeconds(it).build()) },
                visible = isClock,
            )
            ConfigDropdown(
                R.string.widget_time_format,
                timeLabels,
                if (draft.useSystemTimeFormat) 0 else if (draft.use24Hour) 1 else 2,
                visible = isClock,
            ) { index ->
                update(
                    draft.toBuilder()
                        .useSystemTimeFormat(index == 0)
                        .use24Hour(index != 2)
                        .build(),
                )
            }
            ConfigDropdown(
                R.string.widget_zone,
                zones,
                if (draft.useSystemTimeZone) 0 else zones.indexOf(draft.timeZoneId).coerceAtLeast(1),
            ) { index ->
                update(
                    draft.toBuilder()
                        .useSystemTimeZone(index == 0)
                        .timeZoneId(if (index == 0) TimeZone.getDefault().id else zones[index])
                        .build(),
                )
            }
            ConfigDropdown(
                R.string.widget_tap,
                tapLabels,
                TAP_ACTIONS.indexOf(draft.tapAction).coerceAtLeast(0),
            ) { update(draft.toBuilder().tapAction(TAP_ACTIONS[it]).build()) }
            SliderSetting(
                R.string.widget_opacity,
                draft.backgroundAlpha.toFloat(),
                0f..255f,
                { "${it.roundToInt() * 100 / 255}%" },
            ) { update(draft.toBuilder().backgroundAlpha(it.roundToInt()).build()) }
            SliderSetting(
                R.string.widget_scale,
                draft.textScale * 100f,
                (SCALE_MIN * 100f)..(SCALE_MAX * 100f),
                { "${it.roundToInt()}%" },
            ) { update(draft.toBuilder().textScale(it / 100f).build()) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ConfigSwitch(
    @StringRes label: Int,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    if (!visible) return
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun ConfigDropdown(
    @StringRes label: Int,
    values: List<String>,
    selected: Int,
    visible: Boolean = true,
    onSelected: (Int) -> Unit,
) {
    if (!visible) return
    var expanded by remember { mutableStateOf(false) }
    val index = selected.coerceIn(0, (values.size - 1).coerceAtLeast(0))
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(values.getOrElse(index) { "" }) }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                values.forEachIndexed { itemIndex, value ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            expanded = false
                            onSelected(itemIndex)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    @StringRes label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatter: (Float) -> String,
    onChanged: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Text(formatter(value), color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChanged,
        valueRange = range,
    )
}

private val TAP_ACTIONS = arrayOf("open_clock", "open_calendar", "open_weather", "open_config")
private const val SCALE_MIN = .85f
private const val SCALE_MAX = 1.20f
