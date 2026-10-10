package com.ling.astrbot.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.astrbot.core.common.util.TimeUtils
import com.ling.astrbot.core.model.ThemeMode

private val LoginMaxWidth = 520.dp
private val SettingsMaxWidth = 480.dp

private data class BodyFontScaleOption(val label: String, val scale: Float)

private val BodyFontScaleOptions = listOf(
    BodyFontScaleOption("小", 1.0f),
    BodyFontScaleOption("标准", 1.2f),
    BodyFontScaleOption("大", 1.5f),
    BodyFontScaleOption("特大", 1.8f),
)

@Composable
fun SettingsRoute(
    isLogin: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        isLogin = isLogin,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTotpCodeChange = viewModel::onTotpCodeChange,
        onTrustDeviceChange = viewModel::onTrustDeviceChange,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onBodyFontScaleChange = viewModel::onBodyFontScaleChange,
        onLogin = viewModel::login,
        onLogout = viewModel::logout,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    isLogin: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTotpCodeChange: (String) -> Unit,
    onTrustDeviceChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBodyFontScaleChange: (Float) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isLogin) LoginMaxWidth else SettingsMaxWidth)
                    .verticalScroll(rememberScrollState())
                    // 底部多留一点空白，整体比例更舒服。
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isLogin) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("AstrBot", style = MaterialTheme.typography.headlineMedium)
                            SettingsFormContent(
                                state = state,
                                onBaseUrlChange = onBaseUrlChange,
                                onUsernameChange = onUsernameChange,
                                onPasswordChange = onPasswordChange,
                                onTotpCodeChange = onTotpCodeChange,
                                onTrustDeviceChange = onTrustDeviceChange,
                                onThemeModeChange = onThemeModeChange,
                                onDynamicColorChange = onDynamicColorChange,
                                onBodyFontScaleChange = onBodyFontScaleChange,
                                onLogin = onLogin,
                                onLogout = onLogout,
                            )
                        }
                    }
                } else {
                    SettingsFormContent(
                        state = state,
                        onBaseUrlChange = onBaseUrlChange,
                        onUsernameChange = onUsernameChange,
                        onPasswordChange = onPasswordChange,
                        onTotpCodeChange = onTotpCodeChange,
                        onTrustDeviceChange = onTrustDeviceChange,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onBodyFontScaleChange = onBodyFontScaleChange,
                        onLogin = onLogin,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

/** 账号 + 外观表单内容（由调用方放进 Column，因此这里直接依次发射子组件）。 */
@Composable
private fun SettingsFormContent(
    state: SettingsUiState,
    onBaseUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTotpCodeChange: (String) -> Unit,
    onTrustDeviceChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBodyFontScaleChange: (Float) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Text("账号", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(
        value = state.baseUrl,
        onValueChange = onBaseUrlChange,
        label = { Text("服务器地址") },
        placeholder = { Text("https://bot.example.com:6185") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        label = { Text("用户名") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text("密码") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.needsTotp) {
        OutlinedTextField(
            value = state.totpCode,
            onValueChange = onTotpCodeChange,
            label = { Text("TOTP 验证码") },
            placeholder = { Text("6 位数字") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("信任此设备", style = MaterialTheme.typography.bodyLarge)
            Text(
                "约 30 天内免二次验证",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.trustDevice,
            onCheckedChange = onTrustDeviceChange,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onLogin,
            enabled = !state.isLoggingIn,
            modifier = Modifier.weight(1f),
        ) {
            if (state.isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(if (state.loggedInUsername != null) "重新登录" else "登录")
            }
        }
        if (state.loggedInUsername != null) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f),
            ) {
                Text("退出登录")
            }
        }
    }

    state.loggedInUsername?.let { name ->
        Text(
            text = "已登录：$name" + state.expiresAtMs
                ?.let { "（令牌至 ${TimeUtils.formatEpochMs(it)}）" }
                .orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    state.message?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.messageIsError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }

    HorizontalDivider()
    Text("外观", style = MaterialTheme.typography.titleMedium)
    Text("主题", style = MaterialTheme.typography.labelMedium)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.themeMode == mode,
                onClick = { onThemeModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                label = { Text(mode.label()) },
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("动态取色", style = MaterialTheme.typography.bodyLarge)
            Text(
                "跟随壁纸生成配色（Android 12+）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.dynamicColor,
            onCheckedChange = onDynamicColorChange,
        )
    }

    Text("正文字号", style = MaterialTheme.typography.labelMedium)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        BodyFontScaleOptions.forEachIndexed { index, option ->
            SegmentedButton(
                selected = state.bodyFontScale == option.scale,
                onClick = { onBodyFontScaleChange(option.scale) },
                shape = SegmentedButtonDefaults.itemShape(index, BodyFontScaleOptions.size),
                label = { Text(option.label) },
            )
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
