package com.example.stockflow.data.model

data class OrdemServicoResumo(
    val id: Long,
    val status: OrdemServicoStatus,
    val clienteNome: String,
    val clienteTelefone: String?,
    val clienteEndereco: String?,
    val tipoOrdemServicoNome: String?,
    val ajudanteId: Long?,
    val ajudanteNome: String?,
    val dataAgendada: String?,
    val inicioAtendimento: String?
)

enum class OrdemServicoStatus {
    ABERTA,
    AGENDADA,
    EM_ATENDIMENTO,
    AGUARDANDO_CONFERENCIA,
    FINALIZADA,
    CANCELADA
}
