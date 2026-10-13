package com.example.stockflow

import com.example.stockflow.data.model.HistoricoOrdemServicoPagina
import com.example.stockflow.data.remote.StockFlowApi
import com.example.stockflow.data.repository.OrdemServicoRepository
import com.example.stockflow.ui.historico.HistoricoUiState
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HistoricoIntegracaoTest {
    private val requests = mutableListOf<Request>()
    private var code = 200
    private var json = """{"content":[],"totalElements":0,"totalPages":0,"page":0,"size":20}"""
    private var connectionFailure = false
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        requests.add(chain.request())
        if (connectionFailure) throw IOException("Conexão simulada indisponível")
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(code).message("test").body(json.toResponseBody("application/json".toMediaType())).build()
    }.build()
    private val api = Retrofit.Builder().baseUrl("http://localhost/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(StockFlowApi::class.java)
    private val repository = OrdemServicoRepository(api)

    @After fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    @Test fun consultaVaziaUsaSomenteParametrosDePaginacao() = runBlocking {
        val page = repository.listarHistorico()
        assertTrue(page.content.isEmpty())
        val request = requests.single()
        assertEquals("GET", request.method)
        assertEquals("/tecnico/historico", request.url.encodedPath)
        assertEquals(setOf("page", "size"), request.url.queryParameterNames)
        assertEquals("0", request.url.queryParameter("page"))
        assertEquals("20", request.url.queryParameter("size"))
    }

    @Test fun enviaOsSeisParametrosComDatasIso() = runBlocking {
        repository.listarHistorico(125, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8), 3, 2, 20)
        val url = requests.single().url
        assertEquals(setOf("osId", "dataInicialConclusao", "dataFinalConclusao", "tipoOrdemServicoId", "page", "size"), url.queryParameterNames)
        assertEquals("125", url.queryParameter("osId"))
        assertEquals("2026-10-01", url.queryParameter("dataInicialConclusao"))
        assertEquals("2026-10-08", url.queryParameter("dataFinalConclusao"))
        assertEquals("3", url.queryParameter("tipoOrdemServicoId"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test fun rejeitaFiltrosInvalidosAntesDeConsultar() = runBlocking {
        val dia = LocalDate.of(2026, 10, 8)
        val invalidos: List<suspend () -> Unit> = listOf(
            { repository.listarHistorico(dataInicialConclusao = dia) },
            { repository.listarHistorico(dataFinalConclusao = dia) },
            { repository.listarHistorico(dataInicialConclusao = dia, dataFinalConclusao = dia.minusDays(1)) },
            { repository.listarHistorico(page = -1) },
            { repository.listarHistorico(size = 0) },
            { repository.listarHistorico(size = 101) }
        )
        for (consulta in invalidos) {
            try { consulta(); fail("Filtro inválido aceito") } catch (_: IllegalArgumentException) { }
        }
        assertTrue(requests.isEmpty())
    }

    private fun resumo(id: Long, participacao: String) = """{
        "id":$id,"clienteNome":"Cliente teste","tipoOrdemServicoId":3,"tipoOrdemServicoNome":"Instalação",
        "dataAbertura":"2026-10-01","dataAgendada":null,"inicioAtendimento":"2026-10-08T09:00:00",
        "fimAtendimento":"2026-10-08T10:00:00","status":"AGUARDANDO_CONFERENCIA",
        "tecnicoResponsavelId":2,"tecnicoResponsavelNome":"Responsável","ajudanteId":4,
        "ajudanteNome":"Ajudante","participacaoDoTecnico":"$participacao"} """

    @Test fun paginaSemDuplicadosMantemParticipacoesEPermiteReinicio() = runBlocking {
        json = """{"content":[${resumo(125,"RESPONSAVEL")},${resumo(124,"AJUDANTE")}],"totalElements":3,"totalPages":2,"page":0,"size":2}"""
        val first = repository.listarHistorico(size = 2)
        var state = HistoricoUiState().receberPagina(first, true)
        assertTrue(state.canLoadMore)
        assertFalse(state.copy(isLoadingMore = true).canLoadMore)
        assertFalse(state.copy(requiresLogin = true).canLoadMore)
        assertEquals(listOf("RESPONSAVEL", "AJUDANTE"), state.ordens.map { it.participacaoDoTecnico })
        json = """{"content":[${resumo(124,"AJUDANTE")},${resumo(123,"RESPONSAVEL_E_AJUDANTE")}],"totalElements":3,"totalPages":2,"page":1,"size":2}"""
        state = state.receberPagina(repository.listarHistorico(page = 1, size = 2), false)
        assertEquals(listOf(125L, 124L, 123L), state.ordens.map { it.id })
        assertFalse(state.canLoadMore)
        state = state.receberPagina(HistoricoOrdemServicoPagina(emptyList(), 0, 0, 0, 20), true)
        assertTrue(state.isEmpty)
        assertFalse(state.canLoadMore)
    }

    @Test fun detalheLeMateriaisAlmoxarifadoEAnexos() = runBlocking {
        json = """{
            "id":125,"dataAbertura":"2026-10-01","status":"FINALIZADA","descricao":"Instalação",
            "clienteId":1,"clienteNome":"Cliente teste","tecnicoResponsavelNome":"Responsável",
            "fimAtendimento":"2026-10-08T10:00:00","observacaoConclusao":"Concluído","participacaoDoTecnico":"AJUDANTE",
            "produtosUtilizados":[{"id":1,"osId":125,"produtoId":2,"produtoNome":"Cabo","quantidade":10,"almoxarifadoId":3,"almoxarifadoNome":"Central"},
            {"id":2,"osId":125,"produtoId":3,"produtoNome":"Conector","quantidade":2,"almoxarifadoId":3,"almoxarifadoNome":"Central"}],
            "anexos":[{"id":9,"osId":125,"nomeOriginal":"foto.jpg","contentType":"image/jpeg","tamanho":42,"dataUpload":"2026-10-08T10:00:00"}]
        }"""
        val detail = repository.buscarHistoricoPorId(125)
        assertEquals("/tecnico/historico/125", requests.single().url.encodedPath)
        assertEquals(2, detail.produtosUtilizados.size)
        assertEquals(10, detail.produtosUtilizados.first().quantidade)
        assertEquals("Central", detail.produtosUtilizados.first().almoxarifadoNome)
        assertEquals("foto.jpg", detail.anexos.single().nomeOriginal)
        assertEquals("AJUDANTE", detail.participacaoDoTecnico)
    }

    @Test fun detalheSemMateriaisOuAnexos() = runBlocking {
        json = """{"id":125,"dataAbertura":"2026-10-01","status":"FINALIZADA","descricao":"Teste","clienteId":1,"clienteNome":"Cliente teste","fimAtendimento":"2026-10-08T10:00:00","produtosUtilizados":[],"anexos":[]}"""
        val detail = repository.buscarHistoricoPorId(125)
        assertTrue(detail.produtosUtilizados.isEmpty())
        assertTrue(detail.anexos.isEmpty())
        assertNull(detail.observacaoConclusao)
    }

    @Test fun anexoUsaSomenteEndpointProtegidoDoHistorico() = runBlocking {
        json = "arquivo teste"
        repository.baixarAnexoHistorico(9).use { assertEquals(json, it.string()) }
        assertEquals("/tecnico/historico/anexos/9/arquivo", requests.single().url.encodedPath)
        assertEquals("GET", requests.single().method)
    }

    @Test fun preservaCodigosDeErroParaTratamentoNoViewModel() = runBlocking {
        for (status in listOf(401, 403, 404)) {
            code = status
            val consultas: List<suspend () -> Unit> = listOf(
                { repository.listarHistorico() },
                { repository.buscarHistoricoPorId(125) },
                { repository.baixarAnexoHistorico(9).close() }
            )
            for (consulta in consultas) {
                try { consulta(); fail("Resposta de erro aceita") }
                catch (e: HttpException) { assertEquals(status, e.code()) }
            }
        }
    }

    @Test fun propagaErroDeConexao() = runBlocking {
        connectionFailure = true
        try { repository.listarHistorico(); fail("Falha não propagada") } catch (_: IOException) { }
    }
}
