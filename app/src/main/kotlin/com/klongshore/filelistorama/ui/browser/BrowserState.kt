package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.FileRef
import com.klongshore.filelistorama.domain.ViewSettings

/** One level of the path from the root to the folder being shown. */
data class Crumb(val ref: FileRef, val name: String)

enum class Failure { PermissionDenied, NotFound, Io }

sealed interface Listing {
    data object Loading : Listing
    data class Loaded(val entries: List<Entry>) : Listing
    data class Failed(val failure: Failure) : Listing
}

data class BrowserState(
    val crumbs: List<Crumb>,
    val listing: Listing,
    val settings: ViewSettings = ViewSettings(),
    val isRefreshing: Boolean = false,
)

/** One-off requests from the ViewModel to the screen. */
sealed interface BrowserEvent {
    data class OpenFile(val entry: Entry) : BrowserEvent
}
