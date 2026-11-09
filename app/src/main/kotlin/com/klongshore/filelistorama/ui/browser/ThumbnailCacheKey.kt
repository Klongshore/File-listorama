package com.klongshore.filelistorama.ui.browser

import com.klongshore.filelistorama.domain.Entry

/**
 * Cache key for an entry's thumbnail: its identity plus modified time, so editing the image
 * produces a new key and a stale thumbnail is never reused. Never contains a URL.
 */
fun thumbnailCacheKey(entry: Entry): String =
    "thumb:${entry.ref.sourceId}:${entry.ref.itemId}:${entry.modifiedMillis}"
