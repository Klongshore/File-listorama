package com.klongshore.filelistorama.domain

/** Outcome of a [FileSource] call. Errors are values, not exceptions. */
sealed interface FileResult<out T> {
    data class Success<T>(val value: T) : FileResult<T>
    data object PermissionDenied : FileResult<Nothing>
    data object NotFound : FileResult<Nothing>
    data class Io(val message: String?) : FileResult<Nothing>
}
