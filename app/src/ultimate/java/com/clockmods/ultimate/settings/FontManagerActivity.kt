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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.OpenTypeFont
import com.clockmods.background.UserFontStore
import com.clockmods.ui.compose.ClockModsTheme

/** Compose-native Material 3 library; the retained model owns file operations. */
class FontManagerActivity : ComponentActivity() {
    private lateinit var model: FontImportModel
    private var revision by mutableIntStateOf(0)
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        if (it.isNotEmpty()) model.importFiles(it)
    }
    override fun attachBaseContext(base: Context) = super.attachBaseContext(LocaleManager.wrap(base) ?: base)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UserFontStore.initialize(this)
        model = ViewModelProvider(this)[FontImportModel::class.java]
        model.state.observe(this) { revision++ }
        setContent { ClockModsTheme { FontLibrary(revision, model, ::finish) { picker.launch(arrayOf("*/*")) } } }
    }
    companion object {
        @JvmStatic fun coverage(context: Context, font: OpenTypeFont): String =
            context.getString(R.string.font_import_coverage, font.coverage(FontLanguageSamples.CHINESE),
                font.coverage(FontLanguageSamples.ENGLISH), font.coverage(FontLanguageSamples.JAPANESE), font.coverage(FontLanguageSamples.KOREAN))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontLibrary(revision: Int, model: FontImportModel, onBack: () -> Unit, onImport: () -> Unit) {
    val families = remember(revision) { UserFontStore.families().toList() }
    // Capture each model publication so Scaffold/LazyColumn slots observe operation changes.
    val busy = model.busy
    val results = model.results.toList()
    val context = LocalContext.current
    var removing by remember { mutableStateOf<UserFontStore.ImportedFile?>(null) }
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(stringResource(R.string.font_library_title)) },
            navigationIcon = { IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.font_import_back))
            } },
        ) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!busy) onImport() },
                // Extended FAB suppresses its text semantics; name the icon for accessibility.
                icon = { Icon(Icons.Outlined.Add, stringResource(R.string.font_import_add)) },
                text = { Text(stringResource(R.string.font_import_add)) },
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
                item { Text(stringResource(R.string.font_import_summary),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (busy) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.font_import_busy), style = MaterialTheme.typography.bodyMedium)
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
                if (families.isEmpty()) item {
                    Card(shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Aa", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.font_import_empty), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.font_library_empty_hint),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(families, key = { it.id }) { family ->
                    Card(shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(family.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                            Text("12:34 · Aa · 中文 · 日本語 · 한국어", style = MaterialTheme.typography.headlineSmall,
                                fontFamily = remember(family.id, revision) { FontFamily(UserFontStore.resolve(family.id, 400)) })
                            family.faces.distinctBy { it.source.id }.forEach { face ->
                                HorizontalDivider()
                                Text(face.source.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.font_import_weights, face.font.weights.joinToString(" · ")),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FontManagerActivity.coverage(context, face.font), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val deleteLabel = stringResource(R.string.font_import_delete)
                                TextButton(
                                    onClick = { removing = face.source }, enabled = !busy,
                                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                                        .semantics { contentDescription = "$deleteLabel ${face.source.name}" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text(deleteLabel) }
                            }
                        }
                    }
                }
            }
        }
    }
    removing?.let { file ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.font_import_delete)) },
            text = { Text(stringResource(R.string.font_import_delete_confirm, file.name)) },
            confirmButton = { TextButton(onClick = { model.delete(file.id); removing = null }) {
                Text(stringResource(R.string.font_import_delete), color = MaterialTheme.colorScheme.error)
            } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
