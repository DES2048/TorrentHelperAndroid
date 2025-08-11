package com.yae.torrenthelper.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

sealed class DownloadState {
    data class Progress(val progress:Long, val fileSize:Long): DownloadState()
    data object Finished: DownloadState()
    data class Failed(val error:Throwable? = null): DownloadState()
}

sealed class CopyState {
    data class Progress(val progress:Int): CopyState()
    data object Finished: CopyState()
    data class Failed(val error: Throwable? = null): CopyState()
}

fun copyWithProgress(src:InputStream, dst:OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE)
= flow {
    val buffer = ByteArray(bufferSize)
    try {
    var bytes = src.read(buffer)
    var progressBytes = 0L
    while (bytes >= 0) {
        dst.write(buffer, 0, bytes)
        progressBytes += bytes
        emit(CopyState.Progress(progressBytes.toInt()))
        bytes = src.read(buffer)
    }
        emit(CopyState.Finished)
    } catch (e: IOException) {
        emit(CopyState.Failed(e))
    }
}.flowOn(Dispatchers.IO)

fun ResponseBody.download(out:OutputStream, bufferSize:Int= DEFAULT_BUFFER_SIZE): Flow<DownloadState> {
    val body = this@download

    /*return body.byteStream().use { bs ->*/
    return copyWithProgress(body.byteStream(), out)
        .flowOn(Dispatchers.IO)
        .map { state ->
            when (state) {
                is CopyState.Failed -> DownloadState.Failed(state.error)
                CopyState.Finished -> DownloadState.Finished
                is CopyState.Progress -> DownloadState.Progress(
                    state.progress.toLong(),
                    body.contentLength()
                )
            }
        }

}
