package com.klongshore.filelistorama.ui.browser

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileSource
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewMode
import com.klongshore.filelistorama.domain.ViewSettings
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = state.crumbs.size > 1) { viewModel.navigateUp() }
    // Declared last so it wins while the drawer is open.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(ThumbnailKeyer(), ThumbnailModel::class.java)
                add(ThumbnailFetcher.Factory(), ThumbnailModel::class.java)
            }
            .build()
    }
    val sources = state.locations.associate { it.id to it.source }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.OpenFile ->
                    if (!openWithSystem(context, event.entry)) {
                        Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Locations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                )
                state.locations.forEach { location ->
                    NavigationDrawerItem(
                        label = { Text(location.name) },
                        icon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        selected = location.id == state.selectedLocationId,
                        onClick = {
                            viewModel.selectLocation(location.id)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Column(modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Locations")
                }
                Breadcrumbs(state.crumbs, onSelect = viewModel::navigateTo, modifier = Modifier.weight(1f))
                val grid = state.settings.viewMode == ViewMode.Grid
                IconButton(onClick = { viewModel.setViewMode(if (grid) ViewMode.List else ViewMode.Grid) }) {
                    Icon(
                        imageVector = if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                        contentDescription = if (grid) "Switch to list" else "Switch to grid",
                    )
                }
                ViewMenu(
                    settings = state.settings,
                    onSort = viewModel::setSort,
                    onShowHidden = viewModel::setShowHidden,
                )
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val listing = state.listing) {
                    Listing.Loading -> Centered { CircularProgressIndicator() }
                    is Listing.Failed -> ErrorRow(
                        failure = listing.failure,
                        canGoUp = state.crumbs.size > 1,
                        onGoUp = { viewModel.navigateUp() },
                        onRetry = viewModel::refresh,
                    )
                    is Listing.Loaded ->
                        if (listing.entries.isEmpty()) {
                            Centered { Text("This folder is empty") }
                        } else {
                            if (state.settings.viewMode == ViewMode.Grid) {
                            EntryGrid(listing.entries, sources, imageLoader, onOpen = viewModel::open)
                        } else {
                            EntryList(listing.entries, sources, imageLoader, onOpen = viewModel::open)
                        }
                        }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(crumbs: List<Crumb>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(crumbs) { index, crumb ->
            val isCurrent = index == crumbs.lastIndex
            TextButton(onClick = { onSelect(index) }, enabled = !isCurrent) { Text(crumb.name) }
            if (!isCurrent) Text("/")
        }
    }
}

@Composable
private fun ViewMenu(
    settings: ViewSettings,
    onSort: (SortKey, Boolean) -> Unit,
    onShowHidden: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "View options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortKey.entries.forEach { key ->
                val selected = settings.sortKey == key
                val arrow = if (!selected) "" else if (settings.ascending) "  ↑" else "  ↓"
                DropdownMenuItem(
                    text = { Text("Sort by ${key.label()}$arrow") },
                    onClick = {
                        onSort(key, if (selected) !settings.ascending else true)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (settings.showHidden) "Hide hidden files" else "Show hidden files") },
                onClick = {
                    onShowHidden(!settings.showHidden)
                    expanded = false
                },
            )
        }
    }
}

private fun SortKey.label() = when (this) {
    SortKey.Name -> "name"
    SortKey.Date -> "date"
    SortKey.Size -> "size"
}

@Composable
private fun EntryGrid(
    entries: List<Entry>,
    sources: Map<String, FileSource>,
    imageLoader: ImageLoader,
    onOpen: (Entry) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(entries, key = { it.ref.itemId }) { entry ->
            Column(
                modifier = Modifier
                    .clickable { onOpen(entry) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EntryImage(entry, sources, imageLoader, Modifier.size(56.dp))
                Text(
                    entry.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** A thumbnail for images that have one, otherwise (or while loading, or on error) the type icon. */
@Composable
private fun EntryImage(
    entry: Entry,
    sources: Map<String, FileSource>,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    val icon = rememberVectorPainter(entryIcon(entry))
    val source = sources[entry.ref.sourceId]
    if (entry.hasThumbnail && source != null) {
        AsyncImage(
            model = ThumbnailModel(entry, source),
            imageLoader = imageLoader,
            contentDescription = null,
            placeholder = icon,
            error = icon,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(MaterialTheme.shapes.small),
        )
    } else {
        Icon(painter = icon, contentDescription = null, modifier = modifier)
    }
}

private fun entryIcon(entry: Entry): ImageVector =
    if (entry.kind == EntryKind.Folder) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile

@Composable
private fun EntryList(
    entries: List<Entry>,
    sources: Map<String, FileSource>,
    imageLoader: ImageLoader,
    onOpen: (Entry) -> Unit,
) {
    val context = LocalContext.current
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    LazyColumn(Modifier.fillMaxSize()) {
        items(entries, key = { it.ref.itemId }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EntryImage(entry, sources, imageLoader, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val size = entry.sizeBytes?.let { Formatter.formatFileSize(context, it) }
                    val date = dateFormat.format(Date(entry.modifiedMillis))
                    Text(
                        text = listOfNotNull(size, date).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Scrollable so pull-to-refresh still works when there is nothing to list. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ErrorRow(
    failure: Failure,
    canGoUp: Boolean,
    onGoUp: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(failure.message())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canGoUp) TextButton(onClick = onGoUp) { Text("Go up") }
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
    }
}

private fun Failure.message(): String = when (this) {
    Failure.PermissionDenied -> "Permission denied"
    Failure.NotFound -> "This folder no longer exists"
    Failure.Io -> "Could not read this folder"
}
