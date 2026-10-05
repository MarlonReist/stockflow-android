package com.example.stockflow.ui.login

data class LoginUiState(
    val login: String = "",
    val senha: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false
)
