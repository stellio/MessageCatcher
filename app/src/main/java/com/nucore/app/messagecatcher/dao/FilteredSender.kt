package com.nucore.app.messagecatcher.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filtered_senders")
data class FilteredSender(
    @PrimaryKey
    val senderName: String
)