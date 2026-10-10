package com.example.inventariocasa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.inventariocasa.ui.InventarioViewModel
import com.example.inventariocasa.ui.PantallaInventario
import com.example.inventariocasa.ui.tema.TemaInventarioCasa

class MainActivity : ComponentActivity() {

    private val viewModel: InventarioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaInventarioCasa {
                PantallaInventario(viewModel)
            }
        }
    }
}
