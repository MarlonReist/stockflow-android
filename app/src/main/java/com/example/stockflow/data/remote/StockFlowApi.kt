package com.example.stockflow.data.remote

import com.example.stockflow.data.model.AdicionarMaterialRequest
import com.example.stockflow.data.model.Almoxarifado
import com.example.stockflow.data.model.EstoqueDisponivel
import com.example.stockflow.data.model.ConclusaoAtendimentoRequest
import com.example.stockflow.data.model.AtualizarAjudanteRequest
import com.example.stockflow.data.model.TecnicoAjudante
import com.example.stockflow.data.model.LoginRequest
import com.example.stockflow.data.model.LoginResponse
import com.example.stockflow.data.model.OrdemServicoAtualizacao
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.OrdemServicoAnexo
import com.example.stockflow.data.model.OrdemServicoItem
import com.example.stockflow.data.model.OrdemServicoResumo
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface StockFlowApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("tecnico/minhas-os")
    suspend fun listarMinhasOrdens(): List<OrdemServicoResumo>

    @GET("tecnico/ajudantes")
    suspend fun listarAjudantes(): List<TecnicoAjudante>

    @GET("tecnico/os/{id}")
    suspend fun buscarOrdemPorId(
        @Path("id") id: Long
    ): OrdemServicoDetalhe

    @PATCH("tecnico/os/{id}/iniciar")
    suspend fun iniciarAtendimento(
        @Path("id") id: Long
    ): OrdemServicoAtualizacao

    @PATCH("tecnico/os/{id}/concluir-atendimento")
    suspend fun concluirAtendimento(
        @Path("id") id: Long,
        @Body request: ConclusaoAtendimentoRequest
    ): OrdemServicoAtualizacao

    @PATCH("tecnico/os/{id}/ajudante")
    suspend fun atualizarAjudante(
        @Path("id") id: Long,
        @Body request: AtualizarAjudanteRequest
    ): OrdemServicoAtualizacao

    @GET("almoxarifados")
    suspend fun listarAlmoxarifados(): List<Almoxarifado>

    @GET("estoques")
    suspend fun listarEstoques(): List<EstoqueDisponivel>

    @POST("tecnico/os/{id}/itens")
    suspend fun adicionarMaterial(
        @Path("id") ordemId: Long,
        @Body request: AdicionarMaterialRequest
    ): OrdemServicoItem

    @DELETE("tecnico/os/{osId}/itens/{itemId}")
    suspend fun removerMaterial(
        @Path("osId") ordemId: Long,
        @Path("itemId") itemId: Long
    )

    @Multipart
    @POST("tecnico/os/{id}/anexos")
    suspend fun adicionarAnexo(
        @Path("id") ordemId: Long,
        @Part arquivo: MultipartBody.Part
    ): OrdemServicoAnexo
}
