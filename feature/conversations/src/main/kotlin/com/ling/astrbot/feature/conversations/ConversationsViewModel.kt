package com.ling.astrbot.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ling.astrbot.core.model.Conversation
import com.ling.astrbot.data.conversations.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val platforms: List<Pair<String, String>> = emptyList(),
    val platform: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadPlatforms()
        load()
    }

    private fun loadPlatforms() {
        viewModelScope.launch {
            repository.platformOptions().onSuccess { list ->
                _uiState.update { it.copy(platforms = list) }
            }
        }
    }

    fun load() {
        val platform = _uiState.value.platform
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = _uiState.value.conversations.isNotEmpty(), message = null) }
            repository.conversations(platform)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(conversations = list, isLoading = false, isRefreshing = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            message = error.message ?: "加载失败",
                        )
                    }
                }
        }
    }

    fun selectPlatform(platform: String?) {
        _uiState.update { it.copy(platform = platform) }
        load()
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
