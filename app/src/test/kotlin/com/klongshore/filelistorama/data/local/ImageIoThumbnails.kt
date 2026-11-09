package com.klongshore.filelistorama.data.local

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

/** JVM stand-in for the Android bitmap decoder, which a unit test cannot use. */
class ImageIoThumbnails : ThumbnailGenerator {
    override fun generate(file: File, sizePx: Int): ByteArray? {
        val image = ImageIO.read(file) ?: return null
        val scale = minOf(1.0, sizePx.toDouble() / max(image.width, image.height))
        val width = max(1, (image.width * scale).toInt())
        val height = max(1, (image.height * scale).toInt())
        val thumb = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        thumb.createGraphics().apply {
            drawImage(image, 0, 0, width, height, null)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(thumb, "png", it) }.toByteArray()
    }
}
