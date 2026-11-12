package com.zzcargo.flightboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zzcargo.flightboard.ui.FlightViewModel
import com.zzcargo.flightboard.ui.screens.FlightListScreen
import com.zzcargo.flightboard.ui.theme.ZzcargoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZzcargoTheme {
                val app = application as FlightApplication
                val viewModel: FlightViewModel = viewModel(
                    factory = FlightViewModel.factory(
                        app.container.repository,
                        app.container.exporter
                    )
                )
                FlightListScreen(viewModel)
            }
        }
    }
}
