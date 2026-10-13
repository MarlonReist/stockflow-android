package com.example.stockflow.ui.historico

import com.example.stockflow.data.model.HistoricoOrdemServicoDetalhe
import android.graphics.Bitmap
import java.io.File

data class HistoricoDetalheUiState(
    val ordem: HistoricoOrdemServicoDetalhe? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val downloadingId: Long? = null,
    val attachmentError: String? = null,
    val imagem: Bitmap? = null,
    val nomeImagem: String? = null,
    val arquivoParaAbrir: ArquivoHistorico? = null,
    val requiresLogin: Boolean = false
)

data class ArquivoHistorico(val file: File, val contentType: String)
