package com.klongshore.filelistorama.domain

/** Whether the app currently has All files access to shared storage. */
interface StoragePermission {
    fun isGranted(): Boolean
}
