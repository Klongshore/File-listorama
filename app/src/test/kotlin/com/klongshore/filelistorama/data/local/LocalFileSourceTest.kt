package com.klongshore.filelistorama.data.local

import com.klongshore.filelistorama.domain.FileRef
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.FileSource
import com.klongshore.filelistorama.domain.FileSourceContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalFileSourceTest : FileSourceContract() {

    @get:Rule
    val temp = TemporaryFolder()

    override fun sourceWith(files: Map<String, String>): FileSource {
        val root = temp.newFolder("storage")
        files.forEach { (path, content) ->
            if (path.endsWith("/")) {
                File(root, path).mkdirs()
            } else {
                File(root, path).apply { parentFile?.mkdirs() }.writeText(content)
            }
        }
        return LocalFileSource(root, io = Dispatchers.Unconfined)
    }

    @Test
    fun `dot files are flagged hidden and others are not`() = runTest {
        val source = sourceWith(mapOf(".secret" to "x", "visible.txt" to "y"))

        val entries = (source.list(source.root) as FileResult.Success).value.associateBy { it.name }

        assertTrue(entries.getValue(".secret").isHidden)
        assertEquals(false, entries.getValue("visible.txt").isHidden)
    }

    @Test
    fun `modified time comes from the file`() = runTest {
        val source = sourceWith(mapOf("old.txt" to "x"))
        File(source.root.itemId, "old.txt").setLastModified(1_600_000_000_000L)

        val entry = (source.list(source.root) as FileResult.Success).value.single()

        assertEquals(1_600_000_000_000L, entry.modifiedMillis)
    }

    @Test
    fun `files carry a mime type hint from their name`() = runTest {
        val source = sourceWith(mapOf("photo.png" to "x", "mystery" to "y"))

        val entries = (source.list(source.root) as FileResult.Success).value.associateBy { it.name }

        assertEquals("image/png", entries.getValue("photo.png").mimeType)
        assertEquals(null, entries.getValue("mystery").mimeType)
    }

    @Test
    fun `an unreadable folder returns PermissionDenied`() = runTest {
        val source = sourceWith(mapOf("locked/a.txt" to "x"))
        val locked = File(source.root.itemId, "locked")
        locked.setReadable(false)
        assumeTrue("running as a user who can bypass file permissions", !locked.canRead())

        val result = source.list(FileRef(source.sourceId, locked.absolutePath))

        locked.setReadable(true)
        assertEquals(FileResult.PermissionDenied, result)
    }

    @Test
    fun `a folder outside the root returns NotFound`() = runTest {
        val source = sourceWith(mapOf("a.txt" to "x"))
        val outside = temp.newFolder("elsewhere")

        val result = source.list(FileRef(source.sourceId, outside.absolutePath))

        assertEquals(FileResult.NotFound, result)
    }

    @Test
    fun `a ref from another source returns NotFound`() = runTest {
        val source = sourceWith(mapOf("a.txt" to "x"))

        val result = source.list(FileRef("someone-else", source.root.itemId))

        assertEquals(FileResult.NotFound, result)
    }
}
