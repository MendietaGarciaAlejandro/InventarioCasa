package com.example.inventariocasa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.inventariocasa.modelo.Articulo
import com.example.inventariocasa.modelo.normalizarNombre

/**
 * Alta y edición comparten formulario. Con [original] a null es un alta.
 * [alBorrar] solo se ofrece al editar.
 */
@Composable
fun FormularioArticulo(
    original: Articulo?,
    categorias: List<String>,
    ubicaciones: List<String>,
    alGuardar: (Articulo) -> Unit,
    alBorrar: (() -> Unit)?,
    alCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(original?.nombre.orEmpty()) }
    var cantidad by remember { mutableStateOf((original?.cantidad ?: 1).toString()) }
    var categoria by remember { mutableStateOf(original?.categoria ?: "General") }
    var ubicacion by remember { mutableStateOf(original?.ubicacion.orEmpty()) }

    val frases = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (original == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = frases,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { texto -> cantidad = texto.filter { it.isDigit() }.take(4) },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    keyboardOptions = frases,
                    modifier = Modifier.fillMaxWidth()
                )
                Sugerencias(categorias) { categoria = it }
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Dónde está (nevera, despensa…)") },
                    singleLine = true,
                    keyboardOptions = frases,
                    modifier = Modifier.fillMaxWidth()
                )
                Sugerencias(ubicaciones) { ubicacion = it }
            }
        },
        confirmButton = {
            Button(
                enabled = nombre.isNotBlank(),
                onClick = {
                    val base = original ?: Articulo(nombre = "")
                    alGuardar(
                        base.copy(
                            nombre = normalizarNombre(nombre),
                            categoria = categoria.trim().ifBlank { "General" },
                            ubicacion = ubicacion.trim().ifBlank { null }
                        ).conCantidad(cantidad.toIntOrNull() ?: 0)
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (alBorrar != null) {
                    TextButton(onClick = alBorrar) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = alCerrar) { Text("Cancelar") }
            }
        }
    )
}

// Valores ya usados, para no acabar con "Nevera" y "Frigo" refiriéndose a lo mismo.
@Composable
private fun Sugerencias(valores: List<String>, alElegir: (String) -> Unit) {
    if (valores.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(valores) { valor ->
            SuggestionChip(onClick = { alElegir(valor) }, label = { Text(valor) })
        }
    }
}
