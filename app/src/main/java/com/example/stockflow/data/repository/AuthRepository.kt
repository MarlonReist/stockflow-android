package com.example.stockflow.data.repository

import android.content.Context
import com.example.stockflow.data.model.LoginRequest
import com.example.stockflow.data.model.LoginResponse
import com.example.stockflow.data.remote.RetrofitClient
import com.example.stockflow.data.remote.StockFlowApi

class AuthRepository(
    context: Context,
    private val api: StockFlowApi = RetrofitClient.getInstance(context)
) {

    suspend fun login(
        login: String,
        senha: String
    ): LoginResponse {
        val request = LoginRequest(
            login = login,
            senha = senha
        )

        return api.login(request)
    }
}
