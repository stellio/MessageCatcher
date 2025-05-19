package com.nucore.app.messagecatcher

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var messagesContainer: LinearLayout
    private lateinit var database: AppDatabase
    private val dateFormat = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotificationLog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // Header
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.system_accent1_600))
        }

        val titleText = TextView(this).apply {
            text = "MessageCatcher"
            textSize = 24f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val statusIndicator = ImageView(this).apply {
            setImageResource(android.R.drawable.presence_invisible)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(48, 48)
        }

        headerLayout.addView(titleText)
        headerLayout.addView(statusIndicator)

        // Buttons container
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
            gravity = Gravity.CENTER
        }

        val accessButton = MaterialButton(this).apply {
            text = "Настройки доступа"
            setIconResource(android.R.drawable.ic_menu_manage)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val clearButton = MaterialButton(this).apply {
            text = "Очистить"
            setIconResource(android.R.drawable.ic_menu_delete)
            setOnClickListener {
                showClearConfirmationDialog()
            }
        }

        buttonsLayout.addView(accessButton)
        buttonsLayout.addView(clearButton)

        // Messages container
        val scrollView = ScrollView(this)
        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 16)
        }
        scrollView.addView(messagesContainer)

        mainLayout.addView(headerLayout)
        mainLayout.addView(buttonsLayout)
        mainLayout.addView(MaterialDivider(this))
        mainLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(mainLayout)

        if (!isNotificationAccessEnabled()) {
            statusIndicator.setImageResource(android.R.drawable.presence_invisible)
            Toast.makeText(this, "Доступ к уведомлениям отключён", Toast.LENGTH_LONG).show()
        } else {
            statusIndicator.setImageResource(android.R.drawable.presence_online)
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
                messagesContainer.removeAllViews()
                
                if (messages.isEmpty()) {
                    val emptyView = TextView(this@MainActivity).apply {
                        text = "Нет сообщений"
                        textSize = 18f
                        gravity = Gravity.CENTER
                        setPadding(0, 32, 0, 32)
                    }
                    messagesContainer.addView(emptyView)
                    return@collectLatest
                }

                val groupedMessages = messages.groupBy { it.appName }
                groupedMessages.forEach { (appName, appMessages) ->
                    // App header
                    val appHeader = TextView(this@MainActivity).apply {
                        text = appName
                        textSize = 20f
                        setTextColor(ContextCompat.getColor(context, android.R.color.system_accent1_600))
                        setPadding(0, 16, 0, 8)
                    }
                    messagesContainer.addView(appHeader)

                    // Messages
                    appMessages.forEach { message ->
                        val card = MaterialCardView(this@MainActivity).apply {
                            radius = 8f
                            cardElevation = 4f
                            setCardBackgroundColor(Color.WHITE)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 16)
                            }
                        }

                        val cardContent = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 16, 16, 16)
                        }

                        val headerLayout = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }

                        val senderText = TextView(this@MainActivity).apply {
                            text = message.sender
                            textSize = 16f
                            setTextColor(Color.BLACK)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        }

                        val timeText = TextView(this@MainActivity).apply {
                            text = dateFormat.format(message.timestamp)
                            textSize = 12f
                            setTextColor(Color.GRAY)
                        }

                        headerLayout.addView(senderText)
                        headerLayout.addView(timeText)

                        val messageText = TextView(this@MainActivity).apply {
                            text = message.content
                            textSize = 14f
                            setTextColor(Color.DKGRAY)
                            setPadding(0, 8, 0, 0)
                        }

                        cardContent.addView(headerLayout)
                        cardContent.addView(messageText)
                        card.addView(cardContent)
                        messagesContainer.addView(card)
                    }
                }
            }
        }
    }

    private fun showClearConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Очистить историю")
            .setMessage("Вы уверены, что хотите удалить все сообщения?")
            .setPositiveButton("Удалить") { _, _ ->
                clearNotificationLog()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearNotificationLog() {
        lifecycleScope.launch {
            database.messageDao().deleteAll()
            Toast.makeText(this@MainActivity, "История очищена", Toast.LENGTH_SHORT).show()
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
