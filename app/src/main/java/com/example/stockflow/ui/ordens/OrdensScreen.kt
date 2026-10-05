package com.example.stockflow.ui.ordens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockflow.data.model.OrdemServicoResumo
import com.example.stockflow.data.model.OrdemServicoStatus
import com.example.stockflow.ui.theme.StockFlowAccent
import com.example.stockflow.ui.theme.StockFlowBackground
import com.example.stockflow.ui.theme.StockFlowBackgroundEnd
import com.example.stockflow.ui.theme.StockFlowBackgroundStart
import com.example.stockflow.ui.theme.StockFlowCard
import com.example.stockflow.ui.theme.StockFlowPrimary
import com.example.stockflow.ui.theme.StockFlowTextPrimary
import com.example.stockflow.ui.theme.StockFlowTextSecondary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun OrdensScreen(
    onOrdemClick: (Long) -> Unit,
    onSessionExpired: () -> Unit,
    refreshRequested: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: OrdensViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.requiresLogin) {
        if (uiState.requiresLogin) {
            onSessionExpired()
        }
    }

    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            viewModel.carregarOrdens()
            onRefreshHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        StockFlowBackgroundStart,
                        StockFlowBackground,
                        StockFlowBackgroundEnd
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Minhas OS",
                color = StockFlowTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!uiState.isLoading && uiState.errorMessage == null) {
                    Text(
                        text = "${uiState.ordens.size} ${if (uiState.ordens.size == 1) "ordem" else "ordens"}",
                        modifier = Modifier
                            .background(
                                color = StockFlowPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = StockFlowPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = viewModel::logout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Sair",
                        tint = StockFlowTextSecondary
                    )
                }
            }
        }

        Text(
            text = "Ordens atribuídas ao seu atendimento",
            color = StockFlowTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = StockFlowPrimary
                    )
                }

                uiState.errorMessage != null -> {
                    ErrorContent(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = viewModel::carregarOrdens
                    )
                }

                uiState.isEmpty -> {
                    EmptyContent()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = uiState.ordens,
                            key = { ordem -> ordem.id }
                        ) { ordem ->
                            OrdemCard(
                                ordem = ordem,
                                onClick = { onOrdemClick(ordem.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdemCard(
    ordem: OrdemServicoResumo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = StockFlowPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = StockFlowCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            StockFlowPrimary,
                            StockFlowAccent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OS #${ordem.id}",
                    color = StockFlowTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = ordem.status)

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Abrir detalhes",
                        tint = StockFlowTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = ordem.tipoOrdemServicoNome ?: "Ordem de serviço",
                color = StockFlowPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OrdemInfo(
                icon = Icons.Outlined.Person,
                label = "Cliente",
                value = ordem.clienteNome
            )

            ordem.clienteEndereco?.takeIf { it.isNotBlank() }?.let { endereco ->
                OrdemInfo(
                    icon = Icons.Outlined.LocationOn,
                    label = "Endereço",
                    value = endereco
                )
            }

            ordem.clienteTelefone?.takeIf { it.isNotBlank() }?.let { telefone ->
                OrdemInfo(
                    icon = Icons.Outlined.Phone,
                    label = "Telefone",
                    value = telefone
                )
            }

            ordem.dataAgendada.formatDateTime()?.let { data ->
                OrdemInfo(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Agendada para",
                    value = data
                )
            }

            ordem.inicioAtendimento.formatDateTime()?.let { data ->
                OrdemInfo(
                    icon = Icons.Outlined.AccessTime,
                    label = "Atendimento iniciado",
                    value = data
                )
            }
        }
    }
}
@Composable
private fun OrdemInfo(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = StockFlowTextSecondary
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column {
            Text(
                text = label,
                color = StockFlowTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = value,
                color = StockFlowTextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun StatusBadge(status: OrdemServicoStatus) {
    val badgeColor = when (status) {
        OrdemServicoStatus.AGENDADA -> StockFlowAccent
        OrdemServicoStatus.EM_ATENDIMENTO -> Color(0xFFF59E0B)
        OrdemServicoStatus.AGUARDANDO_CONFERENCIA -> StockFlowPrimary
        OrdemServicoStatus.FINALIZADA -> Color(0xFF16A34A)
        OrdemServicoStatus.CANCELADA -> Color(0xFFDC2626)
        OrdemServicoStatus.ABERTA -> StockFlowTextSecondary
    }

    val label = when (status) {
        OrdemServicoStatus.AGENDADA -> "Agendada"
        OrdemServicoStatus.EM_ATENDIMENTO -> "Em atendimento"
        OrdemServicoStatus.AGUARDANDO_CONFERENCIA -> "Aguardando conferência"
        OrdemServicoStatus.FINALIZADA -> "Finalizada"
        OrdemServicoStatus.CANCELADA -> "Cancelada"
        OrdemServicoStatus.ABERTA -> "Aberta"
    }

    Text(
        text = label,
        modifier = Modifier
            .background(
                color = badgeColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = badgeColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nenhuma ordem de serviço",
            color = StockFlowTextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Não há atendimentos atribuídos a você no momento.",
            color = StockFlowTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val apiDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")

private fun String?.formatDateTime(): String? {
    if (this.isNullOrBlank()) {
        return null
    }

    return runCatching {
        LocalDateTime.parse(this, apiDateTimeFormatter)
            .format(displayDateTimeFormatter)
    }.getOrElse {
        this
    }
}
