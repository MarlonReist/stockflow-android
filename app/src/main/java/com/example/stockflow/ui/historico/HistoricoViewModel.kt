package com.example.stockflow.ui.historico

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.TokenManager
import com.example.stockflow.data.repository.OrdemServicoRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HistoricoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrdemServicoRepository(application)
    private val tokenManager = TokenManager(application)
    private val _uiState = MutableStateFlow(HistoricoUiState())
    val uiState: StateFlow<HistoricoUiState> = _uiState.asStateFlow()
    private var consulta: Job? = null
    private var consultaTipos: Job? = null

    init {
        atualizar()
        carregarTiposServico()
    }

    fun aplicarFiltros(filtros: HistoricoFiltros) {
        if (_uiState.value.requiresLogin) return

        val erro = validarFiltros(filtros)
        if (erro != null) {
            _uiState.value = _uiState.value.copy(filterError = erro)
            return
        }
        if (filtros == _uiState.value.filtros && consulta?.isActive == true) return

        reiniciar(filtros)
    }

    fun atualizar() {
        if (consulta?.isActive == true || _uiState.value.requiresLogin) return
        reiniciar(_uiState.value.filtros)
    }

    fun carregarMais() {
        val estado = _uiState.value
        if (!estado.canLoadMore || consulta?.isActive == true) return
        consultar(estado.page + 1, estado.filtros)
    }

    private fun reiniciar(filtros: HistoricoFiltros) {
        consulta?.cancel()
        _uiState.value = HistoricoUiState(
            filtros = filtros,
            tiposServico = _uiState.value.tiposServico,
            isLoadingTypes = _uiState.value.isLoadingTypes,
            typesError = _uiState.value.typesError
        )
        consultar(page = 0, filtros = filtros)
    }

    fun carregarTiposServico() {
        if (consultaTipos?.isActive == true || _uiState.value.requiresLogin) return
        _uiState.value = _uiState.value.copy(isLoadingTypes = true, typesError = null)
        consultaTipos = viewModelScope.launch {
            try {
                val tipos = repository.listarTiposOrdemServicoAtivos()
                ensureActive()
                _uiState.value = _uiState.value.copy(
                    tiposServico = tipos.sortedBy { it.nome },
                    isLoadingTypes = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ensureActive()
                if (exception is HttpException && exception.code() == 401) {
                    consulta?.cancel()
                    expirarSessao()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingTypes = false,
                        typesError = "Não foi possível carregar os tipos de serviço."
                    )
                }
            }
        }
    }

    private fun consultar(page: Int, filtros: HistoricoFiltros) {
        val primeiraPagina = page == 0
        _uiState.value = _uiState.value.copy(
            isLoading = primeiraPagina,
            isLoadingMore = !primeiraPagina,
            errorMessage = null,
            loadMoreError = null
        )

        consulta = viewModelScope.launch {
            try {
                val pagina = repository.listarHistorico(
                    osId = filtros.osId,
                    dataInicialConclusao = filtros.dataInicialConclusao,
                    dataFinalConclusao = filtros.dataFinalConclusao,
                    tipoOrdemServicoId = filtros.tipoOrdemServicoId,
                    page = page,
                    size = 20
                )
                // Uma consulta cancelada não pode substituir os resultados dos novos filtros.
                ensureActive()
                _uiState.value = _uiState.value.receberPagina(pagina, reiniciar = primeiraPagina)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                ensureActive()
                if (exception.code() == 401) {
                    consultaTipos?.cancel()
                    expirarSessao()
                } else {
                    mostrarErro(
                        when (exception.code()) {
                            403 -> "Você não tem permissão para consultar este histórico."
                            404 -> "Histórico ou tipo de serviço não encontrado."
                            400 -> "Não foi possível consultar com os filtros informados."
                            else -> "Não foi possível carregar o histórico. Tente novamente."
                        },
                        primeiraPagina
                    )
                }
            } catch (exception: IOException) {
                ensureActive()
                mostrarErro("Não foi possível conectar ao servidor. Tente novamente.", primeiraPagina)
            } catch (exception: Exception) {
                ensureActive()
                mostrarErro("Ocorreu um erro ao carregar o histórico. Tente novamente.", primeiraPagina)
            }
        }
    }

    private fun mostrarErro(mensagem: String, primeiraPagina: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isLoadingMore = false,
            errorMessage = mensagem.takeIf { primeiraPagina },
            loadMoreError = mensagem.takeUnless { primeiraPagina }
        )
    }

    private suspend fun expirarSessao() {
        try {
            tokenManager.clearToken()
        } catch (_: IOException) {
            // Mesmo com falha no armazenamento, a tela deve solicitar novo login.
        } finally {
            _uiState.value = HistoricoUiState(requiresLogin = true)
        }
    }

    private fun validarFiltros(filtros: HistoricoFiltros): String? {
        if (filtros.osId != null && filtros.osId <= 0) return "Informe um número de OS válido."
        if (filtros.tipoOrdemServicoId != null && filtros.tipoOrdemServicoId <= 0) {
            return "Selecione um tipo de serviço válido."
        }
        val inicio = filtros.dataInicialConclusao
        val fim = filtros.dataFinalConclusao
        if ((inicio == null) != (fim == null)) return "Informe a data inicial e a data final juntas."
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            return "A data inicial não pode ser posterior à data final."
        }
        return null
    }
}
