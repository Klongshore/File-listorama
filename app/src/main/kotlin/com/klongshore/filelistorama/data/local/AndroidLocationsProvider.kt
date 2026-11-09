package com.klongshore.filelistorama.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import com.klongshore.filelistorama.domain.Location
import com.klongshore.filelistorama.domain.LocationsProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy

/** Mounted storage volumes, re-read whenever the system reports a mount or unmount. */
class AndroidLocationsProvider(private val context: Context) : LocationsProvider {

    override val locations: Flow<List<Location>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(readVolumes())
            }
        }
        val filter = IntentFilter().apply {
            MOUNT_ACTIONS.forEach(::addAction)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        trySend(readVolumes())
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChangedBy { list -> list.map { it.id to it.name } }

    private fun readVolumes(): List<Location> {
        val manager = context.getSystemService(StorageManager::class.java)
        return manager.storageVolumes.mapNotNull { volume ->
            val mounted = volume.state == Environment.MEDIA_MOUNTED ||
                volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
            val dir = volume.directory
            if (!mounted || dir == null) return@mapNotNull null
            val id = if (volume.isPrimary) "local" else "local:${volume.uuid ?: dir.name}"
            val name = if (volume.isPrimary) "Internal storage" else volume.getDescription(context)
            Location(LocalFileSource(dir, displayName = name, sourceId = id, thumbnails = BitmapThumbnailGenerator()))
        }
    }

    private companion object {
        val MOUNT_ACTIONS = listOf(
            Intent.ACTION_MEDIA_MOUNTED,
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MEDIA_EJECT,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_BAD_REMOVAL,
            Intent.ACTION_MEDIA_UNMOUNTABLE,
        )
    }
}
