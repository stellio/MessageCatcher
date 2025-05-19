package com.nucore.app.messagecatcher

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.MenuItem
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
import com.nucore.app.messagecatcher.dao.AppDatabase
import com.nucore.app.messagecatcher.dao.FilteredSender
import com.nucore.app.messagecatcher.dao.Message
import com.nucore.app.messagecatcher.fragments.FilteredSendersFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var messagesContainer: LinearLayout
    private lateinit var database: AppDatabase
    private val dateFormat = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())
    private var filteredSenders = setOf<String>()
    private lateinit var mainLayout: LinearLayout

    // App-specific colors
    private val viberColor = Color.parseColor("#7360F2") // Viber purple
    private val telegramColor = Color.parseColor("#0088cc") // Telegram blue
    private val viberLightColor = Color.parseColor("#F0EDFF") // Light purple for Viber
    private val telegramLightColor = Color.parseColor("#E6F7FF") // Light blue for Telegram
    private val warningColor = Color.parseColor("#FFA500") // Orange for warning

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotificationLog()
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun showNotificationListenerWarning() {
        // Remove any existing warning
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is LinearLayout && child.getChildAt(0) is ImageView && 
                (child.getChildAt(0) as ImageView).drawable.constantState == 
                ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)?.constantState) {
                mainLayout.removeView(child)
                break
            }
        }

        val warningLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(warningColor)
            setPadding(16, 12, 16, 12)
        }

        val warningIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_dialog_alert)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(24, 24)
        }

        val warningText = TextView(this).apply {
            text = "Для работы приложения необходимо включить доступ к уведомлениям"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(8, 0, 0, 0)
        }

        val settingsButton = MaterialButton(this).apply {
            text = "Настройки"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        warningLayout.addView(warningIcon)
        warningLayout.addView(warningText, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ))
        warningLayout.addView(settingsButton)

        mainLayout.addView(warningLayout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 30, 0, 0)
        }

        // Header
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Messages container
        val scrollView = ScrollView(this)
        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 16)
        }
        scrollView.addView(messagesContainer)

        mainLayout.addView(headerLayout)
        mainLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(mainLayout)
        loadFilteredSenders()
        loadNotificationLog()
        if (!isNotificationListenerEnabled()) {
            showNotificationListenerWarning()
        }
    }

    private fun loadFilteredSenders() {
        lifecycleScope.launch {
            database.filteredSenderDao().getAllFilteredSenders().collectLatest { senders ->
                filteredSenders = senders.map { it.senderName }.toSet()
                loadNotificationLog()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                true
            }
            R.id.menu_filtered_senders -> {
                showFilteredSendersFragment()
                true
            }
            R.id.menu_clear -> {
                showClearConfirmationDialog()
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(updateReceiver, IntentFilter("com.example.messagecatcher.UPDATE_LOG"), Context.RECEIVER_NOT_EXPORTED)
        loadNotificationLog()
        if (!isNotificationListenerEnabled()) {
            showNotificationListenerWarning()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateReceiver)
    }

    private fun loadNotificationLog() {
        lifecycleScope.launch {
            database.messageDao().getAllMessages().collectLatest { messages ->
                messagesContainer.removeAllViews()
                
                val filteredMessages = messages.filter { it.sender !in filteredSenders }
                
                if (filteredMessages.isEmpty()) {
                    val emptyView = TextView(this@MainActivity).apply {
                        text = "Нет сообщений"
                        textSize = 18f
                        gravity = Gravity.CENTER
                        setPadding(0, 32, 0, 32)
                    }
                    messagesContainer.addView(emptyView)
                    return@collectLatest
                }

                val groupedMessages = filteredMessages.groupBy { it.appName }
                groupedMessages.forEach { (appName, appMessages) ->
                    // App header with icon
                    val appHeaderLayout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 16, 0, 8)
                    }

                    val appIcon = ImageView(this@MainActivity).apply {
                        setImageResource(
                            when (appName) {
                                "Viber" -> android.R.drawable.ic_menu_send
                                "Telegram" -> android.R.drawable.ic_menu_share
                                else -> android.R.drawable.ic_menu_info_details
                            }
                        )
                        setColorFilter(
                            when (appName) {
                                "Viber" -> viberColor
                                "Telegram" -> telegramColor
                                else -> Color.GRAY
                            }
                        )
                        layoutParams = LinearLayout.LayoutParams(32, 32)
                    }

                    val appHeader = TextView(this@MainActivity).apply {
                        text = appName
                        textSize = 20f
                        setTextColor(
                            when (appName) {
                                "Viber" -> viberColor
                                "Telegram" -> telegramColor
                                else -> Color.GRAY
                            }
                        )
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(8, 0, 0, 0)
                    }

                    appHeaderLayout.addView(appIcon)
                    appHeaderLayout.addView(appHeader)
                    messagesContainer.addView(appHeaderLayout)

                    // Messages
                    appMessages.forEach { message ->
                        val card = MaterialCardView(this@MainActivity).apply {
                            radius = 12f
                            cardElevation = 4f
                            setCardBackgroundColor(
                                when (appName) {
                                    "Viber" -> viberLightColor
                                    "Telegram" -> telegramLightColor
                                    else -> Color.WHITE
                                }
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 16)
                            }
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                showMessageContextMenu(it, message)
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
                            setTextColor(
                                when (appName) {
                                    "Viber" -> viberColor
                                    "Telegram" -> telegramColor
                                    else -> Color.BLACK
                                }
                            )
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

    private fun showMessageContextMenu(view: View, message: Message) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Копировать текст")
        popup.menu.add(if (message.sender in filteredSenders) "Убрать из фильтра" else "Добавить в фильтр")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Копировать текст" -> {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Message", message.content)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Текст скопирован", Toast.LENGTH_SHORT).show()
                    true
                }
                "Добавить в фильтр" -> {
                    lifecycleScope.launch {
                        database.filteredSenderDao().insertFilteredSender(FilteredSender(message.sender))
                        Toast.makeText(this@MainActivity, "Отправитель добавлен в фильтр", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                "Убрать из фильтра" -> {
                    lifecycleScope.launch {
                        database.filteredSenderDao().deleteFilteredSender(FilteredSender(message.sender))
                        Toast.makeText(this@MainActivity, "Отправитель убран из фильтра", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
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

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("О приложении")
            .setMessage("MessageCatcher v1.0\n\nПриложение для отслеживания сообщений из различных мессенджеров.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearNotificationLog() {
        lifecycleScope.launch {
            database.messageDao().deleteAll()
            Toast.makeText(this@MainActivity, "История очищена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFilteredSendersFragment() {
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, FilteredSendersFragment())
            .addToBackStack(null)
            .commit()
    }
}
