package com.yae.torrenthelper.torrents

import android.util.Log
import androidx.datastore.core.DataStore
import com.yae.torrenthelper.TestSettings
import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.network.DownloadState
import com.yae.torrenthelper.network.TorrentClientSettings
import com.yae.torrenthelper.network.TorrentHelperApiService
import com.yae.torrenthelper.network.TorrentHelperApiServiceProvider
import com.yae.torrenthelper.network.download
import com.yae.torrenthelper.ui.screen.TorrentActions
import com.yae.torrenthelper.utils.UnpackState
import com.yae.torrenthelper.utils.humanizeBytes
import com.yae.torrenthelper.utils.unpackTarWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.Path

class TorrentsServiceImpl @Inject constructor(
    private val settings: DataStore<TestSettings>
) :TorrentsService {
    private suspend fun getApiClient(): TorrentHelperApiService {
        val sets = settings.data.first()
        return TorrentHelperApiServiceProvider.getClient(
            TorrentClientSettings(sets.backendURL, sets.backendUser, sets.backendPassword)
        )
    }
    private suspend fun checkSettings() {
        val data = settings.data.first()
        if (data.backendURL.isEmpty()) {
            throw IllegalArgumentException("You must provide valid backend url!")
        }
    }
    private fun makeTarFilePath(torrInfo:TorrentInfo, saveFolder:String):String {
        return saveFolder + "/${torrInfo.name}.tar"
    }

    private suspend fun unpackTar(torrInfo:TorrentInfo):Flow<TorrentActionMessage> {
        val tarPath = makeTarFilePath(
            torrInfo = torrInfo,
            saveFolder = settings.data.first().saveFolder
        )
        val dnFolder = settings.data.first().unpackFolder
        return withContext(Dispatchers.IO) {
            val iStream = FileInputStream(tarPath)
            unpackTarWithProgress(iStream, dnFolder).map { tarState ->
                when (tarState) {
                    UnpackState.Completed -> TorrentActionMessage.Finished(
                        "tar unpacking complete"
                    )

                    is UnpackState.Failed -> TorrentActionMessage.Failed(
                        tarState.error?.message ?: ""
                    )

                    is UnpackState.Progress -> TorrentActionMessage.Progress(
                        "unpack: ${tarState.filename}"
                    )
                }
            }.flowOn(Dispatchers.IO)
                .onCompletion {
                    withContext(Dispatchers.IO) {
                        iStream.close()
                    }
                }
        }
    }

    override suspend fun listTorrents(): List<TorrentInfo> {
        checkSettings()
        return getApiClient().listTorrents()
    }

    override suspend fun downloadTar(
        torrInfo: TorrentInfo,
        saveFolder: String?
    ): Flow<DownloadState> {
        // open file
        // TODO choose saveFolder when it is not null
        val filePath = makeTarFilePath(torrInfo, settings.data.first().saveFolder)
        // overwrite if exists
        val file = withContext(Dispatchers.IO) {
            FileOutputStream(filePath, false)
        }
        //return withContext(Dispatchers.IO) {
            val body = getApiClient().getTar(torrInfo.id)

            return body.download(file)
                .onCompletion {
                    Log.i("BODY", "in completion block")
                    //withContext(Dispatchers.IO) {
                        Log.i("BODY", "prepare to close body")
                        body.close()
                        Log.i("BODY", "body closed")
                    //}
                }
                .flowOn(Dispatchers.IO).distinctUntilChanged()
        //}
    }

    override suspend fun deleteTar(tarId: String) {
        getApiClient().deleteTar(tarId)
    }

    override suspend fun deleteTorrent(torrId: String) {
        getApiClient().deleteTorrent(torrId)
    }

    override suspend fun performTorrentActions(
        torrInfo: TorrentInfo,
        actions: TorrentActions
    ): Flow<TorrentActionMessage> = flow {
            // proceed download when needed
            var isFailed = false
            if (actions.needDownload) {
                downloadTar(torrInfo).collect { dnState ->
                    isFailed = dnState is DownloadState.Failed
                    emit(dnState.toTorrentActionMessage())
                }
                if (isFailed) {
                    return@flow
                }

                if (actions.needUnpack) {
                    unpackTar(torrInfo).collect {
                        isFailed = it is TorrentActionMessage.Failed
                        emit(it)
                    }
                    if (isFailed) {
                        return@flow
                    } else  {
                        withContext(Dispatchers.IO) {
                            val tarPath = makeTarFilePath(torrInfo, settings.data.first().saveFolder)
                            Files.delete(Path(tarPath))
                        }
                    }
                }
                deleteTar(torrInfo.id)
            }

            if (actions.needDeleteTorrent) {
                try {
                    emit(TorrentActionMessage.Progress("Deleting torrent"))
                    getApiClient().deleteTorrent(torrInfo.id)
                    emit(TorrentActionMessage.Finished("Torrent successfully deleted"))
                } catch (e: IOException) {
                    emit(TorrentActionMessage.Failed(e.message.toString()))
                    return@flow
                }
            }
            emit(TorrentActionMessage.Finished("Actions finished"))
        }
    }


sealed class TorrentActionMessage {
    data class Progress(val message: String, val progress: Float?=null): TorrentActionMessage()
    data class Finished(val message: String?=null): TorrentActionMessage()
    data class Failed(val message: String): TorrentActionMessage()
}

fun DownloadState.toTorrentActionMessage()
        = when(this) {
    is DownloadState.Finished -> TorrentActionMessage.Finished()
    is DownloadState.Failed -> TorrentActionMessage.Failed("Download failed: ${this.error?.message ?: ""}")
    is DownloadState.Progress -> {
        val progressStr = humanizeBytes(this.progress)
        val totalStr = humanizeBytes(this.fileSize)
        val progressText = "download $progressStr of $totalStr"
        TorrentActionMessage.Progress(progressText)
    }
}
