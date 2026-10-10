package com.ling.astrbot

import androidx.lifecycle.ViewModel
import com.ling.astrbot.core.model.AuthState
import com.ling.astrbot.core.network.client.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionManager: SessionManager,
) : ViewModel() {
    val authState: StateFlow<AuthState> = sessionManager.state
}
