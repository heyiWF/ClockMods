package com.clockmods.ultimate.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clockmods.R
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ThemeLibrary
import com.clockmods.ultimate.clock.UltimateClockPreferences
import com.clockmods.ultimate.clock.UltimateClockStyles
import com.clockmods.ultimate.compose.CalendarThemeCatalog
import com.clockmods.ultimate.compose.CalendarThemeThumbnail
import com.clockmods.ultimate.compose.ClockStyleThumbnail
import com.clockmods.ultimate.compose.previewClockBackground

@Composable
internal fun ThemeLibrarySection(calendar: Boolean, onChanged: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember(calendar) {
        if (calendar) CalendarThemeCatalog.presets().map { it.id }
        else UltimateClockStyles.builtIns().map { it.getMetadata().getId() }
    }
    val library = remember(context, calendar) { ThemeLibrary(context, calendar, catalog) }
    var revision by remember { mutableIntStateOf(0) }
    var storeTab by rememberSaveable { mutableIntStateOf(-1) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    fun changed() { revision++; onChanged() }
    val installed = remember(revision) { library.installed() }
    BackHandler(enabled = editingId != null && storeTab < 0) { editingId = null }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("theme-library").semantics { testTagsAsResourceId = true }) {
        items(installed, key = { it }) { id ->
            Box(Modifier.width(190.dp).height(172.dp)) {
            Card(modifier = Modifier.fillMaxSize().testTag("theme-$id").combinedClickable(
                onClick = { if (editingId != null) editingId = null else { library.select(id); changed() } },
                onLongClick = { editingId = id }),
                colors = CardDefaults.cardColors(containerColor = if (library.selected() == id)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                key(revision) { ThemeTileContents(calendar, id) }
            }
            if (editingId == id) IconButton(onClick = { deleting = id },
                modifier = Modifier.align(Alignment.TopEnd).testTag("delete-$id")) {
                Box(Modifier.size(28.dp).background(Color(0xFFCF3038), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Default.Remove,
                        stringResource(R.string.theme_delete_title, themeName(calendar, id)),
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            }
        }
        item(key = "store") {
            Card(onClick = { editingId = null; storeTab = 0 }, modifier = Modifier.width(190.dp).height(172.dp).testTag("theme-store-entry")) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                    Icon(Icons.Default.Add, null, Modifier.size(36.dp))
                    Text(stringResource(R.string.theme_store))
                }
            }
        }
    }
    Text(stringResource(R.string.theme_library_hint), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    deleting?.let { id ->
        ThemeDeleteConfirmation(calendar, id, installed.size > 1, onDismiss = { deleting = null }) {
            library.remove(id); deleting = null; editingId = null; changed()
        }
    }
    if (storeTab >= 0) {
        ThemeStoreDialog(calendar, library, storeTab, { storeTab = it },
            onChanged = { changed() }, onClose = { storeTab = -1 })
    }
}

@Composable
private fun themeName(calendar: Boolean, id: String): String = stringResource(
    if (calendar) CalendarThemeCatalog.resolve(id).nameRes else UltimateClockStyles.styleNameRes(id))

@Composable
private fun ThemeTileContents(calendar: Boolean, id: String) {
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ThemePreview(calendar, id, Modifier.fillMaxWidth().height(88.dp).clip(MaterialTheme.shapes.small))
        Text(themeName(calendar, id), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall)
        Text(stringResource(if (calendar) CalendarThemeCatalog.resolve(id).summaryRes
            else UltimateClockStyles.styleSummaryRes(id)), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemePreview(calendar: Boolean, id: String, modifier: Modifier) {
    val context = LocalContext.current
    if (calendar) CalendarThemeThumbnail(CalendarThemeCatalog.resolve(id), modifier)
    else {
        val preferences = remember(context) { UltimateClockPreferences(context) }
        val repository = remember(context) { BackgroundRepository(context) }
        ClockStyleThumbnail(styleId = id, palette = preferences.getPalette(id),
            background = previewClockBackground(repository, preferences, 0),
            repository = repository, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeStoreDialog(calendar: Boolean, library: ThemeLibrary, tab: Int,
    onTab: (Int) -> Unit, onChanged: () -> Unit, onClose: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    fun changed() { revision++; onChanged() }
    val installed = remember(revision) { library.installed() }
    val ids = when (tab) {
        1 -> library.catalog().filter { library.isFavorite(it) }
        2 -> installed
        else -> library.catalog()
    }
    val window = LocalWindowInfo.current.containerSize
    val previewRatio = window.width.toFloat() / window.height.coerceAtLeast(1)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onClose)
        Surface(Modifier.fillMaxSize().testTag("theme-store")) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                TopAppBar(title = { Text(stringResource(R.string.theme_store)) }, navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.ultimate_back_to_settings)) }
                }, windowInsets = WindowInsets(0))
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf(R.string.theme_recommended, R.string.theme_favorites, R.string.theme_installed)
                        .forEachIndexed { index, label ->
                            Tab(selected = tab == index, onClick = { onTab(index) },
                                text = { Text(stringResource(label)) })
                        }
                }
                if (ids.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FavoriteBorder, null, Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Text(stringResource(R.string.theme_no_favorites), style = MaterialTheme.typography.bodyMedium)
                    }
                } else LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(ids, key = { it }) { id ->
                        val present = id in installed
                        val favorite = library.isFavorite(id)
                        Column(Modifier.fillMaxWidth().testTag("store-$id"),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().aspectRatio(previewRatio).clip(RoundedCornerShape(14.dp))) {
                                key(revision) { ThemePreview(calendar, id, Modifier.fillMaxSize()) }
                                IconToggleButton(checked = favorite, onCheckedChange = { library.favorite(id); changed() },
                                    modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                                    Surface(shape = CircleShape, color = if (favorite) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                                        Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            stringResource(if (favorite) R.string.theme_unfavorite else R.string.theme_favorite),
                                            Modifier.padding(6.dp).size(22.dp), tint = if (favorite)
                                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            Text(themeName(calendar, id), Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
                                style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val buttonModifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 48.dp)
                            val action = {
                                if (tab == 2) { library.select(id); changed() }
                                else if (present) deleting = id
                                else { library.install(id); changed() }
                            }
                            if (present) OutlinedButton(onClick = action, modifier = buttonModifier,
                                enabled = tab != 2 || library.selected() != id,
                                contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text(stringResource(if (tab != 2) R.string.theme_installed else if (library.selected() == id)
                                    R.string.theme_in_use else R.string.theme_use), maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium)
                            } else Button(onClick = action, modifier = buttonModifier,
                                contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text(stringResource(R.string.theme_install), maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        deleting?.let { id ->
            ThemeDeleteConfirmation(calendar, id, installed.size > 1, onDismiss = { deleting = null }) {
                library.remove(id); deleting = null; changed()
            }
        }
    }
}

@Composable
private fun ThemeDeleteConfirmation(calendar: Boolean, id: String, canDelete: Boolean,
    onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_delete_title, themeName(calendar, id))) },
        text = { Text(stringResource(if (canDelete) R.string.theme_delete_message else R.string.theme_keep_one)) },
        confirmButton = { TextButton(onClick = if (canDelete) onDelete else onDismiss) {
            Text(stringResource(if (canDelete) R.string.theme_delete else R.string.theme_done))
        } },
        dismissButton = { if (canDelete) TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.ultimate_cancel))
        } })
}
