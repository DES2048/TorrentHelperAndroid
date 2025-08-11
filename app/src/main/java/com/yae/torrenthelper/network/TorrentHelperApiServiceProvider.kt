package com.yae.torrenthelper.network

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

data class TorrentClientSettings(val endpoint:String, val username:String,
                                 val password:String)

class TorrentHelperApiServiceProvider {
    companion object {
        @Volatile
        private var _settings: TorrentClientSettings? = null
        @Volatile
        private var _service: TorrentHelperApiService? = null

        fun getClient(settings: TorrentClientSettings):TorrentHelperApiService {
            if (_settings != settings) {
                return synchronized(this) {
                    // build new client
                    val okHttp = OkHttpClient.Builder()
                        .addInterceptor(BasicAuthInterceptor(settings.username,
                            settings.password))
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()
                    val retrofit = Retrofit.Builder()
                        .baseUrl(settings.endpoint)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttp)
                        .build()

                    _service = retrofit.create(TorrentHelperApiService::class.java)
                    _settings = settings
                    _service as TorrentHelperApiService
                }
            } else {
                return _service as TorrentHelperApiService
            }
        }
    }
}