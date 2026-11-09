package com.klongshore.filelistorama.domain

enum class EntryKind { File, Folder }

/**
 * One listed item. [sizeBytes] and [mimeType] are null for folders (and unknown types).
 * [hasThumbnail] says whether [FileSource.thumbnail] can produce a preview, so the UI need not guess.
 */
data class Entry(
    val ref: FileRef,
    val name: String,
    val kind: EntryKind,
    val sizeBytes: Long?,
    val modifiedMillis: Long,
    val mimeType: String?,
    val isHidden: Boolean,
    val hasThumbnail: Boolean = false,
)
