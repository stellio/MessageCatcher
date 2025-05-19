package com.nucore.app.messagecatcher.dao

import androidx.room.*
import com.nucore.app.messagecatcher.dao.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<Message>>

    @Insert
    suspend fun insert(message: Message)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
} 