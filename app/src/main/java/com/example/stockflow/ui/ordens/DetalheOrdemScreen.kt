package com.example.stockflow.ui.ordens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.stockflow.data.model.OrdemServicoAnexo
import com.example.stockflow.data.model.OrdemServicoDetalhe
import com.example.stockflow.data.model.OrdemServicoItem
import com.example.stockflow.data.model.OrdemServicoStatus
import com.example.stockflow.data.model.Almoxarifado
import com.example.stockflow.data.model.EstoqueDisponivel
import com.example.stockflow.data.model.TecnicoAjudante
import com.example.stockflow.ui.theme.StockFlowAccent
import com.example.stockflow.ui.theme.StockFlowBackground
import com.example.stockflow.ui.theme.StockFlowBackgroundEnd
import com.example.stockflow.ui.theme.StockFlowBackgroundStart
import com.example.stockflow.ui.theme.StockFlowCard
import com.example.stockflow.ui.theme.StockFlowInput
import com.example.stockflow.ui.theme.StockFlowPrimary
import com.example.stockflow.ui.theme.StockFlowTextPrimary
import com.example.stockflow.ui.theme.StockFlowTextSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import android.provider.OpenableColumns

@Composable
fun DetalheOrdemScreen(
    onBack: () -> Unit,
    onOrdemUpdated: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: DetalheOrdemViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartConfirmation by rememberSaveable { mutableStateOf(false) }
    var showMaterialDialog by rememberSaveable { mutableStateOf(false) }
    var materialCountBeforeDialog by rememberSaveable { mutableStateOf(0) }
    var materialToRemove by remember { mutableStateOf<OrdemServicoItem?>(null) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachmentName by rememberSaveable { mutableStateOf("") }
    var completionObservation by rememberSaveable { mutableStateOf("") }
    var showCompletionConfirmation by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    fun suggestAttachmentName(uri: android.net.Uri): String {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "anexo"
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingAttachmentUri = it
            attachmentName = suggestAttachmentName(it)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { wasSaved ->
        if (wasSaved) {
            pendingAttachmentUri = pendingCameraUri
            val nextPhotoNumber = (uiState.ordem?.anexos?.size ?: 0) + 1
            attachmentName = "foto $nextPhotoNumber"
        }
        pendingCameraUri = null
    }

    fun openCamera() {
        val imageDirectory = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File.createTempFile("stockflow_", ".jpg", imageDirectory)
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        pendingCameraUri = imageUri
        cameraLauncher.launch(imageUri)
    }

    LaunchedEffect(uiState.requiresLogin) {
        if (uiState.requiresLogin) {
            onSessionExpired()
        }
    }

    LaunchedEffect(uiState.wasUpdated) {
        if (uiState.wasUpdated) {
            onOrdemUpdated()
        }
    }

    LaunchedEffect(uiState.ordem?.produtosUtilizados?.size, uiState.isAddingMaterial) {
        val currentCount = uiState.ordem?.produtosUtilizados?.size ?: 0
        if (showMaterialDialog && !uiState.isAddingMaterial && currentCount > materialCountBeforeDialog) {
            showMaterialDialog = false
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
    ) {
        DetailHeader(
            ordemId = uiState.ordem?.id,
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = StockFlowPrimary)
                }

                uiState.errorMessage != null -> {
                    DetailError(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = viewModel::carregarDetalhe
                    )
                }

                uiState.ordem != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DetailContent(
                            ordem = uiState.ordem!!,
                            removingMaterialId = uiState.removingMaterialId,
                            materialError = uiState.materialError,
                            onRemoveMaterial = { materialToRemove = it },
                            isUploadingAttachment = uiState.isUploadingAttachment,
                            attachmentError = uiState.attachmentError,
                            onChooseImage = { galleryLauncher.launch("image/*") },
                            onTakePhoto = ::openCamera,
                            completionObservation = completionObservation,
                            onCompletionObservationChange = {
                                completionObservation = it
                            },
                            helpers = uiState.ajudantes,
                            isLoadingHelpers = uiState.isLoadingHelpers,
                            isSavingHelper = uiState.isSavingHelper,
                            helperError = uiState.helperError,
                            onLoadHelpers = viewModel::carregarAjudantes,
                            onUpdateHelper = viewModel::atualizarAjudante,
                            modifier = Modifier.weight(1f)
                        )

                        if (uiState.ordem!!.status == OrdemServicoStatus.AGENDADA) {
                            StartActionBar(
                                isStarting = uiState.isStarting,
                                actionError = uiState.actionError,
                                onStart = {
                                    showStartConfirmation = true
                                }
                            )
                        } else if (uiState.ordem!!.status == OrdemServicoStatus.EM_ATENDIMENTO) {
                            MaterialActionBar(
                                isCompleting = uiState.isCompleting,
                                canComplete = completionObservation.isNotBlank(),
                                completionError = uiState.completionError,
                                onAddMaterial = {
                                    materialCountBeforeDialog =
                                        uiState.ordem!!.produtosUtilizados.size
                                    showMaterialDialog = true
                                    viewModel.carregarMateriais()
                                },
                                onComplete = { showCompletionConfirmation = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showStartConfirmation = false
            },
            title = {
                Text("Iniciar atendimento?")
            },
            text = {
                Text(
                    "O horário de início será registrado pelo servidor e a OS passará para Em atendimento."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartConfirmation = false
                        viewModel.iniciarAtendimento()
                    }
                ) {
                    Text("Iniciar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartConfirmation = false
                    }
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = StockFlowCard,
            titleContentColor = StockFlowTextPrimary,
            textContentColor = StockFlowTextSecondary
        )
    }


    if (showMaterialDialog) {
        AddMaterialDialog(
            almoxarifados = uiState.almoxarifados,
            estoques = uiState.estoques,
            isLoading = uiState.isLoadingMaterials,
            isSaving = uiState.isAddingMaterial,
            errorMessage = uiState.materialError,
            onDismiss = {
                if (!uiState.isAddingMaterial) showMaterialDialog = false
            },
            onConfirm = viewModel::adicionarMaterial
        )
    }

    materialToRemove?.let { material ->
        AlertDialog(
            onDismissRequest = {
                if (uiState.removingMaterialId == null) materialToRemove = null
            },
            title = { Text("Remover material?") },
            text = {
                Text(
                    "${material.produtoNome} (${material.quantidade} un.) será removido da OS e devolvido ao estoque."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removerMaterial(material.id)
                        materialToRemove = null
                    },
                    enabled = uiState.removingMaterialId == null
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { materialToRemove = null },
                    enabled = uiState.removingMaterialId == null
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = StockFlowCard,
            titleContentColor = StockFlowTextPrimary,
            textContentColor = StockFlowTextSecondary
        )
    }


    pendingAttachmentUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isUploadingAttachment) pendingAttachmentUri = null
            },
            title = { Text("Nomear anexo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Use um nome que ajude a identificar a imagem na conferência.",
                        color = StockFlowTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = attachmentName,
                        onValueChange = { attachmentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome do anexo") },
                        placeholder = { Text("Ex.: roteador, telhado 1") },
                        singleLine = true,
                        enabled = !uiState.isUploadingAttachment
                    )
                    Text(
                        text = "A extensão da imagem será adicionada automaticamente.",
                        color = StockFlowTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adicionarAnexo(uri, attachmentName)
                        pendingAttachmentUri = null
                    },
                    enabled = attachmentName.isNotBlank() && !uiState.isUploadingAttachment
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingAttachmentUri = null },
                    enabled = !uiState.isUploadingAttachment
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = StockFlowCard,
            titleContentColor = StockFlowTextPrimary,
            textContentColor = StockFlowTextPrimary
        )
    }

    if (showCompletionConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isCompleting) showCompletionConfirmation = false
            },
            title = { Text("Concluir atendimento?") },
            text = {
                Text(
                    "A OS será enviada para Aguardando conferência. Depois disso, materiais e anexos não poderão mais ser alterados pelo técnico."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionConfirmation = false
                        viewModel.concluirAtendimento(completionObservation)
                    },
                    enabled = !uiState.isCompleting
                ) {
                    Text("Concluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCompletionConfirmation = false },
                    enabled = !uiState.isCompleting
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = StockFlowCard,
            titleContentColor = StockFlowTextPrimary,
            textContentColor = StockFlowTextSecondary
        )
    }
}

@Composable
private fun DetailHeader(
    ordemId: Long?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Voltar",
                tint = StockFlowTextPrimary
            )
        }

        Column {
            Text(
                text = ordemId?.let { "OS #$it" } ?: "Detalhes da OS",
                color = StockFlowTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Detalhes do atendimento",
                color = StockFlowTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DetailContent(
    ordem: OrdemServicoDetalhe,
    removingMaterialId: Long?,
    materialError: String?,
    onRemoveMaterial: (OrdemServicoItem) -> Unit,
    isUploadingAttachment: Boolean,
    attachmentError: String?,
    onChooseImage: () -> Unit,
    onTakePhoto: () -> Unit,
    completionObservation: String,
    onCompletionObservationChange: (String) -> Unit,
    helpers: List<TecnicoAjudante>,
    isLoadingHelpers: Boolean,
    isSavingHelper: Boolean,
    helperError: String?,
    onLoadHelpers: () -> Unit,
    onUpdateHelper: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DetailSection(title = "Ordem de serviço") {
                StatusBadge(status = ordem.status)

                Spacer(modifier = Modifier.height(14.dp))

                DetailValue(
                    label = "Tipo",
                    value = ordem.tipoOrdemServicoNome ?: "Não informado"
                )

                DetailValue(
                    label = "Descrição",
                    value = ordem.descricao
                )
            }
        }

        item {
            DetailSection(title = "Cliente") {
                DetailValue("Nome", ordem.clienteNome)
                ordem.clienteTelefone?.takeIf { it.isNotBlank() }?.let {
                    DetailValue("Telefone", it)
                }
                ordem.clienteEndereco?.takeIf { it.isNotBlank() }?.let {
                    DetailValue("Endereço", it)
                }
            }
        }

        item {
            HelperSection(
                ordem = ordem,
                helpers = helpers,
                isLoading = isLoadingHelpers,
                isSaving = isSavingHelper,
                errorMessage = helperError,
                onLoad = onLoadHelpers,
                onUpdate = onUpdateHelper
            )
        }

        item {
            DetailSection(title = "Cronologia") {
                DetailValue(
                    label = "Abertura",
                    value = ordem.dataAbertura.formatApiDate()
                )
                ordem.dataAgendada.formatApiDateTime()?.let {
                    DetailValue("Agendada para", it)
                }
                ordem.inicioAtendimento.formatApiDateTime()?.let {
                    DetailValue("Início do atendimento", it)
                }
                ordem.fimAtendimento.formatApiDateTime()?.let {
                    DetailValue("Fim do atendimento", it)
                }
            }
        }

        item {
            DetailSection(title = "Materiais utilizados") {
                materialError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(bottom = 10.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (ordem.produtosUtilizados.isEmpty()) {
                    EmptySectionText("Nenhum material registrado.")
                } else {
                    ordem.produtosUtilizados.forEachIndexed { index, item ->
                        MaterialItem(
                            item = item,
                            canRemove = ordem.status == OrdemServicoStatus.EM_ATENDIMENTO,
                            isRemoving = removingMaterialId == item.id,
                            onRemove = { onRemoveMaterial(item) }
                        )

                        if (index < ordem.produtosUtilizados.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }

        item {
            DetailSection(title = "Anexos") {
                attachmentError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(bottom = 10.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (ordem.anexos.isEmpty()) {
                    EmptySectionText("Nenhum anexo enviado.")
                } else {
                    ordem.anexos.forEachIndexed { index, anexo ->
                        AttachmentItem(anexo)

                        if (index < ordem.anexos.lastIndex) {
                            SectionDivider()
                        }
                    }
                }

                if (ordem.status == OrdemServicoStatus.EM_ATENDIMENTO) {
                    Spacer(modifier = Modifier.height(14.dp))

                    if (isUploadingAttachment) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = StockFlowPrimary
                            )
                            Text(
                                text = "Enviando imagem...",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onTakePhoto,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Câmera",
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onChooseImage,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Galeria",
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (ordem.status == OrdemServicoStatus.EM_ATENDIMENTO) {
            item {
                DetailSection(title = "Conclusão do atendimento") {
                    Text(
                        text = "Descreva o serviço realizado e informações importantes para a conferência.",
                        color = StockFlowTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = completionObservation,
                        onValueChange = onCompletionObservationChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        label = { Text("Observação obrigatória") },
                        placeholder = {
                            Text("Ex.: Roteador instalado e conexão testada.")
                        },
                        minLines = 4,
                        maxLines = 7
                    )
                }
            }
        }

        ordem.observacaoConclusao
            ?.takeIf { it.isNotBlank() }
            ?.let { observacao ->
                item {
                    DetailSection(title = "Observação da conclusão") {
                        Text(
                            text = observacao,
                            color = StockFlowTextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
    }
}

@Composable
private fun StartActionBar(
    isStarting: Boolean,
    actionError: String?,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StockFlowCard)
            .border(
                width = 1.dp,
                color = StockFlowPrimary.copy(alpha = 0.14f)
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        actionError?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isStarting,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isStarting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = StockFlowTextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Iniciar atendimento",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun HelperSection(
    ordem: OrdemServicoDetalhe,
    helpers: List<TecnicoAjudante>,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onLoad: () -> Unit,
    onUpdate: (Long?) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
    val canEdit = ordem.status == OrdemServicoStatus.AGENDADA ||
        ordem.status == OrdemServicoStatus.EM_ATENDIMENTO

    DetailSection(title = "Técnico ajudante") {
        Text(
            text = "Opcional",
            modifier = Modifier
                .background(
                    StockFlowAccent.copy(alpha = 0.14f),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 9.dp, vertical = 4.dp),
            color = StockFlowAccent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        errorMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (ordem.ajudanteId != null && ordem.ajudanteNome != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StockFlowInput, RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        StockFlowPrimary.copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ordem.ajudanteNome,
                        color = StockFlowTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID ${ordem.ajudanteId}",
                        color = StockFlowTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                        color = StockFlowPrimary
                    )
                } else if (canEdit) {
                    IconButton(
                        onClick = {
                            onLoad()
                            showPicker = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Trocar ajudante",
                            tint = StockFlowPrimary
                        )
                    }
                    IconButton(onClick = { showRemoveConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remover ajudante",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else if (canEdit) {
            OutlinedButton(
                onClick = {
                    onLoad()
                    showPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null
                )
                Text(
                    text = "Adicionar ajudante",
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            EmptySectionText("Nenhum ajudante informado.")
        }
    }

    if (showPicker) {
        HelperPickerDialog(
            helpers = helpers,
            selectedHelperId = ordem.ajudanteId,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onDismiss = { showPicker = false },
            onSelect = { helper ->
                showPicker = false
                onUpdate(helper.colaboradorId)
            }
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Remover ajudante?") },
            text = { Text("A OS ficará sem técnico ajudante vinculado.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirmation = false
                        onUpdate(null)
                    }
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = StockFlowCard,
            titleContentColor = StockFlowTextPrimary,
            textContentColor = StockFlowTextSecondary
        )
    }
}

@Composable
private fun HelperPickerDialog(
    helpers: List<TecnicoAjudante>,
    selectedHelperId: Long?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSelect: (TecnicoAjudante) -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val search = searchText.trim().lowercase()
    val filteredHelpers = helpers.filter { helper ->
        search.isEmpty() ||
            helper.colaboradorNome.lowercase().contains(search) ||
            helper.colaboradorId.toString().contains(search) ||
            helper.login.lowercase().contains(search)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Selecionar ajudante")
                Text(
                    text = "Campo opcional",
                    color = StockFlowTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por nome, login ou ID") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    singleLine = true
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StockFlowPrimary)
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    filteredHelpers.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum técnico encontrado.",
                                color = StockFlowTextSecondary
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHelpers, key = { it.colaboradorId }) { helper ->
                                val selected = helper.colaboradorId == selectedHelperId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(helper) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            StockFlowPrimary.copy(alpha = 0.18f)
                                        } else {
                                            StockFlowInput
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) StockFlowPrimary
                                        else StockFlowTextSecondary.copy(alpha = 0.18f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = helper.colaboradorNome,
                                            color = StockFlowTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "ID ${helper.colaboradorId} • ${helper.login}",
                                            color = StockFlowTextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
        containerColor = StockFlowCard,
        titleContentColor = StockFlowTextPrimary,
        textContentColor = StockFlowTextPrimary
    )
}

@Composable
private fun MaterialActionBar(
    isCompleting: Boolean,
    canComplete: Boolean,
    completionError: String?,
    onAddMaterial: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StockFlowCard)
            .border(1.dp, StockFlowPrimary.copy(alpha = 0.14f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        completionError?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onAddMaterial,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isCompleting,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
                Text(
                    text = "Material",
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = canComplete && !isCompleting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = StockFlowTextPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null
                    )
                    Text(
                        text = "Concluir",
                        modifier = Modifier.padding(start = 6.dp),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMaterialDialog(
    almoxarifados: List<Almoxarifado>,
    estoques: List<EstoqueDisponivel>,
    isLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Int) -> Unit
) {
    var selectedWarehouseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var productIdText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var warehouseMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showProductPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(almoxarifados) {
        if (selectedWarehouseId == null) {
            selectedWarehouseId = almoxarifados.firstOrNull { it.principal }?.id
                ?: almoxarifados.firstOrNull()?.id
        }
    }

    val selectedWarehouse = almoxarifados.find { it.id == selectedWarehouseId }
    val availableProducts = estoques
        .filter { it.almoxarifadoId == selectedWarehouseId }
        .distinctBy { it.produtoId }
    val selectedStock = availableProducts.find { it.produtoId == selectedProductId }
    val quantity = quantityText.toIntOrNull()
    val canConfirm = selectedWarehouseId != null && selectedProductId != null &&
        quantity != null && quantity > 0 && quantity <= (selectedStock?.quantidade ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = StockFlowPrimary)
                        }
                    }

                    almoxarifados.isEmpty() -> {
                        Text("Nenhum almoxarifado disponível.")
                    }

                    else -> {
                        Text("Almoxarifado", color = StockFlowTextSecondary)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { warehouseMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving
                            ) {
                                Text(selectedWarehouse?.nome ?: "Selecionar")
                            }
                            DropdownMenu(
                                expanded = warehouseMenuExpanded,
                                onDismissRequest = { warehouseMenuExpanded = false }
                            ) {
                                almoxarifados.forEach { warehouse ->
                                    DropdownMenuItem(
                                        text = { Text(warehouse.nome) },
                                        onClick = {
                                            selectedWarehouseId = warehouse.id
                                            selectedProductId = null
                                            productIdText = ""
                                            warehouseMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Produto", color = StockFlowTextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(StockFlowInput, RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selectedStock != null) {
                                        StockFlowPrimary.copy(alpha = 0.65f)
                                    } else {
                                        StockFlowTextSecondary.copy(alpha = 0.35f)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = productIdText,
                                onValueChange = { value ->
                                    val numericValue = value.filter(Char::isDigit)
                                    productIdText = numericValue
                                    selectedProductId = numericValue.toLongOrNull()
                                        ?.let { typedId ->
                                            availableProducts.find {
                                                it.produtoId == typedId
                                            }?.produtoId
                                        }
                                },
                                modifier = Modifier
                                    .width(72.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = StockFlowBackground.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(
                                            topStart = 10.dp,
                                            bottomStart = 10.dp
                                        )
                                    )
                                    .padding(horizontal = 12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = StockFlowTextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                enabled = !isSaving,
                                cursorBrush = SolidColor(StockFlowPrimary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (productIdText.isEmpty()) {
                                            Text(
                                                text = "ID",
                                                color = StockFlowTextSecondary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(StockFlowTextSecondary.copy(alpha = 0.28f))
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable(
                                        enabled = availableProducts.isNotEmpty() && !isSaving,
                                        onClick = { showProductPicker = true }
                                    )
                                    .padding(start = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedStock?.produtoNome
                                        ?: if (availableProducts.isEmpty()) {
                                            "Sem produtos com saldo"
                                        } else {
                                            "Produto"
                                        },
                                    modifier = Modifier.weight(1f),
                                    color = if (selectedStock != null) {
                                        StockFlowTextPrimary
                                    } else {
                                        StockFlowTextSecondary
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedStock != null) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    maxLines = 1
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(StockFlowTextSecondary.copy(alpha = 0.28f))
                                )

                                IconButton(
                                    onClick = { showProductPicker = true },
                                    enabled = availableProducts.isNotEmpty() && !isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Pesquisar produto",
                                        tint = StockFlowPrimary
                                    )
                                }
                            }
                        }

                        selectedStock?.let {
                            Text(
                                text = "Saldo disponível: ${it.quantidade}",
                                color = StockFlowAccent,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit)) quantityText = value
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Quantidade") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSaving,
                            isError = quantity != null && selectedStock != null &&
                                quantity > selectedStock.quantidade
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        requireNotNull(selectedProductId),
                        requireNotNull(selectedWarehouseId),
                        requireNotNull(quantity)
                    )
                },
                enabled = canConfirm && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = StockFlowTextPrimary
                    )
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        },
        containerColor = StockFlowCard,
        titleContentColor = StockFlowTextPrimary,
        textContentColor = StockFlowTextPrimary
    )

    if (showProductPicker) {
        ProductPickerDialog(
            products = availableProducts,
            selectedProductId = selectedProductId,
            onDismiss = { showProductPicker = false },
            onSelect = { stock ->
                selectedProductId = stock.produtoId
                productIdText = stock.produtoId.toString()
                showProductPicker = false
            }
        )
    }
}

@Composable
private fun ProductPickerDialog(
    products: List<EstoqueDisponivel>,
    selectedProductId: Long?,
    onDismiss: () -> Unit,
    onSelect: (EstoqueDisponivel) -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val normalizedSearch = searchText.trim().lowercase()
    val filteredProducts = products.filter { product ->
        normalizedSearch.isEmpty() ||
            product.produtoNome.lowercase().contains(normalizedSearch) ||
            product.produtoId.toString().contains(normalizedSearch)
    }.sortedBy { it.produtoNome.lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Selecionar produto")
                Text(
                    text = "${products.size} produto(s) com saldo",
                    color = StockFlowTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por nome ou ID") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum produto encontrado.",
                            color = StockFlowTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredProducts,
                            key = { "${it.almoxarifadoId}-${it.produtoId}" }
                        ) { product ->
                            val isSelected = product.produtoId == selectedProductId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(product) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        StockFlowPrimary.copy(alpha = 0.18f)
                                    } else {
                                        StockFlowInput
                                    }
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) StockFlowPrimary
                                    else StockFlowTextSecondary.copy(alpha = 0.18f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.produtoNome,
                                            color = StockFlowTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "ID ${product.produtoId}",
                                            color = StockFlowTextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        text = "${product.quantidade} un.",
                                        modifier = Modifier
                                            .background(
                                                StockFlowAccent.copy(alpha = 0.15f),
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        color = StockFlowAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        containerColor = StockFlowCard,
        titleContentColor = StockFlowTextPrimary,
        textContentColor = StockFlowTextPrimary
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = StockFlowPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StockFlowCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                color = StockFlowPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = StockFlowTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = value,
            color = StockFlowTextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MaterialItem(
    item: OrdemServicoItem,
    canRemove: Boolean,
    isRemoving: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.produtoNome,
                color = StockFlowTextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Almoxarifado #${item.almoxarifadoId}",
                color = StockFlowTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "${item.quantidade} un.",
            modifier = Modifier
                .background(
                    color = StockFlowAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = StockFlowAccent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        if (canRemove) {
            IconButton(
                onClick = onRemove,
                enabled = !isRemoving
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = StockFlowAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remover ${item.produtoNome}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(anexo: OrdemServicoAnexo) {
    Column {
        Text(
            text = anexo.nomeOriginal,
            color = StockFlowTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "${anexo.contentType} • ${anexo.tamanho.formatFileSize()}",
            color = StockFlowTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        anexo.dataUpload.formatApiDateTime()?.let {
            Text(
                text = "Enviado em $it",
                color = StockFlowTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptySectionText(message: String) {
    Text(
        text = message,
        color = StockFlowTextSecondary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = StockFlowInput
    )
}

@Composable
private fun DetailError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}

private val apiDetailDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val displayDetailDateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
private val apiDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun String?.formatApiDateTime(): String? {
    if (this.isNullOrBlank()) return null

    return runCatching {
        LocalDateTime.parse(this, apiDetailDateTimeFormatter)
            .format(displayDetailDateTimeFormatter)
    }.getOrElse { this }
}

private fun String.formatApiDate(): String {
    return runCatching {
        LocalDate.parse(this, apiDateFormatter)
            .format(displayDateFormatter)
    }.getOrElse { this }
}

private fun Long.formatFileSize(): String {
    return if (this >= 1_048_576) {
        String.format("%.1f MB", this / 1_048_576.0)
    } else {
        String.format("%.1f KB", this / 1024.0)
    }
}
