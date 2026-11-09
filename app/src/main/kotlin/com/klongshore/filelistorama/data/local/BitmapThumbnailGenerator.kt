package com.klongshore.filelistorama.data.local

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Size
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/** Decodes with the platform's thumbnail utility, which downsamples and honours EXIF rotation. */
class BitmapThumbnailGenerator : ThumbnailGenerator {
    override fun generate(file: File, sizePx: Int): ByteArray? {
        val bitmap = try {
            ThumbnailUtils.createImageThumbnail(file, Size(sizePx, sizePx), null)
        } catch (e: IOException) {
            return null
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
