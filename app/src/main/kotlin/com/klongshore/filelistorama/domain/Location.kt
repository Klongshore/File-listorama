package com.klongshore.filelistorama.domain

import kotlinx.coroutines.flow.Flow

/** A top-level root the user can pick from the drawer, such as internal storage or an SD card. */
data class Location(val source: FileSource) {
    val id: String get() = source.sourceId
    val name: String get() = source.displayName
}

/** The currently available [Location]s. Emits again whenever one appears or disappears. */
interface LocationsProvider {
    val locations: Flow<List<Location>>
}
