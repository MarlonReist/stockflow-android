package com.example.stockflow.data.model

enum class PropriedadeRoteador {
    PROPRIO,
    COMODATO
}

data class RoteadorPrincipalRequest(
    val modelo: String,
    val propriedade: PropriedadeRoteador
)

data class ModeloRoteadorSugestao(
    val modelo: String,
    val frequencia: Long
)
