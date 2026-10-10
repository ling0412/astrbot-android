package com.ling.astrbot.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.ling.astrbot.core.model.DefaultBodyFontScale

/**
 * 应用主题。
 *
 * 说明：Material3 1.4.0 起 Expressive 已是默认主题，[MaterialTheme] 自带 motionScheme
 * 参数（默认 expressive），原先的 `MaterialExpressiveTheme` 已标记为 internal。
 */
@Composable
fun AstrbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    bodyFontScale: Float = DefaultBodyFontScale,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalBodyFontScale provides bodyFontScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstrbotTypography,
            content = content,
        )
    }
}
