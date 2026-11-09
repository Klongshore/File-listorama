package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.domain.BrowserPreferences
import com.klongshore.filelistorama.domain.ViewSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Stands in for on-disk preferences (DataStore), which a JVM test cannot use. */
class InMemoryBrowserPreferences(initial: ViewSettings = ViewSettings()) : BrowserPreferences {
    private val current = MutableStateFlow(initial)
    override val settings: Flow<ViewSettings> = current
    override suspend fun update(transform: (ViewSettings) -> ViewSettings) {
        current.value = transform(current.value)
    }
}
