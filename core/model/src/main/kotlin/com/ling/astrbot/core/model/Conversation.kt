package com.ling.astrbot.core.model

/**
 * IM 平台里的一个会话（只读浏览用）。
 */
data class Conversation(
    val cid: String,
    val userId: String,
    val platformId: String,
    val title: String,
    val displayName: String,
    val createdAtMs: Long?,
    val updatedAtMs: Long?,
)

/**
 * 会话历史中的一条消息（只读）。
 */
data class ConversationMessage(
    val role: ChatRole,
    val text: String,
    val reasoning: String? = null,
)

data class ConversationDetail(
    val title: String,
    val messages: List<ConversationMessage>,
)
