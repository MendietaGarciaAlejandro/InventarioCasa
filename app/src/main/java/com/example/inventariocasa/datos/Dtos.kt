package com.example.inventariocasa.datos

import com.example.inventariocasa.modelo.Articulo
import com.example.inventariocasa.modelo.EstadoArticulo
import com.squareup.moshi.Json
import java.time.LocalDate

// Las columnas de Supabase están en inglés porque la base de datos se comparte con la
// app de la compra y la web. @Json marca esa frontera: de aquí hacia dentro, en español.

data class ArticuloDto(
    val id: String? = null,
    @Json(name = "name") val nombre: String,
    @Json(name = "category") val categoria: String = "General",
    @Json(name = "quantity") val cantidad: Int = 0,
    @Json(name = "status") val estado: String = "ok",
    @Json(name = "location") val ubicacion: String? = null,
    @Json(name = "expires_at") val caducidad: String? = null
) {
    fun aModelo() = Articulo(
        id = id,
        nombre = nombre,
        categoria = categoria,
        cantidad = cantidad,
        estado = EstadoArticulo.desdeApi(estado),
        ubicacion = ubicacion,
        // Una fecha que no se entiende no debe tirar la carga entera del inventario
        caducidad = caducidad?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    )
}

/** Un código de barras y el artículo del inventario al que pertenece. */
data class CodigoBarrasDto(
    @Json(name = "barcode") val codigo: String,
    @Json(name = "inventory_id") val idArticulo: String
)

/** Fila de la lista de la compra. Solo interesa el nombre, para saber qué está ya apuntado. */
data class ProductoListaDto(
    @Json(name = "name") val nombre: String
)

/** Lo que la app de la compra recuerda de un producto: precio y cantidad habituales. */
data class HistorialDto(
    @Json(name = "price") val precio: Double = 0.0,
    @Json(name = "quantity") val cantidad: Int = 1
)

fun Articulo.aCampos(): Map<String, Any?> = mapOf(
    "name" to nombre,
    "category" to categoria,
    "quantity" to cantidad,
    "status" to estado.valorApi,
    "location" to ubicacion,
    "expires_at" to caducidad?.toString()
)
