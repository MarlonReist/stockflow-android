package com.example.stockflow.data.model

data class ConclusaoAtendimentoRequest(
    val observacaoConclusao: String,
    val roteadorPrincipal: RoteadorPrincipalRequest? = null
)
