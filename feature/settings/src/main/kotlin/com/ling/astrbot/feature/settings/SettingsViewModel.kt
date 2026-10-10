package com.ling.astrbot.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.model.AuthStatus
import com.ling.astrbot.core.model.DefaultBodyFontScale
import com.ling.astrbot.core.model.ThemeMode
import com.ling.astrbot.core.network.client.LoginResult
import com.ling.astrbot.core.network.client.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val totpCode: String = "",
    val trustDevice: Boolean = true,
    val needsTotp: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val bodyFontScale: Float = DefaultBodyFontScale,
    val isLoading: Boolean = true,
    val isLoggingIn: Boolean = false,
    val loggedInUsername: String? = null,
    val expiresAtMs: Long? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val auth = settingsDataStore.authSettings.first()
            val appearance = settingsDataStore.appearance.first()
            _uiState.update {
                it.copy(
                    baseUrl = auth.baseUrl,
                    username = auth.username,
                    password = auth.password,
                    themeMode = appearance.themeMode,
                    dynamicColor = appearance.dynamicColor,
                    bodyFontScale = appearance.bodyFontScale,
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            sessionManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        loggedInUsername = if (state.status == AuthStatus.LOGGED_IN) {
                            state.username.ifBlank { it.username }
                        } else {
                            null
                        },
                        expiresAtMs = state.expiresAtMs,
                        needsTotp = it.needsTotp || state.status == AuthStatus.NEEDS_TOTP,
                    )
                }
            }
        }
    }

    fun onBaseUrlChange(value: String) = _uiState.update { it.copy(baseUrl = value, message = null) }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, message = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, message = null) }

    fun onTotpCodeChange(value: String) = _uiState.update { it.copy(totpCode = value) }

    fun onTrustDeviceChange(value: Boolean) = _uiState.update { it.copy(trustDevice = value) }

    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        saveAppearance()
    }

    fun onDynamicColorChange(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColor = enabled) }
        saveAppearance()
    }

    fun onBodyFontScaleChange(scale: Float) {
        _uiState.update { it.copy(bodyFontScale = scale) }
        saveAppearance()
    }

    private fun saveAppearance() {
        val state = _uiState.value
        viewModelScope.launch {
            settingsDataStore.saveAppearance(state.themeMode, state.dynamicColor, state.bodyFontScale)
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.baseUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(message = "请填写服务器地址、用户名和密码", messageIsError = true)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, message = null) }
            val code = if (state.needsTotp) state.totpCode else null
            val result = sessionManager.login(
                baseUrl = state.baseUrl,
                username = state.username,
                password = state.password,
                code = code,
                trustDevice = state.trustDevice,
            )
            // 仅在登录成功后持久化凭据，避免把错误密码写入。
            if (result is LoginResult.Success) {
                settingsDataStore.saveAuth(state.baseUrl, state.username, state.password)
            }
            _uiState.update {
                when (result) {
                    is LoginResult.Success -> it.copy(
                        isLoggingIn = false,
                        needsTotp = false,
                        totpCode = "",
                        loggedInUsername = result.username,
                        message = "登录成功",
                        messageIsError = false,
                    )

                    LoginResult.NeedsTotp -> it.copy(
                        isLoggingIn = false,
                        needsTotp = true,
                        message = "请输入 6 位 TOTP 验证码，然后再点一次登录",
                        messageIsError = false,
                    )

                    is LoginResult.Error -> it.copy(
                        isLoggingIn = false,
                        message = result.message,
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            _uiState.update {
                it.copy(
                    loggedInUsername = null,
                    needsTotp = false,
                    totpCode = "",
                    message = "已退出登录",
                    messageIsError = false,
                )
            }
        }
    }
}
