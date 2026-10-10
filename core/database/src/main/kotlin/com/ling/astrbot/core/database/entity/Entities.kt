package com.ling.astrbot.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String,
    val platformId: String,
    val createdAt: Long?,
    val updatedAt: Long?,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val sessionId: String,
    val role: String,
    val text: String,
    val reasoning: String?,
    val attachmentsJson: String?,
    val toolCallsJson: String?,
    val statsJson: String?,
    val refsJson: String?,
    val createdAt: Long?,
)
