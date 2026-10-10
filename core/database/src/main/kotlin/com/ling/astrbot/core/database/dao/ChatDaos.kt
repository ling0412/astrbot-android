package com.ling.astrbot.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ling.astrbot.core.database.entity.ChatMessageEntity
import com.ling.astrbot.core.database.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ChatSessionEntity>)

    @Query("UPDATE chat_sessions SET title = :title WHERE sessionId = :sessionId")
    suspend fun updateTitle(sessionId: String, title: String)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)

    @Query("DELETE FROM chat_sessions WHERE sessionId NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM chat_sessions")
    suspend fun clear()
}

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun observe(sessionId: String): Flow<List<ChatMessageEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId NOT IN (:keepIds)")
    suspend fun deleteNotInSessions(keepIds: List<String>)
}
