package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** ChatUI 项目。 */
@Serializable
data class ChatProjectDto(
    @SerialName("project_id") val projectId: String = "",
    val title: String = "",
    val emoji: String? = null,
    val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("workspace_path") val workspacePath: String? = null,
    @SerialName("resolved_workspace_path") val resolvedWorkspacePath: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** 新建 / 更新项目请求体。 */
@Serializable
data class ChatProjectRequest(
    val title: String? = null,
    val emoji: String? = null,
    val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("workspace_path") val workspacePath: String? = null,
)

/** 工作区目录列表。 */
@Serializable
data class WorkspaceListingDto(
    val path: String = "",
    val entries: List<WorkspaceEntryDto> = emptyList(),
)

/** 工作区目录中的一项。 */
@Serializable
data class WorkspaceEntryDto(
    val name: String = "",
    val path: String = "",
    val type: String = "file",
    val size: Long = 0,
    val readable: Boolean = false,
) {
    val isDirectory: Boolean get() = type == "directory"
}

/** 工作区文本文件内容。 */
@Serializable
data class WorkspaceFileDto(
    val path: String = "",
    val content: String = "",
    val size: Long = 0,
)
