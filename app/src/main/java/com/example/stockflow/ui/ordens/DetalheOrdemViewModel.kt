package com.example.stockflow.ui.ordens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import com.example.stockflow.data.model.OrdemServicoStatus
import com.example.stockflow.data.repository.OrdemServicoRepository
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class DetalheOrdemViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val ordemId: Long = checkNotNull(savedStateHandle["ordemId"])
    private val repository = OrdemServicoRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(DetalheOrdemUiState())
    val uiState: StateFlow<DetalheOrdemUiState> = _uiState.asStateFlow()

    init {
        carregarDetalhe()
    }

    fun carregarDetalhe() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            actionError = null
        )

        viewModelScope.launch {
            try {
                val ordem = repository.buscarOrdemPorId(ordemId)

                _uiState.value = _uiState.value.copy(
                    ordem = ordem,
                    isLoading = false
                )
            } catch (exception: HttpException) {
                Log.e(
                    "StockFlowDetalhe",
                    "Erro HTTP ao carregar o detalhe da OS $ordemId",
                    exception
                )

                when (exception.code()) {
                    401, 403 -> clearSession()
                    404 -> showError("Ordem de serviço não encontrada.")
                    else -> showError("Erro do servidor: HTTP ${exception.code()}.")
                }
            } catch (exception: IOException) {
                Log.e(
                    "StockFlowDetalhe",
                    "Falha ao conectar com a API",
                    exception
                )

                showError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e(
                    "StockFlowDetalhe",
                    "Erro inesperado ao carregar o detalhe",
                    exception
                )

                showError("Ocorreu um erro inesperado.")
            }
        }
    }

    fun iniciarAtendimento() {
        val ordemAtual = _uiState.value.ordem ?: return

        if (ordemAtual.status != OrdemServicoStatus.AGENDADA) {
            _uiState.value = _uiState.value.copy(
                actionError = "Apenas ordens agendadas podem ser iniciadas."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isStarting = true,
            actionError = null
        )

        viewModelScope.launch {
            try {
                val atualizacao = repository.iniciarAtendimento(ordemId)

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(
                        status = atualizacao.status,
                        inicioAtendimento = atualizacao.inicioAtendimento,
                        fimAtendimento = atualizacao.fimAtendimento
                    ),
                    isStarting = false,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e(
                    "StockFlowDetalhe",
                    "Erro HTTP ao iniciar a OS $ordemId",
                    exception
                )

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showActionError(
                        if (exception.code() == 400) {
                            "Não foi possível iniciar. Verifique o status da OS."
                        } else {
                            "Erro do servidor: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e(
                    "StockFlowDetalhe",
                    "Falha de conexão ao iniciar a OS $ordemId",
                    exception
                )

                showActionError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e(
                    "StockFlowDetalhe",
                    "Erro inesperado ao iniciar a OS $ordemId",
                    exception
                )

                showActionError("Ocorreu um erro inesperado.")
            }
        }
    }

    fun carregarMateriais() {
        if (_uiState.value.isLoadingMaterials || _uiState.value.almoxarifados.isNotEmpty()) {
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoadingMaterials = true,
            materialError = null
        )

        viewModelScope.launch {
            try {
                val (almoxarifados, estoques) = coroutineScope {
                    val almoxarifadosRequest = async { repository.listarAlmoxarifados() }
                    val estoquesRequest = async { repository.listarEstoques() }
                    almoxarifadosRequest.await() to estoquesRequest.await()
                }

                _uiState.value = _uiState.value.copy(
                    almoxarifados = almoxarifados,
                    estoques = estoques.filter { it.quantidade > 0 },
                    isLoadingMaterials = false
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao carregar materiais", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showMaterialError("Erro ao carregar materiais: HTTP ${exception.code()}.")
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao carregar materiais", exception)
                showMaterialError("Não foi possível carregar os materiais.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao carregar materiais", exception)
                showMaterialError("Ocorreu um erro ao carregar os materiais.")
            }
        }
    }

    fun adicionarMaterial(produtoId: Long, almoxarifadoId: Long, quantidade: Int) {
        val ordemAtual = _uiState.value.ordem ?: return

        if (ordemAtual.status != OrdemServicoStatus.EM_ATENDIMENTO) {
            showMaterialError("Os materiais só podem ser lançados durante o atendimento.")
            return
        }

        if (quantidade <= 0) {
            showMaterialError("Informe uma quantidade maior que zero.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isAddingMaterial = true,
            materialError = null
        )

        viewModelScope.launch {
            try {
                val novoItem = repository.adicionarMaterial(
                    ordemId = ordemId,
                    produtoId = produtoId,
                    almoxarifadoId = almoxarifadoId,
                    quantidade = quantidade
                )

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(
                        produtosUtilizados = ordemAtual.produtosUtilizados + novoItem
                    ),
                    estoques = _uiState.value.estoques.map { estoque ->
                        if (estoque.produtoId == produtoId &&
                            estoque.almoxarifadoId == almoxarifadoId
                        ) {
                            estoque.copy(quantidade = estoque.quantidade - quantidade)
                        } else {
                            estoque
                        }
                    }.filter { it.quantidade > 0 },
                    isAddingMaterial = false,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao adicionar material", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showMaterialError(
                        if (exception.code() == 400) {
                            "Não foi possível lançar o material. Verifique o saldo e a quantidade."
                        } else {
                            "Erro do servidor: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao adicionar material", exception)
                showMaterialError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao adicionar material", exception)
                showMaterialError("Ocorreu um erro ao lançar o material.")
            }
        }
    }

    fun removerMaterial(itemId: Long) {
        val ordemAtual = _uiState.value.ordem ?: return

        if (ordemAtual.status != OrdemServicoStatus.EM_ATENDIMENTO) {
            showMaterialError("Os materiais só podem ser removidos durante o atendimento.")
            return
        }

        if (_uiState.value.removingMaterialId != null) return

        _uiState.value = _uiState.value.copy(
            removingMaterialId = itemId,
            materialError = null
        )

        viewModelScope.launch {
            try {
                repository.removerMaterial(ordemId, itemId)

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(
                        produtosUtilizados = ordemAtual.produtosUtilizados.filterNot {
                            it.id == itemId
                        }
                    ),
                    // A remoção devolve o saldo no backend. Limpamos este cache para
                    // buscar os valores atualizados quando o formulário abrir novamente.
                    almoxarifados = emptyList(),
                    estoques = emptyList(),
                    removingMaterialId = null,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao remover material $itemId", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showMaterialError(
                        if (exception.code() == 400 || exception.code() == 404) {
                            "Não foi possível remover este material. Atualize a OS e tente novamente."
                        } else {
                            "Erro do servidor: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao remover material", exception)
                showMaterialError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao remover material", exception)
                showMaterialError("Ocorreu um erro ao remover o material.")
            }
        }
    }

    fun adicionarAnexo(uri: Uri, nomeInformado: String) {
        val ordemAtual = _uiState.value.ordem ?: return

        if (ordemAtual.status != OrdemServicoStatus.EM_ATENDIMENTO) {
            showAttachmentError("Anexos só podem ser enviados durante o atendimento.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isUploadingAttachment = true,
            attachmentError = null
        )

        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val contentType = resolver.getType(uri) ?: "application/octet-stream"
                val originalFileName = resolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "anexo_${System.currentTimeMillis()}.jpg"

                val originalExtension = originalFileName
                    .substringAfterLast('.', "")
                    .lowercase()
                val typedBaseName = nomeInformado
                    .trim()
                    .substringBeforeLast('.')
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .ifBlank { "anexo_${System.currentTimeMillis()}" }
                val fileName = if (originalExtension.isBlank()) {
                    typedBaseName
                } else {
                    "$typedBaseName.$originalExtension"
                }

                val fileBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Não foi possível ler o arquivo selecionado.")
                val requestBody = fileBytes.toRequestBody(contentType.toMediaTypeOrNull())
                val multipartFile = MultipartBody.Part.createFormData(
                    "arquivo",
                    fileName,
                    requestBody
                )

                val novoAnexo = repository.adicionarAnexo(ordemId, multipartFile)

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(anexos = ordemAtual.anexos + novoAnexo),
                    isUploadingAttachment = false,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao enviar anexo", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showAttachmentError(
                        if (exception.code() == 413) {
                            "A imagem selecionada é muito grande."
                        } else {
                            "Erro ao enviar anexo: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha ao ler ou enviar anexo", exception)
                showAttachmentError("Não foi possível ler ou enviar a imagem.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao enviar anexo", exception)
                showAttachmentError("Ocorreu um erro ao enviar a imagem.")
            }
        }
    }

    fun concluirAtendimento(observacao: String) {
        val ordemAtual = _uiState.value.ordem ?: return
        val observacaoFormatada = observacao.trim()

        if (ordemAtual.status != OrdemServicoStatus.EM_ATENDIMENTO) {
            showCompletionError("Apenas atendimentos em andamento podem ser concluídos.")
            return
        }

        if (observacaoFormatada.isBlank()) {
            showCompletionError("Informe uma observação sobre a conclusão do atendimento.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isCompleting = true,
            completionError = null
        )

        viewModelScope.launch {
            try {
                val atualizacao = repository.concluirAtendimento(
                    id = ordemId,
                    observacaoConclusao = observacaoFormatada
                )

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(
                        status = atualizacao.status,
                        inicioAtendimento = atualizacao.inicioAtendimento,
                        fimAtendimento = atualizacao.fimAtendimento,
                        observacaoConclusao = observacaoFormatada
                    ),
                    isCompleting = false,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao concluir atendimento", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showCompletionError(
                        if (exception.code() == 400) {
                            "Não foi possível concluir. Confira a observação e o status da OS."
                        } else {
                            "Erro do servidor: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao concluir atendimento", exception)
                showCompletionError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao concluir atendimento", exception)
                showCompletionError("Ocorreu um erro ao concluir o atendimento.")
            }
        }
    }

    fun carregarAjudantes() {
        if (_uiState.value.isLoadingHelpers || _uiState.value.ajudantes.isNotEmpty()) return

        _uiState.value = _uiState.value.copy(
            isLoadingHelpers = true,
            helperError = null
        )

        viewModelScope.launch {
            try {
                val ajudantes = repository.listarAjudantes()

                _uiState.value = _uiState.value.copy(
                    ajudantes = ajudantes.sortedBy { it.colaboradorNome.lowercase() },
                    isLoadingHelpers = false
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao carregar ajudantes", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showHelperError("Erro ao carregar técnicos: HTTP ${exception.code()}.")
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao carregar ajudantes", exception)
                showHelperError("Não foi possível carregar os técnicos.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao carregar ajudantes", exception)
                showHelperError("Ocorreu um erro ao carregar os técnicos.")
            }
        }
    }

    fun atualizarAjudante(ajudanteId: Long?) {
        val ordemAtual = _uiState.value.ordem ?: return

        if (ordemAtual.status != OrdemServicoStatus.AGENDADA &&
            ordemAtual.status != OrdemServicoStatus.EM_ATENDIMENTO
        ) {
            showHelperError("O ajudante só pode ser alterado antes da conferência.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isSavingHelper = true,
            helperError = null
        )

        viewModelScope.launch {
            try {
                val atualizacao = repository.atualizarAjudante(ordemId, ajudanteId)

                _uiState.value = _uiState.value.copy(
                    ordem = ordemAtual.copy(
                        ajudanteId = atualizacao.ajudanteId,
                        ajudanteNome = atualizacao.ajudanteNome
                    ),
                    isSavingHelper = false,
                    wasUpdated = true
                )
            } catch (exception: HttpException) {
                Log.e("StockFlowDetalhe", "Erro HTTP ao atualizar ajudante", exception)

                if (exception.code() == 401 || exception.code() == 403) {
                    clearSession()
                } else {
                    showHelperError(
                        if (exception.code() == 400) {
                            "Não foi possível selecionar este técnico como ajudante."
                        } else {
                            "Erro do servidor: HTTP ${exception.code()}."
                        }
                    )
                }
            } catch (exception: IOException) {
                Log.e("StockFlowDetalhe", "Falha de conexão ao atualizar ajudante", exception)
                showHelperError("Não foi possível conectar ao servidor.")
            } catch (exception: Exception) {
                Log.e("StockFlowDetalhe", "Erro inesperado ao atualizar ajudante", exception)
                showHelperError("Ocorreu um erro ao atualizar o ajudante.")
            }
        }
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    private fun showActionError(message: String) {
        _uiState.value = _uiState.value.copy(
            isStarting = false,
            actionError = message
        )
    }

    private fun showMaterialError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoadingMaterials = false,
            isAddingMaterial = false,
            removingMaterialId = null,
            materialError = message
        )
    }

    private fun showAttachmentError(message: String) {
        _uiState.value = _uiState.value.copy(
            isUploadingAttachment = false,
            attachmentError = message
        )
    }

    private fun showCompletionError(message: String) {
        _uiState.value = _uiState.value.copy(
            isCompleting = false,
            completionError = message
        )
    }

    private fun showHelperError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoadingHelpers = false,
            isSavingHelper = false,
            helperError = message
        )
    }

    private suspend fun clearSession() {
        tokenManager.clearToken()

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStarting = false,
            isLoadingMaterials = false,
            isAddingMaterial = false,
            removingMaterialId = null,
            isUploadingAttachment = false,
            isCompleting = false,
            isLoadingHelpers = false,
            isSavingHelper = false,
            requiresLogin = true
        )
    }
}
