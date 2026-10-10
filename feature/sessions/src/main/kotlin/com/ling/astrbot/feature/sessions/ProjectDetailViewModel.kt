package com.ling.astrbot.feature.sessions

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.ChatProject
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.core.model.WorkspaceEntry
import com.ling.astrbot.core.model.WorkspaceFile
import com.ling.astrbot.core.model.WorkspaceListing
import com.ling.astrbot.data.chat.ChatSessionRepository
import com.ling.astrbot.data.projects.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProjectDetailUiState(
    val projectId: String = "",
    val title: String = "",
    val emoji: String = "📁",
    val description: String? = null,
    val workspaceType: String = ChatProject.WORKSPACE_TYPE_SESSION,
    val sessions: List<ChatSession> = emptyList(),
    val isLoadingSessions: Boolean = true,
    val isAddingSessions: Boolean = false,
    val workspacePath: String = "",
    val listing: WorkspaceListing? = null,
    val isLoadingFiles: Boolean = false,
    val file: WorkspaceFile? = null,
    val isLoadingFile: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val chatSessionRepository: ChatSessionRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: String = savedStateHandle.get<String>("projectId").orEmpty()

    /** 全部会话（用于「添加会话」选择）。 */
    val allSessions: StateFlow<List<ChatSession>> = chatSessionRepository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        loadProject()
        refreshSessions()
    }

    private fun loadProject() {
        if (projectId.isBlank()) {
            _uiState.update { it.copy(message = "项目参数缺失") }
            return
        }
        viewModelScope.launch {
            projectRepository.project(projectId)
                .onSuccess { project ->
                    _uiState.update {
                        it.copy(
                            title = project.title,
                            emoji = project.emoji,
                            description = project.description,
                            workspaceType = project.workspaceType,
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun refreshSessions() {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true) }
            projectRepository.projectSessions(projectId)
                .onSuccess { list -> _uiState.update { it.copy(sessions = list) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
            _uiState.update { it.copy(isLoadingSessions = false) }
        }
    }

    fun openAddSessions() {
        viewModelScope.launch { chatSessionRepository.refreshSessions() }
        _uiState.update { it.copy(isAddingSessions = true) }
    }

    fun closeAddSessions() {
        _uiState.update { it.copy(isAddingSessions = false) }
    }

    fun addSessions(sessionIds: List<String>) {
        if (sessionIds.isEmpty()) {
            closeAddSessions()
            return
        }
        viewModelScope.launch {
            var failure: String? = null
            sessionIds.forEach { id ->
                projectRepository.addSession(projectId, id).onFailure { error ->
                    if (failure == null) failure = error.message
                }
            }
            val failureMessage = failure
            _uiState.update { it.copy(isAddingSessions = false, message = failureMessage) }
            refreshSessions()
        }
    }

    fun removeSession(sessionId: String) {
        viewModelScope.launch {
            projectRepository.removeSession(sessionId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(sessions = state.sessions.filterNot { it.id == sessionId })
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun openDirectory(path: String) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            // 不乐观更新 workspacePath：失败时保持与当前 listing 一致。
            _uiState.update { it.copy(isLoadingFiles = true, file = null) }
            projectRepository.listFiles(projectId, path)
                .onSuccess { listing ->
                    _uiState.update { it.copy(listing = listing, workspacePath = listing.path) }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
            _uiState.update { it.copy(isLoadingFiles = false) }
        }
    }

    fun openFile(entry: WorkspaceEntry) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFile = true) }
            projectRepository.readFile(projectId, entry.path)
                .onSuccess { file -> _uiState.update { it.copy(file = file) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
            _uiState.update { it.copy(isLoadingFile = false) }
        }
    }

    fun closeFile() {
        _uiState.update { it.copy(file = null) }
    }

    fun download(path: String, uri: Uri) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            projectRepository.downloadFile(projectId, path)
                .onSuccess { bytes ->
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                                ?: error("无法写入目标位置")
                        }.isSuccess
                    }
                    _uiState.update { it.copy(message = if (ok) "已保存" else "保存失败") }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
