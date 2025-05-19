package com.nucore.app.messagecatcher

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MessageNotificationListener : NotificationListenerService() {

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

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        val entry = """
            Время: $timestamp
            Приложение: Viber
            От: $title
            Сообщение: $text

        """.trimIndent()

        saveToFile(entry, "viber_notifications.txt")
    }

    private fun handleTelegramNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title") ?: "Без названия"
        val text = extras.getCharSequence("android.text") ?: return

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        val entry = """
            Время: $timestamp
            Приложение: Telegram
            От: $title
            Сообщение: $text

        """.trimIndent()

        saveToFile(entry, "telegram_notifications.txt")
    }

    private fun saveToFile(data: String, filename: String) {
        val dir = File(getExternalFilesDir(null), "MessageLogs")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, filename)
        FileWriter(file, true).use { it.appendLine(data) }
    }
}