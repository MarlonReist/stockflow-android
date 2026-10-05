package com.example.stockflow.data.model

data class TecnicoAjudante(
    val colaboradorId: Long,
    val colaboradorNome: String,
    val usuarioId: Long,
    val login: String
)

data class AtualizarAjudanteRequest(
    val ajudanteId: Long?
)
