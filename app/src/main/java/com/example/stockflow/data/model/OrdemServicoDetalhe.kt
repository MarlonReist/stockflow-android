package com.example.stockflow.data.model

data class OrdemServicoDetalhe(
    val id: Long,
    val dataAbertura: String,
    val status: OrdemServicoStatus,
    val descricao: String,
    val clienteId: Long,
    val clienteNome: String,
    val clienteTelefone: String?,
    val clienteEndereco: String?,
    val tipoOrdemServicoId: Long?,
    val tipoOrdemServicoNome: String?,
    val ajudanteId: Long?,
    val ajudanteNome: String?,
    val dataAgendada: String?,
    val inicioAtendimento: String?,
    val fimAtendimento: String?,
    val observacaoConclusao: String?,
    val produtosUtilizados: List<OrdemServicoItem>,
    val anexos: List<OrdemServicoAnexo>
)

data class OrdemServicoItem(
    val id: Long,
    val osId: Long,
    val produtoId: Long,
    val produtoNome: String,
    val quantidade: Int,
    val valorUnitario: Double?,
    val valorTotal: Double?,
    val almoxarifadoId: Long
)

data class OrdemServicoAnexo(
    val id: Long,
    val osId: Long,
    val nomeOriginal: String,
    val contentType: String,
    val tamanho: Long,
    val dataUpload: String
)

data class OrdemServicoAtualizacao(
    val id: Long,
    val status: OrdemServicoStatus,
    val ajudanteId: Long?,
    val ajudanteNome: String?,
    val inicioAtendimento: String?,
    val fimAtendimento: String?
)
