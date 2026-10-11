package com.example.inventariocasa.modelo

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

/** Lo que se enseña sobre la caducidad de un artículo. [urgente] es caducado o a punto. */
data class AvisoCaducidad(val texto: String, val urgente: Boolean)

data class Articulo(
    val id: String? = null,
    val nombre: String,
    val categoria: String = "General",
    val cantidad: Int = 0,
    val estado: EstadoArticulo = EstadoArticulo.HAY,
    val ubicacion: String? = null,
    // Una sola fecha por artículo: la de la unidad que antes caduca
    val caducidad: LocalDate? = null
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
        return copy(cantidad = cantidadFinal, estado = estadoFinal).sinFechaSiNoQueda()
    }

    fun conEstado(nuevo: EstadoArticulo): Articulo {
        val cantidadFinal = when {
            nuevo == EstadoArticulo.NO_HAY -> 0
            cantidad == 0 -> 1
            else -> cantidad
        }
        return copy(estado = nuevo, cantidad = cantidadFinal).sinFechaSiNoQueda()
    }

    // Una fecha de caducidad de algo que ya no está solo daría avisos falsos
    private fun sinFechaSiNoQueda(): Articulo = if (cantidad == 0) copy(caducidad = null) else this

    fun caducaPronto(hoy: LocalDate): Boolean {
        val fecha = caducidad ?: return false
        return ChronoUnit.DAYS.between(hoy, fecha) <= DIAS_DE_AVISO
    }

    fun avisoCaducidad(hoy: LocalDate): AvisoCaducidad? {
        val fecha = caducidad ?: return null
        val dias = ChronoUnit.DAYS.between(hoy, fecha)
        return when {
            dias < -1 -> AvisoCaducidad("Caducó hace ${-dias} días", urgente = true)
            dias == -1L -> AvisoCaducidad("Caducó ayer", urgente = true)
            dias == 0L -> AvisoCaducidad("Caduca hoy", urgente = true)
            dias == 1L -> AvisoCaducidad("Caduca mañana", urgente = true)
            dias <= DIAS_DE_AVISO -> AvisoCaducidad("Caduca en $dias días", urgente = true)
            else -> AvisoCaducidad("Caduca el ${fecha.format(FORMATO_FECHA)}", urgente = false)
        }
    }

    companion object {
        // Una semana: da tiempo a gastarlo antes de la siguiente compra grande
        const val DIAS_DE_AVISO = 7L
        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}

// Misma regla que la app de la compra y la web: así "leche" y "Leche" no acaban
// siendo dos productos distintos al cruzar el inventario con la lista.
fun normalizarNombre(nombre: String): String =
    nombre.trim().replaceFirstChar { it.uppercase() }

/**
 * Deja el código de barras en su forma de 13 cifras. Un UPC-A (12 cifras, productos americanos)
 * es el mismo código que un EAN-13 con un cero delante, y cada lector lo devuelve de una manera:
 * sin esto, el mismo producto escaneado aquí y en la web serían dos códigos distintos.
 */
fun normalizarCodigo(codigo: String): String {
    val limpio = codigo.trim()
    return if (limpio.length == 12 && limpio.all { it.isDigit() }) "0$limpio" else limpio
}
