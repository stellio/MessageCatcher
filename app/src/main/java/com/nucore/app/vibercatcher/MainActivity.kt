package com.nucore.app.vibercatcher

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
        registerReceiver(updateReceiver, IntentFilter("com.example.vibersaver.UPDATE_LOG"), Context.RECEIVER_NOT_EXPORTED)
        loadNotificationLog()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateReceiver)
    }

    private fun loadNotificationLog() {
        val file = File(getExternalFilesDir(null), "ViberLogs/viber_notifications.txt")
        textView.text = if (file.exists()) {
            file.readText()
        } else {
            "Файл пока не создан или уведомлений ещё не было."
        }
    }

    private fun clearNotificationLog() {
        val file = File(getExternalFilesDir(null), "ViberLogs/viber_notifications.txt")
        if (file.exists()) {
            file.delete()
            Toast.makeText(this, "Лог очищен", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Файл уже пуст", Toast.LENGTH_SHORT).show()
        }
        textView.text = "Файл пока не создан или уведомлений ещё не было."
    }


    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
