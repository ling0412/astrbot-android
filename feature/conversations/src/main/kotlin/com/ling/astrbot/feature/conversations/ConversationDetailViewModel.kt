package com.ling.astrbot.feature.conversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.ConversationMessage
import com.ling.astrbot.data.conversations.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationDetailUiState(
    val title: String = "",
    val messages: List<ConversationMessage> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    private val repository: ConversationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val cid: String = savedStateHandle.get<String>("cid").orEmpty()
    private val userId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _uiState = MutableStateFlow(ConversationDetailUiState())
    val uiState: StateFlow<ConversationDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (cid.isBlank() || userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, message = "会话参数缺失") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            repository.detail(cid, userId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(title = detail.title, messages = detail.messages, isLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "加载失败")
                    }
                }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
