package com.ling.astrbot.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import com.ling.astrbot.core.model.DefaultBodyFontScale

internal val AstrbotTypography = Typography()

/**
 * 聊天正文字号缩放系数。仅用于聊天正文（用户消息与助手 Markdown 正文），
 * 由 [AstrbotTheme] 从外观设置提供；默认 [DefaultBodyFontScale] 表示标准字号。
 */
val LocalBodyFontScale = compositionLocalOf { DefaultBodyFontScale }
