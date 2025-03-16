package com.yae.torrenthelper.utils

import kotlinx.coroutines.flow.flow
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.io.path.Path

sealed class UnpackState {
    data class Progress(val filename:String, val size:Long): UnpackState()
    data object Completed: UnpackState()
    data class Failed(val error:Throwable?=null): UnpackState()
}

fun unpackTarWithProgress(inputStream:InputStream, targetDir:String) = flow<UnpackState> {
    try {
        val dstPath = Path(targetDir)
        TarArchiveInputStream(inputStream).use { tarStream ->
            var entry = tarStream.nextEntry
            while (entry != null) {
                emit(UnpackState.Progress(entry.name, entry.size))
                val file = entry.resolveIn(dstPath).toFile()
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file?.parentFile?.mkdirs()
                    // unpack here
                    org.apache.commons.io.IOUtils.copy(tarStream, file.outputStream())
                }
                entry = tarStream.nextEntry
            }
            emit(UnpackState.Completed)
        }
    } catch (e:IOException) {
        emit(UnpackState.Failed(e))
    }
    finally {

    }
}