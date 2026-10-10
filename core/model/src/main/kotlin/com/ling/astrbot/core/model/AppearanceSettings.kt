package com.ling.astrbot.core.model

/** 主题模式。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** 默认聊天正文字号缩放系数（对应「标准」档）。 */
const val DefaultBodyFontScale: Float = 1.2f

/** 外观设置。 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** 聊天正文字号缩放系数（1.2 = 标准）。 */
    val bodyFontScale: Float = DefaultBodyFontScale,
)
