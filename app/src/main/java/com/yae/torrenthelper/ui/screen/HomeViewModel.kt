package com.yae.torrenthelper.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ArrayCreatingInputMerger
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yae.torrenthelper.TestSettings
import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.torrents.TorrentActionMessage
import com.yae.torrenthelper.torrents.TorrentsService
import com.yae.torrenthelper.torrents.toTorrentActionMessage
import com.yae.torrenthelper.utils.UnpackState
import com.yae.torrenthelper.utils.unpackTarWithProgress
import com.yae.torrenthelper.work.TorrentActionsWorker
import com.yae.torrenthelper.work.toData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.Path

const val LOGCAT_TAG = "home vm"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: DataStore<TestSettings>,
    private val torrentsService: TorrentsService,
    private val workManager: WorkManager
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

    private fun makeTarFilePath(torrInfo:TorrentInfo, saveFolder:String):String {
        return saveFolder + "/${torrInfo.name}.tar"
    }
    fun listTorrents() {
        viewModelScope.launch {
            try {
                _error.value = ""
                _loading.value = true
                torrentsService.listTorrents().also { data ->
                    _torrents = data.toMutableStateList()
                }

            } catch (e:Throwable) {
                _error.value = ("Failed to load torrents: " + e.message) ?: ""
            } finally {
                _loading.value = false
            }
        }
    }

    fun downloadSingleTar(tarId: String) {
        val workRequest = OneTimeWorkRequestBuilder<TorrentActionsWorker>().build()
        workManager.enqueue(workRequest)
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { state ->
                _error.value = state.toString()
            }
        }
    }
    suspend fun downloadTar(tarId: String) {
            val torrInfo = _torrents.first { it.id == tarId }
                    torrentsService.downloadTar(torrInfo, settings.data.first().saveFolder)
                        .map { state ->
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

    fun deleteTar(tarId:String) {
        viewModelScope.launch {
            try {
                _error.value = ""
                torrentsService.deleteTar(tarId)
                val idx = _torrents.indexOfFirst { elem -> elem.id == tarId }
                if (idx >= 0) {
                    _torrents[idx] = _torrents[idx].copy(tarInfo = null)
                }
            } catch (e: Throwable) {
                _error.value = "Failed to delete remote tar: ${e.message}"
            }
        }
    }

    fun deleteTorrent(torrentId:String) {
        viewModelScope.launch {
            try {
                _error.value = ""
                internalDeleteTorrent(torrentId)
            } catch (e: Throwable) {
                _error.value = "Failed to delete torrent: ${e.message}"
            }
        }
    }

    private suspend fun internalDeleteTorrent(torrentId: String) {
        torrentsService.deleteTorrent(torrentId)
        _torrents.removeIf { elem-> elem.id ==torrentId }
    }

    fun performTorrentActions3(torrentId: String, actions: TorrentActions) {
        _error.value = ""
        // create work data
        val workRequest = OneTimeWorkRequestBuilder<TorrentActionsWorker>()
            .setInputMerger(ArrayCreatingInputMerger::class.java)
            .setInputData(torrents.first { it.id == torrentId}.toData())
            .setInputData(actions.toData())
            .build()

        workManager.enqueue(workRequest)
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { state ->
                _error.value = state.toString()
                delay(3000L)
            }
        }

    }
    fun performTorrentActions2(torrentId: String, actions:TorrentActions) {
        _error.value = ""
        viewModelScope.launch {
            torrentActionJob = launch {
                torrentsService.performTorrentActions(
                    torrents.first { it.id == torrentId },
                    actions
                )
                    .collect { state ->
                        _torrentActionState.value = state
                    }
                if (_torrentActionState.value is TorrentActionMessage.Finished) {
                    _torrentActionState.value = null
                }
            }
            torrentActionJob!!.join()
            if (actions.needDeleteTorrent) {
                _torrents.removeIf { elem ->
                    elem.id == torrentId
                }
            }
            torrentActionJob = null
        }

    }
    fun performTorrentActions(torrentId:String, actions:TorrentActions) {
        _error.value = ""
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
        viewModelScope.launch {
            torrentActionJob?.cancelAndJoin()
            torrentActionJob = null
            _torrentActionState.value = null
        }
    }
}
