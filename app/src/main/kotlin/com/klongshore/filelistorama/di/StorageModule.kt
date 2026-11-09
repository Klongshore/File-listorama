package com.klongshore.filelistorama.di

import android.os.Environment
import com.klongshore.filelistorama.data.local.AndroidStoragePermission
import com.klongshore.filelistorama.data.local.LocalFileSource
import com.klongshore.filelistorama.domain.FileSource
import com.klongshore.filelistorama.domain.StoragePermission
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideFileSource(): FileSource =
        LocalFileSource(Environment.getExternalStorageDirectory())

    @Provides
    @Singleton
    fun provideStoragePermission(): StoragePermission = AndroidStoragePermission()
}
