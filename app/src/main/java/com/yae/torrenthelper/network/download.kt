package com.yae.torrenthelper.network

import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.OutputStream

sealed class DownloadState {
    data class Progress(val progress:Long, val fileSize:Long): DownloadState()
    data object Finished: DownloadState()
    data class Failed(val error:Throwable? = null): DownloadState()
}

fun ResponseBody.download(out:OutputStream, bufferSize:Int= DEFAULT_BUFFER_SIZE) = flow<DownloadState> {
    val body = this@download
    emit(DownloadState.Progress(0, body.contentLength()))

    try {
       body.byteStream().use { bs->
               val buffer = ByteArray(bufferSize)
               var bytes = bs.read(buffer)
               var progressBytes = 0L
               while (bytes >= 0) {
                   out.write(buffer,0, bytes)
                   progressBytes += bytes
                   emit(DownloadState.Progress(progressBytes, body.contentLength()))
                   bytes = bs.read(buffer)
               }
       }
       emit(DownloadState.Finished)
    } catch (e:Exception) {
        emit(DownloadState.Failed(e))
    }
}
