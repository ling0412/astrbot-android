package com.ling.astrbot.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.astrbot.core.model.ChatProject
import com.ling.astrbot.core.model.ChatSession

/**
 * 「会话 / 项目」抽屉内容。窄屏用于 ModalNavigationDrawer，宽屏用于左侧常驻栏。
 *
 * @param onOpenSession 打开某个会话（调用前会先 [onCloseDrawer]）
 * @param onNewChat 新建对话
 * @param onOpenProject 打开某个项目
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsDrawerContent(
    onOpenSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenProject: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    sessionsViewModel: SessionsViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(SessionsTab.CHATS) }

    val sessions by sessionsViewModel.sessions.collectAsStateWithLifecycle()
    val sessionsState by sessionsViewModel.uiState.collectAsStateWithLifecycle()
    val projectsState by projectsViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        sessionsViewModel.refresh()
        projectsViewModel.refresh()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 有内容时用 Snackbar 提示；为空且失败时留给空态展示。
    LaunchedEffect(sessionsState.message, sessions.isEmpty(), tab) {
        val message = sessionsState.message ?: return@LaunchedEffect
        val failedWithEmpty = sessions.isEmpty() && !sessionsState.isLoading
        if (tab != SessionsTab.CHATS || !failedWithEmpty) {
            snackbarHostState.showSnackbar(message)
            sessionsViewModel.consumeMessage()
        }
    }
    LaunchedEffect(projectsState.message, projectsState.projects.isEmpty(), tab) {
        val message = projectsState.message ?: return@LaunchedEffect
        val failedWithEmpty = projectsState.projects.isEmpty() && !projectsState.isLoading
        if (tab != SessionsTab.PROJECTS || !failedWithEmpty) {
            snackbarHostState.showSnackbar(message)
            projectsViewModel.consumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 不额外铺底色，让外层抽屉（Permanent/ModalDrawerSheet）的容器色透出。
        Column(modifier = Modifier.fillMaxSize()) {
            if (sessionsState.selectionMode && tab == SessionsTab.CHATS) {
                    SelectionHeader(
                        count = sessionsState.selectedIds.size,
                        onClear = sessionsViewModel::clearSelection,
                        onDelete = sessionsViewModel::requestBatchDelete,
                    )
                } else {
                    HeaderRow(
                        tab = tab,
                        onTabChange = { tab = it },
                        onRefresh = {
                            if (tab == SessionsTab.CHATS) {
                                sessionsViewModel.refresh()
                            } else {
                                projectsViewModel.refresh()
                            }
                        },
                    )
                }

                when (tab) {
                    SessionsTab.CHATS -> SessionsList(
                        sessions = sessions,
                        state = sessionsState,
                        onOpenSession = {
                            onCloseDrawer()
                            onOpenSession(it)
                        },
                        onNewChat = {
                            onCloseDrawer()
                            onNewChat()
                        },
                        onQueryChange = sessionsViewModel::onQueryChange,
                        onToggleSelection = sessionsViewModel::toggleSelection,
                        onStartSelection = sessionsViewModel::startSelection,
                        onRename = sessionsViewModel::requestRename,
                        onDelete = sessionsViewModel::requestDelete,
                    )

                    SessionsTab.PROJECTS -> ProjectsList(
                        projects = projectsState.projects,
                        isLoading = projectsState.isLoading,
                        onOpenProject = {
                            onCloseDrawer()
                            onOpenProject(it)
                        },
                        onCreate = projectsViewModel::startCreate,
                        onEdit = projectsViewModel::startEdit,
                        onDelete = projectsViewModel::requestDelete,
                    )
                }
            }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // ---------- 会话对话框 ----------
    sessionsState.pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = sessionsViewModel::cancelDelete,
            title = { Text("删除会话") },
            text = { Text("确定删除「${session.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = sessionsViewModel::confirmDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = sessionsViewModel::cancelDelete) { Text("取消") }
            },
        )
    }
    if (sessionsState.pendingBatchDelete) {
        AlertDialog(
            onDismissRequest = sessionsViewModel::cancelBatchDelete,
            title = { Text("删除会话") },
            text = { Text("确定删除选中的 ${sessionsState.selectedIds.size} 个会话吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = sessionsViewModel::confirmBatchDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = sessionsViewModel::cancelBatchDelete) { Text("取消") }
            },
        )
    }
    sessionsState.renaming?.let { session ->
        RenameDialog(
            initialTitle = session.title,
            onDismiss = sessionsViewModel::cancelRename,
            onConfirm = sessionsViewModel::confirmRename,
        )
    }

    // ---------- 项目对话框 ----------
    projectsState.pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = projectsViewModel::cancelDelete,
            title = { Text("删除项目") },
            text = { Text("确定删除「${project.title}」吗？项目内的会话本身不会被删除。") },
            confirmButton = {
                TextButton(onClick = projectsViewModel::confirmDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = projectsViewModel::cancelDelete) { Text("取消") }
            },
        )
    }
    projectsState.editor?.let { editor ->
        ProjectEditorDialog(
            editor = editor,
            isSaving = projectsState.isSaving,
            onChange = projectsViewModel::updateEditor,
            onDismiss = projectsViewModel::cancelEditor,
            onSave = projectsViewModel::saveEditor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderRow(
    tab: SessionsTab,
    onTabChange: (SessionsTab) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionsTabSwitch(
            selected = tab,
            onSelect = onTabChange,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
        }
    }
}

@Composable
private fun SelectionHeader(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = "退出选择")
        }
        Text(
            text = "已选择 $count 项",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete, enabled = count > 0) {
            Icon(Icons.Filled.Delete, contentDescription = "删除所选")
        }
    }
}

@Composable
private fun SessionsList(
    sessions: List<ChatSession>,
    state: SessionsUiState,
    onOpenSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSelection: (ChatSession) -> Unit,
    onStartSelection: (ChatSession) -> Unit,
    onRename: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!state.selectionMode) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("搜索会话") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            if (!state.selectionMode) {
                item(key = "new_chat") {
                    NewActionRow(
                        icon = Icons.Filled.Add,
                        label = "新建对话",
                        onClick = onNewChat,
                    )
                    HorizontalDivider()
                }
            }

            when {
                sessions.isNotEmpty() -> items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        selected = session.id in state.selectedIds,
                        selectionMode = state.selectionMode,
                        onClick = {
                            if (state.selectionMode) {
                                onToggleSelection(session)
                            } else {
                                onOpenSession(session.id)
                            }
                        },
                        onLongClick = { onStartSelection(session) },
                        onRename = { onRename(session) },
                        onDelete = { onDelete(session) },
                    )
                    HorizontalDivider()
                }

                state.isLoading -> item(key = "loading") { LoadingRow() }

                else -> item(key = "empty") {
                    EmptyRow(
                        text = if (state.query.isNotBlank()) "没有匹配的会话" else "还没有会话",
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsList(
    projects: List<ChatProject>,
    isLoading: Boolean,
    onOpenProject: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (ChatProject) -> Unit,
    onDelete: (ChatProject) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "new_project") {
            NewActionRow(
                icon = Icons.Filled.Add,
                label = "新建项目",
                onClick = onCreate,
            )
            HorizontalDivider()
        }

        when {
            projects.isNotEmpty() -> items(projects, key = { it.id }) { project ->
                ProjectRow(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onEdit = { onEdit(project) },
                    onDelete = { onDelete(project) },
                )
                HorizontalDivider()
            }

            isLoading -> item(key = "loading") { LoadingRow() }

            else -> item(key = "empty") { EmptyRow(text = "还没有项目") }
        }
    }
}

@Composable
private fun NewActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    )
}
