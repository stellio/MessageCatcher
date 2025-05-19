package com.nucore.app.messagecatcher

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilteredSenderDao {
    @Query("SELECT * FROM filtered_senders")
    fun getAllFilteredSenders(): Flow<List<FilteredSender>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilteredSender(sender: FilteredSender)

    @Delete
    suspend fun deleteFilteredSender(sender: FilteredSender)

    @Query("SELECT EXISTS(SELECT 1 FROM filtered_senders WHERE senderName = :senderName)")
    suspend fun isSenderFiltered(senderName: String): Boolean
} 