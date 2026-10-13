package com.example.stockflow.data.model

data class HistoricoOrdemServicoDetalhe(
    val id: Long,
    val dataAbertura: String,
    val status: OrdemServicoStatus,
    val descricao: String,
    val clienteId: Long,
    val clienteNome: String,
    val clienteTelefone: String?,
    val clienteEndereco: String?,
    val tecnicoResponsavelId: Long?,
    val tecnicoResponsavelNome: String?,
    val ajudanteId: Long?,
    val ajudanteNome: String?,
    val tipoOrdemServicoId: Long?,
    val tipoOrdemServicoNome: String?,
    val dataAgendada: String?,
    val inicioAtendimento: String?,
    val fimAtendimento: String,
    val observacaoConclusao: String?,
    val participacaoDoTecnico: String?,
    val produtosUtilizados: List<OrdemServicoItem>,
    val anexos: List<OrdemServicoAnexo>
)
