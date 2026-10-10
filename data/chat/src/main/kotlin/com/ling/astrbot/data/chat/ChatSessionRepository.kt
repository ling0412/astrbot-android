package com.ling.astrbot.data.chat

import com.ling.astrbot.core.database.dao.ChatMessageDao
import com.ling.astrbot.core.database.dao.ChatSessionDao
import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.ChatMessage
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.core.model.AuthSettings
import com.ling.astrbot.core.network.client.AstrbotClientFactory
import com.ling.astrbot.core.network.error.toUserError
import com.ling.astrbot.core.network.model.RenameSessionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebChat 会话数据仓库：网络读写 + Room 缓存。
 */
@Singleton
class ChatSessionRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AstrbotClientFactory,
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao,
) {
    val sessions: Flow<List<ChatSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun messages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.observe(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun refreshSessions(): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getChatSessions(settings.username)
        if (!envelope.isOk) error(envelope.message ?: "加载会话失败")
        val items = parseSessions(envelope.data).mapNotNull { it.toEntityOrNull() }
        // 注意：解析失败也会得到空列表，此时不要清空缓存（避免误删）。
        if (items.isNotEmpty()) {
            sessionDao.upsertAll(items)
            val keepIds = items.map { it.sessionId }
            sessionDao.deleteNotIn(keepIds)
            // 清理不在会话表里的历史消息，避免孤儿行。
            messageDao.deleteNotInSessions(keepIds)
        }
    }.toUserError()

    /** 新建会话，返回服务端生成的 session_id。 */
    suspend fun createSession(): Result<String> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.createChatSession()
        if (!envelope.isOk) error(envelope.message ?: "新建会话失败")
        envelope.data?.sessionId?.takeIf { it.isNotBlank() }
            ?: error("新建会话失败：未返回 session_id")
    }.toUserError()

    suspend fun renameSession(sessionId: String, title: String): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.renameChatSession(sessionId, RenameSessionRequest(title))
        if (!envelope.isOk) error(envelope.message ?: "重命名失败")
        sessionDao.updateTitle(sessionId, title)
    }.toUserError()

    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.deleteChatSession(sessionId)
        if (!envelope.isOk) error(envelope.message ?: "删除失败")
        messageDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }.toUserError()

    /** 批量删除：后端无批量接口，逐个删除并汇总首个错误。 */
    suspend fun deleteSessions(sessionIds: List<String>): Result<Unit> = runCatching {
        var firstError: String? = null
        for (id in sessionIds) {
            deleteSession(id).onFailure { error ->
                if (firstError == null) firstError = error.message ?: "删除失败"
            }
        }
        val failure = firstError
        if (failure != null) error(failure)
    }.toUserError()

    suspend fun refreshSession(sessionId: String): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getChatSession(sessionId)
        if (!envelope.isOk) error(envelope.message ?: "加载历史失败")
        val detail = envelope.data
        val messages = detail?.history.orEmpty().mapNotNull { it.toEntityOrNull(sessionId) }
        messageDao.upsertAll(messages)
        val session = detail?.session?.toEntityOrNull()
        if (session != null) {
            sessionDao.upsertAll(listOf(session))
        }
    }.toUserError()

    private suspend fun requireSettings(): AuthSettings {
        val settings = settingsDataStore.authSettings.first()
        if (settings.baseUrl.isBlank()) {
            error("请先在设置中登录")
        }
        return settings
    }
}
