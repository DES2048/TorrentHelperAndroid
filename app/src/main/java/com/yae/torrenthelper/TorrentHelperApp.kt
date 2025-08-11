package com.yae.torrenthelper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yae.torrenthelper.work.MyWorkerFactory
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class TorrentHelperApp() :Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): MyWorkerFactory
    }
    override val workManagerConfiguration: Configuration
        = Configuration.Builder().setWorkerFactory(
            EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory()
        ).build()

    override fun onCreate() {
        super.onCreate()
        val notificationChannel = NotificationChannel(
            TORR_ACTIONS_CHANNEL_ID,
            TORR_ACTIONS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

    }
}