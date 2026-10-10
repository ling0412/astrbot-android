package com.ling.astrbot.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.AgentStats
import com.ling.astrbot.core.model.ChatAttachment
import com.ling.astrbot.core.model.ChatConfigProfile
import com.ling.astrbot.core.model.ChatMessage
import com.ling.astrbot.core.model.ChatRole
import com.ling.astrbot.core.model.DefaultChatConfigId
import com.ling.astrbot.core.model.ToolCall
import com.ling.astrbot.core.network.client.CredentialsHolder
import com.ling.astrbot.data.chat.ChatConfigRepository
import com.ling.astrbot.data.chat.ChatRepository
import com.ling.astrbot.data.chat.ChatSessionRepository
import com.ling.astrbot.data.chat.ChatStreamEvent
import com.ling.astrbot.data.chat.applyToolCallResult
import com.ling.astrbot.data.chat.parseAgentStatsEvent
import com.ling.astrbot.data.chat.parseToolCallEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StreamingState(
    val text: String = "",
    val reasoning: String = "",
    val toolCalls: List<ToolCall> = emptyList(),
    val stats: AgentStats? = null,
)

/** 引用（回复）的目标消息。 */
data class ReplyTarget(
    val messageId: Long,
    val preview: String,
    val fromUser: Boolean,
)

data class ChatUiState(
    val sessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streaming: StreamingState? = null,
    val input: String = "",
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val replyTo: ReplyTarget? = null,
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val error: String? = null,
    /** 可选的聊天配置文件（配置档案）。 */
    val configProfiles: List<ChatConfigProfile> = emptyList(),
    /** 当前会话使用的配置文件 id。 */
    val selectedConfigId: String = DefaultChatConfigId,
    val isLoadingConfigs: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: ChatSessionRepository,
    private val configRepository: ChatConfigRepository,
    private val credentialsHolder: CredentialsHolder,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val rawSessionArg: String? = savedStateHandle.get<String>("sessionId")

    private var currentSessionId: String? =
        rawSessionArg?.takeIf { it.isNotBlank() && !it.startsWith("new") }

    private val _uiState = MutableStateFlow(
        ChatUiState(sessionId = currentSessionId, isLoading = currentSessionId != null),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var runJob: Job? = null
    private var tempIdCounter = -1L
    private var pendingUserTempId: Long? = null

    init {
        // 指定了会话 id 才加载历史；CHAT 根 / chat/new-* 默认都是空白新会话。
        if (currentSessionId != null) {
            loadHistory()
        }
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConfigs = true) }
            val stored = configRepository.selectedConfigId()
            val profiles = configRepository.listProfiles().getOrDefault(emptyList())
            val sessionId = currentSessionId
            // 已有会话以服务端绑定为准；新会话沿用上次选择。
            val bound = if (sessionId != null) {
                configRepository.resolveSessionConfigId(sessionId).getOrDefault(DefaultChatConfigId)
            } else {
                stored
            }
            val ids = profiles.mapTo(mutableSetOf()) { it.id }
            val effective = bound.takeIf { it == DefaultChatConfigId || it in ids } ?: DefaultChatConfigId
            _uiState.update {
                it.copy(
                    configProfiles = profiles,
                    selectedConfigId = effective,
                    isLoadingConfigs = false,
                )
            }
        }
    }

    /** 切换当前会话（或之后新建会话）使用的配置文件。 */
    fun selectConfig(configId: String) {
        if (configId == _uiState.value.selectedConfigId) return
        _uiState.update { it.copy(selectedConfigId = configId) }
        viewModelScope.launch {
            configRepository.saveSelectedConfigId(configId)
            val sessionId = currentSessionId ?: return@launch
            configRepository.setSessionConfig(sessionId, configId).onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "应用配置文件失败") }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun attachmentUrl(attachmentId: String): String = credentialsHolder.attachmentUrl(attachmentId)

    fun setReply(message: ChatMessage) {
        if (message.id < 0) return
        val preview = message.text.ifBlank { "[附件]" }.take(80)
        _uiState.update {
            it.copy(replyTo = ReplyTarget(message.id, preview, message.role == ChatRole.USER))
        }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyTo = null) }
    }

    fun uploadAttachment(fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            chatRepository.uploadFile(fileName, mimeType, bytes)
                .onSuccess { attachment ->
                    _uiState.update {
                        it.copy(pendingAttachments = it.pendingAttachments + attachment)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "上传失败") }
                }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filterNot { a -> a.id == attachmentId })
        }
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        val attachments = state.pendingAttachments
        val replyTo = state.replyTo
        if ((text.isEmpty() && attachments.isEmpty()) || state.isSending) return

        val tempId = tempIdCounter--
        pendingUserTempId = tempId
        _uiState.update {
            it.copy(
                input = "",
                pendingAttachments = emptyList(),
                replyTo = null,
                isSending = true,
                error = null,
                streaming = StreamingState(),
                messages = it.messages + ChatMessage(
                    id = tempId,
                    sessionId = currentSessionId.orEmpty(),
                    role = ChatRole.USER,
                    text = text,
                    attachments = attachments,
                ),
            )
        }

        val sessionId = currentSessionId
        if (sessionId != null) {
            startRun(chatRepository.sendMessage(sessionId, text, attachments, replyTo?.messageId))
        } else {
            // 服务端要求必须带 session_id，所以新会话先建会话再发送。
            runJob = viewModelScope.launch {
                val created = sessionRepository.createSession()
                val newId = created.getOrNull()
                if (newId == null) {
                    pendingUserTempId = null
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            streaming = null,
                            error = created.exceptionOrNull()?.message ?: "新建会话失败",
                        )
                    }
                    return@launch
                }
                currentSessionId = newId
                _uiState.update { it.copy(sessionId = newId) }
                // 服务端要求先绑定会话的配置文件，再发送，才能让本次回复生效。
                val configId = _uiState.value.selectedConfigId
                if (configId != DefaultChatConfigId) {
                    configRepository.setSessionConfig(newId, configId).onFailure { error ->
                        _uiState.update { it.copy(error = error.message ?: "应用配置文件失败") }
                    }
                }
                collectRun(
                    chatRepository.sendMessage(newId, text, attachments, replyTo?.messageId),
                )
            }
        }
    }

    fun stop() {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch { chatRepository.stopSession(sessionId) }
    }

    fun regenerate(messageId: Long) {
        val sessionId = currentSessionId ?: return
        if (_uiState.value.isSending) return
        _uiState.update {
            it.copy(
                isSending = true,
                error = null,
                streaming = StreamingState(),
                // 移除被再生的旧回复，避免与新回复同时出现（也避免潜在重复 key）。
                messages = it.messages.filterNot { m ->
                    m.id == messageId && m.role == ChatRole.ASSISTANT
                },
            )
        }
        startRun(chatRepository.regenerate(sessionId, messageId))
    }

    /**
     * 收集一次生成的事件流；若流在 `end` 之前异常结束（断线），
     * 用 run_id 通过 `/chat/runs/{id}/stream` 续订并回填 run_snapshot。
     */
    private fun startRun(flow: Flow<ChatStreamEvent>) {
        runJob = viewModelScope.launch { collectRun(flow) }
    }

    private suspend fun collectRun(flow: Flow<ChatStreamEvent>) {
        var runId: String? = null
        var ended = false
        flow.collect { event ->
            if (event is ChatStreamEvent.RunStarted) runId = event.runId
            if (event is ChatStreamEvent.End) ended = true
            handleEvent(event)
        }

        var attempt = 0
        while (!ended && runId != null && attempt < MAX_RESUME_ATTEMPTS) {
            attempt++
            delay(RESUME_DELAY_MS)
            chatRepository.resumeRun(runId!!).collect { event ->
                if (event is ChatStreamEvent.End) ended = true
                handleEvent(event)
            }
        }
        finishRun()
    }

    private fun handleEvent(event: ChatStreamEvent) {
        when (event) {
            is ChatStreamEvent.RunStarted -> Unit

            is ChatStreamEvent.Snapshot -> _uiState.update { state ->
                val content = event.content
                state.copy(
                    streaming = StreamingState(
                        text = content.text,
                        reasoning = content.reasoning.orEmpty(),
                        toolCalls = content.toolCalls,
                        stats = content.stats,
                    ),
                )
            }

            is ChatStreamEvent.SessionId -> {
                currentSessionId = event.sessionId
                // 记住会话 id，进程重建后仍能恢复到该会话而不是再建一个。
                savedStateHandle["sessionId"] = event.sessionId
                _uiState.update { it.copy(sessionId = event.sessionId) }
            }

            is ChatStreamEvent.UserMessageSaved -> {
                val temp = pendingUserTempId ?: return
                pendingUserTempId = null
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.id == temp) it.copy(id = event.id) else it
                        },
                    )
                }
            }

            is ChatStreamEvent.Plain -> _uiState.update { state ->
                val streaming = state.streaming ?: StreamingState()
                state.copy(streaming = streaming.copy(text = streaming.text + event.text))
            }

            is ChatStreamEvent.Think -> _uiState.update { state ->
                val streaming = state.streaming ?: StreamingState()
                state.copy(
                    streaming = streaming.copy(reasoning = streaming.reasoning + event.text),
                )
            }

            is ChatStreamEvent.ToolCall -> {
                val call = parseToolCallEvent(event.json) ?: return
                _uiState.update { state ->
                    val streaming = state.streaming ?: StreamingState()
                    if (streaming.toolCalls.any { it.id == call.id }) {
                        state
                    } else {
                        state.copy(streaming = streaming.copy(toolCalls = streaming.toolCalls + call))
                    }
                }
            }

            is ChatStreamEvent.ToolCallResult -> _uiState.update { state ->
                val streaming = state.streaming ?: StreamingState()
                state.copy(
                    streaming = streaming.copy(
                        toolCalls = applyToolCallResult(streaming.toolCalls, event.json),
                    ),
                )
            }

            is ChatStreamEvent.AgentStats -> {
                val stats = parseAgentStatsEvent(event.json) ?: return
                _uiState.update { state ->
                    val streaming = state.streaming ?: StreamingState()
                    state.copy(streaming = streaming.copy(stats = stats))
                }
            }

            is ChatStreamEvent.MessageSaved -> finalizeAssistant(event.id)

            is ChatStreamEvent.Error -> _uiState.update { it.copy(error = event.message) }

            else -> Unit
        }
    }

    private fun finalizeAssistant(id: Long?) {
        val streaming = _uiState.value.streaming ?: return
        if (streaming.text.isBlank() && streaming.reasoning.isBlank() &&
            streaming.toolCalls.isEmpty() && streaming.stats == null
        ) {
            _uiState.update { it.copy(streaming = null) }
            return
        }
        val messageId = id ?: tempIdCounter--
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    id = messageId,
                    sessionId = currentSessionId.orEmpty(),
                    role = ChatRole.ASSISTANT,
                    text = streaming.text,
                    reasoning = streaming.reasoning,
                    toolCalls = streaming.toolCalls,
                    stats = streaming.stats,
                ),
                streaming = null,
            )
        }
    }

    private suspend fun finishRun() {
        finalizeAssistant(null)
        _uiState.update { it.copy(isSending = false) }

        val sessionId = currentSessionId ?: return
        sessionRepository.refreshSession(sessionId)
        sessionRepository.refreshSessions()

        // refs（引用来源）与 createdAt 只在历史里，SSE 不带，落库后按 id 回填到 UI。
        val storedById = sessionRepository.messages(sessionId).first().associateBy { it.id }
        if (storedById.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { message ->
                        val stored = storedById[message.id] ?: return@map message
                        message.copy(
                            refs = stored.refs.ifEmpty { message.refs },
                            createdAtMs = stored.createdAtMs ?: message.createdAtMs,
                        )
                    },
                )
            }
        }
    }

    fun retryLoad() {
        if (currentSessionId == null) return
        loadHistory()
    }

    private fun loadHistory() {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val result = sessionRepository.refreshSession(sessionId)
            val stored = sessionRepository.messages(sessionId).first()
            _uiState.update {
                it.copy(
                    messages = stored,
                    isLoading = false,
                    // 有缓存时照常展示，仅当无任何内容时才进入错误态。
                    loadError = result.exceptionOrNull()?.message?.takeIf { stored.isEmpty() },
                )
            }
        }
    }
}

private const val MAX_RESUME_ATTEMPTS = 3
private const val RESUME_DELAY_MS = 1500L
