package com.klongshore.filelistorama.data.local

import android.os.Environment
import com.klongshore.filelistorama.domain.StoragePermission

/** All files access (`MANAGE_EXTERNAL_STORAGE`), granted by the user in system settings. */
class AndroidStoragePermission : StoragePermission {
    override fun isGranted(): Boolean = Environment.isExternalStorageManager()
}
