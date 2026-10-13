package com.example.stockflow.ui.historico

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockflow.data.model.HistoricoOrdemServicoResumo
import com.example.stockflow.ui.ordens.StatusBadge
import com.example.stockflow.ui.ordens.formatDateTime
import com.example.stockflow.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoricoScreen(
    onBack: () -> Unit,
    onOrdemClick: (Long) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HistoricoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.requiresLogin) {
        if (state.requiresLogin) onSessionExpired()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(
                StockFlowBackgroundStart, StockFlowBackground, StockFlowBackgroundEnd
            )))
            .safeDrawingPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar para Minhas OS")
            }
            Text(
                "Histórico de OS",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = viewModel::atualizar,
                enabled = !state.isLoading && !state.isLoadingMore && !state.requiresLogin
            ) {
                Icon(Icons.Outlined.Refresh, "Atualizar histórico")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Atendimentos concluídos como responsável ou ajudante. Somente consulta.",
                    color = StockFlowTextSecondary
                )
            }
            item {
                HistoricoFiltrosCard(
                    state = state,
                    onApply = viewModel::aplicarFiltros,
                    onRetryTypes = viewModel::carregarTiposServico
                )
            }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> item {
                    HistoricoErro(state.errorMessage.orEmpty(), viewModel::atualizar)
                }
                state.isEmpty -> item {
                    Text("Nenhum atendimento encontrado", fontWeight = FontWeight.Bold)
                    Text("Tente outro número de OS ou ajuste os filtros.", color = StockFlowTextSecondary)
                }
                state.page >= 0 -> {
                    item {
                        Text(
                            "${state.ordens.size} de ${state.totalElements} atendimentos",
                            color = StockFlowTextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    items(state.ordens, key = { it.id }) { ordem ->
                        HistoricoCard(ordem, onClick = { onOrdemClick(ordem.id) })
                    }
                    item {
                        when {
                            state.isLoadingMore -> Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                            state.loadMoreError != null -> HistoricoErro(
                                state.loadMoreError.orEmpty(), viewModel::carregarMais
                            )
                            state.canLoadMore -> OutlinedButton(
                                onClick = viewModel::carregarMais,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Carregar mais") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricoFiltrosCard(
    state: HistoricoUiState,
    onApply: (HistoricoFiltros) -> Unit,
    onRetryTypes: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var numero by rememberSaveable { mutableStateOf(state.filtros.osId?.toString().orEmpty()) }
    var inicio by rememberSaveable { mutableStateOf(state.filtros.dataInicialConclusao?.toString()) }
    var fim by rememberSaveable { mutableStateOf(state.filtros.dataFinalConclusao?.toString()) }
    var tipoId by rememberSaveable { mutableStateOf(state.filtros.tipoOrdemServicoId) }
    var tipoMenu by remember { mutableStateOf(false) }
    var numeroError by rememberSaveable { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val filtrosAtivos = state.filtros != HistoricoFiltros()

    Card(colors = CardDefaults.cardColors(containerColor = StockFlowCard), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Ocultar filtros" else if (filtrosAtivos) "Filtros aplicados • Alterar" else "Pesquisar e filtrar")
            }
            if (expanded) {
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it; numeroError = null },
                    label = { Text("Número da OS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = numeroError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Período de conclusão técnica", style = MaterialTheme.typography.labelLarge)
                HistoricoDataButton("Data inicial", inicio) { inicio = it }
                HistoricoDataButton("Data final", fim) { fim = it }
                if (inicio != null || fim != null) {
                    TextButton(onClick = { inicio = null; fim = null }) { Text("Remover período") }
                }
                Box {
                    OutlinedButton(
                        onClick = { tipoMenu = true },
                        enabled = !state.isLoadingTypes,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoadingTypes) "Carregando tipos…" else
                            state.tiposServico.firstOrNull { it.id == tipoId }?.nome ?: "Todos os tipos de serviço")
                    }
                    DropdownMenu(expanded = tipoMenu, onDismissRequest = { tipoMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos os tipos") }, onClick = { tipoId = null; tipoMenu = false })
                        state.tiposServico.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo.nome) }, onClick = { tipoId = tipo.id; tipoMenu = false })
                        }
                    }
                }
                Text("A seleção de tipos lista os serviços ativos.", style = MaterialTheme.typography.bodySmall, color = StockFlowTextSecondary)
                state.typesError?.let { HistoricoErro(it, onRetryTypes) }
                (numeroError ?: state.filterError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        val texto = numero.trim()
                        val id = texto.toLongOrNull()
                        if (texto.isNotEmpty() && (id == null || id <= 0)) {
                            numeroError = "Informe um número de OS válido."
                        } else {
                            numeroError = null
                            keyboard?.hide()
                            onApply(HistoricoFiltros(id, inicio?.let(LocalDate::parse), fim?.let(LocalDate::parse), tipoId))
                        }
                    },
                    enabled = !state.requiresLogin,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aplicar filtros") }
                TextButton(
                    onClick = {
                        numero = ""; inicio = null; fim = null; tipoId = null; numeroError = null
                        keyboard?.hide()
                        onApply(HistoricoFiltros())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Limpar filtros") }
            }
        }
    }
}

@Composable
private fun HistoricoDataButton(label: String, value: String?, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val date = value?.let(LocalDate::parse) ?: LocalDate.now()
            DatePickerDialog(context, { _, year, month, day ->
                onSelected(LocalDate.of(year, month + 1, day).toString())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }
    ) {
        Text("$label: ${value?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) } ?: "Selecionar"}")
    }
}

@Composable
private fun HistoricoCard(ordem: HistoricoOrdemServicoResumo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StockFlowCard)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("OS #${ordem.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(ordem.tipoOrdemServicoNome ?: "Tipo não informado", color = StockFlowPrimary)
            Text("Cliente: ${ordem.clienteNome}")
            Text("Conclusão técnica: ${ordem.fimAtendimento.formatDateTime()}", color = StockFlowTextSecondary)
            StatusBadge(ordem.status)
            val participacao = ordem.participacaoDoTecnico.participacaoLabel()
            participacao?.let { Text("Sua participação: $it", style = MaterialTheme.typography.bodySmall) }
            Text("Ver detalhes", color = StockFlowPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HistoricoErro(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
    }
}
