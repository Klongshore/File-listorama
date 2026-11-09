package com.klongshore.filelistorama.data.local

import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileRef
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.FileSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URLConnection

/** [FileSource] over a directory tree on the device. Item ids are absolute paths below [rootDir]. */
class LocalFileSource(
    private val rootDir: File,
    override val displayName: String = "Internal storage",
    override val sourceId: String = "local",
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val thumbnails: ThumbnailGenerator = ThumbnailGenerator.None,
) : FileSource {

    private val canonicalRoot: File = rootDir.canonicalFile

    override val root: FileRef = FileRef(sourceId, rootDir.absolutePath)

    override suspend fun list(folder: FileRef): FileResult<List<Entry>> = withContext(io) {
        if (folder.sourceId != sourceId) return@withContext FileResult.NotFound
        try {
            val dir = File(folder.itemId)
            when {
                !isInsideRoot(dir) || !dir.exists() -> FileResult.NotFound
                !dir.isDirectory -> FileResult.Io("Not a folder: ${dir.name}")
                !dir.canRead() -> FileResult.PermissionDenied
                else -> {
                    val children = dir.listFiles()
                        ?: return@withContext FileResult.Io("Could not list ${dir.name}")
                    FileResult.Success(children.map(::toEntry))
                }
            }
        } catch (e: SecurityException) {
            FileResult.PermissionDenied
        } catch (e: IOException) {
            FileResult.Io(e.message)
        }
    }

    override suspend fun thumbnail(entry: Entry, sizePx: Int): FileResult<ByteArray> = withContext(io) {
        if (entry.ref.sourceId != sourceId) return@withContext FileResult.NotFound
        try {
            val file = File(entry.ref.itemId)
            when {
                !isInsideRoot(file) || !file.exists() -> FileResult.NotFound
                !file.isFile -> FileResult.Io("Not a file: ${file.name}")
                !file.canRead() -> FileResult.PermissionDenied
                else -> thumbnails.generate(file, sizePx)
                    ?.let { FileResult.Success(it) }
                    ?: FileResult.Io("No thumbnail for ${file.name}")
            }
        } catch (e: SecurityException) {
            FileResult.PermissionDenied
        } catch (e: IOException) {
            FileResult.Io(e.message)
        }
    }

    private fun isInsideRoot(file: File): Boolean {
        val path = file.canonicalFile.toPath()
        return path.startsWith(canonicalRoot.toPath())
    }

    private fun toEntry(file: File): Entry {
        val isFolder = file.isDirectory
        return Entry(
            ref = FileRef(sourceId, file.absolutePath),
            name = file.name,
            kind = if (isFolder) EntryKind.Folder else EntryKind.File,
            sizeBytes = if (isFolder) null else file.length(),
            modifiedMillis = file.lastModified(),
            mimeType = if (isFolder) null else URLConnection.guessContentTypeFromName(file.name),
            isHidden = file.name.startsWith("."),
            hasThumbnail = !isFolder && file.extension.lowercase() in IMAGE_EXTENSIONS,
        )
    }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    }
}
