package com.example.stockflow.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import com.example.stockflow.data.repository.AuthRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginChange(value: String) {
        _uiState.value = _uiState.value.copy(
            login = value,
            errorMessage = null
        )
    }

    fun onSenhaChange(value: String) {
        _uiState.value = _uiState.value.copy(
            senha = value,
            errorMessage = null
        )
    }

    fun login() {
        val currentState = _uiState.value

        if (currentState.login.isBlank() ||
            currentState.senha.isBlank()
        ) {
            _uiState.value = currentState.copy(
                errorMessage = "Informe o login e a senha."
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val response = repository.login(
                    login = currentState.login,
                    senha = currentState.senha
                )

                if (response.perfil != "TECNICO") {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Acesso permitido somente para técnicos ."
                    )
                    return@launch
                }

                tokenManager.saveToken(response.token)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = response.autenticado
                )
            } catch (exception: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = when (exception.code()) {
                        400 -> "Login ou senha inválidos."
                        403 -> "Você não possui permissão para acessar."
                        else -> "Erro do servidor: HTTP ${exception.code()}."
                    }
                )
            } catch (exception: IOException) {
                Log.e(
                    "StockFlowLogin",
                    "Falha ao conectar com a API",
                    exception
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar ao servidor."
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ocorreu um erro inesperado."
                )
            }
        }
    }
}
