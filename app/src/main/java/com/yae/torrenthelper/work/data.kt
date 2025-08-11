package com.yae.torrenthelper.work

import androidx.work.Data
import com.yae.torrenthelper.data.TorrentInfo
import com.yae.torrenthelper.ui.screen.TorrentActions

fun TorrentInfo.toData():Data {
    return Data.Builder()
        .putString("torrInfo.name", name)
        .putString("torrInfo.id", id)
        .build()
}

fun TorrentActions.toData():Data {
    return Data.Builder()
        .putBoolean("torrActions.needDownload", needDownload)
        .putBoolean("torrActions.needUnpack", needUnpack)
        .putBoolean("torrActions.needDeleteTorrent", needDeleteTorrent)
        .build()
}

fun Data.toTorrentInfo():TorrentInfo {
    return TorrentInfo(
        name = checkNotNull(this.getString("torrInfo.name")),
        id = checkNotNull(getString("torrInfo.id")),
        tarInfo = null
    )
}

fun Data.toTorrentActions():TorrentActions {
    return TorrentActions(
        needDownload = getBoolean("torrActions.needDownload", false),
        needUnpack = getBoolean("torrActions.needUnpack", false),
        needDeleteTorrent = getBoolean("torrActions.needDeleteTorrent", false)
    )
}