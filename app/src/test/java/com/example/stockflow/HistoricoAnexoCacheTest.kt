package com.example.stockflow

import com.example.stockflow.data.local.HistoricoAnexoCache
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HistoricoAnexoCacheTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun salvaSomenteDentroDoCacheEApagaAoEncerrar() = runBlocking {
        val dir = File(temporary.root, "sessao")
        val cache = HistoricoAnexoCache(dir)
        val file = cache.salvar("conteudo".toResponseBody(), "../../privado.pdf")
        assertEquals(dir.canonicalFile, checkNotNull(file.parentFile).canonicalFile)
        assertEquals("conteudo", file.readText())
        assertEquals("pdf", file.extension)
        cache.limpar()
        assertFalse(file.exists())
        try { cache.salvar("outro".toResponseBody(), "foto.jpg"); fail("Cache encerrado reutilizado") }
        catch (_: IllegalStateException) { }
    }

    @Test fun arquivoVazioNaoDeixaResiduo() = runBlocking {
        val dir = File(temporary.root, "sessao")
        val cache = HistoricoAnexoCache(dir)
        try { cache.salvar("".toResponseBody(), "arquivo.pdf"); fail("Arquivo vazio aceito") }
        catch (_: IOException) { }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test fun limpaSomenteSessoesAntigas() {
        val old = temporary.newFolder("antiga")
        val recent = temporary.newFolder("recente")
        File(old, "anexo").writeText("dados")
        File(recent, "anexo").writeText("dados")
        val now = System.currentTimeMillis()
        assertTrue(old.setLastModified(now - 7_200_000))
        HistoricoAnexoCache.limparAntigos(temporary.root, now)
        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    @Test fun downloadInterrompidoFechaCorpoERemoveParcial() = runBlocking {
        val dir = File(temporary.root, "sessao")
        var closed = false
        val source = object : ForwardingSource(Buffer().writeUtf8("parcial")) {
            var first = true
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (!first) throw IOException("Conexão interrompida")
                first = false
                return super.read(sink, byteCount)
            }
            override fun close() { closed = true; super.close() }
        }.buffer()
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source(): BufferedSource = source
        }
        try { HistoricoAnexoCache(dir).salvar(body, "arquivo.pdf"); fail("Falha ignorada") }
        catch (_: IOException) { }
        assertTrue(closed)
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test fun tamanhoIncompletoNaoDeixaArquivoDisponivel() = runBlocking {
        val dir = File(temporary.root, "sessao")
        val source = Buffer().writeUtf8("curto")
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = 100L
            override fun source(): BufferedSource = source
        }
        try { HistoricoAnexoCache(dir).salvar(body, "arquivo.pdf"); fail("Arquivo incompleto aceito") }
        catch (_: IOException) { }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }
}
