package com.example.stockflow.data.model

data class HistoricoOrdemServicoResumo(
    val id: Long,
    val clienteNome: String,
    val tipoOrdemServicoId: Long?,
    val tipoOrdemServicoNome: String?,
    val dataAbertura: String,
    val dataAgendada: String?,
    val inicioAtendimento: String?,
    val fimAtendimento: String,
    val status: OrdemServicoStatus,
    val tecnicoResponsavelId: Long?,
    val tecnicoResponsavelNome: String?,
    val ajudanteId: Long?,
    val ajudanteNome: String?,
    val participacaoDoTecnico: String?
)
