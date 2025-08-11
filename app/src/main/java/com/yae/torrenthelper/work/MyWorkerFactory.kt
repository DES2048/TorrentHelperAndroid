package com.yae.torrenthelper.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.yae.torrenthelper.torrents.TorrentsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyWorkerFactory @Inject constructor(
    private val torrentsService: TorrentsService):WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            TorrentActionsWorker::class.java.name ->TorrentActionsWorker(torrentsService,
                appContext, workerParameters)
            else -> null
        }
    }
}