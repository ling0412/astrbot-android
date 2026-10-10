package com.ling.astrbot.data.chat

import com.ling.astrbot.core.model.AgentStats
import com.ling.astrbot.core.model.ToolCall

/**
 * `POST /api/v1/chat` 与 `GET /api/v1/chat/runs/{id}/stream` SSE 流中的事件。
 */
sealed interface ChatStreamEvent {
    data class SessionId(val sessionId: String) : ChatStreamEvent
    data class RunStarted(val runId: String) : ChatStreamEvent
    data class Snapshot(val content: SnapshotContent) : ChatStreamEvent
    data class UserMessageSaved(val id: Long) : ChatStreamEvent
    data class Plain(val text: String, val streaming: Boolean) : ChatStreamEvent
    data class Think(val text: String) : ChatStreamEvent
    data class AgentStats(val json: String) : ChatStreamEvent
    data class ToolCall(val json: String) : ChatStreamEvent
    data class ToolCallResult(val json: String) : ChatStreamEvent
    data class AttachmentSaved(val id: String, val type: String) : ChatStreamEvent
    data class MessageSaved(val id: Long) : ChatStreamEvent
    data class Error(val message: String) : ChatStreamEvent
    data object End : ChatStreamEvent
    data class Unknown(val type: String) : ChatStreamEvent
}

/** `run_snapshot` 累积状态。 */
data class SnapshotContent(
    val text: String,
    val reasoning: String?,
    val toolCalls: List<ToolCall>,
    val stats: AgentStats?,
)
