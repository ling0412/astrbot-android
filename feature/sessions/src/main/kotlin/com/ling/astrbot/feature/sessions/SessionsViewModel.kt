package com.ling.astrbot.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.ChatSession
import com.ling.astrbot.data.chat.ChatSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null,
    val pendingDelete: ChatSession? = null,
    val query: String = "",
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val renaming: ChatSession? = null,
    val pendingBatchDelete: Boolean = false,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: ChatSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    /** 按搜索词过滤后的会话列表。 */
    val sessions: StateFlow<List<ChatSession>> =
        combine(repository.sessions, _uiState) { list, state ->
            val query = state.query.trim()
            if (query.isEmpty()) {
                list
            } else {
                list.filter { it.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            repository.refreshSessions().onFailure { error ->
                _uiState.update { it.copy(message = error.message) }
            }
            _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun requestDelete(session: ChatSession) {
        _uiState.update { it.copy(pendingDelete = session) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val session = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.deleteSession(session.id).onFailure { error ->
                _uiState.update { it.copy(message = error.message) }
            }
        }
    }

    // ---------- 多选 ----------

    fun startSelection(session: ChatSession) {
        _uiState.update {
            it.copy(selectionMode = true, selectedIds = setOf(session.id))
        }
    }

    fun toggleSelection(session: ChatSession) {
        _uiState.update {
            val ids = it.selectedIds.toMutableSet()
            if (!ids.add(session.id)) ids.remove(session.id)
            it.copy(selectedIds = ids, selectionMode = ids.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun requestBatchDelete() {
        if (_uiState.value.selectedIds.isEmpty()) return
        _uiState.update { it.copy(pendingBatchDelete = true) }
    }

    fun cancelBatchDelete() {
        _uiState.update { it.copy(pendingBatchDelete = false) }
    }

    fun confirmBatchDelete() {
        val ids = _uiState.value.selectedIds.toList()
        _uiState.update {
            it.copy(pendingBatchDelete = false, selectionMode = false, selectedIds = emptySet())
        }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteSessions(ids).onFailure { error ->
                _uiState.update { it.copy(message = error.message) }
            }
        }
    }

    // ---------- 重命名 ----------

    fun requestRename(session: ChatSession) {
        _uiState.update { it.copy(renaming = session) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(renaming = null) }
    }

    fun confirmRename(title: String) {
        val session = _uiState.value.renaming ?: return
        val newTitle = title.trim()
        _uiState.update { it.copy(renaming = null) }
        if (newTitle.isEmpty() || newTitle == session.title) return
        viewModelScope.launch {
            repository.renameSession(session.id, newTitle).onFailure { error ->
                _uiState.update { it.copy(message = error.message) }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
