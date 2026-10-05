package com.example.stockflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.stockflow.navigation.StockFlowNavigation
import com.example.stockflow.ui.theme.StockFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            StockFlowTheme {
                StockFlowNavigation()
            }
        }
    }
}
