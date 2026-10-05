package com.example.stockflow.data.remote

import android.content.Context
import com.example.stockflow.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // O build debug cria automaticamente o redirecionamento ADB para esta porta.
    private const val BASE_URL = "http://127.0.0.1:8080/"

    @Volatile
    private var apiInstance: StockFlowApi? = null

    fun getInstance(context: Context): StockFlowApi {
        return apiInstance ?: synchronized(this) {
            apiInstance ?: createApi(context.applicationContext).also {
                apiInstance = it
            }
        }
    }

    private fun createApi(context: Context): StockFlowApi {
        val tokenManager = TokenManager(context)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StockFlowApi::class.java)
    }
}
