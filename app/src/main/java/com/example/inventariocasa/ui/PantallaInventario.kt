package com.example.inventariocasa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.inventariocasa.modelo.Articulo
import com.example.inventariocasa.modelo.AvisoCaducidad
import com.example.inventariocasa.modelo.EstadoArticulo
import com.example.inventariocasa.ui.tema.ColorHay
import com.example.inventariocasa.ui.tema.ColorNoHay
import com.example.inventariocasa.ui.tema.ColorQuedaPoco
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInventario(viewModel: InventarioViewModel) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }

    var soloPorReponer by rememberSaveable { mutableStateOf(false) }
    var soloCaducanPronto by rememberSaveable { mutableStateOf(false) }
    val hoy = LocalDate.now()
    var busqueda by rememberSaveable { mutableStateOf("") }
    var mostrarAlta by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Articulo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.avisos.collect { avisos.showSnackbar(it) }
    }

    val visibles = estado.articulos.filter { articulo ->
        (!soloPorReponer || articulo.hayQueReponer) &&
            (!soloCaducanPronto || articulo.caducaPronto(hoy)) &&
            (busqueda.isBlank() || articulo.nombre.contains(busqueda.trim(), ignoreCase = true))
    }
    val porCategoria = visibles.groupBy { it.categoria }.toSortedMap(String.CASE_INSENSITIVE_ORDER)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventario de casa", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = resumen(estado),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::apuntarTodoLoQueFalta,
                        enabled = estado.porReponer > 0
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Apuntar en la lista todo lo que falta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarAlta = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir artículo")
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { margenes ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(margenes)
        ) {
            estado.error?.let { BandaDeError(it) }

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = soloPorReponer,
                    onClick = { soloPorReponer = !soloPorReponer },
                    label = { Text("Por reponer (${estado.porReponer})") }
                )
                FilterChip(
                    selected = soloCaducanPronto,
                    onClick = { soloCaducanPronto = !soloCaducanPronto },
                    label = { Text("Caducan pronto (${estado.caducanPronto(hoy)})") }
                )
            }

            when {
                estado.cargando -> Centrado { CircularProgressIndicator() }

                estado.articulos.isEmpty() -> Centrado {
                    Text(
                        "El inventario está vacío.\nSe irá llenando solo al finalizar compras, " +
                            "o puedes añadir artículos con el botón +.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(32.dp)
                    )
                }

                visibles.isEmpty() -> Centrado {
                    Text("Nada que mostrar con este filtro", color = MaterialTheme.colorScheme.outline)
                }

                else -> LazyColumn(
                    // El margen de abajo deja sitio al botón flotante sobre el último artículo
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    porCategoria.forEach { (categoria, articulos) ->
                        item(key = "categoria-$categoria") {
                            Text(
                                text = categoria,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                            )
                        }
                        items(articulos, key = { it.id ?: it.nombre }) { articulo ->
                            TarjetaArticulo(
                                articulo = articulo,
                                avisoCaducidad = articulo.avisoCaducidad(hoy),
                                enListaCompra = estado.estaEnListaCompra(articulo),
                                alCambiarCantidad = { viewModel.cambiarCantidad(articulo, it) },
                                alCambiarEstado = { viewModel.cambiarEstado(articulo, it) },
                                alApuntar = { viewModel.apuntarEnListaCompra(articulo) },
                                alEditar = { enEdicion = articulo }
                            )
                        }
                    }
                }
            }
        }
    }

    val categorias = estado.articulos.map { it.categoria }.distinct().sorted()
    val ubicaciones = estado.articulos.mapNotNull { it.ubicacion }.distinct().sorted()

    if (mostrarAlta) {
        FormularioArticulo(
            original = null,
            categorias = categorias,
            ubicaciones = ubicaciones,
            alGuardar = {
                viewModel.anadir(it)
                mostrarAlta = false
            },
            alBorrar = null,
            alCerrar = { mostrarAlta = false }
        )
    }

    enEdicion?.let { articulo ->
        FormularioArticulo(
            original = articulo,
            categorias = categorias,
            ubicaciones = ubicaciones,
            alGuardar = {
                viewModel.guardarEdicion(it)
                enEdicion = null
            },
            alBorrar = {
                viewModel.borrar(articulo)
                enEdicion = null
            },
            alCerrar = { enEdicion = null }
        )
    }
}

private fun resumen(estado: EstadoPantalla): String {
    val total = "${estado.articulos.size} artículos"
    return if (estado.porReponer > 0) "$total · ${estado.porReponer} por reponer" else total
}

@Composable
private fun BandaDeError(mensaje: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaArticulo(
    articulo: Articulo,
    avisoCaducidad: AvisoCaducidad?,
    enListaCompra: Boolean,
    alCambiarCantidad: (Int) -> Unit,
    alCambiarEstado: (EstadoArticulo) -> Unit,
    alApuntar: () -> Unit,
    alEditar: () -> Unit
) {
    ElevatedCard(onClick = alEditar, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = articulo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (articulo.estado == EstadoArticulo.NO_HAY) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface
                    )
                    articulo.ubicacion?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    avisoCaducidad?.let {
                        Text(
                            text = it.texto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (it.urgente) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (it.urgente) ColorNoHay else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = { alCambiarCantidad(-1) },
                    enabled = articulo.cantidad > 0,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Uno menos", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${articulo.cantidad}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
                FilledTonalIconButton(
                    onClick = { alCambiarCantidad(1) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Uno más", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    EstadoArticulo.entries.forEachIndexed { indice, opcion ->
                        SegmentedButton(
                            selected = articulo.estado == opcion,
                            onClick = { alCambiarEstado(opcion) },
                            shape = SegmentedButtonDefaults.itemShape(indice, EstadoArticulo.entries.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colorDe(opcion),
                                activeContentColor = Color.White
                            )
                        ) {
                            Text(opcion.etiqueta, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = alApuntar, enabled = !enListaCompra) {
                    if (enListaCompra) {
                        Icon(Icons.Default.Check, contentDescription = "Ya está en la lista de la compra")
                    } else {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Apuntar en la lista de la compra")
                    }
                }
            }
        }
    }
}

private fun colorDe(estado: EstadoArticulo): Color = when (estado) {
    EstadoArticulo.HAY -> ColorHay
    EstadoArticulo.QUEDA_POCO -> ColorQuedaPoco
    EstadoArticulo.NO_HAY -> ColorNoHay
}
