package com.klongshore.filelistorama.domain

/** A place files live (local storage today; cloud drives later). Read-only for now. */
interface FileSource {
    val sourceId: String
    val displayName: String
    val root: FileRef

    suspend fun list(folder: FileRef): FileResult<List<Entry>>

    /** Encoded image bytes for [entry], no larger than [sizePx] on its longest side. */
    suspend fun thumbnail(entry: Entry, sizePx: Int): FileResult<ByteArray>
}
