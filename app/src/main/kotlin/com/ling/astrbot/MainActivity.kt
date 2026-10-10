package com.ling.astrbot

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.designsystem.theme.AstrbotTheme
import com.ling.astrbot.core.model.AppearanceSettings
import com.ling.astrbot.core.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by settingsDataStore.appearance.collectAsStateWithLifecycle(
                initialValue = AppearanceSettings(),
            )
            val darkTheme = when (appearance.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AstrbotTheme(
                darkTheme = darkTheme,
                dynamicColor = appearance.dynamicColor,
                bodyFontScale = appearance.bodyFontScale,
            ) {
                // 系统栏图标明暗跟随 App 主题（而非系统深浅色），避免浅色主题下白图标看不清。
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = !darkTheme
                        controller.isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                AstrbotApp()
            }
        }
    }
}
