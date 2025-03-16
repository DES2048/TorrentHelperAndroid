package com.yae.torrenthelper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.yae.torrenthelper.data.TestSettingsSerializer
import com.yae.torrenthelper.network.BasicAuthInterceptor
import com.yae.torrenthelper.network.TorrentHelperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
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

    /*
    @Singleton
    @Provides
    fun provideTorrentHelperApiService(settings:DataStore<TestSettings>): TorrentHelperApiService {
        // get settings sync
        val data = runBlocking {
            settings.data.first()
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(data.backendUser, data.backendPassword))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(data.backendURL)
            .client(okHttp)
            .build()

        return retrofit.create(TorrentHelperApiService::class.java)
    } */
}