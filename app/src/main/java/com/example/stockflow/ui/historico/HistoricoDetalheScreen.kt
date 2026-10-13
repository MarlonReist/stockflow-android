package com.example.stockflow.ui.historico

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockflow.ui.ordens.AttachmentItem
import com.example.stockflow.ui.ordens.DetailSection
import com.example.stockflow.ui.ordens.DetailValue
import com.example.stockflow.ui.ordens.EmptySectionText
import com.example.stockflow.ui.ordens.SectionDivider
import com.example.stockflow.ui.ordens.StatusBadge
import com.example.stockflow.ui.ordens.formatApiDate
import com.example.stockflow.ui.ordens.formatDateTime
import com.example.stockflow.ui.theme.*

@Composable
fun HistoricoDetalheScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HistoricoDetalheViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(state.requiresLogin) {
        if (state.requiresLogin) onSessionExpired()
    }
    LaunchedEffect(state.arquivoParaAbrir) {
        val arquivo = state.arquivoParaAbrir ?: return@LaunchedEffect
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo.file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, arquivo.contentType)
                clipData = ClipData.newRawUri("Anexo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            viewModel.arquivoEncaminhado()
        } catch (_: ActivityNotFoundException) {
            viewModel.arquivoEncaminhado("Nenhum aplicativo instalado consegue abrir este tipo de arquivo.")
        } catch (_: Exception) {
            viewModel.arquivoEncaminhado("Não foi possível abrir o arquivo no visualizador.")
        }
    }

    state.imagem?.let { bitmap ->
        var scale by remember(bitmap) { mutableFloatStateOf(1f) }
        var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
        Dialog(onDismissRequest = viewModel::fecharImagem, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                    Text(state.nomeImagem.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text("Use dois dedos para ampliar.", style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(bitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }) {
                        Image(
                            bitmap.asImageBitmap(), state.nomeImagem,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            },
                            contentScale = ContentScale.Fit
                        )
                    }
                    TextButton(onClick = viewModel::fecharImagem, modifier = Modifier.fillMaxWidth()) { Text("Fechar imagem") }
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(
                StockFlowBackgroundStart, StockFlowBackground, StockFlowBackgroundEnd
            )))
            .safeDrawingPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar ao histórico")
            }
            Text(
                state.ordem?.let { "Histórico • OS #${it.id}" } ?: "Detalhes do histórico",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::carregarDetalhe) { Text("Tentar novamente") }
            }
            else -> state.ordem?.let { ordem ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Text("Atendimento concluído • Somente consulta", color = StockFlowTextSecondary) }
                    item {
                        DetailSection("Informações gerais") {
                            DetailValue("Número da OS", "#${ordem.id}")
                            DetailValue("Cliente", ordem.clienteNome)
                            DetailValue("Tipo de serviço", ordem.tipoOrdemServicoNome.informado())
                            Text("Status atual", style = MaterialTheme.typography.labelMedium, color = StockFlowTextSecondary)
                            StatusBadge(ordem.status)
                            DetailValue("Descrição", ordem.descricao.informado())
                            DetailValue("Técnico responsável", ordem.tecnicoResponsavelNome.informado())
                            DetailValue("Ajudante", ordem.ajudanteNome.informado("Sem ajudante registrado"))
                            ordem.participacaoDoTecnico.participacaoLabel()?.let { DetailValue("Sua participação", it) }
                            ordem.clienteTelefone?.takeIf { it.isNotBlank() }?.let { DetailValue("Telefone", it) }
                            ordem.clienteEndereco?.takeIf { it.isNotBlank() }?.let { DetailValue("Endereço", it) }
                        }
                    }
                    item {
                        DetailSection("Datas do atendimento") {
                            DetailValue("Abertura", ordem.dataAbertura.formatApiDate())
                            DetailValue("Agendamento", ordem.dataAgendada.formatDateTime().informado())
                            DetailValue("Início do atendimento", ordem.inicioAtendimento.formatDateTime().informado())
                            DetailValue("Conclusão técnica", ordem.fimAtendimento.formatDateTime().informado())
                            Text(
                                "A conclusão técnica registra o término do atendimento. A finalização administrativa é uma etapa distinta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockFlowTextSecondary
                            )
                        }
                    }
                    item {
                        DetailSection("Observação técnica") {
                            Text(ordem.observacaoConclusao.informado("Nenhuma observação de conclusão registrada."))
                        }
                    }
                    item {
                        DetailSection("Materiais registrados") {
                            if (ordem.produtosUtilizados.isEmpty()) EmptySectionText("Nenhum material registrado.")
                            ordem.produtosUtilizados.forEachIndexed { index, material ->
                                if (index > 0) SectionDivider()
                                Text(material.produtoNome, fontWeight = FontWeight.SemiBold)
                                DetailValue("Quantidade", material.quantidade.toString())
                                DetailValue("Almoxarifado de origem", material.almoxarifadoNome.informado("Almoxarifado #${material.almoxarifadoId}"))
                            }
                        }
                    }
                    item {
                        DetailSection("Anexos") {
                            state.attachmentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (ordem.anexos.isEmpty()) EmptySectionText("Nenhum anexo registrado.")
                            ordem.anexos.forEachIndexed { index, anexo ->
                                if (index > 0) SectionDivider()
                                AttachmentItem(anexo)
                                OutlinedButton(
                                    onClick = { viewModel.abrirAnexo(anexo.id) },
                                    enabled = state.downloadingId == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (state.downloadingId == anexo.id) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Baixando…")
                                    } else Text("Abrir anexo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.informado(fallback: String = "Não informado"): String =
    this?.takeIf { it.isNotBlank() } ?: fallback

internal fun String?.participacaoLabel(): String? = when (this) {
    "RESPONSAVEL" -> "Responsável"
    "AJUDANTE" -> "Ajudante"
    "RESPONSAVEL_E_AJUDANTE" -> "Responsável e ajudante"
    else -> null
}
