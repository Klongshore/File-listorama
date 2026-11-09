package com.klongshore.filelistorama.ui.browser

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.klongshore.filelistorama.domain.Entry
import java.io.File

/**
 * Hands [entry] to an app the user picks, with a read-only grant for just that file.
 * Returns false if the file cannot be shared or no installed app can open it.
 *
 * Local files only: item ids are paths. Cloud sources will download to a cache file first.
 */
fun openWithSystem(context: Context, entry: Entry): Boolean {
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(entry.ref.itemId))
    } catch (e: IllegalArgumentException) {
        return false
    }
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, entry.mimeType ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (context.packageManager.queryIntentActivities(view, 0).isEmpty()) return false
    context.startActivity(Intent.createChooser(view, null))
    return true
}
