package com.researchflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ResearchFlowApp : Application() {

    companion object {
        const val CHANNEL_RESEARCH = "research_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_RESEARCH,
            "Araştırma Bildirimleri",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Araştırma tamamlandığında bildirim gönderir"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
