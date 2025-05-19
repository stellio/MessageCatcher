package com.nucore.app.messagecatcher

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var database: AppDatabase

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotificationLog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

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
        lifecycleScope.launch {
            database.messageDao().getAllMessages().collectLatest { messages ->
                if (messages.isEmpty()) {
                    textView.text = "Сообщений пока нет"
                    return@collectLatest
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val groupedMessages = messages.groupBy { it.appName }

                textView.text = buildString {
                    groupedMessages.forEach { (appName, appMessages) ->
                        append("=== $appName сообщения ===\n\n")
                        appMessages.forEach { message ->
                            append("Время: ${dateFormat.format(message.timestamp)}\n")
                            append("От: ${message.sender}\n")
                            append("Сообщение: ${message.content}\n\n")
                        }
                    }
                }
            }
        }
    }

    private fun clearNotificationLog() {
        lifecycleScope.launch {
            database.messageDao().deleteAll()
            Toast.makeText(this@MainActivity, "Логи очищены", Toast.LENGTH_SHORT).show()
            textView.text = "Сообщений пока нет"
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
