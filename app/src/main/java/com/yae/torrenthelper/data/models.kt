package com.yae.torrenthelper.data

data class TarInfo(val path:String, val size:Int, val url:String)

data class TorrentInfo(
    val name:String,
    val id:String,
    val tarInfo: TarInfo?)