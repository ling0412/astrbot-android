package com.ling.astrbot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ling.astrbot.core.model.AuthStatus
import com.ling.astrbot.feature.chat.ChatRoute
import com.ling.astrbot.feature.conversations.ConversationDetailRoute
import com.ling.astrbot.feature.conversations.ConversationsRoute
import com.ling.astrbot.feature.sessions.ProjectDetailRoute
import com.ling.astrbot.feature.sessions.SessionsDrawerContent
import com.ling.astrbot.feature.settings.SettingsRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AstrbotApp(appViewModel: AppViewModel = hiltViewModel()) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    if (authState.status != AuthStatus.LOGGED_IN) {
        SettingsRoute(isLogin = true)
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val wide = LocalConfiguration.current.screenWidthDp >= TWO_PANE_MIN_WIDTH_DP

    if (wide) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(WIDE_DRAWER_WIDTH_DP.dp),
                    // 抽屉底色比内容深一档，靠色差区分，不再用分割线。
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    WideDrawerPanel(
                        currentDestination = currentDestination,
                        onNavigate = { navController.navigateTo(it) },
                        onOpenSession = {
                            navController.navigate("chat/$it") { popUpTo("chat") }
                        },
                        onNewChat = {
                            navController.navigate("chat/new-${System.currentTimeMillis()}") {
                                popUpTo("chat")
                            }
                        },
                        onOpenProject = { navController.navigate("project/$it") },
                    )
                }
            },
        ) {
            AppNavHost(
                navController = navController,
                wide = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        val density = LocalDensity.current
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        var barHeightPx by remember { mutableIntStateOf(0) }

        // 内容区底部留白 = 导航栏高度随时间被键盘"吃掉"，与键盘动画同源，避免跳变。
        val bottomReserveDp = with(density) {
            (barHeightPx - imeBottomPx).coerceAtLeast(0).toDp()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(
                navController = navController,
                wide = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomReserveDp),
            )

            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, imeBottomPx) }
                    .onSizeChanged { barHeightPx = it.height },
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        destination.matches(it.route)
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateTo(destination) },
                        icon = {
                            Icon(imageVector = destination.icon, contentDescription = null)
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        }
    }
}

/** 宽屏左侧常驻栏：顶层导航 + 会话/项目列表。 */
@Composable
private fun WideDrawerPanel(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    onOpenSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        TopLevelDestination.entries.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.labelRes)) },
                selected = currentDestination?.hierarchy?.any {
                    destination.matches(it.route)
                } == true,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        SessionsDrawerContent(
            onOpenSession = onOpenSession,
            onNewChat = onNewChat,
            onOpenProject = onOpenProject,
            onCloseDrawer = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChatDestination(
    navController: NavHostController,
    wide: Boolean,
) {
    if (wide) {
        // 宽屏：悬浮输入框，消息铺满到手势条处。
        ChatRoute(onOpenDrawer = null, floatingInput = true)
        return
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                SessionsDrawerContent(
                    onOpenSession = {
                        navController.navigate("chat/$it") { popUpTo("chat") }
                    },
                    onNewChat = {
                        navController.navigate("chat/new-${System.currentTimeMillis()}") {
                            popUpTo("chat")
                        }
                    },
                    onOpenProject = { navController.navigate("project/$it") },
                    onCloseDrawer = closeDrawer,
                )
            }
        },
    ) {
        ChatRoute(onOpenDrawer = { scope.launch { drawerState.open() } })
    }
}

private fun NavHostController.navigateTo(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.CHAT.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.CHAT.route) {
            ChatDestination(navController = navController, wide = wide)
        }
        composable("chat/{sessionId}") {
            ChatDestination(navController = navController, wide = wide)
        }
        composable(TopLevelDestination.CONVERSATIONS.route) {
            ConversationsRoute(
                onOpenConversation = { cid, userId ->
                    navController.navigate("conversation/$cid/$userId")
                },
            )
        }
        composable("conversation/{cid}/{userId}") {
            ConversationDetailRoute(onBack = { navController.popBackStack() })
        }
        composable(TopLevelDestination.SETTINGS.route) { SettingsRoute() }
        composable("project/{projectId}") {
            ProjectDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId -> navController.navigate("chat/$sessionId") },
            )
        }
    }
}

private const val TWO_PANE_MIN_WIDTH_DP = 600
private const val WIDE_DRAWER_WIDTH_DP = 320
