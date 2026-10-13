package com.example.stockflow.ui.historico

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import com.example.stockflow.data.local.HistoricoAnexoCache
import com.example.stockflow.data.repository.OrdemServicoRepository
import java.io.IOException
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HistoricoDetalheViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val ordemId: Long = checkNotNull(savedStateHandle["ordemId"])
    private val repository = OrdemServicoRepository(application)
    private val tokenManager = TokenManager(application)
    private val _uiState = MutableStateFlow(HistoricoDetalheUiState())
    val uiState = _uiState.asStateFlow()
    private val cacheRoot = File(application.cacheDir, "historico_anexos")
    private val cache = HistoricoAnexoCache(File(cacheRoot, UUID.randomUUID().toString()))

    init {
        carregarDetalhe()
    }

    fun carregarDetalhe() {
        if (_uiState.value.isLoading || _uiState.value.downloadingId != null || _uiState.value.requiresLogin) return
        _uiState.value = HistoricoDetalheUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val ordem = repository.buscarHistoricoPorId(ordemId)
                _uiState.value = HistoricoDetalheUiState(ordem = ordem)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                when (exception.code()) {
                    401 -> expirarSessao()
                    403 -> mostrarErro("Você não tem permissão para consultar esta OS no histórico.")
                    404 -> mostrarErro("Ordem de serviço não encontrada no histórico.")
                    else -> mostrarErro("Não foi possível carregar os detalhes. Tente novamente.")
                }
            } catch (_: IOException) {
                mostrarErro("Não foi possível conectar ao servidor. Tente novamente.")
            } catch (_: Exception) {
                mostrarErro("Ocorreu um erro ao carregar os detalhes. Tente novamente.")
            }
        }
    }

    private fun mostrarErro(message: String) {
        _uiState.value = HistoricoDetalheUiState(errorMessage = message)
    }

    fun abrirAnexo(anexoId: Long) {
        val state = _uiState.value
        if (state.downloadingId != null || state.requiresLogin) return
        val anexo = state.ordem?.anexos?.firstOrNull { it.id == anexoId } ?: return
        _uiState.value = state.copy(downloadingId = anexoId, attachmentError = null)
        viewModelScope.launch {
            var arquivo: File? = null
            var manterParaVisualizador = false
            try {
                withContext(Dispatchers.IO) { HistoricoAnexoCache.limparAntigos(cacheRoot) }
                val body = repository.baixarAnexoHistorico(anexoId)
                val mime = body.contentType()?.let { "${it.type}/${it.subtype}" }
                    ?: anexo.contentType.ifBlank { "application/octet-stream" }
                // Fechar também quando houver cancelamento antes de entrar no bloco de IO.
                body.use { arquivo = cache.salvar(it, anexo.nomeOriginal) }
                val file = checkNotNull(arquivo)
                if (mime.startsWith("image/")) {
                    val bitmap = withContext(Dispatchers.IO) { lerImagem(file) }
                    ensureActive()
                    _uiState.value = _uiState.value.copy(
                        downloadingId = null, imagem = bitmap, nomeImagem = anexo.nomeOriginal
                    )
                } else {
                    ensureActive()
                    _uiState.value = _uiState.value.copy(
                        downloadingId = null, arquivoParaAbrir = ArquivoHistorico(file, mime)
                    )
                    manterParaVisualizador = true
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                if (exception.code() == 401) expirarSessao()
                else erroAnexo(when (exception.code()) {
                    403 -> "Você não tem permissão para abrir este anexo."
                    404 -> "Arquivo indisponível ou não encontrado."
                    else -> "Não foi possível baixar o anexo. Tente novamente."
                })
            } catch (_: IOException) {
                erroAnexo("Não foi possível ler ou baixar o arquivo. Verifique a conexão e tente novamente.")
            } catch (_: Exception) {
                erroAnexo("Não foi possível abrir este arquivo.")
            } finally {
                if (!manterParaVisualizador) arquivo?.delete()
            }
        }
    }

    fun arquivoEncaminhado(erro: String? = null) {
        if (erro != null) _uiState.value.arquivoParaAbrir?.file?.delete()
        _uiState.value = _uiState.value.copy(arquivoParaAbrir = null, attachmentError = erro)
    }

    fun fecharImagem() {
        _uiState.value = _uiState.value.copy(imagem = null, nomeImagem = null)
    }

    private fun erroAnexo(message: String) {
        _uiState.value = _uiState.value.copy(downloadingId = null, attachmentError = message)
    }

    private suspend fun expirarSessao() {
        try {
            tokenManager.clearToken()
        } catch (_: IOException) {
            // Solicitar login mesmo quando o armazenamento estiver indisponível.
        } finally {
            cache.limpar()
            _uiState.value = HistoricoDetalheUiState(requiresLogin = true)
        }
    }

    override fun onCleared() {
        cache.limpar()
    }

    private fun lerImagem(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Imagem indisponível.")
        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Imagem inválida.")
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
