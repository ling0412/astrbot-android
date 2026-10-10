package com.ling.astrbot.core.model

/**
 * 一个 WebChat 会话。
 */
data class ChatSession(
    val id: String,
    val title: String,
    val platformId: String,
    val createdAtMs: Long?,
    val updatedAtMs: Long?,
)

/**
 * 一个可选的聊天配置文件（AstrBot「配置文件」，决定人格/模型/插件等）。
 */
data class ChatConfigProfile(
    val id: String,
    val name: String,
) {
    /** 服务端内置的兜底配置，界面上显示为「默认」。 */
    val isDefault: Boolean get() = id == DefaultChatConfigId
}

/** 默认配置文件的 id（服务端约定）。 */
const val DefaultChatConfigId: String = "default"

/**
 * 消息附件。
 */
data class ChatAttachment(
    val id: String,
    val type: String,
    val filename: String? = null,
    val mimeType: String? = null,
)

/**
 * 一次工具调用。
 */
data class ToolCall(
    val id: String,
    val name: String? = null,
    val arguments: String? = null,
    val result: String? = null,
    val finished: Boolean = false,
)

/**
 * Agent 运行统计。
 */
data class AgentStats(
    val inputOther: Long? = null,
    val inputCached: Long? = null,
    val output: Long? = null,
    val contextTokens: Long? = null,
    /** 首字时间（秒）。 */
    val timeToFirstTokenSec: Float? = null,
    /** 总耗时（秒）。 */
    val durationSec: Float? = null,
) {
    val isEmpty: Boolean
        get() = inputOther == null && inputCached == null && output == null &&
            contextTokens == null && timeToFirstTokenSec == null && durationSec == null
}

/**
 * 助手消息中的一条引用（来源，如联网搜索）。
 */
data class ChatRef(
    val index: String,
    val url: String? = null,
    val title: String? = null,
    val snippet: String? = null,
    val favicon: String? = null,
)

/**
 * 会话中的一条消息。
 */
data class ChatMessage(
    val id: Long,
    val sessionId: String,
    val role: ChatRole,
    val text: String,
    val reasoning: String? = null,
    val attachments: List<ChatAttachment> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val stats: AgentStats? = null,
    val refs: List<ChatRef> = emptyList(),
    val createdAtMs: Long? = null,
)

enum class ChatRole {
    USER,
    ASSISTANT,
}
