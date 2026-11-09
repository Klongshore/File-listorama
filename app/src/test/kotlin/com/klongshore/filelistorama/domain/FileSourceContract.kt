package com.klongshore.filelistorama.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour every [FileSource] backend must provide. Backends subclass this and supply a
 * fixture builder; OneDrive and Google Drive reuse it later.
 */
abstract class FileSourceContract {

    /**
     * Builds a source containing [files]: keys are slash-separated paths below the root,
     * values are file contents. A key ending in "/" is an empty folder.
     */
    abstract fun sourceWith(files: Map<String, String>): FileSource

    private suspend fun FileSource.rootEntries(): List<Entry> =
        (list(root) as FileResult.Success).value

    @Test
    fun `listing the root returns the files and folders in it`() = runTest {
        val source = sourceWith(mapOf("notes.txt" to "hello", "photos/a.jpg" to "x"))

        val names = source.rootEntries().map { it.name }

        assertEquals(setOf("notes.txt", "photos"), names.toSet())
    }

    @Test
    fun `folders are reported as folders and files as files`() = runTest {
        val source = sourceWith(mapOf("notes.txt" to "hello", "photos/a.jpg" to "x"))

        val entries = source.rootEntries().associateBy { it.name }

        assertEquals(EntryKind.Folder, entries.getValue("photos").kind)
        assertEquals(EntryKind.File, entries.getValue("notes.txt").kind)
    }

    @Test
    fun `a file reports its size and a folder reports none`() = runTest {
        val source = sourceWith(mapOf("notes.txt" to "hello", "photos/a.jpg" to "x"))

        val entries = source.rootEntries().associateBy { it.name }

        assertEquals(5L, entries.getValue("notes.txt").sizeBytes)
        assertEquals(null, entries.getValue("photos").sizeBytes)
    }

    @Test
    fun `listing a folder returns its children`() = runTest {
        val source = sourceWith(mapOf("photos/a.jpg" to "x", "photos/b.jpg" to "yy"))
        val photos = source.rootEntries().single { it.name == "photos" }

        val children = (source.list(photos.ref) as FileResult.Success).value

        assertEquals(setOf("a.jpg", "b.jpg"), children.map { it.name }.toSet())
    }

    @Test
    fun `an empty folder lists as empty`() = runTest {
        val source = sourceWith(mapOf("empty/" to ""))
        val empty = source.rootEntries().single { it.name == "empty" }

        val children = (source.list(empty.ref) as FileResult.Success).value

        assertTrue(children.isEmpty())
    }

    @Test
    fun `entries belong to the source that listed them`() = runTest {
        val source = sourceWith(mapOf("notes.txt" to "hello"))

        val entry = source.rootEntries().single()

        assertEquals(source.sourceId, entry.ref.sourceId)
        assertEquals(source.sourceId, source.root.sourceId)
    }

    @Test
    fun `listing a folder that does not exist returns NotFound`() = runTest {
        val source = sourceWith(mapOf("photos/a.jpg" to "x"))
        val photos = source.rootEntries().single { it.name == "photos" }
        val missing = photos.ref.copy(itemId = photos.ref.itemId + "-gone")

        assertEquals(FileResult.NotFound, source.list(missing))
    }

    @Test
    fun `listing a file is not a success`() = runTest {
        val source = sourceWith(mapOf("notes.txt" to "hello"))
        val file = source.rootEntries().single()

        val result = source.list(file.ref)

        assertFalse(result is FileResult.Success)
        assertNotNull(result)
    }

    @Test
    fun `a thumbnail for a missing entry is NotFound`() = runTest {
        val source = sourceWith(mapOf("photo.png" to "x"))
        val entry = source.rootEntries().single()
        val missing = entry.copy(ref = entry.ref.copy(itemId = entry.ref.itemId + "-gone"), hasThumbnail = true)

        assertEquals(FileResult.NotFound, source.thumbnail(missing, sizePx = 64))
    }

    @Test
    fun `a thumbnail for a folder is not a success`() = runTest {
        val source = sourceWith(mapOf("photos/a.png" to "x"))
        val folder = source.rootEntries().single()

        assertFalse(source.thumbnail(folder, sizePx = 64) is FileResult.Success)
    }
}
