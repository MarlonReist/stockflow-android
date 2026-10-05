package com.example.stockflow.data.repository

import android.content.Context
import com.example.stockflow.data.model.AdicionarMaterialRequest
import com.example.stockflow.data.model.Almoxarifado
import com.example.stockflow.data.model.EstoqueDisponivel
import com.example.stockflow.data.model.ConclusaoAtendimentoRequest
import com.example.stockflow.data.model.OrdemServicoAtualizacao
import com.example.stockflow.data.model.OrdemServicoAnexo
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.OrdemServicoItem
import com.example.stockflow.data.model.OrdemServicoResumo
import com.example.stockflow.data.remote.RetrofitClient
import com.example.stockflow.data.remote.StockFlowApi
import okhttp3.MultipartBody

class OrdemServicoRepository(
    context: Context,
    private val api: StockFlowApi = RetrofitClient.getInstance(context)
) {

    suspend fun listarMinhasOrdens(): List<OrdemServicoResumo> {
        return api.listarMinhasOrdens()
    }

    suspend fun buscarOrdemPorId(id: Long): OrdemServicoDetalhe {
        return api.buscarOrdemPorId(id)
    }

    suspend fun iniciarAtendimento(id: Long): OrdemServicoAtualizacao {
        return api.iniciarAtendimento(id)
    }

    suspend fun concluirAtendimento(
        id: Long,
        observacaoConclusao: String
    ): OrdemServicoAtualizacao {
        return api.concluirAtendimento(
            id,
            ConclusaoAtendimentoRequest(observacaoConclusao)
        )
    }

    suspend fun listarAlmoxarifados(): List<Almoxarifado> {
        return api.listarAlmoxarifados()
    }

    suspend fun listarEstoques(): List<EstoqueDisponivel> {
        return api.listarEstoques()
    }

    suspend fun adicionarMaterial(
        ordemId: Long,
        produtoId: Long,
        almoxarifadoId: Long,
        quantidade: Int
    ): OrdemServicoItem {
        val request = AdicionarMaterialRequest(
            produtoId = produtoId,
            almoxarifadoId = almoxarifadoId,
            quantidade = quantidade
        )

        return api.adicionarMaterial(ordemId, request)
    }

    suspend fun removerMaterial(ordemId: Long, itemId: Long) {
        api.removerMaterial(ordemId, itemId)
    }

    suspend fun adicionarAnexo(
        ordemId: Long,
        arquivo: MultipartBody.Part
    ): OrdemServicoAnexo {
        return api.adicionarAnexo(ordemId, arquivo)
    }
}
