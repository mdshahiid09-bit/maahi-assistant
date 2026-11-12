package com.maahiassistant.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.maahiassistant.android.data.db.AppDatabase
import com.maahiassistant.android.data.local.AppPreferences
import com.maahiassistant.android.data.local.SecureKeyStore
import com.maahiassistant.android.data.repository.ChatRepository
import com.maahiassistant.android.data.repository.SettingsRepository

class MaahiApplication : Application() {

    companion object {
        const val SESSION_CHANNEL_ID = "maahi_session_channel"
        lateinit var instance: MaahiApplication
            private set
    }

    lateinit var database: AppDatabase
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        chatRepository = ChatRepository(database.chatMessageDao())

        val secureKeyStore = SecureKeyStore(this)
        val appPreferences = AppPreferences(this)
        settingsRepository = SettingsRepository(secureKeyStore, appPreferences)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(SESSION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
