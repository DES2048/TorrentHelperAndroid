package com.yae.torrenthelper.network

import com.yae.torrenthelper.data.TorrentInfo
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface TorrentHelperApiService {
    @GET("/api/torrents")
    suspend fun listTorrents():List<TorrentInfo>

    @GET("/tars/{id}")
    @Streaming
    suspend fun getTar(@Path("id") tarId:String): ResponseBody

    @DELETE("/api/tars/{id}")
    suspend fun deleteTar(@Path("id") tarId: String)

    @DELETE("/api/torrents/{id}")
    suspend fun deleteTorrent(@Path("id") torrentId:String)
}


