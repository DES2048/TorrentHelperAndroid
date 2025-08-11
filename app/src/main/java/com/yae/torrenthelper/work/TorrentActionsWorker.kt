package com.yae.torrenthelper.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yae.torrenthelper.torrents.TorrentsService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class TorrentActionsWorker
    @AssistedInject constructor(
        private val torrentsService: TorrentsService,
        @Assisted context:Context,
        @Assisted params:WorkerParameters):CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO get data from data
        try {
            val torrInfo = inputData.toTorrentInfo()
            val torrActions = inputData.toTorrentActions()
            setProgress(workDataOf("torrId" to torrInfo.id))
            delay(2000L)
            return Result.success(workDataOf("torrId" to torrInfo.id))
        } catch (e:IllegalStateException) {
            return Result.failure()
        }
        // create notification
        // run perform actions
        // update progress
    }

}