package com.nucore.app.messagecatcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FilteredSendersFragment : Fragment() {
    private lateinit var database: AppDatabase
    private lateinit var container: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }.also { this@FilteredSendersFragment.container = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())
        loadFilteredSenders()
    }

    private fun loadFilteredSenders() {
        viewLifecycleOwner.lifecycleScope.launch {
            database.filteredSenderDao().getAllFilteredSenders().collectLatest { senders ->
                container.removeAllViews()

                if (senders.isEmpty()) {
                    val emptyView = TextView(context).apply {
                        text = "Нет отфильтрованных отправителей"
                        textSize = 18f
                        setPadding(0, 32, 0, 32)
                    }
                    container.addView(emptyView)
                    return@collectLatest
                }

                senders.forEach { sender ->
                    val card = MaterialCardView(context).apply {
                        radius = 12f
                        cardElevation = 4f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 16)
                        }
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            showRemoveConfirmationDialog(sender.senderName)
                        }
                    }

                    val cardContent = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(16, 16, 16, 16)
                    }

                    val senderText = TextView(context).apply {
                        text = sender.senderName
                        textSize = 16f
                        setTextColor(android.graphics.Color.BLACK)
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val removeButton = TextView(context).apply {
                        text = "✕"
                        textSize = 20f
                        setTextColor(android.graphics.Color.RED)
                        setPadding(16, 0, 0, 0)
                    }

                    cardContent.addView(senderText)
                    cardContent.addView(removeButton)
                    card.addView(cardContent)
                    container.addView(card)
                }
            }
        }
    }

    private fun showRemoveConfirmationDialog(senderName: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Убрать из фильтра")
            .setMessage("Убрать $senderName из списка отфильтрованных отправителей?")
            .setPositiveButton("Убрать") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    database.filteredSenderDao().deleteFilteredSender(FilteredSender(senderName))
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
} 