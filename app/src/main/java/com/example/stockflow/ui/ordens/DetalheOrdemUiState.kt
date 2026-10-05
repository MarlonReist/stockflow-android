package com.example.stockflow.ui.ordens

import com.example.stockflow.data.model.Almoxarifado
import com.example.stockflow.data.model.EstoqueDisponivel
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.TecnicoAjudante

data class DetalheOrdemUiState(
    val ordem: OrdemServicoDetalhe? = null,
    val isLoading: Boolean = true,
    val isStarting: Boolean = false,
    val isLoadingMaterials: Boolean = false,
    val isAddingMaterial: Boolean = false,
    val removingMaterialId: Long? = null,
    val isUploadingAttachment: Boolean = false,
    val isCompleting: Boolean = false,
    val isLoadingHelpers: Boolean = false,
    val isSavingHelper: Boolean = false,
    val almoxarifados: List<Almoxarifado> = emptyList(),
    val estoques: List<EstoqueDisponivel> = emptyList(),
    val ajudantes: List<TecnicoAjudante> = emptyList(),
    val errorMessage: String? = null,
    val actionError: String? = null,
    val materialError: String? = null,
    val attachmentError: String? = null,
    val completionError: String? = null,
    val helperError: String? = null,
    val wasUpdated: Boolean = false,
    val requiresLogin: Boolean = false
)
