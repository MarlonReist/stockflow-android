package com.example.stockflow.data.repository

import android.content.Context
import com.example.stockflow.data.model.AdicionarMaterialRequest
import com.example.stockflow.data.model.Almoxarifado
import com.example.stockflow.data.model.EstoqueDisponivel
import com.example.stockflow.data.model.ConclusaoAtendimentoRequest
import com.example.stockflow.data.model.ModeloRoteadorSugestao
import com.example.stockflow.data.model.RoteadorPrincipalRequest
import com.example.stockflow.data.model.AtualizarAjudanteRequest
import com.example.stockflow.data.model.TecnicoAjudante
import com.example.stockflow.data.model.OrdemServicoAtualizacao
import com.example.stockflow.data.model.OrdemServicoAnexo
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.OrdemServicoItem
import com.example.stockflow.data.model.OrdemServicoResumo
import com.example.stockflow.data.model.HistoricoOrdemServicoDetalhe
import com.example.stockflow.data.model.HistoricoOrdemServicoPagina
import com.example.stockflow.data.model.TipoOrdemServico
import com.example.stockflow.data.remote.RetrofitClient
import com.example.stockflow.data.remote.StockFlowApi
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OrdemServicoRepository(
    private val api: StockFlowApi
) {
    constructor(context: Context) : this(RetrofitClient.getInstance(context))

    suspend fun listarMinhasOrdens(): List<OrdemServicoResumo> {
        return api.listarMinhasOrdens()
    }

    suspend fun listarTiposOrdemServicoAtivos(): List<TipoOrdemServico> {
        return api.listarTiposOrdemServicoAtivos()
    }

    suspend fun listarHistorico(
        osId: Long? = null,
        dataInicialConclusao: LocalDate? = null,
        dataFinalConclusao: LocalDate? = null,
        tipoOrdemServicoId: Long? = null,
        page: Int = 0,
        size: Int = 20
    ): HistoricoOrdemServicoPagina {
        require(page >= 0) { "A página deve ser maior ou igual a zero." }
        require(size in 1..100) { "O tamanho da página deve estar entre 1 e 100." }
        require((dataInicialConclusao == null) == (dataFinalConclusao == null)) {
            "Informe a data inicial e a data final juntas."
        }
        if (dataInicialConclusao != null && dataFinalConclusao != null) {
            require(!dataInicialConclusao.isAfter(dataFinalConclusao)) {
                "A data inicial não pode ser posterior à data final."
            }
        }

        return api.listarHistorico(
            osId = osId,
            dataInicialConclusao = dataInicialConclusao?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            dataFinalConclusao = dataFinalConclusao?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            tipoOrdemServicoId = tipoOrdemServicoId,
            page = page,
            size = size
        )
    }

    suspend fun buscarHistoricoPorId(id: Long): HistoricoOrdemServicoDetalhe {
        return api.buscarHistoricoPorId(id)
    }

    // Quem consumir o arquivo deve ler em Dispatchers.IO e fechar o corpo com use.
    suspend fun baixarAnexoHistorico(anexoId: Long): ResponseBody {
        return api.baixarAnexoHistorico(anexoId)
    }

    suspend fun listarAjudantes(): List<TecnicoAjudante> {
        return api.listarAjudantes()
    }

    suspend fun buscarOrdemPorId(id: Long): OrdemServicoDetalhe {
        return api.buscarOrdemPorId(id)
    }

    suspend fun iniciarAtendimento(id: Long): OrdemServicoAtualizacao {
        return api.iniciarAtendimento(id)
    }

    suspend fun concluirAtendimento(
        id: Long,
        observacaoConclusao: String,
        roteadorPrincipal: RoteadorPrincipalRequest? = null
    ): OrdemServicoAtualizacao {
        roteadorPrincipal?.let {
            require(it.modelo.isNotBlank()) { "Informe o modelo do roteador." }
            require(it.modelo.length <= 150) { "O modelo do roteador deve ter no máximo 150 caracteres." }
        }
        return api.concluirAtendimento(
            id,
            ConclusaoAtendimentoRequest(observacaoConclusao, roteadorPrincipal)
        )
    }

    suspend fun buscarSugestoesRoteador(
        consulta: String,
        limite: Int = 10
    ): List<ModeloRoteadorSugestao> {
        val texto = consulta.trim()
        if (texto.isBlank()) return emptyList()
        require(limite in 1..20) { "O limite de sugestões deve estar entre 1 e 20." }
        return api.buscarSugestoesRoteador(texto, limite)
    }

    suspend fun atualizarAjudante(
        id: Long,
        ajudanteId: Long?
    ): OrdemServicoAtualizacao {
        return api.atualizarAjudante(id, AtualizarAjudanteRequest(ajudanteId))
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
