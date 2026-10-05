package com.example.stockflow.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StockFlowColorScheme = darkColorScheme(
    primary = StockFlowPrimary,
    secondary = StockFlowAccent,
    background = StockFlowBackground,
    surface = StockFlowCard,
    surfaceVariant = StockFlowInput,
    onPrimary = StockFlowTextPrimary,
    onSecondary = StockFlowTextPrimary,
    onBackground = StockFlowTextPrimary,
    onSurface = StockFlowTextPrimary,
    onSurfaceVariant = StockFlowTextSecondary,
    error = StockFlowError
)

@Composable
fun StockFlowTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StockFlowColorScheme,
        typography = Typography,
        content = content
    )
}
