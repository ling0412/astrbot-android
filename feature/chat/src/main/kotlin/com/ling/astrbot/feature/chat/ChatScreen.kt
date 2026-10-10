package com.ling.astrbot.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ling.astrbot.core.common.util.TimeUtils
import com.ling.astrbot.core.designsystem.component.ExpressiveLoadingIndicator
import com.ling.astrbot.core.designsystem.theme.LocalBodyFontScale
import com.ling.astrbot.core.model.AgentStats
import com.ling.astrbot.core.model.ChatAttachment
import com.ling.astrbot.core.model.ChatConfigProfile
import com.ling.astrbot.core.model.ChatMessage
import com.ling.astrbot.core.model.ChatRef
import com.ling.astrbot.core.model.ChatRole
import com.ling.astrbot.core.model.DefaultChatConfigId
import com.ling.astrbot.core.model.ToolCall
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatRoute(
    onOpenDrawer: (() -> Unit)? = null,
    floatingInput: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val payload = withContext(Dispatchers.IO) { readAttachment(context, uri) }
                if (payload != null) {
                    viewModel.uploadAttachment(payload.first, payload.second, payload.third)
                }
            }
        }
    }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val payload = withContext(Dispatchers.IO) { readAttachment(context, uri) }
                if (payload != null) {
                    viewModel.uploadAttachment(payload.first, payload.second, payload.third)
                }
            }
        }
    }

    ChatScreen(
        state = state,
        onOpenDrawer = onOpenDrawer,
        floatingInput = floatingInput,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onRegenerate = viewModel::regenerate,
        onErrorShown = viewModel::consumeError,
        onAttachImage = {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        },
        onAttachFile = { pickFile.launch("*/*") },
        onRemoveAttachment = viewModel::removeAttachment,
        onSetReply = viewModel::setReply,
        onClearReply = viewModel::clearReply,
        onRetryLoad = viewModel::retryLoad,
        onSelectConfig = viewModel::selectConfig,
        attachmentUrl = viewModel::attachmentUrl,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatUiState,
    onOpenDrawer: (() -> Unit)?,
    floatingInput: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRegenerate: (Long) -> Unit,
    onErrorShown: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSetReply: (ChatMessage) -> Unit,
    onClearReply: () -> Unit,
    onRetryLoad: () -> Unit,
    onSelectConfig: (String) -> Unit,
    attachmentUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    val listState = rememberLazyListState()
    val streaming = state.streaming
    val lastUserIndex = state.messages.indexOfLast { it.role == ChatRole.USER }
    val lastMessage = state.messages.lastOrNull()
    var listHeightPx by remember { mutableStateOf(0) }
    // null = 回复高度尚未测量：此时 spacer 取 0，避免先用 0 撑大、测量后再收缩导致回弹。
    var lastReplyHeightPx by remember { mutableStateOf<Int?>(null) }
    val spacerHeightPx = lastReplyHeightPx?.let { (listHeightPx - it).coerceAtLeast(0) } ?: 0
    val hasReply = streaming != null || lastMessage?.role == ChatRole.ASSISTANT

    LaunchedEffect(hasReply) {
        if (!hasReply) lastReplyHeightPx = null
    }

    LaunchedEffect(lastUserIndex) {
        if (lastUserIndex >= 0) {
            lastReplyHeightPx = null
            withFrameNanos { }
            runCatching { listState.scrollToItem(lastUserIndex) }
        }
    }

    // 加载会话时：列表先以不可见状态参与布局、完成锚点滚动，几帧后再揭示，避免"跳转"。
    var revealed by remember { mutableStateOf(true) }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            revealed = false
        } else {
            withFrameNanos { }
            withFrameNanos { }
            withFrameNanos { }
            revealed = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("聊天") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = "会话列表")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val showEmpty = state.messages.isEmpty() && streaming == null
        // 加载 / 揭示期间显示加载指示器，直到列表稳定（锚点滚动完成）再展示。
        val showSpinner = state.isLoading || (state.messages.isNotEmpty() && !revealed)

        if (floatingInput) {
            // 宽屏：消息铺满（可延伸到手势条），输入框做成悬浮卡片浮在底部。
            var composerHeightPx by remember { mutableIntStateOf(0) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (!showEmpty) {
                    ChatMessagesArea(
                        state = state,
                        streaming = streaming,
                        listState = listState,
                        spacerHeightPx = spacerHeightPx,
                        attachmentUrl = attachmentUrl,
                        onRegenerate = onRegenerate,
                        onCopyMessage = { copyToClipboard(context, it.text) },
                        onOpenMessageMenu = { menuMessage = it },
                        onLastReplyMeasured = { lastReplyHeightPx = it },
                        wide = floatingInput,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { listHeightPx = it.height }
                            .alpha(if (revealed) 1f else 0f),
                        bottomContentPadding = with(LocalDensity.current) {
                            composerHeightPx.toDp()
                        } + 12.dp,
                    )
                }
                when {
                    showSpinner -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExpressiveLoadingIndicator()
                    }

                    showEmpty -> ChatEmptyState(
                        state = state,
                        onRetryLoad = onRetryLoad,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                FloatingComposer(
                    value = state.input,
                    isSending = state.isSending,
                    isUploading = state.isUploading,
                    canSend = state.input.isNotBlank() || state.pendingAttachments.isNotEmpty(),
                    reply = state.replyTo,
                    attachments = state.pendingAttachments,
                    attachmentUrl = attachmentUrl,
                    configProfiles = state.configProfiles,
                    selectedConfigId = state.selectedConfigId,
                    isLoadingConfigs = state.isLoadingConfigs,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop,
                    onAttachImage = onAttachImage,
                    onAttachFile = onAttachFile,
                    onRemoveAttachment = onRemoveAttachment,
                    onClearReply = onClearReply,
                    onSelectConfig = onSelectConfig,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { composerHeightPx = it.height },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (!showEmpty) {
                        ChatMessagesArea(
                            state = state,
                            streaming = streaming,
                            listState = listState,
                            spacerHeightPx = spacerHeightPx,
                            attachmentUrl = attachmentUrl,
                            onRegenerate = onRegenerate,
                            onCopyMessage = { copyToClipboard(context, it.text) },
                            onOpenMessageMenu = { menuMessage = it },
                            onLastReplyMeasured = { lastReplyHeightPx = it },
                            wide = floatingInput,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { listHeightPx = it.height }
                                .alpha(if (revealed) 1f else 0f),
                        )
                    }
                    when {
                        showSpinner -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExpressiveLoadingIndicator()
                        }

                        showEmpty -> ChatEmptyState(
                            state = state,
                            onRetryLoad = onRetryLoad,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (state.replyTo != null) {
                    ReplyBar(reply = state.replyTo, onCancel = onClearReply)
                }

                if (state.pendingAttachments.isNotEmpty()) {
                    PendingAttachmentsRow(
                        attachments = state.pendingAttachments,
                        attachmentUrl = attachmentUrl,
                        onRemove = onRemoveAttachment,
                    )
                }

                InputBar(
                    value = state.input,
                    isSending = state.isSending,
                    isUploading = state.isUploading,
                    canSend = state.input.isNotBlank() || state.pendingAttachments.isNotEmpty(),
                    configProfiles = state.configProfiles,
                    selectedConfigId = state.selectedConfigId,
                    isLoadingConfigs = state.isLoadingConfigs,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop,
                    onAttachImage = onAttachImage,
                    onAttachFile = onAttachFile,
                    onSelectConfig = onSelectConfig,
                )
            }
        }

        menuMessage?.let { target ->
            ModalBottomSheet(onDismissRequest = { menuMessage = null }) {
                if (target.text.isNotBlank()) {
                    MessageActionRow(
                        icon = Icons.Filled.ContentCopy,
                        label = "复制",
                        onClick = {
                            copyToClipboard(context, target.text)
                            menuMessage = null
                        },
                    )
                }
                if (target.id > 0) {
                    MessageActionRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        label = "引用",
                        onClick = {
                            onSetReply(target)
                            menuMessage = null
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    state: ChatUiState,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            state.loadError != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "加载失败：${state.loadError}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetryLoad) { Text("重试") }
            }

            else -> Text(
                text = "开始新的对话吧",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatMessagesArea(
    state: ChatUiState,
    streaming: StreamingState?,
    listState: LazyListState,
    spacerHeightPx: Int,
    attachmentUrl: (String) -> String,
    onRegenerate: (Long) -> Unit,
    onCopyMessage: (ChatMessage) -> Unit,
    onOpenMessageMenu: (ChatMessage) -> Unit,
    onLastReplyMeasured: (Int) -> Unit,
    wide: Boolean = false,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            count = state.messages.size,
            key = { index -> state.messages[index].id },
        ) { index ->
            val message = state.messages[index]
            val isLastAssistant =
                index == state.messages.lastIndex && message.role == ChatRole.ASSISTANT
            MessageBubble(
                role = message.role,
                text = message.text,
                reasoning = message.reasoning,
                attachments = message.attachments,
                toolCalls = message.toolCalls,
                stats = message.stats,
                refs = message.refs,
                wide = wide,
                attachmentUrl = attachmentUrl,
                createdAtMs = message.createdAtMs,
                showRegenerate = isLastAssistant && !state.isSending,
                onRegenerate = { onRegenerate(message.id) },
                onCopy = { onCopyMessage(message) },
                onLongPress = { onOpenMessageMenu(message) },
                modifier = if (isLastAssistant && streaming == null) {
                    Modifier.onSizeChanged { onLastReplyMeasured(it.height) }
                } else {
                    Modifier
                },
            )
        }
        if (streaming != null) {
            item(key = "streaming") {
                val hasContent = streaming.text.isNotBlank() ||
                    streaming.reasoning.isNotBlank() ||
                    streaming.toolCalls.isNotEmpty()
                if (hasContent) {
                    MessageBubble(
                        role = ChatRole.ASSISTANT,
                        text = streaming.text,
                        reasoning = streaming.reasoning,
                        toolCalls = streaming.toolCalls,
                        stats = streaming.stats,
                        streaming = true,
                        wide = wide,
                        showRegenerate = false,
                        onRegenerate = {},
                        onLongPress = {},
                        modifier = Modifier.onSizeChanged { onLastReplyMeasured(it.height) },
                    )
                } else {
                    TypingIndicator()
                }
            }
        }
        item(key = "bottom_spacer") {
            Spacer(
                modifier = Modifier.height(
                    with(LocalDensity.current) { spacerHeightPx.toDp() },
                ),
            )
        }
    }
}

private val FloatingComposerMaxWidth = 640.dp

@Composable
private fun FloatingComposer(
    value: String,
    isSending: Boolean,
    isUploading: Boolean,
    canSend: Boolean,
    reply: ReplyTarget?,
    attachments: List<ChatAttachment>,
    attachmentUrl: (String) -> String,
    configProfiles: List<ChatConfigProfile>,
    selectedConfigId: String,
    isLoadingConfigs: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onClearReply: () -> Unit,
    onSelectConfig: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            // 限宽并居中，避免宽屏下悬浮输入框铺得过宽。
            .widthIn(max = FloatingComposerMaxWidth)
            .fillMaxWidth()
            // 悬浮在底部：避让键盘/手势条中较大者；消息列表在其后方铺满。
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Column {
                if (reply != null) {
                    ReplyRow(reply = reply, onCancel = onClearReply)
                }
                if (attachments.isNotEmpty()) {
                    AttachmentsRow(
                        attachments = attachments,
                        attachmentUrl = attachmentUrl,
                        onRemove = onRemoveAttachment,
                    )
                }
                InputRow(
                    value = value,
                    isSending = isSending,
                    isUploading = isUploading,
                    canSend = canSend,
                    configProfiles = configProfiles,
                    selectedConfigId = selectedConfigId,
                    isLoadingConfigs = isLoadingConfigs,
                    onValueChange = onValueChange,
                    onSend = onSend,
                    onStop = onStop,
                    onAttachImage = onAttachImage,
                    onAttachFile = onAttachFile,
                    onSelectConfig = onSelectConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

@Composable
private fun ReplyBar(reply: ReplyTarget, onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        ReplyRow(reply = reply, onCancel = onCancel)
    }
}

@Composable
private fun ReplyRow(reply: ReplyTarget, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (reply.fromUser) "回复你的消息" else "回复助手",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = reply.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = "取消引用")
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    isSending: Boolean,
    isUploading: Boolean,
    canSend: Boolean,
    configProfiles: List<ChatConfigProfile>,
    selectedConfigId: String,
    isLoadingConfigs: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    onSelectConfig: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        InputRow(
            value = value,
            isSending = isSending,
            isUploading = isUploading,
            canSend = canSend,
            configProfiles = configProfiles,
            selectedConfigId = selectedConfigId,
            isLoadingConfigs = isLoadingConfigs,
            onValueChange = onValueChange,
            onSend = onSend,
            onStop = onStop,
            onAttachImage = onAttachImage,
            onAttachFile = onAttachFile,
            onSelectConfig = onSelectConfig,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InputRow(
    value: String,
    isSending: Boolean,
    isUploading: Boolean,
    canSend: Boolean,
    configProfiles: List<ChatConfigProfile>,
    selectedConfigId: String,
    isLoadingConfigs: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    onSelectConfig: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var configDialogOpen by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val currentConfigLabel = remember(configProfiles, selectedConfigId) {
        configLabel(configProfiles, selectedConfigId)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = !isUploading) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("图片 / 视频") },
                    onClick = {
                        menuOpen = false
                        onAttachImage()
                    },
                )
                DropdownMenuItem(
                    text = { Text("文件") },
                    onClick = {
                        menuOpen = false
                        onAttachFile()
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("配置文件") },
                    trailingIcon = {
                        Text(
                            text = currentConfigLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        configDialogOpen = true
                    },
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入消息…") },
            maxLines = 5,
        )
        Spacer(Modifier.width(8.dp))
        if (isSending) {
            FilledIconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = "停止")
            }
        } else {
            FilledIconButton(
                onClick = {
                    // 发送时收起键盘。
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSend()
                },
                enabled = canSend,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }
    }

    if (configDialogOpen) {
        ConfigSelectorDialog(
            configProfiles = configProfiles,
            selectedConfigId = selectedConfigId,
            isLoading = isLoadingConfigs,
            onDismiss = { configDialogOpen = false },
            onConfirm = { id ->
                configDialogOpen = false
                onSelectConfig(id)
            },
        )
    }
}

/** 配置文件在界面上的显示名：`default` 显示为「默认」。 */
private fun configLabel(profiles: List<ChatConfigProfile>, configId: String): String {
    val found = profiles.firstOrNull { it.id == configId }
    if (found != null) {
        return if (found.isDefault) "默认" else found.name
    }
    return if (configId == DefaultChatConfigId) "默认" else configId
}

@Composable
private fun ConfigSelectorDialog(
    configProfiles: List<ChatConfigProfile>,
    selectedConfigId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pending by remember(selectedConfigId) { mutableStateOf(selectedConfigId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择配置文件") },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                configProfiles.isEmpty() -> Text(
                    text = "暂无可选配置，请先在服务端创建配置文件。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    configProfiles.forEach { profile ->
                        val label = if (profile.isDefault) "默认" else profile.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = pending == profile.id,
                                    role = Role.RadioButton,
                                    onClick = { pending = profile.id },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = pending == profile.id,
                                onClick = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                if (!profile.isDefault) {
                                    Text(
                                        text = profile.id,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pending) },
                enabled = !isLoading && configProfiles.isNotEmpty(),
            ) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PendingAttachmentsRow(
    attachments: List<ChatAttachment>,
    attachmentUrl: (String) -> String,
    onRemove: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        AttachmentsRow(
            attachments = attachments,
            attachmentUrl = attachmentUrl,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun AttachmentsRow(
    attachments: List<ChatAttachment>,
    attachmentUrl: (String) -> String,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
                Box {
                    if (attachment.type == "image") {
                        AsyncImage(
                            model = attachmentUrl(attachment.id),
                            contentDescription = attachment.filename,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(MaterialTheme.shapes.small),
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = attachment.filename ?: "文件",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(120.dp),
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { onRemove(attachment.id) },
                        modifier = Modifier.align(Alignment.TopEnd).size(22.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "移除", Modifier.size(14.dp))
                    }
                }
            }
        }
}

/** 按正文字号缩放系数等比缩放字号与行高。 */
private fun TextStyle.scaledBy(factor: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * factor else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight,
)

/** 当前聊天正文字体样式（受设置页「正文字号」影响）。 */
@Composable
private fun chatBodyTextStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.scaledBy(LocalBodyFontScale.current)

/**
 * 聊天正文统一使用的 Markdown 排版：正文贴近 bodyMedium，标题按固定比例跟随正文缩放。
 */
@Composable
private fun chatMarkdownTypography(): MarkdownTypography {
    val body = chatBodyTextStyle()
    return markdownTypography(
        // 标题相对正文的固定比例（与「适中」档一致）。
        h1 = body.scaledBy(1.57f).copy(fontWeight = FontWeight.Bold),
        h2 = body.scaledBy(1.14f).copy(fontWeight = FontWeight.Bold),
        h3 = body.copy(fontWeight = FontWeight.Bold),
        h4 = body.copy(fontWeight = FontWeight.Bold),
        h5 = body.copy(fontWeight = FontWeight.Bold),
        h6 = body.scaledBy(0.86f).copy(fontWeight = FontWeight.Bold),
        text = body,
        paragraph = body,
        list = body,
        ordered = body,
        bullet = body,
        quote = body.copy(fontStyle = FontStyle.Italic),
        code = body.copy(fontFamily = FontFamily.Monospace),
        inlineCode = body.copy(fontFamily = FontFamily.Monospace),
        table = body,
        // 链接做成小药片：背景圆角由 RoundedCornerSpanPainter 绘制。
        textLink = TextLinkStyles(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                background = MaterialTheme.colorScheme.secondaryContainer,
                fontWeight = FontWeight.Medium,
            ),
        ),
        alertTitle = body.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun chatMarkdownColors() = markdownColor(
    text = MaterialTheme.colorScheme.onSurfaceVariant,
    // 表格不要底色。
    tableBackground = Color.Transparent,
)

/**
 * 自定义 Markdown 组件：表格单元格允许换行（库默认 `maxLines=1` + 省略号会把长文字截断看不到）。
 */
private fun chatMarkdownComponents() = markdownComponents(
    checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
    table = { model ->
        MarkdownTable(
            content = model.content,
            node = model.node,
            style = model.typography.table,
            headerBlock = { content, header, tableWidth, style ->
                MarkdownTableHeader(
                    content = content,
                    header = header,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            },
            rowBlock = { content, row, tableWidth, style ->
                MarkdownTableRow(
                    content = content,
                    header = row,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                )
            },
        )
    },
)

/**
 * 助手消息正文。流式与定稿共用同一条渲染路径；[rememberMarkdownState] 开启 `retainState`，
 * 内容增量更新时保留上一帧、避免闪空白。
 */
@Composable
private fun AssistantMarkdown(
    text: String,
    refs: List<ChatRef>,
    streaming: Boolean,
    modifier: Modifier = Modifier,
) {
    // 把正文里的 <ref>N</ref> 标记替换成对应域名的小药片链接。
    val rendered = remember(text, refs) { applyRefs(text, refs) }
    // 定稿消息同步解析（immediate=true）：显示时已排版完成，避免"气泡慢慢撑开"的过程。
    val state = rememberMarkdownState(
        content = rendered,
        retainState = true,
        immediate = !streaming,
    )
    // 流式中用纯文本兜底（内容一直在变）；定稿消息解析前不显示，避免"纯文本→排版"的闪烁。
    val loadingSlot: @Composable (Modifier) -> Unit = if (streaming) {
        {
            Text(
                text = rendered,
                style = chatBodyTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        {}
    }
    Markdown(
        markdownState = state,
        colors = chatMarkdownColors(),
        typography = chatMarkdownTypography(),
        components = chatMarkdownComponents(),
        // 把带背景的链接 span 画成圆角小药片。
        extendedSpans = markdownExtendedSpans {
            remember {
                ExtendedSpans(
                    RoundedCornerSpanPainter(
                        cornerRadius = 8.sp,
                        padding = RoundedCornerSpanPainter.TextPaddingValues(
                            horizontal = 4.sp,
                            vertical = 1.sp,
                        ),
                        topMargin = 2.sp,
                        bottomMargin = 2.sp,
                    ),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        loading = loadingSlot,
    )
}

private val RefPattern = Regex("""<ref>\s*([^<]*?)\s*</ref>""")
private val HostPattern = Regex("""^(?:[a-zA-Z][a-zA-Z0-9+.-]*://)?([^/?#\s]+)""")

/** 取 URL 的域名（去掉 www.）。 */
private fun domainOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val host = runCatching { java.net.URI(url).host }.getOrNull()
        ?: HostPattern.find(url)?.groupValues?.getOrNull(1)
    return host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
}

/** 将正文中的 `<ref>N</ref>` 转成对应域名的小药片链接；无对应 URL 时直接去掉编号。 */
private fun applyRefs(text: String, refs: List<ChatRef>): String {
    if (!text.contains("<ref>")) return text
    val byIndex = refs.associateBy { it.index }
    return RefPattern.replace(text) { match ->
        val url = byIndex[match.groupValues[1]]?.url
        if (url.isNullOrBlank()) "" else "[${domainOf(url) ?: url}]($url)"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    role: ChatRole,
    text: String,
    reasoning: String?,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit,
    onLongPress: () -> Unit,
    onCopy: () -> Unit = {},
    wide: Boolean = false,
    createdAtMs: Long? = null,
    attachments: List<ChatAttachment> = emptyList(),
    toolCalls: List<ToolCall> = emptyList(),
    stats: AgentStats? = null,
    refs: List<ChatRef> = emptyList(),
    streaming: Boolean = false,
    attachmentUrl: (String) -> String = { "" },
    modifier: Modifier = Modifier,
) {
    val isUser = role == ChatRole.USER
    // 只有用户消息保留气泡；助手回复去掉卡片背景。
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        // 长按只触发菜单，不要水波纹/高亮（否则看起来像弹出卡片）。
        val clickModifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = onLongPress,
        )
        if (isUser) {
            Surface(
                color = bubbleColor,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .then(clickModifier),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    BubbleContent(
                        isUser = true,
                        text = text,
                        reasoning = reasoning,
                        attachments = attachments,
                        toolCalls = toolCalls,
                        stats = stats,
                        refs = refs,
                        streaming = streaming,
                        attachmentUrl = attachmentUrl,
                        textColor = textColor,
                    )
                }
            }
        } else {
            // 助手回复去掉卡片背景，直接铺在页面上（保留左右边距）。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    // 宽屏时助手正文左右各再留 48dp；窄屏 8dp（叠加列表本身的 12dp）。
                    .padding(horizontal = if (wide) 48.dp else 8.dp, vertical = 2.dp),
            ) {
                BubbleContent(
                    isUser = false,
                    text = text,
                    reasoning = reasoning,
                    attachments = attachments,
                    toolCalls = toolCalls,
                    stats = stats,
                    refs = refs,
                    streaming = streaming,
                    attachmentUrl = attachmentUrl,
                    textColor = textColor,
                )
                AssistantActions(
                    showRegenerate = showRegenerate,
                    onRegenerate = onRegenerate,
                    canCopy = text.isNotBlank(),
                    onCopy = onCopy,
                    stats = stats,
                    time = TimeUtils.formatClock(createdAtMs),
                )
            }
        }
    }
}

@Composable
private fun AssistantActions(
    showRegenerate: Boolean,
    onRegenerate: () -> Unit,
    canCopy: Boolean,
    onCopy: () -> Unit,
    stats: AgentStats?,
    time: String,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showRegenerate) {
            IconButton(onClick = onRegenerate, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "重新生成",
                    tint = tint.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (canCopy) {
            IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "复制",
                    tint = tint.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (stats != null && !stats.isEmpty) {
            var detailsOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { detailsOpen = true }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "详情",
                        tint = tint.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(
                    expanded = detailsOpen,
                    onDismissRequest = { detailsOpen = false },
                ) {
                    StatsDetails(stats = stats)
                }
            }
        }
        if (time.isNotBlank()) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun StatsDetails(stats: AgentStats) {
    Column(
        modifier = Modifier
            .widthIn(min = 190.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stats.inputCached?.let { StatsRow("输入（缓存）", it.toString()) }
        stats.inputOther?.let { StatsRow("输入（其他）", it.toString()) }
        stats.output?.let { StatsRow("输出 Token", it.toString()) }
        stats.timeToFirstTokenSec?.let { StatsRow("首字时间", formatSeconds(it)) }
        stats.durationSec?.let { StatsRow("耗时", formatSeconds(it)) }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(24.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatSeconds(value: Float): String =
    String.format(java.util.Locale.US, "%.1fs", value)

@Composable
private fun BubbleContent(
    isUser: Boolean,
    text: String,
    reasoning: String?,
    attachments: List<ChatAttachment>,
    toolCalls: List<ToolCall>,
    stats: AgentStats?,
    refs: List<ChatRef>,
    streaming: Boolean,
    attachmentUrl: (String) -> String,
    textColor: Color,
) {
    if (!reasoning.isNullOrBlank()) {
        ReasoningSection(reasoning = reasoning, contentColor = textColor)
    }
    if (toolCalls.isNotEmpty()) {
        ToolCallSection(toolCalls = toolCalls, contentColor = textColor)
    }
    if (attachments.isNotEmpty()) {
        AttachmentContent(
            attachments = attachments,
            attachmentUrl = attachmentUrl,
            modifier = if (text.isNotBlank()) Modifier.padding(bottom = 8.dp) else Modifier,
        )
    }
    if (text.isNotBlank()) {
        if (isUser) {
            Text(
                text = text,
                style = chatBodyTextStyle(),
                color = textColor,
            )
        } else {
            // 流式与定稿都用同一套 Markdown 配置，避免切换时字号/颜色/间距跳变。
            AssistantMarkdown(text = text, refs = refs, streaming = streaming)
        }
    }
    if (refs.isNotEmpty()) {
        RefsSection(refs = refs, contentColor = textColor)
    }
}

@Composable
private fun AttachmentContent(
    attachments: List<ChatAttachment>,
    attachmentUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        attachments.forEach { attachment ->
            if (attachment.type == "image") {
                AsyncImage(
                    model = attachmentUrl(attachment.id),
                    contentDescription = attachment.filename,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(MaterialTheme.shapes.small),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = attachment.filename ?: "文件",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasoningSection(reasoning: String, contentColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "收起思考过程" else "思考过程",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = contentColor.copy(alpha = 0.8f),
            )
        }
        if (expanded) {
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ToolCallSection(toolCalls: List<ToolCall>, contentColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Build,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "工具调用（${toolCalls.size}）",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = contentColor.copy(alpha = 0.8f),
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                toolCalls.forEach { call ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = call.name ?: call.id,
                                style = MaterialTheme.typography.labelMedium,
                                color = contentColor,
                            )
                            call.arguments?.let {
                                Text(
                                    text = "参数：$it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.75f),
                                )
                            }
                            Text(
                                text = if (call.finished) {
                                    "结果：${call.result.orEmpty().take(500)}"
                                } else {
                                    "运行中…"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefsSection(refs: List<ChatRef>, contentColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Link,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "引用来源（${refs.size}）",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = contentColor.copy(alpha = 0.8f),
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                refs.forEach { ref ->
                    val url = ref.url
                    val domain = domainOf(url)
                    val clickModifier = if (url.isNullOrBlank()) {
                        Modifier
                    } else {
                        Modifier.clickable { runCatching { uriHandler.openUri(url) } }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(clickModifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = domain ?: ref.index,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ref.title.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private val TypingHints = listOf(
    "(ﾉ◕ヮ◕)ﾉ 酝酿答案中...",
    "(・_・ヾ 琢磨中...",
    "( ˘ω˘ ) 脑内推演中...",
    "(つ•̀ω•́)つ 组织语言中...",
)

@Composable
private fun TypingIndicator() {
    // 每次进入组合（即每次生成的无内容等待阶段）随机固定一条。
    val hint = remember { TypingHints.random() }
    val transition = rememberInfiniteTransition(label = "typing-hint")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "typing-hint-alpha",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .alpha(alpha)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

private fun readAttachment(context: Context, uri: Uri): Triple<String, String, ByteArray>? {
    return try {
        val resolver = context.contentResolver
        var name = "file"
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index) ?: name
            }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        Triple(name, mime, bytes)
    } catch (_: Exception) {
        null
    }
}
