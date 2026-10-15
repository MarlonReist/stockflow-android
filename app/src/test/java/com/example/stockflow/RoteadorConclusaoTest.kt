package com.example.stockflow

import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.PropriedadeRoteador
import com.example.stockflow.ui.ordens.RoteadorConclusaoUiState
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class RoteadorConclusaoTest {
    private val semRoteador = Gson().fromJson(
        """{"id":125,"dataAbertura":"2026-10-01","status":"EM_ATENDIMENTO","descricao":"Teste", "clienteId":1,"clienteNome":"Cliente teste","produtosUtilizados":[],"anexos":[]}""",
        OrdemServicoDetalhe::class.java
    )
    private val comRoteador = semRoteador.copy(
        modeloRoteadorPrincipal = "Archer C6", propriedadeRoteadorPrincipal = PropriedadeRoteador.COMODATO
    )

    @Test fun semCadastroExigeModeloEPropriedade() {
        val vazio = RoteadorConclusaoUiState()
        assertTrue(vazio.deveEnviar(semRoteador))
        assertNotNull(vazio.erroValidacao(semRoteador))
        assertNotNull(vazio.copy(modelo = "Archer C6").erroValidacao(semRoteador))
        assertNotNull(vazio.copy(propriedade = PropriedadeRoteador.PROPRIO).erroValidacao(semRoteador))
        val valido = vazio.copy(modelo = "  Archer C6  ", propriedade = PropriedadeRoteador.PROPRIO)
        assertNull(valido.erroValidacao(semRoteador))
        assertEquals("Archer C6", valido.request(semRoteador)?.modelo)
        assertEquals(PropriedadeRoteador.PROPRIO, valido.request(semRoteador)?.propriedade)
    }

    @Test fun cadastroParcialTambemExigePreenchimento() {
        val vazio = RoteadorConclusaoUiState()
        assertTrue(vazio.deveEnviar(comRoteador.copy(propriedadeRoteadorPrincipal = null)))
        assertTrue(vazio.deveEnviar(comRoteador.copy(modeloRoteadorPrincipal = "  ")))
    }

    @Test fun manterCadastroIgnoraRascunhoEOmiteRequest() {
        val rascunho = RoteadorConclusaoUiState(modelo = "Outro modelo", propriedade = PropriedadeRoteador.PROPRIO)
        assertFalse(rascunho.deveEnviar(comRoteador))
        assertNull(rascunho.erroValidacao(comRoteador))
        assertNull(rascunho.request(comRoteador))
        assertNull(rascunho.copy(modelo = "").erroValidacao(comRoteador))
    }

    @Test fun atualizacaoOpcionalValidaCamposEEnviaEscolha() {
        val rascunho = RoteadorConclusaoUiState(atualizar = true, modelo = "Novo modelo", propriedade = PropriedadeRoteador.PROPRIO)
        assertEquals("Novo modelo", rascunho.request(comRoteador)?.modelo)
        assertEquals(PropriedadeRoteador.PROPRIO, rascunho.request(comRoteador)?.propriedade)
        assertNotNull(rascunho.copy(modelo = " ").erroValidacao(comRoteador))
        assertNotNull(rascunho.copy(propriedade = null).erroValidacao(comRoteador))
        assertNull(rascunho.copy(atualizar = false).request(comRoteador))
    }

    @Test fun respeitaLimiteDe150CaracteresAposTrim() {
        val rascunho = RoteadorConclusaoUiState(atualizar = true, modelo = "a".repeat(150), propriedade = PropriedadeRoteador.COMODATO)
        assertNull(rascunho.erroValidacao(comRoteador))
        assertNotNull(rascunho.copy(modelo = "a".repeat(151)).erroValidacao(comRoteador))
        assertNull(rascunho.copy(modelo = "  ${rascunho.modelo} ").erroValidacao(comRoteador))
    }
}
