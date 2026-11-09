package com.klongshore.filelistorama.ui.browser

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewSettings
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.crumbs.size > 1) { viewModel.navigateUp() }

    val context = LocalContext.current
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

    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Breadcrumbs(state.crumbs, onSelect = viewModel::navigateTo, modifier = Modifier.weight(1f))
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
                        EntryList(listing.entries, onOpen = viewModel::open)
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
private fun EntryList(entries: List<Entry>, onOpen: (Entry) -> Unit) {
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
                Icon(
                    imageVector = if (entry.kind == EntryKind.Folder) {
                        Icons.Outlined.Folder
                    } else {
                        Icons.AutoMirrored.Outlined.InsertDriveFile
                    },
                    contentDescription = null,
                )
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
