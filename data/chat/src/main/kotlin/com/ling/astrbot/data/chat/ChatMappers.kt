package com.ling.astrbot.data.chat

import com.ling.astrbot.core.common.util.TimeUtils
import com.ling.astrbot.core.database.entity.ChatMessageEntity
import com.ling.astrbot.core.database.entity.ChatSessionEntity
import com.ling.astrbot.core.model.AgentStats
import com.ling.astrbot.core.model.ChatAttachment
import com.ling.astrbot.core.model.ChatMessage
import com.ling.astrbot.core.model.ChatRef
import com.ling.astrbot.core.model.ChatRole
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.core.model.ToolCall
import com.ling.astrbot.core.network.model.ChatHistoryEntryDto
import com.ling.astrbot.core.network.model.ChatSessionDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class AttachmentJson(
    val id: String,
    val type: String,
    val filename: String? = null,
    val mimeType: String? = null,
)

@Serializable
private data class ToolCallJson(
    val id: String,
    val name: String? = null,
    val arguments: String? = null,
    val result: String? = null,
    val finished: Boolean = false,
)

@Serializable
private data class StatsJson(
    val inputOther: Long? = null,
    val inputCached: Long? = null,
    val output: Long? = null,
    val contextTokens: Long? = null,
    val timeToFirstTokenSec: Float? = null,
    val durationSec: Float? = null,
)

@Serializable
private data class RefJson(
    val index: String,
    val url: String? = null,
    val title: String? = null,
    val snippet: String? = null,
    val favicon: String? = null,
)

private val attachmentListSerializer = ListSerializer(AttachmentJson.serializer())
private val toolCallListSerializer = ListSerializer(ToolCallJson.serializer())
private val refListSerializer = ListSerializer(RefJson.serializer())

// ---------- DTO -> Entity ----------

/** 解析会话列表：兼容「分页对象 `{sessions:[...]}`」与「legacy 数组 `[...]`」两种返回。 */
fun parseSessions(data: JsonElement?): List<ChatSessionDto> {
    val array = when (data) {
        is JsonArray -> data
        is JsonObject -> data["sessions"] as? JsonArray
        else -> null
    } ?: return emptyList()
    return array.mapNotNull { element ->
        runCatching {
            json.decodeFromJsonElement(ChatSessionDto.serializer(), element)
        }.getOrNull()
    }
}

internal fun ChatSessionDto.toEntityOrNull(): ChatSessionEntity? {
    if (sessionId.isBlank()) return null
    return ChatSessionEntity(
        sessionId = sessionId,
        title = displayName?.takeIf { it.isNotBlank() } ?: "未命名会话",
        platformId = platformId,
        createdAt = TimeUtils.parseIsoToEpochMs(createdAt),
        updatedAt = TimeUtils.parseIsoToEpochMs(updatedAt),
    )
}

internal fun ChatHistoryEntryDto.toEntityOrNull(sessionId: String): ChatMessageEntity? {
    val entryId = id ?: return null
    return ChatMessageEntity(
        id = entryId,
        sessionId = sessionId,
        role = parseRole(content).name,
        text = parseText(content),
        reasoning = parseReasoning(content),
        attachmentsJson = serializeAttachments(parseAttachments(content)),
        toolCallsJson = serializeToolCalls(parseToolCalls(content)),
        statsJson = serializeStats(parseStats(content)),
        refsJson = serializeRefs(parseRefs(content)),
        createdAt = TimeUtils.parseIsoToEpochMs(createdAt),
    )
}

// ---------- Entity -> Domain ----------

internal fun ChatSessionEntity.toDomain(): ChatSession = ChatSession(
    id = sessionId,
    title = title,
    platformId = platformId,
    createdAtMs = createdAt,
    updatedAtMs = updatedAt,
)

internal fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    sessionId = sessionId,
    role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.USER),
    text = text,
    reasoning = reasoning,
    attachments = deserializeAttachments(attachmentsJson),
    toolCalls = deserializeToolCalls(toolCallsJson),
    stats = deserializeStats(statsJson),
    refs = deserializeRefs(refsJson),
    createdAtMs = createdAt,
)

// ---------- 序列化（Room 列）----------

private fun serializeAttachments(items: List<ChatAttachment>): String? {
    if (items.isEmpty()) return null
    return json.encodeToString(
        attachmentListSerializer,
        items.map { AttachmentJson(it.id, it.type, it.filename, it.mimeType) },
    )
}

private fun deserializeAttachments(raw: String?): List<ChatAttachment> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        json.decodeFromString(attachmentListSerializer, raw).map {
            ChatAttachment(it.id, it.type, it.filename, it.mimeType)
        }
    }.getOrDefault(emptyList())
}

private fun serializeToolCalls(items: List<ToolCall>): String? {
    if (items.isEmpty()) return null
    return json.encodeToString(
        toolCallListSerializer,
        items.map { ToolCallJson(it.id, it.name, it.arguments, it.result, it.finished) },
    )
}

private fun deserializeToolCalls(raw: String?): List<ToolCall> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        json.decodeFromString(toolCallListSerializer, raw).map {
            ToolCall(it.id, it.name, it.arguments, it.result, it.finished)
        }
    }.getOrDefault(emptyList())
}

private fun serializeStats(stats: AgentStats?): String? {
    if (stats == null || stats.isEmpty) return null
    return json.encodeToString(
        StatsJson.serializer(),
        StatsJson(
            stats.inputOther,
            stats.inputCached,
            stats.output,
            stats.contextTokens,
            stats.timeToFirstTokenSec,
            stats.durationSec,
        ),
    )
}

private fun deserializeStats(raw: String?): AgentStats? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val dto = json.decodeFromString(StatsJson.serializer(), raw)
        AgentStats(
            dto.inputOther,
            dto.inputCached,
            dto.output,
            dto.contextTokens,
            dto.timeToFirstTokenSec,
            dto.durationSec,
        ).takeUnless { it.isEmpty }
    }.getOrNull()
}

private fun serializeRefs(items: List<ChatRef>): String? {
    if (items.isEmpty()) return null
    return json.encodeToString(
        refListSerializer,
        items.map { RefJson(it.index, it.url, it.title, it.snippet, it.favicon) },
    )
}

private fun deserializeRefs(raw: String?): List<ChatRef> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        json.decodeFromString(refListSerializer, raw).map {
            ChatRef(it.index, it.url, it.title, it.snippet, it.favicon)
        }
    }.getOrDefault(emptyList())
}

private fun parseRefs(content: JsonElement?): List<ChatRef> {
    val refs = (content as? JsonObject)?.get("refs") as? JsonObject ?: return emptyList()
    val used = refs["used"] as? JsonArray ?: return emptyList()
    return used.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val index = obj.string("index") ?: return@mapNotNull null
        ChatRef(
            index = index,
            url = obj.string("url")?.takeIf { it.isNotBlank() },
            title = obj.string("title")?.takeIf { it.isNotBlank() },
            snippet = obj.string("snippet")?.takeIf { it.isNotBlank() },
            favicon = obj.string("favicon")?.takeIf { it.isNotBlank() },
        )
    }
}

// ---------- SSE 增量事件解析 ----------

/** 解析 SSE `tool_call` 事件里的工具调用。 */
fun parseToolCallEvent(raw: String): ToolCall? {
    val obj = raw.parseToObjectOrNull() ?: return null
    val id = obj.string("id") ?: return null
    val fn = obj["function"] as? JsonObject
    return ToolCall(
        id = id,
        name = fn?.string("name"),
        arguments = fn?.get("arguments")?.let(::jsonText),
        result = obj["result"]?.let(::jsonText),
        finished = obj["finished_ts"] != null,
    )
}

/** 把 `tool_call_result` 事件合并进已有工具调用列表。 */
fun applyToolCallResult(current: List<ToolCall>, raw: String): List<ToolCall> {
    val obj = raw.parseToObjectOrNull() ?: return current
    val id = obj.string("id") ?: return current
    val result = obj["result"]?.let(::jsonText)
    val index = current.indexOfFirst { it.id == id }
    return if (index >= 0) {
        current.toMutableList().also { it[index] = it[index].copy(result = result, finished = true) }
    } else {
        current + ToolCall(id = id, result = result, finished = true)
    }
}

/** 解析 SSE `agent_stats` 事件里的统计。 */
fun parseAgentStatsEvent(raw: String): AgentStats? =
    parseStatsObject(raw.parseToObjectOrNull())
        ?.takeUnless { it.isEmpty }

/** 解析 `run_snapshot` 的 data（含 content）为累积状态。 */
fun parseSnapshotContent(data: JsonElement?): SnapshotContent {
    val content = (data as? JsonObject)?.get("content")
    val text = parseText(content)
    return SnapshotContent(
        text = text,
        reasoning = parseReasoning(content),
        toolCalls = parseToolCalls(content),
        stats = parseStats(content),
    )
}

// ---------- 内部解析 ----------

private fun String.parseToObjectOrNull(): JsonObject? =
    runCatching { json.parseToJsonElement(this) as? JsonObject }.getOrNull()

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.parts(): JsonArray? = this["message"] as? JsonArray

private fun jsonText(element: JsonElement): String = when (element) {
    is JsonPrimitive -> element.contentOrNull ?: element.toString()
    else -> element.toString()
}

private fun parseRole(content: JsonElement?): ChatRole {
    val type = (content as? JsonObject)?.string("type")
    return if (type == "bot") ChatRole.ASSISTANT else ChatRole.USER
}

private fun parseText(content: JsonElement?): String {
    val parts = (content as? JsonObject)?.parts() ?: return ""
    val sb = StringBuilder()
    for (part in parts) {
        val obj = part as? JsonObject ?: continue
        when (obj.string("type")) {
            "plain", "text" -> sb.append(obj.string("text").orEmpty())
        }
    }
    return sb.toString()
}

private fun parseReasoning(content: JsonElement?): String? {
    val obj = content as? JsonObject ?: return null
    obj.string("reasoning")?.takeIf { it.isNotBlank() }?.let { return it }
    val parts = obj.parts() ?: return null
    val sb = StringBuilder()
    for (part in parts) {
        val p = part as? JsonObject ?: continue
        if (p.string("type") == "think") {
            sb.append(p.string("think").orEmpty())
        }
    }
    return sb.toString().takeIf { it.isNotBlank() }
}

private val attachmentTypes = setOf("image", "record", "video", "file", "audio")

private fun parseAttachments(content: JsonElement?): List<ChatAttachment> {
    val parts = (content as? JsonObject)?.parts() ?: return emptyList()
    val result = mutableListOf<ChatAttachment>()
    for (part in parts) {
        val p = part as? JsonObject ?: continue
        val type = p.string("type") ?: continue
        if (type !in attachmentTypes) continue
        val id = p.string("attachment_id") ?: continue
        if (id.isBlank()) continue
        result += ChatAttachment(
            id = id,
            type = if (type == "audio") "record" else type,
            filename = p.string("filename"),
            mimeType = p.string("mime_type"),
        )
    }
    return result
}

private fun parseToolCalls(content: JsonElement?): List<ToolCall> {
    val parts = (content as? JsonObject)?.parts() ?: return emptyList()
    val result = mutableListOf<ToolCall>()
    for (part in parts) {
        val p = part as? JsonObject ?: continue
        if (p.string("type") != "tool_call") continue
        val calls = p["tool_calls"] as? JsonArray ?: continue
        for (call in calls) {
            val obj = call as? JsonObject ?: continue
            val id = obj.string("id") ?: continue
            val fn = obj["function"] as? JsonObject
            result += ToolCall(
                id = id,
                name = fn?.string("name"),
                arguments = fn?.get("arguments")?.let(::jsonText),
                result = obj["result"]?.let(::jsonText),
                finished = obj["finished_ts"] != null,
            )
        }
    }
    return result
}

private fun parseStats(content: JsonElement?): AgentStats? {
    val stats = (content as? JsonObject)?.get("agent_stats") as? JsonObject ?: return null
    return parseStatsObject(stats)?.takeUnless { it.isEmpty }
}

private fun parseStatsObject(obj: JsonObject?): AgentStats? {
    if (obj == null) return null
    val usage = obj["token_usage"] as? JsonObject
    // 注意：start_time/end_time 是 Unix 时间秒（约 1.2e9），必须用 Double 解析，
    // 用 Float 会因尾数精度不足（此量级约 128s）导致 end-start 被抹成 0。
    val startTime = (obj["start_time"] as? JsonPrimitive)?.doubleOrNull
    val endTime = (obj["end_time"] as? JsonPrimitive)?.doubleOrNull
    return AgentStats(
        inputOther = (usage?.get("input_other") as? JsonPrimitive)?.longOrNull,
        inputCached = (usage?.get("input_cached") as? JsonPrimitive)?.longOrNull,
        output = (usage?.get("output") as? JsonPrimitive)?.longOrNull,
        contextTokens = (obj["current_context_tokens"] as? JsonPrimitive)?.longOrNull,
        timeToFirstTokenSec = (obj["time_to_first_token"] as? JsonPrimitive)?.floatOrNull,
        durationSec = if (startTime != null && endTime != null && endTime >= startTime) {
            (endTime - startTime).toFloat()
        } else {
            null
        },
    )
}
