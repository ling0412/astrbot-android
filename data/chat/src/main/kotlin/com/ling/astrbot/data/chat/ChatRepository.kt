package com.ling.astrbot.data.chat

import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.AuthSettings
import com.ling.astrbot.core.model.ChatAttachment
import com.ling.astrbot.core.network.client.AstrbotClientFactory
import com.ling.astrbot.core.network.error.toUserError
import com.ling.astrbot.core.network.error.userMessage
import com.ling.astrbot.core.network.model.ChatFlagsDto
import com.ling.astrbot.core.network.model.ChatRequestDto
import com.ling.astrbot.core.network.model.MessagePartDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebChat 数据仓库：SSE 发送、停止、再生、附件上传。
 * 会话列表/历史见 [ChatSessionRepository]。
 */
@Singleton
class ChatRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AstrbotClientFactory,
    private val json: Json,
) {
    /** 上传附件，返回可用于消息链的 [ChatAttachment]。 */
    suspend fun uploadFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<ChatAttachment> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val envelope = client.api.uploadFile(part)
        if (!envelope.isOk) error(envelope.message ?: "上传失败")
        val data = envelope.data ?: error("上传失败：无返回数据")
        if (data.attachmentId.isBlank()) error("上传失败：未返回 attachment_id")
        ChatAttachment(
            id = data.attachmentId,
            type = attachmentTypeFor(mimeType),
            filename = data.filename ?: fileName,
            mimeType = mimeType,
        )
    }.toUserError()

    /** 发送消息并流式返回事件。[sessionId] 为 null 时由服务端新建会话。 */
    fun sendMessage(
        sessionId: String?,
        text: String,
        attachments: List<ChatAttachment> = emptyList(),
        replyToMessageId: Long? = null,
    ): Flow<ChatStreamEvent> = flow {
        try {
            val settings = requireSettings()
            val client = clientFactory.create(settings.baseUrl)
            val body = client.api.sendChat(
                ChatRequestDto(
                    username = settings.username,
                    sessionId = sessionId,
                    message = buildMessage(text, attachments, replyToMessageId),
                    flags = ChatFlagsDto(),
                ),
            )
            consumeSse(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ChatStreamEvent.Error(userMessage(e)))
        }
    }.flowOn(Dispatchers.IO)

    /** 重新生成某条消息并流式返回事件。 */
    fun regenerate(sessionId: String, messageId: Long): Flow<ChatStreamEvent> = flow {
        try {
            val settings = requireSettings()
            val client = clientFactory.create(settings.baseUrl)
            val body = client.api.regenerateChatMessage(sessionId, messageId)
            consumeSse(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ChatStreamEvent.Error(userMessage(e)))
        }
    }.flowOn(Dispatchers.IO)

    /** 停止当前会话的生成。 */
    suspend fun stopSession(sessionId: String): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.stopChatSession(sessionId)
        if (!envelope.isOk) error(envelope.message ?: "停止失败")
    }.toUserError()

    /** 续订一次正在运行的生成（SSE，首帧 run_snapshot）。 */
    fun resumeRun(runId: String): Flow<ChatStreamEvent> = flow {
        try {
            val settings = requireSettings()
            val client = clientFactory.create(settings.baseUrl)
            consumeSse(client.api.resumeChatRun(runId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ChatStreamEvent.Error(userMessage(e)))
        }
    }.flowOn(Dispatchers.IO)

    private fun buildMessage(
        text: String,
        attachments: List<ChatAttachment>,
        replyToMessageId: Long?,
    ): JsonElement {
        if (attachments.isEmpty() && replyToMessageId == null) return JsonPrimitive(text)
        return buildJsonArray {
            if (replyToMessageId != null) {
                add(
                    json.encodeToJsonElement(
                        MessagePartDto.serializer(),
                        MessagePartDto(type = "reply", messageId = replyToMessageId),
                    ),
                )
            }
            if (text.isNotBlank()) {
                add(
                    json.encodeToJsonElement(
                        MessagePartDto.serializer(),
                        MessagePartDto(type = "plain", text = text),
                    ),
                )
            }
            attachments.forEach { attachment ->
                add(
                    json.encodeToJsonElement(
                        MessagePartDto.serializer(),
                        MessagePartDto(type = attachment.type, attachmentId = attachment.id),
                    ),
                )
            }
        }
    }

    private suspend fun FlowCollector<ChatStreamEvent>.consumeSse(body: ResponseBody) {
        body.use { rb ->
            val source = rb.source()
            val buffer = StringBuilder()
            while (true) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.isEmpty() -> {
                        if (buffer.isNotEmpty()) {
                            parseChatEvent(buffer.toString())?.let { emit(it) }
                            buffer.clear()
                        }
                    }

                    line.startsWith("data:") -> {
                        if (buffer.isNotEmpty()) buffer.append('\n')
                        buffer.append(line.substring(5).trimStart())
                    }

                    // 服务端在出错时会返回非 SSE 的 JSON 信封（HTTP 200），这里兜底识别。
                    line.startsWith("{") -> {
                        val obj = runCatching {
                            json.parseToJsonElement(line) as? JsonObject
                        }.getOrNull()
                        val status = obj?.get("status")?.jsonPrimitive?.contentOrNull
                        val message = obj?.get("message")?.jsonPrimitive?.contentOrNull
                        if (status != null && status != "ok") {
                            emit(ChatStreamEvent.Error(message ?: "请求失败"))
                        }
                    }
                }
            }
            if (buffer.isNotEmpty()) {
                parseChatEvent(buffer.toString())?.let { emit(it) }
            }
        }
    }

    private suspend fun requireSettings(): AuthSettings {
        val settings = settingsDataStore.authSettings.first()
        if (settings.baseUrl.isBlank()) {
            error("请先在设置中登录")
        }
        return settings
    }

    private fun attachmentTypeFor(mimeType: String): String = when {
        mimeType.startsWith("image") -> "image"
        mimeType.startsWith("audio") -> "record"
        mimeType.startsWith("video") -> "video"
        else -> "file"
    }
}
