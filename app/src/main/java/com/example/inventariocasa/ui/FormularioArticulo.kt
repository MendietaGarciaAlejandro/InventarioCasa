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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.inventariocasa.modelo.Articulo
import com.example.inventariocasa.modelo.normalizarNombre
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    alCerrar: () -> Unit,
    // Al dar de alta desde un código escaneado: lo que propone el catálogo
    nombrePropuesto: String = "",
    nota: String? = null
) {
    var nombre by remember { mutableStateOf(original?.nombre ?: nombrePropuesto) }
    var cantidad by remember { mutableStateOf((original?.cantidad ?: 1).toString()) }
    var categoria by remember { mutableStateOf(original?.categoria ?: "General") }
    var ubicacion by remember { mutableStateOf(original?.ubicacion.orEmpty()) }
    var caducidad by remember { mutableStateOf(original?.caducidad) }
    var eligiendoFecha by remember { mutableStateOf(false) }

    val frases = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (original == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nota?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.weight(1f)) {
                        Text(caducidad?.let { "Caduca el ${it.format(Articulo.FORMATO_FECHA)}" } ?: "Sin fecha de caducidad")
                    }
                    if (caducidad != null) {
                        TextButton(onClick = { caducidad = null }) { Text("Quitar") }
                    }
                }
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
                            ubicacion = ubicacion.trim().ifBlank { null },
                            caducidad = caducidad
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

    if (eligiendoFecha) {
        SelectorDeFecha(
            inicial = caducidad,
            alElegir = {
                caducidad = it
                eligiendoFecha = false
            },
            alCerrar = { eligiendoFecha = false }
        )
    }
}

// El selector de Material trabaja con el instante de medianoche en UTC, no con la fecha local.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeFecha(inicial: LocalDate?, alElegir: (LocalDate) -> Unit, alCerrar: () -> Unit) {
    val seleccion = rememberDatePickerState(
        initialSelectedDateMillis = inicial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(
                enabled = seleccion.selectedDateMillis != null,
                onClick = {
                    seleccion.selectedDateMillis?.let { milis ->
                        alElegir(Instant.ofEpochMilli(milis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                }
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    ) {
        DatePicker(state = seleccion)
    }
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
