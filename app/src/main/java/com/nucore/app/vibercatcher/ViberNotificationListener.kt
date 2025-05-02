package com.nucore.app.vibercatcher


import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ViberNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.viber.voip") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title") ?: "Без названия"
        val text = extras.getCharSequence("android.text") ?: return

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        val entry = """
            Время: $timestamp
            От: $title
            Сообщение: $text

        """.trimIndent()

        saveToFile(entry)

        // Уведомим активити об обновлении
        val intent = Intent("com.example.vibersaver.UPDATE_LOG")
        sendBroadcast(intent)
    }

    private fun saveToFile(data: String) {
        val dir = File(getExternalFilesDir(null), "ViberLogs")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "viber_notifications.txt")
        FileWriter(file, true).use { it.appendLine(data) }
    }
}