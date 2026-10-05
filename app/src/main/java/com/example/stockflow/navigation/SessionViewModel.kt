package com.example.stockflow.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SessionState {
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED
}

class SessionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)

    private val _sessionState = MutableStateFlow(SessionState.LOADING)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedToken = tokenManager.token.first()

            _sessionState.value = if (savedToken.isNullOrBlank()) {
                SessionState.UNAUTHENTICATED
            } else {
                SessionState.AUTHENTICATED
            }
        }
    }
}
