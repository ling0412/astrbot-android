package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatSessionDto(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("platform_id") val platformId: String = "",
    val creator: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class NewSessionDto(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("platform_id") val platformId: String = "",
)

@Serializable
data class ChatSessionDetailDto(
    val session: ChatSessionDto? = null,
    val history: List<ChatHistoryEntryDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 1000,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("is_running") val isRunning: Boolean = false,
)

@Serializable
data class ChatHistoryEntryDto(
    val id: Long? = null,
    @SerialName("sender_name") val senderName: String? = null,
    val content: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class RenameSessionRequest(
    @SerialName("display_name") val displayName: String,
)
