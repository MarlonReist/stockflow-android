package com.example.stockflow.data.model

data class LoginResponse(
    val usuarioId: Long,
    val nome: String,
    val login: String,
    val perfil: String,
    val status: String,
    val autenticado: Boolean,
    val token: String
)