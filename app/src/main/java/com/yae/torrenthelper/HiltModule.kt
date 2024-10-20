package com.yae.torrenthelper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.yae.torrenthelper.data.TestSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object HiltAppModule {

    @Singleton
    @Provides
    fun provideSettingsDataStore(@ApplicationContext context: Context):DataStore<TestSettings> {
        return DataStoreFactory.create(
            serializer = TestSettingsSerializer,
            produceFile = {context.dataStoreFile("test_settings.pb")},
            corruptionHandler = null,
            scope = CoroutineScope(Dispatchers.IO+ SupervisorJob())
        )
    }
}