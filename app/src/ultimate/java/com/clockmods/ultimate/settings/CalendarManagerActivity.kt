package com.clockmods.ultimate.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.calendar.imports.UserCalendarStore
import com.clockmods.ui.compose.ClockModsTheme

/** Compose-native Material 3 library; the retained model owns file operations. */
class CalendarManagerActivity : ComponentActivity() {
    private lateinit var model: CalendarImportModel
    private var revision by mutableIntStateOf(0)
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        if (it.isNotEmpty()) model.importFiles(it)
    }
    override fun attachBaseContext(base: Context) = super.attachBaseContext(LocaleManager.wrap(base) ?: base)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UserCalendarStore.get(this)
        model = ViewModelProvider(this)[CalendarImportModel::class.java]
        model.state.observe(this) { revision++ }
        UserCalendarStore.get(this).changes.observe(this) { revision++ }
        setContent { ClockModsTheme { CalendarLibrary(revision, model, ::finish) { picker.launch(arrayOf("*/*")) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarLibrary(revision: Int, model: CalendarImportModel, onBack: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { UserCalendarStore.get(context) }
    val files = remember(revision) { store.files().toList() }
    // Capture each model publication so Scaffold/LazyColumn slots observe operation changes.
    val busy = model.busy
    val results = model.results.toList()
    var showLink by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var linkError by rememberSaveable { mutableStateOf(false) }
    val showEvents = remember(revision) { store.showInDateCarousel() }
    val carouselLabel = stringResource(R.string.calendar_import_carousel)
    var removing by remember { mutableStateOf<UserCalendarStore.Source?>(null) }
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(stringResource(R.string.calendar_import_title)) },
            navigationIcon = { IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.font_import_back))
            } },
        ) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!busy) onImport() },
                // Extended FAB suppresses its text semantics; name the icon for accessibility.
                icon = { Icon(Icons.Outlined.Add, stringResource(R.string.calendar_import_add)) },
                text = { Text(stringResource(R.string.calendar_import_add)) },
                modifier = Modifier.semantics { if (busy) disabled() },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text(stringResource(R.string.calendar_import_summary),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showLink = true }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(painterResource(R.drawable.import_ic_link), null)
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.calendar_import_link))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(stringResource(R.string.calendar_import_carousel), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.calendar_import_carousel_hint),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = showEvents, onCheckedChange = { store.setShowInDateCarousel(it) },
                                modifier = Modifier.semantics { contentDescription = carouselLabel })
                        }
                    }
                }
                if (busy) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.calendar_import_busy), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                items(results) { result ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(20.dp)) {
                        Text(stringResource(result.message, result.name),
                            Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (files.isEmpty()) item {
                    Card(shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.calendar_import_empty), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.calendar_import_empty_hint),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(files, key = { it.id }) { file ->
                    Card(shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(file.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                            Text(file.name, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.calendar_import_count, file.eventCount,
                                android.text.format.Formatter.formatShortFileSize(context, file.bytes)),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (store.error(file.id) != null) Text(stringResource(R.string.calendar_import_query_error),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            val deleteLabel = stringResource(R.string.calendar_import_delete)
                            TextButton(onClick = { removing = file }, enabled = !busy,
                                modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                                    .semantics { contentDescription = "$deleteLabel ${file.name}" },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text(deleteLabel) }
                        }
                    }
                }
            }
        }
    }
    if (showLink) AlertDialog(
        onDismissRequest = { showLink = false },
        title = { Text(stringResource(R.string.calendar_import_link)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.calendar_import_link_hint))
            OutlinedTextField(value = link, onValueChange = { link = it; linkError = false },
                label = { Text(stringResource(R.string.calendar_import_url)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), isError = linkError,
                supportingText = { if (linkError) Text(stringResource(R.string.calendar_import_url_invalid)) })
        } },
        confirmButton = { TextButton(enabled = !busy && link.isNotBlank(), onClick = {
            try {
                com.clockmods.calendar.imports.CalendarLink.normalize(link)
                model.importLink(link); showLink = false; link = ""
            } catch (_: java.io.IOException) { linkError = true }
        }) { Text(stringResource(R.string.calendar_import_add)) } },
        dismissButton = { TextButton(onClick = { showLink = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
    removing?.let { file ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.calendar_import_delete)) },
            text = { Text(stringResource(R.string.calendar_import_delete_confirm, file.name)) },
            confirmButton = { TextButton(onClick = { model.delete(file.id); removing = null }) {
                Text(stringResource(R.string.calendar_import_delete), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
