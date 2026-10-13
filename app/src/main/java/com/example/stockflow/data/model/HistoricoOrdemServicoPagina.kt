package com.example.stockflow.data.model

data class HistoricoOrdemServicoPagina(
    val content: List<HistoricoOrdemServicoResumo>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)
