package com.yae.torrenthelper.ui.screen

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yae.torrenthelper.R
import com.yae.torrenthelper.data.TarInfo
import com.yae.torrenthelper.data.TorrentInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentList(torrents: List<TorrentInfo>, isLoading:Boolean, onRefresh:()->Unit,
                onDeleteTar:(String)->Unit, onDeleteTorrent:(String)->Unit,
                onDownloadTar:(String)->Unit, onAction: (String) ->Unit,
                modifier: Modifier = Modifier) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh
    ) {
        // show list of torrents
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(torrents, key = { it.id }) { torrInfo ->
                TorrentListItem(torrInfo, onDeleteTar, onDeleteTorrent, onDownloadTar, onAction)
            }
        }
    }
}

@Composable
fun TorrentListItem(torrInfo: TorrentInfo, onDeleteTar:(String)->Unit, onDeleteTorrent:(String)->Unit,
                    onDownloadTar:(String)->Unit, onAction: (String) ->Unit,
                    modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = torrInfo.name,
                    modifier = Modifier.padding(5.dp)
                )
                IconButton(onClick = { onDeleteTorrent(torrInfo.id) },
                    modifier=Modifier.requiredSize(49.dp, 50.dp).weight(1F,fill = true)) {
                    Icon(Icons.Filled.Delete, "",
                        )
                }
            }
            // tar info row
            if (torrInfo.tarInfo != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeRepr = Formatter.formatFileSize(
                        LocalContext.current, torrInfo.tarInfo.size.toLong()
                    )
                    Text(text = "Size:")
                    Text(text = sizeRepr)
                    IconButton(onClick = { onAction(torrInfo.id) }) {
                        Icon(Icons.Filled.PlayArrow, "")
                    }
                    IconButton(onClick = { onDownloadTar(torrInfo.id) }) {
                        Icon(painterResource(R.drawable.arrow_down_bold), "")
                    }
                    IconButton(onClick = { onDeleteTar(torrInfo.id) }) {
                        Icon(Icons.Filled.Delete, "")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SamplePreview() {
    Text("Sample text")
}

@Preview
@Composable
fun PreviewTorrentListItem() {
    val torrInfo = TorrentInfo("Sample torrent with a very very very  very long long long name", "id",
        TarInfo("some path", 65536, "some url")
    )
    TorrentListItem(torrInfo, {}, {}, {}, {})
}