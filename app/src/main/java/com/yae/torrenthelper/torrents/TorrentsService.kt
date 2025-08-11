package com.yae.torrenthelper.torrents

import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.network.DownloadState
import com.yae.torrenthelper.ui.screen.TorrentActions
import kotlinx.coroutines.flow.Flow

interface TorrentsService {
    suspend fun listTorrents():List<TorrentInfo>
    suspend fun downloadTar(torrInfo:TorrentInfo, saveFolder:String?=null):Flow<DownloadState>
    suspend fun deleteTar(tarId:String)
    suspend fun deleteTorrent(torrId:String)
    suspend fun performTorrentActions(torrInfo: TorrentInfo, actions: TorrentActions):
            Flow<TorrentActionMessage>
}