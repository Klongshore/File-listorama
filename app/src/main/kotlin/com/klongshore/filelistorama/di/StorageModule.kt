package com.klongshore.filelistorama.di

import android.content.Context
import com.klongshore.filelistorama.data.local.AndroidLocationsProvider
import com.klongshore.filelistorama.data.local.AndroidStoragePermission
import com.klongshore.filelistorama.domain.LocationsProvider
import com.klongshore.filelistorama.domain.StoragePermission
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideLocations(@ApplicationContext context: Context): LocationsProvider =
        AndroidLocationsProvider(context)

    @Provides
    @Singleton
    fun provideStoragePermission(): StoragePermission = AndroidStoragePermission()
}
