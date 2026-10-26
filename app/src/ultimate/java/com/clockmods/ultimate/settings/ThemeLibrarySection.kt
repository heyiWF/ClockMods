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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    fun changed() { revision++; onChanged() }
    val installed = remember(revision) { library.installed() }
    BackHandler(enabled = editing && storeTab < 0) { editing = false }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (editing) TextButton(onClick = { editing = false }) { Text(stringResource(R.string.theme_done)) }
        TextButton(onClick = { storeTab = 2 }) { Text(stringResource(R.string.theme_manage)) }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("theme-library").semantics { testTagsAsResourceId = true }) {
        items(installed, key = { it }) { id ->
            Box(Modifier.width(190.dp).height(172.dp)) {
            Card(modifier = Modifier.fillMaxSize().testTag("theme-$id").combinedClickable(
                onClick = { if (editing) editing = false else { library.select(id); changed() } },
                onLongClick = { editing = true }),
                colors = CardDefaults.cardColors(containerColor = if (library.selected() == id)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                key(revision) { ThemeTileContents(calendar, id) }
            }
            if (editing) IconButton(onClick = { deleting = id },
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
            Card(onClick = { storeTab = 0 }, modifier = Modifier.width(190.dp).height(172.dp).testTag("theme-store-entry")) {
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
            library.remove(id); deleting = null; changed()
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
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(88.dp).clip(MaterialTheme.shapes.small)) {
            if (calendar) CalendarThemeThumbnail(CalendarThemeCatalog.resolve(id), Modifier.fillMaxSize())
            else {
                val preferences = remember(context) { UltimateClockPreferences(context) }
                val repository = remember(context) { BackgroundRepository(context) }
                ClockStyleThumbnail(styleId = id, palette = preferences.getPalette(id),
                    background = previewClockBackground(repository, preferences, 0),
                    repository = repository, modifier = Modifier.fillMaxSize())
            }
        }
        Text(themeName(calendar, id), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall)
        Text(stringResource(if (calendar) CalendarThemeCatalog.resolve(id).summaryRes
            else UltimateClockStyles.styleSummaryRes(id)), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.theme_store_hint), Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall)
                if (ids.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.theme_no_favorites))
                } else LazyVerticalGrid(columns = GridCells.Adaptive(180.dp),
                    contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ids, key = { it }) { id ->
                        Card(Modifier.fillMaxWidth().testTag("store-$id")) {
                            key(revision) { ThemeTileContents(calendar, id) }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { library.favorite(id); changed() }) {
                                    Icon(if (library.isFavorite(id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        stringResource(if (library.isFavorite(id)) R.string.theme_unfavorite else R.string.theme_favorite))
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    if (library.isInstalled(id)) library.select(id) else library.install(id)
                                    changed()
                                }, enabled = !library.isInstalled(id) || library.selected() != id) {
                                    Text(stringResource(if (!library.isInstalled(id)) R.string.theme_install
                                        else if (library.selected() == id) R.string.theme_in_use else R.string.theme_use))
                                }
                            }
                            if (tab == 2) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { library.move(id, -1); changed() }, enabled = installed.first() != id) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.theme_move_before))
                                    }
                                    IconButton(onClick = { library.move(id, 1); changed() }, enabled = installed.last() != id) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.theme_move_after))
                                    }
                                    TextButton(onClick = { deleting = id }, enabled = installed.size > 1) {
                                        Text(stringResource(R.string.theme_delete))
                                    }
                                }
                                if (installed.size == 1) Text(stringResource(R.string.theme_keep_one),
                                    Modifier.padding(10.dp), style = MaterialTheme.typography.labelSmall)
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
