package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * `POST /api/v1/chat` 请求体（服务端 `additionalProperties=false`，只发这些字段）。
 *
 * `message` 可以是纯文本字符串，也可以是消息段数组（含附件），因此用 [JsonElement]。
 */
@Serializable
data class ChatRequestDto(
    val username: String,
    @SerialName("session_id") val sessionId: String? = null,
    val message: JsonElement,
    val flags: ChatFlagsDto? = null,
)

@Serializable
data class ChatFlagsDto(
    @SerialName("enable_streaming") val enableStreaming: Boolean = true,
    @SerialName("enable_reasoning") val enableReasoning: Boolean = true,
)

/**
 * 消息段，用于把文本 + 附件拼成消息链。
 */
@Serializable
data class MessagePartDto(
    val type: String,
    val text: String? = null,
    @SerialName("attachment_id") val attachmentId: String? = null,
    @SerialName("message_id") val messageId: Long? = null,
    @SerialName("selected_text") val selectedText: String? = null,
)
