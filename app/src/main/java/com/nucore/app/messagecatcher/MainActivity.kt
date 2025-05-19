package com.nucore.app.messagecatcher

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotificationLog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val accessButton = Button(this).apply {
            text = "Разрешить доступ к уведомлениям"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val clearButton = Button(this).apply {
            text = "Очистить лог"
            setOnClickListener {
                clearNotificationLog()
            }
        }

        textView = TextView(this).apply {
            textSize = 16f
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }

        layout.addView(accessButton)
        layout.addView(clearButton)
        layout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(layout)

        if (!isNotificationAccessEnabled()) {
            Toast.makeText(this, "Доступ к уведомлениям отключён", Toast.LENGTH_LONG).show()
        }

        loadNotificationLog()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(updateReceiver, IntentFilter("com.example.messagecatcher.UPDATE_LOG"), Context.RECEIVER_NOT_EXPORTED)
        loadNotificationLog()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateReceiver)
    }

    private fun loadNotificationLog() {
        val dir = File(getExternalFilesDir(null), "MessageLogs")
        if (!dir.exists()) {
            textView.text = "Файлы пока не созданы или уведомлений ещё не было."
            return
        }

        val viberFile = File(dir, "viber_notifications.txt")
        val telegramFile = File(dir, "telegram_notifications.txt")

        val viberContent = if (viberFile.exists()) viberFile.readText() else ""
        val telegramContent = if (telegramFile.exists()) telegramFile.readText() else ""

        textView.text = if (viberContent.isNotEmpty() || telegramContent.isNotEmpty()) {
            buildString {
                if (viberContent.isNotEmpty()) {
                    append("=== Viber сообщения ===\n\n")
                    append(viberContent)
                }
                if (telegramContent.isNotEmpty()) {
                    if (viberContent.isNotEmpty()) append("\n\n")
                    append("=== Telegram сообщения ===\n\n")
                    append(telegramContent)
                }
            }
        } else {
            "Файлы пока не созданы или уведомлений ещё не было."
        }
    }

    private fun clearNotificationLog() {
        val dir = File(getExternalFilesDir(null), "MessageLogs")
        if (!dir.exists()) {
            Toast.makeText(this, "Файлы уже пусты", Toast.LENGTH_SHORT).show()
            return
        }

        val viberFile = File(dir, "viber_notifications.txt")
        val telegramFile = File(dir, "telegram_notifications.txt")

        var deleted = false
        if (viberFile.exists()) {
            viberFile.delete()
            deleted = true
        }
        if (telegramFile.exists()) {
            telegramFile.delete()
            deleted = true
        }

        if (deleted) {
            Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Файлы уже пусты", Toast.LENGTH_SHORT).show()
        }
        
        textView.text = "Файлы пока не созданы или уведомлений ещё не было."
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
