package com.example.stockflow.ui.ordens

import com.example.stockflow.data.model.OrdemServicoResumo

data class OrdensUiState(
    val ordens: List<OrdemServicoResumo> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val requiresLogin: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && ordens.isEmpty()
}
