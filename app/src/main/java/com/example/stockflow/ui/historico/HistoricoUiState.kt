package com.example.stockflow.ui.historico

import com.example.stockflow.data.model.HistoricoOrdemServicoResumo
import com.example.stockflow.data.model.HistoricoOrdemServicoPagina
import com.example.stockflow.data.model.TipoOrdemServico
import java.time.LocalDate

data class HistoricoFiltros(
    val osId: Long? = null,
    val dataInicialConclusao: LocalDate? = null,
    val dataFinalConclusao: LocalDate? = null,
    val tipoOrdemServicoId: Long? = null
)

data class HistoricoUiState(
    val ordens: List<HistoricoOrdemServicoResumo> = emptyList(),
    val filtros: HistoricoFiltros = HistoricoFiltros(),
    val page: Int = -1,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val filterError: String? = null,
    val tiposServico: List<TipoOrdemServico> = emptyList(),
    val isLoadingTypes: Boolean = false,
    val typesError: String? = null,
    val requiresLogin: Boolean = false
) {
    internal fun receberPagina(pagina: HistoricoOrdemServicoPagina, reiniciar: Boolean): HistoricoUiState {
        val anteriores = if (reiniciar) emptyList() else ordens
        return copy(
            ordens = (anteriores + pagina.content).distinctBy { it.id },
            page = pagina.page,
            totalPages = pagina.totalPages,
            totalElements = pagina.totalElements,
            isLoading = false,
            isLoadingMore = false,
            errorMessage = null,
            loadMoreError = null
        )
    }

    val isEmpty: Boolean
        get() = page >= 0 && !isLoading && errorMessage == null && ordens.isEmpty()

    val canLoadMore: Boolean
        get() = page >= 0 && page + 1 < totalPages &&
            !isLoading && !isLoadingMore && !requiresLogin
}
