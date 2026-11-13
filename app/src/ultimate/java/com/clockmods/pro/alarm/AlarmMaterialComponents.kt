package com.clockmods.pro.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.clockmods.R
import com.clockmods.ui.compose.TimeText
import com.clockmods.ultimate.navigation.PredictiveAlertDialog as AlertDialog
import com.clockmods.ultimate.navigation.PredictiveDialogBack
import java.time.*
import java.time.format.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmScaffold(title: String, back: () -> Unit, save: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.font_import_back)) }
        }) },
        bottomBar = { Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = save, modifier = Modifier.height(48.dp).widthIn(min = 112.dp)) { Text(stringResource(R.string.apply)) }
            }
        } }, content = content,
    )
}

@Composable
internal fun AlarmColumn(insets: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), content = content)
    }
}

@Composable
internal fun AlarmRow(icon: ImageVector, title: String, value: String = "", onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = trailing ?: if (value.isNotEmpty()) ({
                TimeText(value, Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp / 3).coerceAtMost(160).dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.End)
            }) else null,
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.heightIn(min = 68.dp),
        )
    }
}

@Composable
internal fun AlarmTimeHeader(minutes: Int, click: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        TimeText(String.format(locale, "%02d:%02d", minutes / 60, minutes % 60),
            Modifier.weight(1f).clickable(onClick = click), fontSize = 64.sp, maxLines = 1)
        FilledTonalIconButton(onClick = click, modifier = Modifier.size(56.dp)) { Icon(Icons.Outlined.Edit, stringResource(R.string.alarm_choose_time)) }
    }
}

@Composable
internal fun AlarmWeekdays(selected: Int, change: (Int) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (index in 0..6) {
            val day = DayOfWeek.of(index + 1)
            FilledTonalIconToggleButton(checked = selected and (1 shl index) != 0, onCheckedChange = { change(selected xor (1 shl index)) },
                modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }) {
                Text(day.getDisplayName(TextStyle.NARROW_STANDALONE, locale))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmTimeDialog(minutes: Int, is24: Boolean, dismiss: () -> Unit, confirm: (Int) -> Unit) {
    val picker = rememberTimePickerState(minutes / 60, minutes % 60, is24)
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenHeightDp < 600
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.alarm_choose_time)) },
        text = { Column(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp - 160).coerceAtLeast(160).dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            if (keyboard) TimeInput(picker) else TimePicker(picker,
                layoutType = if (compact) TimePickerLayoutType.Horizontal else TimePickerLayoutType.Vertical)
            IconButton(onClick = { keyboard = !keyboard }, modifier = Modifier.align(Alignment.Start)) {
                Icon(if (keyboard) Icons.Outlined.Schedule else Icons.Outlined.Keyboard, stringResource(R.string.alarm_choose_time))
            }
        } }, confirmButton = { TextButton(onClick = { confirm(picker.hour * 60 + picker.minute) }) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
        properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.widthIn(max = if (compact) 700.dp else 560.dp).padding(horizontal = 24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmDateDialog(date: LocalDate, dismiss: () -> Unit, confirm: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(onDismissRequest = dismiss, confirmButton = {
        PredictiveDialogBack(dismiss)
        TextButton(onClick = { picker.selectedDateMillis?.let { confirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } }, enabled = picker.selectedDateMillis != null) { Text(stringResource(R.string.apply)) }
    }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } }) { DatePicker(picker) }
}

@Composable
internal fun AlarmNumberDialog(title: String, value: Int, minimum: Int, maximum: Int, dismiss: () -> Unit, confirm: (Int) -> Unit) {
    var input by rememberSaveable { mutableStateOf(value.toString()) }
    val number = input.toIntOrNull()
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        OutlinedTextField(value = input, onValueChange = { input = it.filter(Char::isDigit).take(2) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), supportingText = { Text("$minimum–$maximum") },
            isError = number == null || number !in minimum..maximum, modifier = Modifier.fillMaxWidth())
    }, confirmButton = { TextButton(onClick = { number?.let(confirm) }, enabled = number != null && number in minimum..maximum) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun alarmDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}
