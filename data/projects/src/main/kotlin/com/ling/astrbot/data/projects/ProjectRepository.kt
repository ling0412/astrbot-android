package com.ling.astrbot.data.projects

import com.ling.astrbot.core.common.util.TimeUtils
import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.ChatProject
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.core.model.WorkspaceEntry
import com.ling.astrbot.core.model.WorkspaceFile
import com.ling.astrbot.core.model.WorkspaceListing
import com.ling.astrbot.core.network.client.AstrbotClient
import com.ling.astrbot.core.network.client.AstrbotClientFactory
import com.ling.astrbot.core.network.error.toUserError
import com.ling.astrbot.core.network.model.ChatProjectDto
import com.ling.astrbot.core.network.model.ChatProjectRequest
import com.ling.astrbot.core.network.model.ChatSessionDto
import com.ling.astrbot.core.network.model.WorkspaceListingDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatUI 项目与工作区数据仓库。
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val clientFactory: AstrbotClientFactory,
) {
    suspend fun projects(): Result<List<ChatProject>> = runCatching {
        val envelope = client().api.getChatProjects()
        if (!envelope.isOk) error(envelope.message ?: "加载项目失败")
        envelope.data.orEmpty().map { it.toDomain() }
    }.toUserError()

    suspend fun project(projectId: String): Result<ChatProject> = runCatching {
        val envelope = client().api.getChatProject(projectId)
        if (!envelope.isOk) error(envelope.message ?: "加载项目失败")
        envelope.data?.toDomain() ?: error("加载项目失败：无返回数据")
    }.toUserError()

    suspend fun createProject(
        title: String,
        emoji: String?,
        description: String?,
        workspaceType: String,
        workspacePath: String?,
    ): Result<ChatProject> = runCatching {
        val envelope = client().api.createChatProject(
            ChatProjectRequest(
                title = title,
                emoji = emoji,
                description = description,
                workspaceType = workspaceType,
                workspacePath = workspacePath,
            ),
        )
        if (!envelope.isOk) error(envelope.message ?: "新建项目失败")
        envelope.data?.toDomain() ?: error("新建项目失败：无返回数据")
    }.toUserError()

    suspend fun updateProject(
        projectId: String,
        title: String,
        emoji: String?,
        description: String?,
        workspaceType: String,
        workspacePath: String?,
    ): Result<Unit> = runCatching {
        val envelope = client().api.updateChatProject(
            projectId,
            ChatProjectRequest(
                title = title,
                emoji = emoji,
                description = description,
                workspaceType = workspaceType,
                workspacePath = workspacePath,
            ),
        )
        if (!envelope.isOk) error(envelope.message ?: "更新项目失败")
    }.toUserError()

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        val envelope = client().api.deleteChatProject(projectId)
        if (!envelope.isOk) error(envelope.message ?: "删除项目失败")
    }.toUserError()

    suspend fun projectSessions(projectId: String): Result<List<ChatSession>> = runCatching {
        val envelope = client().api.getChatProjectSessions(projectId)
        if (!envelope.isOk) error(envelope.message ?: "加载项目会话失败")
        envelope.data.orEmpty().map { it.toChatSession() }
    }.toUserError()

    suspend fun addSession(projectId: String, sessionId: String): Result<Unit> = runCatching {
        val envelope = client().api.addChatProjectSession(projectId, sessionId)
        if (!envelope.isOk) error(envelope.message ?: "加入项目失败")
    }.toUserError()

    suspend fun removeSession(sessionId: String): Result<Unit> = runCatching {
        val envelope = client().api.removeChatProjectSession(sessionId)
        if (!envelope.isOk) error(envelope.message ?: "移出项目失败")
    }.toUserError()

    suspend fun listFiles(projectId: String, path: String): Result<WorkspaceListing> = runCatching {
        val envelope = client().api.listWorkspaceFiles(projectId, path)
        if (!envelope.isOk) error(envelope.message ?: "读取目录失败")
        envelope.data?.toDomain() ?: error("读取目录失败：无返回数据")
    }.toUserError()

    suspend fun readFile(projectId: String, path: String): Result<WorkspaceFile> = runCatching {
        val envelope = client().api.readWorkspaceFile(projectId, path)
        if (!envelope.isOk) error(envelope.message ?: "读取文件失败")
        val data = envelope.data ?: error("读取文件失败：无返回数据")
        WorkspaceFile(path = data.path, content = data.content, size = data.size)
    }.toUserError()

    suspend fun downloadFile(projectId: String, path: String): Result<ByteArray> = runCatching {
        client().api.downloadWorkspaceFile(projectId, path).use { it.bytes() }
    }.toUserError()

    private suspend fun client(): AstrbotClient {
        val settings = settingsDataStore.authSettings.first()
        if (settings.baseUrl.isBlank()) error("请先在设置中登录")
        return clientFactory.create(settings.baseUrl)
    }
}

private fun ChatProjectDto.toDomain() = ChatProject(
    id = projectId,
    title = title.ifBlank { "未命名项目" },
    emoji = emoji?.takeIf { it.isNotBlank() } ?: "📁",
    description = description?.takeIf { it.isNotBlank() },
    workspaceType = workspaceType?.takeIf { it.isNotBlank() } ?: ChatProject.WORKSPACE_TYPE_SESSION,
    workspacePath = workspacePath,
    resolvedWorkspacePath = resolvedWorkspacePath,
    createdAtMs = TimeUtils.parseIsoToEpochMs(createdAt),
    updatedAtMs = TimeUtils.parseIsoToEpochMs(updatedAt),
)

private fun ChatSessionDto.toChatSession() = ChatSession(
    id = sessionId,
    title = displayName?.takeIf { it.isNotBlank() } ?: "未命名会话",
    platformId = platformId,
    createdAtMs = TimeUtils.parseIsoToEpochMs(createdAt),
    updatedAtMs = TimeUtils.parseIsoToEpochMs(updatedAt),
)

private fun WorkspaceListingDto.toDomain() = WorkspaceListing(
    path = path,
    entries = entries.map {
        WorkspaceEntry(
            name = it.name,
            path = it.path,
            isDirectory = it.isDirectory,
            size = it.size,
            readable = it.readable,
        )
    },
)
