package com.klongshore.filelistorama.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klongshore.filelistorama.domain.BrowserPreferences
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileRef
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.Location
import com.klongshore.filelistorama.domain.LocationsProvider
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val locationsProvider: LocationsProvider,
    private val prefs: BrowserPreferences,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val locations = MutableStateFlow<List<Location>>(emptyList())
    private val selectedId = MutableStateFlow<String?>(null)
    private val crumbs = MutableStateFlow<List<Crumb>>(emptyList())

    /** What the source returned for the current folder, before sorting and filtering. */
    private val rawListing = MutableStateFlow<Listing>(Listing.Loading)

    private val refreshing = MutableStateFlow(false)

    private data class Navigation(
        val crumbs: List<Crumb>,
        val selectedId: String?,
        val locations: List<Location>,
    )

    private val navigation = combine(crumbs, selectedId, locations, ::Navigation)

    val state: StateFlow<BrowserState> = combine(
        navigation,
        rawListing,
        refreshing,
        prefs.settings,
    ) { nav, raw, refreshing, settings ->
        BrowserState(
            crumbs = nav.crumbs,
            listing = if (raw is Listing.Loaded) Listing.Loaded(raw.entries.arranged(settings)) else raw,
            settings = settings,
            isRefreshing = refreshing,
            locations = nav.locations,
            selectedLocationId = nav.selectedId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BrowserState(emptyList(), Listing.Loading),
    )

    private val _events = Channel<BrowserEvent>(Channel.BUFFERED)

    /** One-off requests for the screen, such as handing a file to another app. */
    val events: Flow<BrowserEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { locationsProvider.locations.collect(::onLocationsChanged) }
    }

    /** Switches to another location, starting at its root. */
    fun selectLocation(id: String) {
        val location = locations.value.firstOrNull { it.id == id } ?: return
        selectedId.value = id
        setCrumbs(listOf(rootCrumb(location)))
        loadCurrent()
    }

    private fun onLocationsChanged(available: List<Location>) {
        val wasAvailable = locations.value.any { it.id == selectedId.value }
        locations.value = available
        val current = selectedId.value
        when {
            available.isEmpty() -> rawListing.value = Listing.Failed(Failure.NotFound)
            // First emission: go back to where the user was, or start at the primary location.
            current == null -> start(available)
            available.none { it.id == current } -> {
                // The volume being browsed was removed.
                loadJob?.cancel()
                refreshing.value = false
                rawListing.value = Listing.Failed(Failure.NotFound)
            }
            !wasAvailable -> loadCurrent() // it came back
        }
    }

    private fun start(available: List<Location>) {
        val saved = savedState.get<String>(KEY_LOCATION)
        val location = available.firstOrNull { it.id == saved } ?: available.first()
        selectedId.value = location.id
        crumbs.value = restoreCrumbs(location)
        loadCurrent()
    }

    /** Folders open in place; files are handed to the screen to open with another app. */
    fun open(entry: Entry) {
        if (entry.kind != EntryKind.Folder) {
            _events.trySend(BrowserEvent.OpenFile(entry))
            return
        }
        setCrumbs(crumbs.value + Crumb(entry.ref, entry.name))
        loadCurrent()
    }

    /** Jumps back to the breadcrumb at [index]. Indexes outside the earlier levels are ignored. */
    fun navigateTo(index: Int) {
        if (index !in 0 until crumbs.value.lastIndex) return
        setCrumbs(crumbs.value.take(index + 1))
        loadCurrent()
    }

    /** Goes up one level. Returns false at the top level so the caller can let the app exit. */
    fun navigateUp(): Boolean {
        val depth = crumbs.value.size
        if (depth <= 1) return false
        navigateTo(depth - 2)
        return true
    }

    /** Rereads the current folder, keeping the old listing on screen until the new one arrives. */
    fun refresh() {
        loadCurrent(keepListing = true)
    }

    fun setSort(key: SortKey, ascending: Boolean) = updateSettings {
        it.copy(sortKey = key, ascending = ascending)
    }

    fun setShowHidden(show: Boolean) = updateSettings { it.copy(showHidden = show) }

    private fun updateSettings(transform: (ViewSettings) -> ViewSettings) {
        viewModelScope.launch { prefs.update(transform) }
    }

    private fun loadCurrent(keepListing: Boolean = false) {
        loadJob?.cancel()
        if (keepListing) {
            refreshing.value = true
        } else {
            refreshing.value = false
            rawListing.value = Listing.Loading
        }
        val folder = crumbs.value.last().ref
        val source = locations.value.firstOrNull { it.id == selectedId.value }?.source
        if (source == null) {
            rawListing.value = Listing.Failed(Failure.NotFound)
            return
        }
        loadJob = viewModelScope.launch {
            rawListing.value = when (val result = source.list(folder)) {
                is FileResult.Success -> Listing.Loaded(result.value)
                FileResult.PermissionDenied -> Listing.Failed(Failure.PermissionDenied)
                FileResult.NotFound -> Listing.Failed(Failure.NotFound)
                is FileResult.Io -> Listing.Failed(Failure.Io)
            }
            refreshing.value = false
        }
    }

    private fun setCrumbs(next: List<Crumb>) {
        crumbs.value = next
        // Saved as plain string lists so they survive process death.
        savedState[KEY_LOCATION] = selectedId.value
        savedState[KEY_IDS] = ArrayList(next.map { it.ref.itemId })
        savedState[KEY_NAMES] = ArrayList(next.map { it.name })
    }

    private fun rootCrumb(location: Location) = Crumb(location.source.root, location.name)

    /** Rebuilds the breadcrumb trail saved before the process died, or starts at the root. */
    private fun restoreCrumbs(location: Location): List<Crumb> {
        val root = rootCrumb(location)
        val ids = savedState.get<ArrayList<String>>(KEY_IDS)
        val names = savedState.get<ArrayList<String>>(KEY_NAMES)
        val valid = savedState.get<String>(KEY_LOCATION) == location.id &&
            ids != null && names != null && ids.size == names.size &&
            ids.isNotEmpty() && ids.first() == location.source.root.itemId
        if (!valid) return listOf(root)
        return ids!!.zip(names!!) { id, name -> Crumb(FileRef(location.id, id), name) }
    }

    private companion object {
        const val KEY_LOCATION = "location_id"
        const val KEY_IDS = "crumb_ids"
        const val KEY_NAMES = "crumb_names"
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
