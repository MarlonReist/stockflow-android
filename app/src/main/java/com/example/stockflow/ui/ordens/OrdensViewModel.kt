package com.example.stockflow.ui.ordens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import com.example.stockflow.data.repository.OrdemServicoRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class OrdensViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = OrdemServicoRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(OrdensUiState())
    val uiState: StateFlow<OrdensUiState> = _uiState.asStateFlow()

    init {
        carregarOrdens()
    }

    fun carregarOrdens() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val ordens = repository.listarMinhasOrdens()

                _uiState.value = _uiState.value.copy(
                    ordens = ordens,
                    isLoading = false
                )
            } catch (exception: HttpException) {
                Log.e(
                    "StockFlowOrdens",
                    "Erro HTTP ao carregar as ordens",
                    exception
                )

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Erro do servidor: HTTP ${exception.code()}."
                    )
                }
            } catch (exception: IOException) {
                Log.e(
                    "StockFlowOrdens",
                    "Falha ao conectar com a API",
                    exception
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar ao servidor."
                )
            } catch (exception: Exception) {
                Log.e(
                    "StockFlowOrdens",
                    "Erro inesperado ao carregar as ordens",
                    exception
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ocorreu um erro inesperado."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            clearSession()
        }
    }

    private suspend fun clearSession() {
        tokenManager.clearToken()

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            requiresLogin = true
        )
    }
}
