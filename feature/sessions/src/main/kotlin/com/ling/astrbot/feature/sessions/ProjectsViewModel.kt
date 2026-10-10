package com.ling.astrbot.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.ChatProject
import com.ling.astrbot.data.projects.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 新建/编辑项目的草稿；[projectId] 为 null 表示新建。 */
data class ProjectEditor(
    val projectId: String? = null,
    val title: String = "",
    val emoji: String = "📁",
    val description: String = "",
    val workspaceType: String = ChatProject.WORKSPACE_TYPE_PROJECT,
    val workspacePath: String = "",
)

data class ProjectsUiState(
    val projects: List<ChatProject> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val editor: ProjectEditor? = null,
    val isSaving: Boolean = false,
    val pendingDelete: ChatProject? = null,
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = it.projects.isNotEmpty(),
                    isLoading = it.projects.isEmpty(),
                    message = null,
                )
            }
            repository.projects()
                .onSuccess { list -> _uiState.update { it.copy(projects = list) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
            _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
        }
    }

    fun startCreate() {
        _uiState.update { it.copy(editor = ProjectEditor()) }
    }

    fun startEdit(project: ChatProject) {
        _uiState.update {
            it.copy(
                editor = ProjectEditor(
                    projectId = project.id,
                    title = project.title,
                    emoji = project.emoji,
                    description = project.description.orEmpty(),
                    workspaceType = project.workspaceType,
                    workspacePath = project.workspacePath.orEmpty(),
                ),
            )
        }
    }

    fun updateEditor(transform: (ProjectEditor) -> ProjectEditor) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            state.copy(editor = transform(editor))
        }
    }

    fun cancelEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        val title = editor.title.trim()
        if (title.isEmpty()) return
        val custom = editor.workspaceType == ChatProject.WORKSPACE_TYPE_CUSTOM
        val emoji = editor.emoji.ifBlank { "📁" }
        val description = editor.description.trim().takeIf { it.isNotEmpty() }
        val workspacePath = if (custom) {
            editor.workspacePath.trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (editor.projectId == null) {
                repository.createProject(
                    title = title,
                    emoji = emoji,
                    description = description,
                    workspaceType = editor.workspaceType,
                    workspacePath = workspacePath,
                ).map { }
            } else {
                repository.updateProject(
                    projectId = editor.projectId,
                    title = title,
                    emoji = emoji,
                    description = description,
                    workspaceType = editor.workspaceType,
                    workspacePath = workspacePath,
                )
            }
            _uiState.update { it.copy(isSaving = false) }
            result
                .onSuccess {
                    _uiState.update { it.copy(editor = null) }
                    refresh()
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun requestDelete(project: ChatProject) {
        _uiState.update { it.copy(pendingDelete = project) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val project = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.deleteProject(project.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(projects = state.projects.filterNot { it.id == project.id })
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
