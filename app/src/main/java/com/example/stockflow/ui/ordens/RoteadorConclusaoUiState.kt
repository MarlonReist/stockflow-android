package com.example.stockflow.ui.ordens

import com.example.stockflow.data.model.ModeloRoteadorSugestao
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.PropriedadeRoteador
import com.example.stockflow.data.model.RoteadorPrincipalRequest

data class RoteadorConclusaoUiState(
    val atualizar: Boolean = false,
    val modelo: String = "",
    val propriedade: PropriedadeRoteador? = null,
    val sugestoes: List<ModeloRoteadorSugestao> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
) {
    fun deveEnviar(ordem: OrdemServicoDetalhe): Boolean = !ordem.possuiRoteadorPrincipal() || atualizar

    fun erroValidacao(ordem: OrdemServicoDetalhe): String? {
        if (!deveEnviar(ordem)) return null
        if (modelo.isBlank()) return "Informe o modelo do roteador principal."
        if (modelo.trim().length > 150) return "O modelo deve ter no máximo 150 caracteres."
        if (propriedade == null) return "Selecione Próprio ou Comodato."
        return null
    }

    fun request(ordem: OrdemServicoDetalhe): RoteadorPrincipalRequest? {
        if (!deveEnviar(ordem)) return null
        require(erroValidacao(ordem) == null) { erroValidacao(ordem).orEmpty() }
        return RoteadorPrincipalRequest(modelo.trim(), checkNotNull(propriedade))
    }
}

internal fun OrdemServicoDetalhe.possuiRoteadorPrincipal(): Boolean =
    !modeloRoteadorPrincipal.isNullOrBlank() && propriedadeRoteadorPrincipal != null

internal fun PropriedadeRoteador.label(): String = when (this) {
    PropriedadeRoteador.PROPRIO -> "Próprio"
    PropriedadeRoteador.COMODATO -> "Comodato"
}
