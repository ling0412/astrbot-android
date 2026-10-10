package com.ling.astrbot.core.model

/** 账号密码登录配置。 */
data class AuthSettings(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

/** 已登录令牌。 */
data class AuthToken(
    val token: String,
    val expiresAtMs: Long,
)

enum class AuthStatus {
    LOGGED_OUT,
    LOGGED_IN,
    NEEDS_TOTP,
}

/** 认证状态（供 UI 观察）。 */
data class AuthState(
    val status: AuthStatus = AuthStatus.LOGGED_OUT,
    val username: String = "",
    val expiresAtMs: Long? = null,
    val message: String? = null,
)
