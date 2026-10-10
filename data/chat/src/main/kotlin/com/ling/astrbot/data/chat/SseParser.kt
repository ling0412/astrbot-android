package com.ling.astrbot.data.chat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

private val sseJson = Json { ignoreUnknownKeys = true }

/** 安全取 primitive：非 primitive（对象/数组）返回 null，避免 jsonPrimitive 抛异常。 */
private fun JsonElement?.primitive(): JsonPrimitive? = this as? JsonPrimitive

/**
 * 解析一条 SSE `data:` 负载为 [ChatStreamEvent]。
 */
internal fun parseChatEvent(raw: String): ChatStreamEvent? {
    val obj = runCatching { sseJson.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return null
    val type = obj["type"].primitive()?.contentOrNull ?: return null
    val data = obj["data"]
    return when (type) {
        "run_started" ->
            (data as? JsonObject)?.get("run_id").primitive()?.contentOrNull
                ?.let(ChatStreamEvent::RunStarted)

        "run_snapshot" ->
            ChatStreamEvent.Snapshot(parseSnapshotContent(data))

        "session_id" ->
            obj["session_id"].primitive()?.contentOrNull?.let(ChatStreamEvent::SessionId)

        "user_message_saved" ->
            data.objectLong("id")?.let(ChatStreamEvent::UserMessageSaved)

        "plain" -> {
            val chainType = obj["chain_type"].primitive()?.contentOrNull
            val text = data.primitive()?.contentOrNull.orEmpty()
            val streaming = obj["streaming"].primitive()?.booleanOrNull ?: false
            when (chainType) {
                // 服务端把推理内容也以 plain 事件下发，仅 chain_type 不同。
                "reasoning" -> ChatStreamEvent.Think(text)
                "tool_call" -> ChatStreamEvent.ToolCall(text)
                "tool_call_result" -> ChatStreamEvent.ToolCallResult(text)
                null, "", "plain" -> ChatStreamEvent.Plain(text, streaming)
                else -> ChatStreamEvent.Unknown(chainType)
            }
        }

        "think" ->
            ChatStreamEvent.Think(data.primitive()?.contentOrNull.orEmpty())

        "agent_stats" ->
            ChatStreamEvent.AgentStats(data?.toString().orEmpty())

        "attachment_saved" -> {
            val o = data as? JsonObject
            ChatStreamEvent.AttachmentSaved(
                o?.get("id").primitive()?.contentOrNull.orEmpty(),
                o?.get("type").primitive()?.contentOrNull.orEmpty(),
            )
        }

        "message_saved" ->
            data.objectLong("id")?.let(ChatStreamEvent::MessageSaved)

        "error" ->
            ChatStreamEvent.Error(data.primitive()?.contentOrNull.orEmpty())

        "end" -> ChatStreamEvent.End

        else -> ChatStreamEvent.Unknown(type)
    }
}

private fun JsonElement?.objectLong(key: String): Long? =
    (this as? JsonObject)?.get(key).primitive()?.longOrNull
