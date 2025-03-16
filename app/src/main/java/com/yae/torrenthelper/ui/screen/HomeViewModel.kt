package com.yae.torrenthelper.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yae.torrenthelper.TestSettings
import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.network.DownloadState
import com.yae.torrenthelper.network.TorrentClientSettings
import com.yae.torrenthelper.network.TorrentHelperApiService
import com.yae.torrenthelper.network.TorrentHelperApiServiceProvider
import com.yae.torrenthelper.network.download
import com.yae.torrenthelper.utils.UnpackState
import com.yae.torrenthelper.utils.humanizeBytes
import com.yae.torrenthelper.utils.unpackTarWithProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.Path

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: DataStore<TestSettings>
): ViewModel() {
    // torrents list
    private var _torrents = mutableStateListOf<TorrentInfo>()
    val torrents:List<TorrentInfo>
        get() = _torrents

    // loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    // error message
    private val _error = mutableStateOf("")
    val error: State<String> = _error

    private val _torrentActionState = MutableStateFlow<TorrentActionMessage?>(null)
    val torrentActionState = _torrentActionState.asStateFlow()

    private var downloadJob:Job? = null
    private var torrentActionJob:Job? = null

    private suspend fun checkSettings() {
        val data = settings.data.first()
        if (data.backendURL.isEmpty()) {
            throw IllegalArgumentException("You must provide valid backend url!")
        }
    }
    private suspend fun getApiClient(): TorrentHelperApiService {
        val sets = settings.data.first()
        return TorrentHelperApiServiceProvider.getClient(
            TorrentClientSettings(sets.backendURL, sets.backendUser, sets.backendPassword)
        )
    }
    fun listTorrents() {
        viewModelScope.launch {
            try {
                _error.value = ""
                _loading.value = true
                checkSettings()
                getApiClient().listTorrents().also { data ->
                    _torrents = data.toMutableStateList()
                }

            } catch (e:Throwable) {
                _error.value = e.message ?: ""
            } finally {
                _loading.value = false
            }
        }
    }
    private fun makeTarFilePath(torrInfo:TorrentInfo, saveFolder:String):String {
        return saveFolder + "/${torrInfo.name}.tar"
    }
    private suspend fun internalDownloadTar(tarId: String): Flow<DownloadState> {
              val downloadFileName = _torrents.first { it.id == tarId }.name
              // open file
              val filePath = settings.data.first().saveFolder + "/${downloadFileName}.tar"
              // overwrite if exists
              val file = withContext(Dispatchers.IO) {
                  FileOutputStream(filePath, false)
              }
              return getApiClient().getTar(tarId)
                      .download(file)
                      .flowOn(Dispatchers.IO).distinctUntilChanged()
    }
    fun downloadSingleTar(tarId: String) {
        downloadJob = viewModelScope.launch {
            downloadTar(tarId)
            if (_torrentActionState.value is TorrentActionMessage.Finished) {
                _torrentActionState.value = null
            }
        }
    }
    suspend fun downloadTar(tarId: String) {
                    internalDownloadTar(tarId).map { state ->
                      state.toTorrentActionMessage()
                    }.collect { msg->
                        _torrentActionState.value = msg
                    }
            if (_torrentActionState.value is TorrentActionMessage.Finished) {
                _torrentActionState.value = TorrentActionMessage.Progress("Deleting remote tar")
                deleteTar(tarId)
                _torrentActionState.value = TorrentActionMessage.Finished("remote tar deleting sucessfully")
            }

    }

    fun cancelSingleDownload() {
        downloadJob?.cancel("")
        _torrentActionState.value = null
    }

    fun deleteTar(tarId:String) {
        viewModelScope.launch {
            getApiClient().deleteTar(tarId)
            // TODO delete tar info from torrent entry
            val idx = _torrents.indexOfFirst { elem-> elem.id == tarId }
            if (idx>=0) {
                _torrents[idx] = _torrents[idx].copy(tarInfo = null)
            }
        }
    }

    fun deleteTorrent(torrentId:String) {
        viewModelScope.launch {
          internalDeleteTorrent(torrentId)
        }
    }

    private suspend fun internalDeleteTorrent(torrentId: String) {
        getApiClient().deleteTorrent(torrentId)
        _torrents.removeIf { elem-> elem.id ==torrentId }
    }

    fun performTorrentActions(torrentId:String, actions:TorrentActions) {
        torrentActionJob = viewModelScope.launch {
            // proceed download when needed
            if (actions.needDownload) {
                downloadTar(torrentId)
                if (_torrentActionState.value is TorrentActionMessage.Failed) {
                    return@launch
                }
            }
            if (actions.needUnpack) {
                val tarPath = makeTarFilePath(
                    torrInfo = _torrents.first { it.id == torrentId },
                    saveFolder = settings.data.first().saveFolder
                )
                val dnFolder = settings.data.first().unpackFolder
                FileInputStream(tarPath).use { tarStream ->
                    unpackTarWithProgress(tarStream, dnFolder).map { tarState->
                        when(tarState) {
                            UnpackState.Completed -> TorrentActionMessage.Finished(
                                "tar unpacking complete"
                            )
                            is UnpackState.Failed -> TorrentActionMessage.Failed(
                                tarState.error?.message ?:"")
                            is UnpackState.Progress -> TorrentActionMessage.Progress(
                                "unpack: ${tarState.filename}"
                            )
                        }
                    }.flowOn(Dispatchers.IO)
                        .collect { state->
                            _torrentActionState.value = state
                        }
                }
                if (_torrentActionState.value is TorrentActionMessage.Failed) {
                    return@launch
                } else if (torrentActionState.value is TorrentActionMessage.Finished) {
                    withContext(Dispatchers.IO) {
                        Files.delete(Path(tarPath))
                    }
                }
            }

            if (actions.needDeleteTorrent) {
                try {
                    _torrentActionState.value = TorrentActionMessage.Progress("Deleting torrent")
                    internalDeleteTorrent(torrentId)
                    _torrentActionState.value = TorrentActionMessage.Finished("Torrent successfully deleted")
                } catch (e:IOException) {
                    _torrentActionState.value = TorrentActionMessage.Failed(e.message.toString())
                    return@launch
                }
            }
            _torrentActionState.value = null
        }
    }

    fun cancelTorrentActions() {
        torrentActionJob?.cancel()
        torrentActionJob = null
        _torrentActionState.value = null
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