package com.nucore.app.messagecatcher.dao

import androidx.room.*
import com.nucore.app.messagecatcher.dao.FilteredSender
import kotlinx.coroutines.flow.Flow

@Dao
interface FilteredSenderDao {
    @Query("SELECT * FROM filtered_senders")
    fun getAllFilteredSenders(): Flow<List<FilteredSender>>

    @Insert
    suspend fun insertFilteredSender(filteredSender: FilteredSender)

    @Delete
    suspend fun deleteFilteredSender(filteredSender: FilteredSender)
} 