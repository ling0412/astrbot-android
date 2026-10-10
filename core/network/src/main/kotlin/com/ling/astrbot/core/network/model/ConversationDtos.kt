package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ConversationsPageDto(
    val conversations: List<ConversationDto> = emptyList(),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
)

@Serializable
data class ConversationDto(
    @SerialName("platform_id") val platformId: String = "",
    @SerialName("user_id") val userId: String = "",
    val cid: String = "",
    val title: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
    @SerialName("umo_info") val umoInfo: UmoInfoDto? = null,
)

@Serializable
data class UmoInfoDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("auto_name") val autoName: String? = null,
    @SerialName("user_alias") val userAlias: String? = null,
)

@Serializable
data class ConversationFilterOptionsDto(
    val bots: List<BotOptionDto> = emptyList(),
)

@Serializable
data class BotOptionDto(
    val id: String = "",
    val type: String = "",
)

@Serializable
data class ConversationDetailDto(
    @SerialName("user_id") val userId: String? = null,
    val cid: String? = null,
    val title: String? = null,
    val history: JsonElement? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)
