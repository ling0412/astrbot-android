package com.ling.astrbot.feature.sessions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.core.model.WorkspaceEntry
import com.ling.astrbot.core.model.WorkspaceFile

@Composable
fun ProjectDetailRoute(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    var pendingDownload by remember { mutableStateOf<String?>(null) }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val path = pendingDownload
        pendingDownload = null
        if (uri != null && path != null) {
            viewModel.download(path, uri)
        }
    }

    ProjectDetailScreen(
        state = state,
        allSessions = allSessions,
        onBack = onBack,
        onOpenSession = onOpenSession,
        onRemoveSession = viewModel::removeSession,
        onOpenAddSessions = viewModel::openAddSessions,
        onCloseAddSessions = viewModel::closeAddSessions,
        onAddSessions = viewModel::addSessions,
        onOpenDirectory = viewModel::openDirectory,
        onOpenFile = viewModel::openFile,
        onCloseFile = viewModel::closeFile,
        onDownload = { path ->
            pendingDownload = path
            createDocument.launch(path.substringAfterLast('/'))
        },
        onMessageShown = viewModel::consumeMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    allSessions: List<ChatSession>,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onRemoveSession: (String) -> Unit,
    onOpenAddSessions: () -> Unit,
    onCloseAddSessions: () -> Unit,
    onAddSessions: (List<String>) -> Unit,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onCloseFile: () -> Unit,
    onDownload: (String) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    var tab by rememberSaveable { mutableStateOf(0) }

    val file = state.file
    if (file != null) {
        WorkspaceFileViewer(file = file, onBack = onCloseFile, onDownload = { onDownload(file.path) })
        if (state.isAddingSessions) {
            AddSessionsDialog(
                candidates = allSessions.filterNot { candidate ->
                    state.sessions.any { it.id == candidate.id }
                },
                onDismiss = onCloseAddSessions,
                onConfirm = onAddSessions,
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${state.emoji} ${state.title.ifBlank { "项目" }}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = onOpenAddSessions) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "添加会话")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("会话") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                        if (state.listing == null && !state.isLoadingFiles) {
                            onOpenDirectory(state.workspacePath)
                        }
                    },
                    text = { Text("工作区") },
                )
            }

            if (tab == 0) {
                SessionsTabContent(
                    sessions = state.sessions,
                    isLoading = state.isLoadingSessions,
                    onOpenSession = onOpenSession,
                    onRemove = onRemoveSession,
                )
            } else {
                WorkspaceTabContent(
                    path = state.workspacePath,
                    listing = state.listing,
                    isLoading = state.isLoadingFiles,
                    onOpenDirectory = onOpenDirectory,
                    onOpenFile = onOpenFile,
                    onDownload = onDownload,
                )
            }
        }
    }

    if (state.isAddingSessions) {
        AddSessionsDialog(
            candidates = allSessions.filterNot { candidate ->
                state.sessions.any { it.id == candidate.id }
            },
            onDismiss = onCloseAddSessions,
            onConfirm = onAddSessions,
        )
    }
}

@Composable
private fun SessionsTabContent(
    sessions: List<ChatSession>,
    isLoading: Boolean,
    onOpenSession: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            sessions.isNotEmpty() -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sessions, key = { it.id }) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSession(session.id) }
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemove(session.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "移出项目")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            else -> Text(
                text = "项目里还没有会话",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun WorkspaceTabContent(
    path: String,
    listing: com.ling.astrbot.core.model.WorkspaceListing?,
    isLoading: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onDownload: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onOpenDirectory(parentPath(path)) },
                enabled = path.isNotEmpty(),
            ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "上级目录")
            }
            Text(
                text = if (path.isEmpty()) "/" else "/$path",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                listing != null && listing.entries.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(listing.entries, key = { it.path }) { entry ->
                            WorkspaceEntryRow(
                                entry = entry,
                                onOpenDirectory = onOpenDirectory,
                                onOpenFile = onOpenFile,
                                onDownload = onDownload,
                            )
                            HorizontalDivider()
                        }
                    }
                }

                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                else -> Text(
                    text = "空目录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceEntryRow(
    entry: WorkspaceEntry,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onDownload: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (entry.isDirectory) {
                    onOpenDirectory(entry.path)
                } else if (entry.readable) {
                    onOpenFile(entry)
                } else {
                    onDownload(entry.path)
                }
            }
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
            contentDescription = null,
            tint = if (entry.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!entry.isDirectory) {
                Text(
                    text = formatSize(entry.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (entry.isDirectory) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            IconButton(onClick = { onDownload(entry.path) }) {
                Icon(Icons.Filled.Download, contentDescription = "下载")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceFileViewer(
    file: WorkspaceFile,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file.path.substringAfterLast('/'),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = "下载")
                    }
                },
            )
        },
    ) { padding ->
        SelectionContainer {
            Text(
                text = file.content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun AddSessionsDialog(
    candidates: List<ChatSession>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加会话到项目") },
        text = {
            if (candidates.isEmpty()) {
                Text("没有可添加的会话")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(candidates, key = { it.id }) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (session.id in selected) {
                                        selected - session.id
                                    } else {
                                        selected + session.id
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = session.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) {
                                        selected + session.id
                                    } else {
                                        selected - session.id
                                    }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = session.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun parentPath(path: String): String {
    if (path.isEmpty()) return ""
    val trimmed = path.trimEnd('/')
    val index = trimmed.lastIndexOf('/')
    return if (index <= 0) "" else trimmed.substring(0, index)
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
