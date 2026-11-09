package com.klongshore.filelistorama.domain

import kotlinx.coroutines.flow.Flow

enum class SortKey { Name, Date, Size }

enum class ViewMode { List, Grid }

/** How a folder is presented. Persisted across launches. */
data class ViewSettings(
    val sortKey: SortKey = SortKey.Name,
    val ascending: Boolean = true,
    val showHidden: Boolean = false,
    val viewMode: ViewMode = ViewMode.List,
)

/** Persistent storage for [ViewSettings]. */
interface BrowserPreferences {
    val settings: Flow<ViewSettings>
    suspend fun update(transform: (ViewSettings) -> ViewSettings)
}
