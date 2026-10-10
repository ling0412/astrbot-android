package com.ling.astrbot.data.conversations

import com.ling.astrbot.core.model.ChatRole
import com.ling.astrbot.core.model.ConversationMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

private val systemReminderRegex =
    Regex("<system_reminder>.*?</system_reminder>", RegexOption.DOT_MATCHES_ALL)

/**
 * 解析 AstrBot 会话历史。
 *
 * 该字段是 LLM 上下文格式：可能是 JSON 字符串或数组，
 * 形如 `[{"role":"user","content":[{"type":"text","text":...}]}, ...]`，含 `_checkpoint` 条目。
 */
fun parseConversationHistory(raw: JsonElement?): List<ConversationMessage> {
    val array = when (raw) {
        null -> return emptyList()
        is JsonArray -> raw
        is JsonPrimitive -> runCatching {
            json.parseToJsonElement(raw.content) as? JsonArray
        }.getOrNull() ?: return emptyList()

        else -> return emptyList()
    }

    val result = mutableListOf<ConversationMessage>()
    for (item in array) {
        val obj = item as? JsonObject ?: continue
        val role = obj["role"]?.jsonPrimitive?.contentOrNull ?: continue
        if (role != "user" && role != "assistant") continue

        val content = obj["content"]
        val text = StringBuilder()
        val think = StringBuilder()
        when (content) {
            is JsonArray -> {
                for (part in content) {
                    val p = part as? JsonObject ?: continue
                    when (p["type"]?.jsonPrimitive?.contentOrNull) {
                        "text" -> text.append(p["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
                        "think" -> think.append(p["think"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    }
                }
            }

            is JsonPrimitive -> text.append(content.contentOrNull.orEmpty())
            else -> Unit
        }

        val cleanText = systemReminderRegex.replace(text.toString(), "").trim()
        val cleanThink = think.toString().trim()
        if (cleanText.isEmpty() && cleanThink.isEmpty()) continue

        result += ConversationMessage(
            role = if (role == "assistant") ChatRole.ASSISTANT else ChatRole.USER,
            text = cleanText,
            reasoning = cleanThink.ifBlank { null },
        )
    }
    return result
}
