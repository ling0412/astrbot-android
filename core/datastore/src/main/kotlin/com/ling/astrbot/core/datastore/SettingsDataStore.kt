package com.ling.astrbot.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ling.astrbot.core.datastore.security.KeystoreCrypto
import com.ling.astrbot.core.model.AppearanceSettings
import com.ling.astrbot.core.model.AuthSettings
import com.ling.astrbot.core.model.DefaultBodyFontScale
import com.ling.astrbot.core.model.DefaultChatConfigId
import com.ling.astrbot.core.model.AuthToken
import com.ling.astrbot.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "astrbot_settings")

/**
 * 账号登录配置 + JWT + 外观设置的持久化存储。
 * 密码与 JWT 使用 Keystore 加密后保存。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD_ENCRYPTED = stringPreferencesKey("password_encrypted")
        val JWT_ENCRYPTED = stringPreferencesKey("jwt_encrypted")
        val JWT_EXPIRES = longPreferencesKey("jwt_expires")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BODY_FONT_SCALE = floatPreferencesKey("body_font_scale")
        val CHAT_CONFIG_ID = stringPreferencesKey("chat_config_id")
    }

    val authSettings: Flow<AuthSettings> = context.settingsDataStore.data.map { prefs ->
        AuthSettings(
            baseUrl = prefs[Keys.BASE_URL].orEmpty(),
            username = prefs[Keys.USERNAME].orEmpty(),
            password = KeystoreCrypto.decrypt(prefs[Keys.PASSWORD_ENCRYPTED].orEmpty()),
        )
    }

    val authToken: Flow<AuthToken?> = context.settingsDataStore.data.map { prefs ->
        val token = KeystoreCrypto.decrypt(prefs[Keys.JWT_ENCRYPTED].orEmpty())
        val expires = prefs[Keys.JWT_EXPIRES] ?: 0L
        if (token.isBlank()) null else AuthToken(token, expires)
    }

    val appearance: Flow<AppearanceSettings> = context.settingsDataStore.data.map { prefs ->
        AppearanceSettings(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            bodyFontScale = prefs[Keys.BODY_FONT_SCALE] ?: DefaultBodyFontScale,
        )
    }

    /** 上次选择的配置文件 id（默认 `default`）。 */
    val chatConfigId: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.CHAT_CONFIG_ID]?.takeIf { it.isNotBlank() } ?: DefaultChatConfigId
    }

    suspend fun saveAuth(baseUrl: String, username: String, password: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl.trim()
            prefs[Keys.USERNAME] = username.trim()
            prefs[Keys.PASSWORD_ENCRYPTED] = KeystoreCrypto.encrypt(password)
        }
    }

    suspend fun saveToken(token: String, expiresAtMs: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.JWT_ENCRYPTED] = KeystoreCrypto.encrypt(token)
            prefs[Keys.JWT_EXPIRES] = expiresAtMs
        }
    }

    suspend fun clearToken() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(Keys.JWT_ENCRYPTED)
            prefs.remove(Keys.JWT_EXPIRES)
        }
    }

    suspend fun saveAppearance(
        themeMode: ThemeMode,
        dynamicColor: Boolean,
        bodyFontScale: Float,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = dynamicColor
            prefs[Keys.BODY_FONT_SCALE] = bodyFontScale
        }
    }

    /** 记住上次选择的「配置文件」id，用于新建会话时绑定。 */
    suspend fun saveChatConfigId(configId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CHAT_CONFIG_ID] = configId
        }
    }
}
