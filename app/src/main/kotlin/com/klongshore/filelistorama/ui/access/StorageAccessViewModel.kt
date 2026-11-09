package com.klongshore.filelistorama.ui.access

import androidx.lifecycle.ViewModel
import com.klongshore.filelistorama.domain.StoragePermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class StorageAccess {
    /** Not granted and the user has not been sent to settings yet. */
    Explaining,

    /** The user went to settings and came back without granting. */
    Denied,

    Granted,
}

@HiltViewModel
class StorageAccessViewModel @Inject constructor(
    private val permission: StoragePermission,
) : ViewModel() {

    private var requested = false

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<StorageAccess> = _state.asStateFlow()

    /** Call when sending the user to settings, so coming back ungranted counts as a denial. */
    fun requestAccess() {
        requested = true
    }

    /** Call whenever the screen resumes (for example on return from settings). */
    fun refresh() {
        _state.value = compute()
    }

    private fun compute(): StorageAccess = when {
        permission.isGranted() -> StorageAccess.Granted
        requested -> StorageAccess.Denied
        else -> StorageAccess.Explaining
    }
}
