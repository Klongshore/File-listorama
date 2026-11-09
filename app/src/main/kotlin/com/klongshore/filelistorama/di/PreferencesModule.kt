package com.klongshore.filelistorama.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.klongshore.filelistorama.data.prefs.DataStoreBrowserPreferences
import com.klongshore.filelistorama.domain.BrowserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.browserDataStore by preferencesDataStore(name = "browser")

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideBrowserPreferences(@ApplicationContext context: Context): BrowserPreferences =
        DataStoreBrowserPreferences(context.browserDataStore)
}
