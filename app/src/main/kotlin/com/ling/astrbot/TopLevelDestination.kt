package com.ling.astrbot

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶层导航目的地。
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    CHAT("chat", R.string.destination_chat, Icons.AutoMirrored.Filled.Chat),
    CONVERSATIONS("conversations", R.string.destination_conversations, Icons.Filled.Forum),
    SETTINGS("settings", R.string.destination_settings, Icons.Filled.Settings),
    ;

    /** 当前导航路由是否属于该顶层目的地（聊天含 chat、chat/{id}、project/{id} 等）。 */
    fun matches(route: String?): Boolean = when (this) {
        CHAT -> route != null && (route.startsWith("chat") || route.startsWith("project"))
        CONVERSATIONS -> route != null && route.startsWith("conversation")
        else -> route == this.route
    }
}
