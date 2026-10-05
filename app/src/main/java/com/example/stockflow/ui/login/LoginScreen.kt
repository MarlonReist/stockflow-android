package com.example.stockflow.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockflow.ui.theme.StockFlowAccent
import com.example.stockflow.ui.theme.StockFlowBackground
import com.example.stockflow.ui.theme.StockFlowBackgroundEnd
import com.example.stockflow.ui.theme.StockFlowBackgroundStart
import com.example.stockflow.ui.theme.StockFlowCard
import com.example.stockflow.ui.theme.StockFlowInput
import com.example.stockflow.ui.theme.StockFlowPrimary
import com.example.stockflow.ui.theme.StockFlowTextPrimary
import com.example.stockflow.ui.theme.StockFlowTextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
        }
    }

    Box(
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .border(
                    width = 1.dp,
                    color = StockFlowPrimary.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = StockFlowCard
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 18.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 28.dp,
                    vertical = 32.dp
                ),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "StockFlow",
                    color = StockFlowTextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Acesse sua conta de técnico.",
                    color = StockFlowTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = uiState.login,
                    onValueChange = viewModel::onLoginChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Login") },
                    placeholder = { Text("Digite seu login") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = stockFlowTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.senha,
                    onValueChange = viewModel::onSenhaChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Senha") },
                    placeholder = { Text("Digite sua senha") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Ocultar senha"
                                } else {
                                    "Mostrar senha"
                                },
                                tint = StockFlowTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = stockFlowTextFieldColors()
                )

                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = viewModel::login,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    StockFlowPrimary,
                                    StockFlowAccent
                                )
                            )
                        ),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = StockFlowInput
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Entrar",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun stockFlowTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = StockFlowTextPrimary,
    unfocusedTextColor = StockFlowTextPrimary,
    focusedContainerColor = StockFlowInput,
    unfocusedContainerColor = StockFlowInput,
    disabledContainerColor = StockFlowInput.copy(alpha = 0.65f),
    focusedBorderColor = StockFlowPrimary,
    unfocusedBorderColor = StockFlowPrimary.copy(alpha = 0.35f),
    focusedLabelColor = StockFlowPrimary,
    unfocusedLabelColor = StockFlowTextSecondary,
    focusedPlaceholderColor = StockFlowTextSecondary.copy(alpha = 0.65f),
    unfocusedPlaceholderColor = StockFlowTextSecondary.copy(alpha = 0.65f),
    cursorColor = StockFlowPrimary
)
