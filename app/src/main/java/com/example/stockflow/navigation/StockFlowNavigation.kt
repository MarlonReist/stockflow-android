package com.example.stockflow.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stockflow.ui.login.LoginScreen
import com.example.stockflow.ui.ordens.DetalheOrdemScreen
import com.example.stockflow.ui.ordens.OrdensScreen
import com.example.stockflow.ui.theme.StockFlowAccent
import com.example.stockflow.ui.theme.StockFlowBackground
import com.example.stockflow.ui.theme.StockFlowBackgroundEnd
import com.example.stockflow.ui.theme.StockFlowBackgroundStart
import com.example.stockflow.ui.theme.StockFlowPrimary
import com.example.stockflow.ui.theme.StockFlowTextPrimary

private object Routes {
    const val LOGIN = "login"
    const val ORDENS = "ordens"
    const val DETALHE_ORDEM = "ordens/{ordemId}"

    fun detalheOrdem(ordemId: Long): String = "ordens/$ordemId"
}

@Composable
fun StockFlowNavigation(
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()

    if (sessionState == SessionState.LOADING) {
        SessionLoadingScreen()
        return
    }

    val navController = rememberNavController()
    val startDestination = if (sessionState == SessionState.AUTHENTICATED) {
        Routes.ORDENS
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.ORDENS) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.ORDENS) { backStackEntry ->
            val refreshRequested by backStackEntry.savedStateHandle
                .getStateFlow("refreshOrdens", false)
                .collectAsState()

            OrdensScreen(
                onOrdemClick = { ordemId ->
                    navController.navigate(Routes.detalheOrdem(ordemId))
                },
                onSessionExpired = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ORDENS) {
                            inclusive = true
                        }
                    }
                },
                refreshRequested = refreshRequested,
                onRefreshHandled = {
                    backStackEntry.savedStateHandle["refreshOrdens"] = false
                }
            )
        }

        composable(
            route = Routes.DETALHE_ORDEM,
            arguments = listOf(
                navArgument("ordemId") {
                    type = NavType.LongType
                }
            )
        ) {
            DetalheOrdemScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOrdemUpdated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refreshOrdens", true)
                },
                onSessionExpired = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ORDENS) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SessionLoadingScreen() {
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
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "StockFlow",
            color = StockFlowTextPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        CircularProgressIndicator(
            color = StockFlowPrimary,
            trackColor = StockFlowAccent.copy(alpha = 0.18f)
        )
    }
}
