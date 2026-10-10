package com.ling.astrbot.data.chat

import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.AuthSettings
import com.ling.astrbot.core.model.ChatConfigProfile
import com.ling.astrbot.core.model.DefaultChatConfigId
import com.ling.astrbot.core.network.client.AstrbotClientFactory
import com.ling.astrbot.core.network.error.toUserError
import com.ling.astrbot.core.network.model.ConfigRouteUpsertRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天「配置文件」（AstrBot 配置档案）数据仓库。
 *
 * 服务端按会话（UMO）绑定配置文件：`umo = webchat:FriendMessage:webchat!{用户名}!{会话id}`。
 */
@Singleton
class ChatConfigRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AstrbotClientFactory,
) {
    /** 上次选择用于新建会话的配置文件 id。 */
    suspend fun selectedConfigId(): String = settingsDataStore.chatConfigId.first()

    suspend fun saveSelectedConfigId(configId: String) {
        settingsDataStore.saveChatConfigId(configId)
    }

    /** 列出可选配置文件。 */
    suspend fun listProfiles(): Result<List<ChatConfigProfile>> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getChatConfigs()
        if (!envelope.isOk) error(envelope.message ?: "加载配置文件失败")
        envelope.data?.infoList.orEmpty().map { dto ->
            ChatConfigProfile(id = dto.id, name = dto.name.ifBlank { dto.id })
        }
    }.toUserError()

    /** 读取某会话当前绑定的配置文件 id（未绑定则返回 `default`）。 */
    suspend fun resolveSessionConfigId(sessionId: String): Result<String> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val envelope = client.api.getConfigRoutes()
        if (!envelope.isOk) error(envelope.message ?: "加载配置路由失败")
        val umo = webchatUmo(settings.username, sessionId)
        envelope.data?.routing.orEmpty()
            .entries
            .firstOrNull { matchesUmoPattern(it.key, umo) }
            ?.value
            ?: DefaultChatConfigId
    }.toUserError()

    /** 把某会话绑定到指定配置文件；`default` 表示解除绑定、回退默认。 */
    suspend fun setSessionConfig(sessionId: String, configId: String): Result<Unit> = runCatching {
        val settings = requireSettings()
        val client = clientFactory.create(settings.baseUrl)
        val umo = webchatUmo(settings.username, sessionId)
        val envelope = client.api.upsertConfigRoute(umo, ConfigRouteUpsertRequest(configId))
        if (!envelope.isOk) error(envelope.message ?: "应用配置文件失败")
    }.toUserError()

    private suspend fun requireSettings(): AuthSettings {
        val settings = settingsDataStore.authSettings.first()
        if (settings.baseUrl.isBlank()) {
            error("请先在设置中登录")
        }
        return settings
    }
}

/** 拼出 WebChat 会话的 UMO（与服务端 umop_config_router 约定一致）。 */
internal fun webchatUmo(username: String, sessionId: String): String =
    "webchat:FriendMessage:webchat!$username!$sessionId"

/**
 * 判断路由 pattern 是否匹配目标 UMO，支持 `*` 与空段通配（与服务端 WebUI 一致）。
 */
internal fun matchesUmoPattern(pattern: String, target: String): Boolean {
    val parts = pattern.split(':')
    val targetParts = target.split(':')
    if (parts.size != 3 || targetParts.size != 3) return false
    return parts.indices.all { index ->
        val part = parts[index]
        part.isEmpty() || part == "*" || part == targetParts[index]
    }
}
