package com.example.stockflow

import com.example.stockflow.data.model.PropriedadeRoteador
import com.example.stockflow.data.model.RoteadorPrincipalRequest
import com.example.stockflow.data.remote.StockFlowApi
import com.example.stockflow.data.repository.OrdemServicoRepository
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RoteadorContratoTest {
    private val requests = mutableListOf<Request>()
    private var json = """{"id":125,"status":"AGUARDANDO_CONFERENCIA","fimAtendimento":"2026-10-15T10:00:00"}"""
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        requests.add(chain.request())
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(200).message("test")
            .body(json.toResponseBody("application/json".toMediaType())).build()
    }.build()
    private val api = Retrofit.Builder().baseUrl("http://localhost/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(StockFlowApi::class.java)
    private val repository = OrdemServicoRepository(api)

    @After fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    @Test fun manterRoteadorOmiteObjetoNaConclusao() = runBlocking {
        repository.concluirAtendimento(125, "Conexão testada.")
        val request = requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/tecnico/os/125/concluir-atendimento", request.url.encodedPath)
        val buffer = Buffer()
        checkNotNull(request.body).writeTo(buffer)
        val body = JsonParser.parseString(buffer.readUtf8()).asJsonObject
        assertEquals(setOf("observacaoConclusao"), body.keySet())
        assertEquals("Conexão testada.", body["observacaoConclusao"].asString)
    }

    @Test fun enviarRoteadorUsaObjetoAninhadoEValoresDoBackend() = runBlocking {
        for (propriedade in PropriedadeRoteador.entries) {
            repository.concluirAtendimento(125, "Instalação concluída.", RoteadorPrincipalRequest("Archer C6", propriedade))
            val buffer = Buffer()
            checkNotNull(requests.last().body).writeTo(buffer)
            val body = JsonParser.parseString(buffer.readUtf8()).asJsonObject
            assertEquals(setOf("observacaoConclusao", "roteadorPrincipal"), body.keySet())
            val roteador = body["roteadorPrincipal"].asJsonObject
            assertEquals(setOf("modelo", "propriedade"), roteador.keySet())
            assertEquals("Archer C6", roteador["modelo"].asString)
            assertEquals(propriedade.name, roteador["propriedade"].asString)
        }
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.method == "PATCH" && it.url.encodedPath == "/tecnico/os/125/concluir-atendimento" })
    }

    @Test fun modeloInvalidoNaoEnviaConclusao() = runBlocking {
        for (modelo in listOf("", "   ", "a".repeat(151))) {
            try {
                repository.concluirAtendimento(125, "Concluído", RoteadorPrincipalRequest(modelo, PropriedadeRoteador.PROPRIO))
                fail("Modelo inválido aceito")
            } catch (_: IllegalArgumentException) { }
        }
        assertTrue(requests.isEmpty())
        repository.concluirAtendimento(125, "Concluído", RoteadorPrincipalRequest("a".repeat(150), PropriedadeRoteador.PROPRIO))
        assertEquals(1, requests.size)
    }

    @Test fun consultaVaziaNaoChamaAutocomplete() = runBlocking {
        for (texto in listOf("", "   ", "\n\t")) assertTrue(repository.buscarSugestoesRoteador(texto).isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test fun autocompleteUsaParametrosCorretosEPreservaOrdemDeFrequencia() = runBlocking {
        json = """[{"modelo":"Archer C6","frequencia":42},{"modelo":"Archer C80","frequencia":9}]"""
        val sugestoes = repository.buscarSugestoesRoteador("  Archer  ")
        val request = requests.single()
        assertEquals("GET", request.method)
        assertEquals("/clientes/roteador-principal/sugestoes", request.url.encodedPath)
        assertEquals(setOf("q", "limite"), request.url.queryParameterNames)
        assertEquals("Archer", request.url.queryParameter("q"))
        assertEquals("10", request.url.queryParameter("limite"))
        assertEquals(listOf("Archer C6", "Archer C80"), sugestoes.map { it.modelo })
        assertEquals(listOf(42L, 9L), sugestoes.map { it.frequencia })
    }

    @Test fun limiteInvalidoNaoConsultaAutocomplete() = runBlocking {
        for (limite in listOf(0, -1, 21)) {
            try { repository.buscarSugestoesRoteador("Archer", limite); fail("Limite inválido aceito") }
            catch (_: IllegalArgumentException) { }
        }
        assertTrue(requests.isEmpty())
    }

    @Test fun detalheLeRoteadorAtualEAceitaAusenciaDosCampos() = runBlocking {
        val campos = """"id":125,"dataAbertura":"2026-10-01","status":"EM_ATENDIMENTO","descricao":"Teste","clienteId":1,"clienteNome":"Cliente teste","produtosUtilizados":[],"anexos":[]"""
        json = """{$campos,"modeloRoteadorPrincipal":"Archer C6","propriedadeRoteadorPrincipal":"COMODATO","dataAtualizacaoRoteador":"2026-10-15T10:00:00"}"""
        val detail = repository.buscarOrdemPorId(125)
        assertEquals("Archer C6", detail.modeloRoteadorPrincipal)
        assertEquals(PropriedadeRoteador.COMODATO, detail.propriedadeRoteadorPrincipal)
        assertEquals("2026-10-15T10:00:00", detail.dataAtualizacaoRoteador)
        json = "{$campos}"
        val semRoteador = repository.buscarOrdemPorId(125)
        assertNull(semRoteador.modeloRoteadorPrincipal)
        assertNull(semRoteador.propriedadeRoteadorPrincipal)
        assertNull(semRoteador.dataAtualizacaoRoteador)
    }
}
