package com.klongshore.filelistorama.domain

/** A place files live (local storage today; cloud drives later). Read-only for now. */
interface FileSource {
    val sourceId: String
    val displayName: String
    val root: FileRef

    suspend fun list(folder: FileRef): FileResult<List<Entry>>
}
