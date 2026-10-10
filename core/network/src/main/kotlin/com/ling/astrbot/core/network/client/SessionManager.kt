package com.ling.astrbot.core.network.client

import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.AuthState
import com.ling.astrbot.core.model.AuthStatus
import com.ling.astrbot.core.network.api.AuthApiService
import com.ling.astrbot.core.network.error.userMessage
import com.ling.astrbot.core.network.model.LoginRequestDto
import com.ling.astrbot.core.network.model.LoginResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginResult {
    data class Success(val username: String, val expiresAtMs: Long) : LoginResult
    data object NeedsTotp : LoginResult
    data class Error(val message: String) : LoginResult
}

/**
 * 账号密码登录 + JWT 生命周期管理（登录 / 自动重登 / 登出）。
 */
@Singleton
class SessionManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val cookieJar: PersistentCookieJar,
    private val json: Json,
) {
    @Volatile
    private var token: String? = null

    @Volatile
    private var expiresAtMs: Long = 0L

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val mutex = Mutex()

    private val authApi: AuthApiService by lazy {
        val client = OkHttpClient.Builder().cookieJar(cookieJar).build()
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val stored = settingsDataStore.authToken.first() ?: return@launch
            if (stored.expiresAtMs > System.currentTimeMillis()) {
                token = stored.token
                expiresAtMs = stored.expiresAtMs
                _state.value = AuthState(AuthStatus.LOGGED_IN, expiresAtMs = stored.expiresAtMs)
            } else if (!refresh()) {
                // 已过期且无法自动重登 → 清理并回到登录页。
                settingsDataStore.clearToken()
                _state.value = AuthState(AuthStatus.LOGGED_OUT)
            }
        }
    }

    fun currentToken(): String? = token

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
        code: String?,
        trustDevice: Boolean,
    ): LoginResult {
        return try {
            val response = authApi.login(
                loginUrl(baseUrl),
                LoginRequestDto(
                    username = username.trim(),
                    password = password,
                    code = code?.trim()?.takeIf { it.isNotBlank() },
                    trustDeviceFlag = trustDevice,
                ),
            )
            if (response.isSuccessful) {
                val data = response.body()?.data
                val newToken = data?.token
                if (newToken.isNullOrBlank()) return LoginResult.Error("登录失败：未返回令牌")
                val expires = System.currentTimeMillis() + JWT_TTL_MS
                token = newToken
                expiresAtMs = expires
                settingsDataStore.saveToken(newToken, expires)
                val name = data?.username?.takeIf { it.isNotBlank() } ?: username
                _state.value = AuthState(AuthStatus.LOGGED_IN, username = name, expiresAtMs = expires)
                LoginResult.Success(name, expires)
            } else {
                val raw = response.errorBody()?.string()
                val dto = raw?.let {
                    runCatching { json.decodeFromString(LoginResponseDto.serializer(), it) }.getOrNull()
                }
                if (dto?.data?.totpRequired == true) {
                    _state.value = _state.value.copy(
                        status = AuthStatus.NEEDS_TOTP,
                        username = username,
                        message = dto.message,
                    )
                    LoginResult.NeedsTotp
                } else {
                    LoginResult.Error(dto?.message ?: "登录失败（HTTP ${response.code()}）")
                }
            }
        } catch (e: Exception) {
            LoginResult.Error(userMessage(e))
        }
    }

    /** 用已保存的密码尝试自动重登（不带 TOTP）。 */
    suspend fun refresh(): Boolean {
        val settings = settingsDataStore.authSettings.first()
        if (!settings.isConfigured) return false
        return when (
            login(settings.baseUrl, settings.username, settings.password, code = null, trustDevice = true)
        ) {
            is LoginResult.Success -> true
            else -> false
        }
    }

    /** 供 OkHttp Authenticator 调用的阻塞式刷新。 */
    fun refreshBlocking(): String? = runBlocking {
        mutex.withLock {
            if (refresh()) token else null
        }
    }

    suspend fun logout() {
        token = null
        expiresAtMs = 0L
        settingsDataStore.clearToken()
        _state.value = AuthState(AuthStatus.LOGGED_OUT)
    }

    private fun loginUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return "$withScheme/api/auth/login"
    }

    private companion object {
        const val JWT_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
