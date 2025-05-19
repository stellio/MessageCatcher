package com.nucore.app.messagecatcher

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nucore.app.messagecatcher.dao.AppDatabase
import com.nucore.app.messagecatcher.dao.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MessageNotificationListener : NotificationListenerService() {
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        when (sbn.packageName) {
            "com.viber.voip" -> handleViberNotification(sbn)
            "org.telegram.messenger" -> handleTelegramNotification(sbn)
            else -> return
        }

        // Уведомим активити об обновлении
        val intent = Intent("com.example.messagecatcher.UPDATE_LOG")
        sendBroadcast(intent)
    }

    private fun handleViberNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title") ?: "Без названия"
        val text = extras.getCharSequence("android.text") ?: return

        val message = Message(
            timestamp = Date(),
            appName = "Viber",
            sender = title.toString(),
            content = text.toString()
        )

        saveMessage(message)
    }

    private fun handleTelegramNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title") ?: "Без названия"
        val text = extras.getCharSequence("android.text") ?: return

        val message = Message(
            timestamp = Date(),
            appName = "Telegram",
            sender = title.toString(),
            content = text.toString()
        )

        saveMessage(message)
    }

    private fun saveMessage(message: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            database.messageDao().insert(message)
        }
    }
}