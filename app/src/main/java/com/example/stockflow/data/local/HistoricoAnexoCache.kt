package com.example.stockflow.data.local

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/** Armazena apenas arquivos temporários da sessão de consulta. */
internal class HistoricoAnexoCache(private val directory: File) {
    private var closed = false

    suspend fun salvar(body: ResponseBody, nomeOriginal: String): File = withContext(Dispatchers.IO) {
        body.use { response ->
            val file = synchronized(this@HistoricoAnexoCache) {
                check(!closed) { "Consulta encerrada." }
                if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cache indisponível.")
                // O nome vindo da API nunca é usado como caminho de arquivo.
                val extension = nomeOriginal.substringAfterLast('.', "")
                    .takeIf { it.matches(Regex("[a-zA-Z0-9]{1,10}")) }
                File.createTempFile("anexo_", extension?.let { ".$it" } ?: ".bin", directory)
            }
            var complete = false
            try {
                response.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                        }
                    }
                }
                ensureActive()
                if (file.length() == 0L) throw IOException("Arquivo vazio ou indisponível.")
                if (response.contentLength() >= 0 && file.length() != response.contentLength()) {
                    throw IOException("Download incompleto.")
                }
                complete = true
                file
            } finally {
                synchronized(this@HistoricoAnexoCache) {
                    if (!complete || closed) file.delete()
                    if (closed) directory.delete()
                }
            }
        }
    }

    @Synchronized
    fun limpar() {
        closed = true
        directory.deleteRecursively()
    }

    companion object {
        /** Remove resíduos deixados por encerramento inesperado do processo. */
        fun limparAntigos(root: File, now: Long = System.currentTimeMillis()) {
            root.listFiles()?.filter { now - it.lastModified() > 60 * 60 * 1000L }
                ?.forEach { it.deleteRecursively() }
        }
    }
}
