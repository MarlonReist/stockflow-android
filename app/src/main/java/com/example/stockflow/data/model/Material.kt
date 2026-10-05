package com.example.stockflow.data.model

data class Almoxarifado(
    val id: Long,
    val nome: String,
    val principal: Boolean
)

data class EstoqueDisponivel(
    val id: Long,
    val almoxarifadoId: Long,
    val almoxarifadoNome: String,
    val produtoId: Long,
    val produtoNome: String,
    val quantidade: Int
)

data class AdicionarMaterialRequest(
    val produtoId: Long,
    val almoxarifadoId: Long,
    val quantidade: Int
)
