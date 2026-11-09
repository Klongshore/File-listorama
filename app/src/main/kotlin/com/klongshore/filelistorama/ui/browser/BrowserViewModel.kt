package com.klongshore.filelistorama.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.FileSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val source: FileSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BrowserState(
            crumbs = listOf(Crumb(source.root, source.displayName)),
            listing = Listing.Loading,
        ),
    )
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCurrent()
    }

    /** Opens [entry] if it is a folder; files are ignored here. */
    fun open(entry: Entry) {
        if (entry.kind != EntryKind.Folder) return
        _state.update { it.copy(crumbs = it.crumbs + Crumb(entry.ref, entry.name)) }
        loadCurrent()
    }

    /** Jumps back to the breadcrumb at [index]. Indexes outside the earlier levels are ignored. */
    fun navigateTo(index: Int) {
        val crumbs = _state.value.crumbs
        if (index !in 0 until crumbs.lastIndex) return
        _state.update { it.copy(crumbs = it.crumbs.take(index + 1)) }
        loadCurrent()
    }

    /** Goes up one level. Returns false at the top level so the caller can let the app exit. */
    fun navigateUp(): Boolean {
        val depth = _state.value.crumbs.size
        if (depth <= 1) return false
        navigateTo(depth - 2)
        return true
    }

    private fun loadCurrent() {
        loadJob?.cancel()
        _state.update { it.copy(listing = Listing.Loading) }
        val folder = _state.value.crumbs.last().ref
        loadJob = viewModelScope.launch {
            val listing = when (val result = source.list(folder)) {
                is FileResult.Success -> Listing.Loaded(result.value.sortedForDisplay())
                FileResult.PermissionDenied -> Listing.Failed(Failure.PermissionDenied)
                FileResult.NotFound -> Listing.Failed(Failure.NotFound)
                is FileResult.Io -> Listing.Failed(Failure.Io)
            }
            _state.update { it.copy(listing = listing) }
        }
    }

    private fun List<Entry>.sortedForDisplay(): List<Entry> =
        sortedWith(compareBy<Entry> { it.kind != EntryKind.Folder }.thenBy { it.name.lowercase() })
}
