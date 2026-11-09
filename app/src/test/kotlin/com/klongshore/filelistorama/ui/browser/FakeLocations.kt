package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.data.local.LocalFileSource
import com.klongshore.filelistorama.domain.Location
import com.klongshore.filelistorama.domain.LocationsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/** Stands in for Android's mounted-volume list, which a JVM test cannot observe. */
class FakeLocations(initial: List<Location>) : LocationsProvider {
    private val current = MutableStateFlow(initial)
    override val locations: Flow<List<Location>> = current

    fun set(locations: List<Location>) {
        current.value = locations
    }

    companion object {
        /** A real [LocalFileSource] over [dir] so tests list real files. */
        fun location(dir: File, name: String, id: String) =
            Location(LocalFileSource(dir, displayName = name, sourceId = id, io = Dispatchers.Unconfined))
    }
}
