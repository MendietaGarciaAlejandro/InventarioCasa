package com.example.inventariocasa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inventariocasa.datos.ProductoCatalogo
import com.example.inventariocasa.modelo.Articulo

@Composable
fun DialogoBuscandoCodigo() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("Buscando el código…")
            }
        }
    )
}

/**
 * Primera vez que se ve un código. Se puede crear un artículo nuevo o decir que es uno
 * que ya está en el inventario: lo normal, porque los artículos entran desde la lista de
 * la compra con su nombre de casa ("Leche") y no con el de la etiqueta.
 */
@Composable
fun DialogoCodigoNuevo(
    codigo: String,
    propuesta: ProductoCatalogo?,
    articulos: List<Articulo>,
    alCrear: () -> Unit,
    alVincular: (Articulo) -> Unit,
    alCerrar: () -> Unit
) {
    var filtro by remember { mutableStateOf("") }
    val candidatos = articulos.filter { filtro.isBlank() || it.nombre.contains(filtro.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Código nuevo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (propuesta != null) {
                    Text(propuesta.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    propuesta.detalle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Text("No aparece en el catálogo. Ponle tú el nombre.")
                }
                Text(codigo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                Button(onClick = alCrear, modifier = Modifier.fillMaxWidth()) {
                    Text("Crear artículo nuevo")
                }

                if (articulos.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("O es uno que ya tienes:", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = filtro,
                        onValueChange = { filtro = it },
                        placeholder = { Text("Buscar en el inventario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(candidatos, key = { it.id ?: it.nombre }) { articulo ->
                            Text(
                                text = articulo.nombre,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { alVincular(articulo) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}
