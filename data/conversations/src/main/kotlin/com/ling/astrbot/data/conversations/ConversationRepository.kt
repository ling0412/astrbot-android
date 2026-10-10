package com.ling.astrbot.data.conversations

import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.Conversation
import com.ling.astrbot.core.model.ConversationDetail
import com.ling.astrbot.core.model.AuthSettings
import com.ling.astrbot.core.network.client.AstrbotClientFactory
import com.ling.astrbot.core.network.error.toUserError
import com.ling.astrbot.core.network.model.ConversationDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IM 平台会话历史（只读）仓库。
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AstrbotClientFactory,
) {
    suspend fun conversations(platform: String? = null): Result<List<Conversation>> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getConversations(platforms = platform.orEmpty())
        if (!envelope.isOk) error(envelope.message ?: "加载会话失败")
        envelope.data?.conversations.orEmpty().mapNotNull { it.toDomainOrNull() }
    }.toUserError()

    suspend fun platformOptions(): Result<List<Pair<String, String>>> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getConversationFilterOptions()
        if (!envelope.isOk) error(envelope.message ?: "加载失败")
        envelope.data?.bots.orEmpty().map { it.id to it.type }
    }.toUserError()

    suspend fun detail(cid: String, userId: String): Result<ConversationDetail> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getConversationDetail(cid, userId)
        if (!envelope.isOk) error(envelope.message ?: "加载历史失败")
        val data = envelope.data ?: error("无数据")
        ConversationDetail(
            title = data.title.orEmpty(),
            messages = parseConversationHistory(data.history),
        )
    }.toUserError()

    private suspend fun requireSettings(): AuthSettings {
        val settings = settingsDataStore.authSettings.first()
        if (settings.baseUrl.isBlank()) {
            error("请先在设置中登录")
        }
        return settings
    }
}

private fun ConversationDto.toDomainOrNull(): Conversation? {
    if (cid.isBlank()) return null
    val display = umoInfo?.displayName?.takeIf { it.isNotBlank() }
    val name = title?.takeIf { it.isNotBlank() }
        ?: display
        ?: umoInfo?.autoName?.takeIf { it.isNotBlank() }
        ?: userId
    return Conversation(
        cid = cid,
        userId = userId,
        platformId = platformId,
        title = name,
        displayName = display.orEmpty(),
        createdAtMs = createdAt?.times(1000),
        updatedAtMs = updatedAt?.times(1000),
    )
}
