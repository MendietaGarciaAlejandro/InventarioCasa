package com.example.inventariocasa.modelo

/**
 * Estado "a ojo" de un artículo. Es lo que de verdad se mantiene al día en casa:
 * nadie cuenta yogures, pero sí se ve de un vistazo si queda poco.
 * [valorApi] es el texto que guarda la base de datos.
 */
enum class EstadoArticulo(val valorApi: String, val etiqueta: String) {
    HAY("ok", "Hay"),
    QUEDA_POCO("low", "Poco"),
    NO_HAY("out", "No hay");

    companion object {
        fun desdeApi(valor: String?): EstadoArticulo =
            entries.firstOrNull { it.valorApi == valor } ?: HAY
    }
}

data class Articulo(
    val id: String? = null,
    val nombre: String,
    val categoria: String = "General",
    val cantidad: Int = 0,
    val estado: EstadoArticulo = EstadoArticulo.HAY,
    val ubicacion: String? = null
) {
    val hayQueReponer: Boolean get() = estado != EstadoArticulo.HAY

    /** Cantidad y estado van de la mano: a cero es "no hay", y al reponer deja de serlo. */
    fun conCantidad(nueva: Int): Articulo {
        val cantidadFinal = nueva.coerceAtLeast(0)
        val estadoFinal = when {
            cantidadFinal == 0 -> EstadoArticulo.NO_HAY
            estado == EstadoArticulo.NO_HAY -> EstadoArticulo.HAY
            else -> estado
        }
        return copy(cantidad = cantidadFinal, estado = estadoFinal)
    }

    fun conEstado(nuevo: EstadoArticulo): Articulo {
        val cantidadFinal = when {
            nuevo == EstadoArticulo.NO_HAY -> 0
            cantidad == 0 -> 1
            else -> cantidad
        }
        return copy(estado = nuevo, cantidad = cantidadFinal)
    }
}

// Misma regla que la app de la compra y la web: así "leche" y "Leche" no acaban
// siendo dos productos distintos al cruzar el inventario con la lista.
fun normalizarNombre(nombre: String): String =
    nombre.trim().replaceFirstChar { it.uppercase() }
