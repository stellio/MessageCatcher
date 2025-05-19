package com.nucore.app.messagecatcher

import androidx.room.*
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