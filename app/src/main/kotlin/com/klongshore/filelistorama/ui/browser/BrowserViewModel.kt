package com.klongshore.filelistorama.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klongshore.filelistorama.domain.BrowserPreferences
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.FileSource
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val source: FileSource,
    private val prefs: BrowserPreferences,
) : ViewModel() {

    private val crumbs = MutableStateFlow(listOf(Crumb(source.root, source.displayName)))

    /** What the source returned for the current folder, before sorting and filtering. */
    private val rawListing = MutableStateFlow<Listing>(Listing.Loading)

    val state: StateFlow<BrowserState> = combine(crumbs, rawListing, prefs.settings) { crumbs, raw, settings ->
        BrowserState(
            crumbs = crumbs,
            listing = if (raw is Listing.Loaded) Listing.Loaded(raw.entries.arranged(settings)) else raw,
            settings = settings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BrowserState(crumbs.value, Listing.Loading),
    )

    private var loadJob: Job? = null

    init {
        loadCurrent()
    }

    /** Opens [entry] if it is a folder; files are ignored here. */
    fun open(entry: Entry) {
        if (entry.kind != EntryKind.Folder) return
        crumbs.update { it + Crumb(entry.ref, entry.name) }
        loadCurrent()
    }

    /** Jumps back to the breadcrumb at [index]. Indexes outside the earlier levels are ignored. */
    fun navigateTo(index: Int) {
        if (index !in 0 until crumbs.value.lastIndex) return
        crumbs.update { it.take(index + 1) }
        loadCurrent()
    }

    /** Goes up one level. Returns false at the top level so the caller can let the app exit. */
    fun navigateUp(): Boolean {
        val depth = crumbs.value.size
        if (depth <= 1) return false
        navigateTo(depth - 2)
        return true
    }

    fun setSort(key: SortKey, ascending: Boolean) = updateSettings {
        it.copy(sortKey = key, ascending = ascending)
    }

    fun setShowHidden(show: Boolean) = updateSettings { it.copy(showHidden = show) }

    private fun updateSettings(transform: (ViewSettings) -> ViewSettings) {
        viewModelScope.launch { prefs.update(transform) }
    }

    private fun loadCurrent() {
        loadJob?.cancel()
        rawListing.value = Listing.Loading
        val folder = crumbs.value.last().ref
        loadJob = viewModelScope.launch {
            rawListing.value = when (val result = source.list(folder)) {
                is FileResult.Success -> Listing.Loaded(result.value)
                FileResult.PermissionDenied -> Listing.Failed(Failure.PermissionDenied)
                FileResult.NotFound -> Listing.Failed(Failure.NotFound)
                is FileResult.Io -> Listing.Failed(Failure.Io)
            }
        }
    }

    /** Hides hidden files unless asked, puts folders first, then orders each group by [ViewSettings]. */
    private fun List<Entry>.arranged(settings: ViewSettings): List<Entry> {
        val visible = if (settings.showHidden) this else filterNot { it.isHidden }
        val byName = compareBy<Entry> { it.name.lowercase() }
        val fileKey: Comparator<Entry> = when (settings.sortKey) {
            SortKey.Name -> byName
            SortKey.Date -> compareBy<Entry> { it.modifiedMillis }.then(byName)
            SortKey.Size -> compareBy<Entry> { it.sizeBytes ?: 0L }.then(byName)
        }
        // Folders have no size, so when sorting by size they fall back to name order.
        val folderKey = if (settings.sortKey == SortKey.Size) byName else fileKey
        val direction = { key: Comparator<Entry> -> if (settings.ascending) key else key.reversed() }
        val folders = visible.filter { it.kind == EntryKind.Folder }.sortedWith(direction(folderKey))
        val files = visible.filter { it.kind != EntryKind.Folder }.sortedWith(direction(fileKey))
        return folders + files
    }
}
