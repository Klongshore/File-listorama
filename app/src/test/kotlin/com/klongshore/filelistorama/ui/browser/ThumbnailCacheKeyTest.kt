package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.EntryKind
import com.klongshore.filelistorama.domain.FileRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThumbnailCacheKeyTest {

    private fun image(
        source: String = "local",
        id: String = "/storage/photo.png",
        modified: Long = 1_000L,
    ) = Entry(
        ref = FileRef(source, id),
        name = "photo.png",
        kind = EntryKind.File,
        sizeBytes = 10,
        modifiedMillis = modified,
        mimeType = "image/png",
        isHidden = false,
        hasThumbnail = true,
    )

    @Test
    fun `the same unchanged image always gets the same key`() {
        assertEquals(thumbnailCacheKey(image()), thumbnailCacheKey(image()))
    }

    @Test
    fun `editing an image changes its key so a stale thumbnail is not reused`() {
        assertNotEquals(thumbnailCacheKey(image(modified = 1_000L)), thumbnailCacheKey(image(modified = 2_000L)))
    }

    @Test
    fun `different files get different keys`() {
        assertNotEquals(thumbnailCacheKey(image(id = "/a.png")), thumbnailCacheKey(image(id = "/b.png")))
    }

    @Test
    fun `the same item id in different sources gets different keys`() {
        assertNotEquals(thumbnailCacheKey(image(source = "local")), thumbnailCacheKey(image(source = "local:SD")))
    }
}
