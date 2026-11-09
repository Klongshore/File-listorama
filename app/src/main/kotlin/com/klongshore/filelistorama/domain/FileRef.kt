package com.klongshore.filelistorama.domain

/**
 * Opaque identity of one item: the [Source][FileSource] it lives in plus an id only that source
 * understands. Never a path; paths are for display.
 */
data class FileRef(val sourceId: String, val itemId: String)
