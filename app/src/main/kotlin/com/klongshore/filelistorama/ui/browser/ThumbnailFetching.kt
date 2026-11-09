package com.klongshore.filelistorama.ui.browser

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import coil.size.Dimension
import com.klongshore.filelistorama.domain.Entry
import com.klongshore.filelistorama.domain.FileResult
import com.klongshore.filelistorama.domain.FileSource
import okio.Buffer
import java.io.IOException

/** What Coil is asked to load: an entry plus the source that can produce its thumbnail. */
data class ThumbnailModel(val entry: Entry, val source: FileSource)

private const val DEFAULT_THUMBNAIL_PX = 256

class ThumbnailKeyer : Keyer<ThumbnailModel> {
    override fun key(data: ThumbnailModel, options: Options): String = thumbnailCacheKey(data.entry)
}

class ThumbnailFetcher(
    private val model: ThumbnailModel,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val sizePx = (options.size.width as? Dimension.Pixels)?.px ?: DEFAULT_THUMBNAIL_PX
        return when (val result = model.source.thumbnail(model.entry, sizePx)) {
            is FileResult.Success -> SourceResult(
                source = ImageSource(Buffer().write(result.value), options.context),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
            // Coil reports this as an error, which the UI shows as the type icon.
            else -> throw IOException("No thumbnail for ${model.entry.name}")
        }
    }

    class Factory : Fetcher.Factory<ThumbnailModel> {
        override fun create(data: ThumbnailModel, options: Options, imageLoader: ImageLoader): Fetcher =
            ThumbnailFetcher(data, options)
    }
}
