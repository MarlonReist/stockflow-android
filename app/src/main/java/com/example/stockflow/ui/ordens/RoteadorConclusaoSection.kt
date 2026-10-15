package com.example.stockflow.ui.ordens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.OrdemServicoStatus
import com.example.stockflow.data.model.PropriedadeRoteador
import com.example.stockflow.ui.theme.StockFlowTextSecondary

@Composable
internal fun RoteadorResumoSection(
    ordem: OrdemServicoDetalhe,
    state: RoteadorConclusaoUiState,
    isRefreshing: Boolean,
    refreshError: String?,
    onRetryRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    DetailSection("Roteador principal") {
        when {
            isRefreshing -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Atualizando dados do roteador…")
            }
            refreshError != null -> {
                Text(refreshError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryRefresh) { Text("Atualizar dados") }
            }
            else -> {
                DetailValue("Modelo", ordem.modeloRoteadorPrincipal?.takeIf { it.isNotBlank() } ?: "Não cadastrado")
                DetailValue("Propriedade", ordem.propriedadeRoteadorPrincipal?.label() ?: "Não informada")
                ordem.dataAtualizacaoRoteador.formatDateTime()?.let { DetailValue("Atualizado em", it) }
            }
        }
        if (ordem.status == OrdemServicoStatus.EM_ATENDIMENTO) {
            if (state.atualizar && state.erroValidacao(ordem) == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Alteração pendente: ${state.modelo.trim()} • ${state.propriedade?.label()}",
                    color = StockFlowTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Ela será registrada somente ao concluir a OS.",
                    color = StockFlowTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(if (ordem.possuiRoteadorPrincipal()) "Atualizar roteador" else "Cadastrar roteador")
            }
        }
    }
}

@Composable
internal fun RoteadorEditDialog(
    ordem: OrdemServicoDetalhe,
    state: RoteadorConclusaoUiState,
    isCompleting: Boolean,
    onDismiss: () -> Unit,
    onModelChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onPropertyChange: (PropriedadeRoteador) -> Unit,
    onConfirm: () -> Unit
) {
    val erro = state.erroValidacao(ordem)
    AlertDialog(
        onDismissRequest = { if (!isCompleting) onDismiss() },
        title = { Text(if (ordem.possuiRoteadorPrincipal()) "Atualizar roteador" else "Cadastrar roteador") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(if (ordem.possuiRoteadorPrincipal()) "Os dados serão alterados junto com a conclusão da OS." else "Preencha os dados para concluir este atendimento.", color = StockFlowTextSecondary) }
                item { OutlinedTextField(value = state.modelo, onValueChange = onModelChange, label = { Text("Modelo do roteador") }, placeholder = { Text("Ex.: TP-Link Archer C6") }, supportingText = { Text("${state.modelo.trim().length}/150 • Digitação livre") }, isError = state.modelo.trim().length > 150, enabled = !isCompleting, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                if (state.isSearching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                state.searchError?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall, color = StockFlowTextSecondary) } }
                items(state.sugestoes.size) { index ->
                    val sugestao = state.sugestoes[index]
                    TextButton(onClick = { onSuggestionSelected(sugestao.modelo) }, enabled = !isCompleting, modifier = Modifier.fillMaxWidth()) { Text(sugestao.modelo) }
                }
                item { Text("Propriedade", style = MaterialTheme.typography.labelLarge) }
                items(PropriedadeRoteador.entries.size) { index ->
                    val propriedade = PropriedadeRoteador.entries[index]
                    OutlinedButton(onClick = { onPropertyChange(propriedade) }, enabled = !isCompleting, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = state.propriedade == propriedade, onClick = null, enabled = !isCompleting)
                        Spacer(Modifier.width(8.dp)); Text(propriedade.label())
                    }
                }
                erro?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = erro == null && !isCompleting) { Text("Salvar rascunho") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCompleting) { Text("Cancelar") } }
    )
}
