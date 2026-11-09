package com.klongshore.filelistorama.data.local

import java.io.File

/** Turns an image file into an encoded thumbnail no larger than [sizePx] on its longest side. */
fun interface ThumbnailGenerator {
    /** Returns encoded image bytes, or null if [file] is not a decodable image. */
    fun generate(file: File, sizePx: Int): ByteArray?

    companion object {
        /** Generates nothing; used where no decoder is available. */
        val None = ThumbnailGenerator { _, _ -> null }
    }
}
