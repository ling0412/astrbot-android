package com.ling.astrbot.core.model

/**
 * 一个 ChatUI 项目。
 */
data class ChatProject(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String? = null,
    /** 工作区模式：`session`（默认）/ `project` / `custom`。 */
    val workspaceType: String = WORKSPACE_TYPE_SESSION,
    val workspacePath: String? = null,
    val resolvedWorkspacePath: String? = null,
    val createdAtMs: Long? = null,
    val updatedAtMs: Long? = null,
) {
    companion object {
        const val WORKSPACE_TYPE_SESSION = "session"
        const val WORKSPACE_TYPE_PROJECT = "project"
        const val WORKSPACE_TYPE_CUSTOM = "custom"
    }
}

/**
 * 项目工作区目录中的一项。
 */
data class WorkspaceEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val readable: Boolean,
)

/**
 * 项目工作区目录列表。
 */
data class WorkspaceListing(
    val path: String,
    val entries: List<WorkspaceEntry>,
)

/**
 * 项目工作区文本文件。
 */
data class WorkspaceFile(
    val path: String,
    val content: String,
    val size: Long,
)
