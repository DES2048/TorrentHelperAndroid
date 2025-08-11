package com.yae.torrenthelper.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.torrents.TorrentActionMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm:HomeViewModel= hiltViewModel()) {

    LaunchedEffect(Unit) {
        vm.listTorrents()
    }

    if (vm.error.value.isNotEmpty()) {
        Toast.makeText(LocalContext.current, vm.error.value, Toast.LENGTH_LONG).show()
    }

    val torrentActionState by vm.torrentActionState.collectAsState()
    if (torrentActionState != null) {
        TorrentActionProgressDialog (progressState = torrentActionState!!, vm::cancelTorrentActions)
    }

    var openTorrentActionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedTorrentId by remember {
        mutableStateOf("")
    }

    TorrentList(torrents = vm.torrents, isLoading = vm.loading.value, onRefresh = vm::listTorrents,
        onDeleteTar = vm::deleteTar, onDeleteTorrent = vm::deleteTorrent,
        onDownloadTar = vm::downloadSingleTar,
        onAction = { tarId ->
            selectedTorrentId = tarId
            openTorrentActionDialog = true
        }
    )

    if (openTorrentActionDialog) {
        TorrentActionDialog({
            openTorrentActionDialog = false
            vm.performTorrentActions2(selectedTorrentId, it) },
            { openTorrentActionDialog = false })
    }
}

@Composable
fun TorrentActionProgressDialog(progressState: TorrentActionMessage, onCancel:()->Unit) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnClickOutside = false, dismissOnBackPress = false,
        )
    ) {
        var buttonText by remember {
            mutableStateOf("Cancel")
        }

        Card(modifier = Modifier.padding(5.dp)) {
            Text("Performing actions", fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            HorizontalDivider()
            when (progressState) {
                is TorrentActionMessage.Failed ->
                    Text(text=progressState.message,
                        color = Color.Red).also {  buttonText = "Ok" }
                is TorrentActionMessage.Finished -> Text("Complete!")
                is TorrentActionMessage.Progress ->
                    Column() {
                        Text(text=progressState.message)
                        if (progressState.progress != null) {
                            LinearProgressIndicator(
                                progress = {progressState.progress}
                            )
                        }
                    }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onCancel) {
                    Text(text = buttonText)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DemoTorrentActionProgressDialog() {
    Column(modifier = Modifier.fillMaxSize()) {
        // in progress
        val progressState = TorrentActionMessage.Progress("Download file 3.5mb/78mb",
            .50F)
        TorrentActionProgressDialog(progressState, {})

        val completeState = TorrentActionMessage.Finished()
        TorrentActionProgressDialog(completeState, {})
        val failedState = TorrentActionMessage.Failed("Download failed")
        TorrentActionProgressDialog(failedState, {})
    }
}

suspend fun pipeline(tor: TorrentWithActions) {
    val downloadChannel = Channel<TorrentAction.DownloadTar>()

    tor.actions[TorrentActionTypes.DOWNLOAD_TAR]?.let { action ->
        // TODO download torrent
        // launch downloader
        coroutineScope {
            launch {
                for (action in downloadChannel) {
                    // TODO process download
                }
            }
        }
        coroutineScope {
            launch {
                downloadChannel.send(action as TorrentAction.DownloadTar)
            }
        }
    }
    // pipeline actions, like download, unpack, delete tar, delete torrent, or else
    // some channels: download channel, unpack channel, delete tar, delete torrent
}

data class TorrentWithActions(val torrentInfo: TorrentInfo, val actions:Map<TorrentActionTypes, TorrentAction>) {
}

enum class TorrentActionTypes {
    DOWNLOAD_TAR,
    UNPACK_TAR,
    DELETE_LOCAL_TAR,
    DELETE_REMOTE_TAR,
    DELETE_TORRENT
}
sealed class TorrentAction {
    data class DownloadTar(val torrentInfo:TorrentInfo): TorrentAction()
    data class UnpackTar(val tarFile:String, val outFolder:String): TorrentAction()
    data class DeleteLocalTar(val tarFile:String): TorrentAction()
    data class DeleteRemoteTar(val tarId:String): TorrentAction()
    data class DeleteTorrent(val torrentId:String): TorrentAction()
}