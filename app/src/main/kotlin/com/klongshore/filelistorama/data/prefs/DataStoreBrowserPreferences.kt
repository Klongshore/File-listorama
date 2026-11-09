package com.klongshore.filelistorama.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.klongshore.filelistorama.domain.BrowserPreferences
import com.klongshore.filelistorama.domain.SortKey
import com.klongshore.filelistorama.domain.ViewMode
import com.klongshore.filelistorama.domain.ViewSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreBrowserPreferences(
    private val store: DataStore<Preferences>,
) : BrowserPreferences {

    override val settings: Flow<ViewSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    override suspend fun update(transform: (ViewSettings) -> ViewSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[SORT_KEY] = next.sortKey.name
            prefs[ASCENDING] = next.ascending
            prefs[SHOW_HIDDEN] = next.showHidden
            prefs[VIEW_MODE] = next.viewMode.name
        }
    }

    private fun Preferences.toSettings() = ViewSettings(
        sortKey = this[SORT_KEY]?.let { name -> SortKey.entries.firstOrNull { it.name == name } }
            ?: SortKey.Name,
        ascending = this[ASCENDING] ?: true,
        showHidden = this[SHOW_HIDDEN] ?: false,
        viewMode = this[VIEW_MODE]?.let { name -> ViewMode.entries.firstOrNull { it.name == name } }
            ?: ViewMode.List,
    )

    private companion object {
        val SORT_KEY = stringPreferencesKey("sort_key")
        val ASCENDING = booleanPreferencesKey("sort_ascending")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val VIEW_MODE = stringPreferencesKey("view_mode")
    }
}
