package com.example.inventariocasa.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Azul añil, para que no se confunda de un vistazo con la app de la compra (verde).
private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF283593),
    secondary = Color(0xFF00695C),
    tertiary = Color(0xFFB26A00)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFCC80)
)

// Colores de los tres estados. Fijos en ambos temas: se leen como un semáforo.
val ColorHay = Color(0xFF2E7D32)
val ColorQuedaPoco = Color(0xFFB26A00)
val ColorNoHay = Color(0xFFC62828)

@Composable
fun TemaInventarioCasa(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        content = contenido
    )
}
