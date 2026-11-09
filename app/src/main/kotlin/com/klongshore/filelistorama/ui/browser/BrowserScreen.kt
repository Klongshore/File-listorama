package com.klongshore.filelistorama.ui.browser

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import java.text.DateFormat
import java.util.Date

@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.crumbs.size > 1) { viewModel.navigateUp() }

    Column(modifier.fillMaxSize()) {
        Breadcrumbs(state.crumbs, onSelect = viewModel::navigateTo)
        when (val listing = state.listing) {
            Listing.Loading -> Centered { CircularProgressIndicator() }
            is Listing.Failed -> Centered { Text(listing.failure.message()) }
            is Listing.Loaded ->
                if (listing.entries.isEmpty()) {
                    Centered { Text("This folder is empty") }
                } else {
                    EntryList(listing.entries, onOpen = viewModel::open)
                }
        }
    }
}

@Composable
private fun Breadcrumbs(crumbs: List<Crumb>, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun Failure.message(): String = when (this) {
    Failure.PermissionDenied -> "Permission denied"
    Failure.NotFound -> "This folder no longer exists"
    Failure.Io -> "Could not read this folder"
}
