package com.klongshore.filelistorama.data.local

import java.io.File

/**
 * Stands in for the Android bitmap decoder, which a unit test cannot use. A file counts as a
 * decodable image only if its text starts with "IMG:"; the "thumbnail" records the size asked for.
 */
class FakeThumbnails : ThumbnailGenerator {
    override fun generate(file: File, sizePx: Int): ByteArray? {
        if (!file.readText().startsWith("IMG:")) return null
        return "thumb:$sizePx:${file.name}".toByteArray()
    }
}
