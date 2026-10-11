package com.example.inventariocasa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventariocasa.datos.ErrorSupabase
import com.example.inventariocasa.datos.ProductoCatalogo
import com.example.inventariocasa.datos.RepositorioInventario
import com.example.inventariocasa.modelo.Articulo
import com.example.inventariocasa.modelo.EstadoArticulo
import com.example.inventariocasa.modelo.normalizarCodigo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EstadoPantalla(
    val articulos: List<Articulo> = emptyList(),
    val nombresEnListaCompra: Set<String> = emptySet(),
    val cargando: Boolean = true,
    val error: String? = null
) {
    val porReponer: Int get() = articulos.count { it.hayQueReponer }

    fun caducanPronto(hoy: LocalDate): Int = articulos.count { it.caducaPronto(hoy) }

    fun estaEnListaCompra(articulo: Articulo): Boolean =
        articulo.nombre.lowercase() in nombresEnListaCompra
}

/** En qué punto está un escaneo. Null en el ViewModel significa que no hay ninguno en marcha. */
sealed interface Escaneo {
    data object Buscando : Escaneo
    data class Conocido(val idArticulo: String) : Escaneo
    data class Nuevo(val codigo: String, val propuesta: ProductoCatalogo?) : Escaneo
}

class InventarioViewModel : ViewModel() {

    private val repositorio = RepositorioInventario()

    private val _estado = MutableStateFlow(EstadoPantalla())
    val estado: StateFlow<EstadoPantalla> = _estado.asStateFlow()

    private val _avisos = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    private val _escaneo = MutableStateFlow<Escaneo?>(null)
    val escaneo: StateFlow<Escaneo?> = _escaneo.asStateFlow()

    // Para que una recarga que iba de camino no pise un cambio recién pintado:
    // si mientras se pedían los datos alguien tocó algo, esa respuesta ya es vieja.
    private var cambiosLocales = 0
    private var escriturasEnCurso = 0

    init {
        // Sondeo sencillo en vez de websocket: lo que cambie desde la web o al finalizar
        // una compra aparece en pocos segundos, y no hay conexión que mantener viva.
        viewModelScope.launch {
            while (true) {
                recargar()
                delay(INTERVALO_SONDEO_MS)
            }
        }
    }

    private suspend fun recargar() {
        if (!repositorio.estaConfigurado) {
            _estado.update { it.copy(cargando = false, error = "Faltan las claves de Supabase en local.properties") }
            return
        }
        try {
            val cambiosAlPedir = cambiosLocales
            val foto = repositorio.cargar()
            if (cambiosAlPedir != cambiosLocales || escriturasEnCurso > 0) return
            _estado.update {
                it.copy(
                    articulos = foto.articulos,
                    nombresEnListaCompra = foto.nombresEnListaCompra,
                    cargando = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            _estado.update { it.copy(cargando = false, error = describir(e)) }
        }
    }

    fun cambiarCantidad(articulo: Articulo, diferencia: Int) =
        guardarCambio(articulo.conCantidad(articulo.cantidad + diferencia))

    fun cambiarEstado(articulo: Articulo, estado: EstadoArticulo) {
        if (articulo.estado != estado) guardarCambio(articulo.conEstado(estado))
    }

    fun guardarEdicion(articulo: Articulo) = guardarCambio(articulo)

    /**
     * Si ya hay un artículo con ese nombre, se le suma la cantidad en vez de duplicarlo.
     * Con [codigo], el artículo que resulte se queda asociado a ese código de barras.
     */
    fun anadir(nuevo: Articulo, codigo: String? = null) {
        val existente = _estado.value.articulos.firstOrNull { it.nombre.equals(nuevo.nombre, ignoreCase = true) }
        if (existente != null) {
            guardarCambio(
                existente
                    .conCantidad(existente.cantidad + nuevo.cantidad)
                    .copy(
                        ubicacion = nuevo.ubicacion ?: existente.ubicacion,
                        caducidad = nuevo.caducidad ?: existente.caducidad
                    )
            )
            if (codigo != null && existente.id != null) {
                ejecutar { repositorio.vincularCodigo(codigo, existente.id) }
            }
            return
        }
        ejecutar {
            val id = repositorio.crear(nuevo.conCantidad(nuevo.cantidad))
            if (codigo != null && id != null) repositorio.vincularCodigo(codigo, id)
        }
    }

    // ── Código de barras ─────────────────────────────────────────────────────

    /** Lo primero es mirar si el código ya es de casa; solo si no, se pregunta al catálogo. */
    fun codigoEscaneado(leido: String) {
        val codigo = normalizarCodigo(leido)
        _escaneo.value = Escaneo.Buscando
        viewModelScope.launch {
            try {
                val idArticulo = repositorio.idArticuloDeCodigo(codigo)
                if (idArticulo != null) recargar()
                val esDeCasa = idArticulo != null && _estado.value.articulos.any { it.id == idArticulo }
                _escaneo.value =
                    if (esDeCasa) Escaneo.Conocido(idArticulo!!)
                    else Escaneo.Nuevo(codigo, repositorio.consultarCatalogo(codigo))
            } catch (e: Exception) {
                _escaneo.value = null
                _avisos.tryEmit(describir(e))
            }
        }
    }

    fun vincularCodigo(codigo: String, articulo: Articulo) {
        val idArticulo = articulo.id ?: return
        _escaneo.value = Escaneo.Conocido(idArticulo)
        ejecutar(avisoAlTerminar = "Código asociado a \"${articulo.nombre}\"") {
            repositorio.vincularCodigo(codigo, idArticulo)
        }
    }

    fun cerrarEscaneo() {
        _escaneo.value = null
    }

    fun borrar(articulo: Articulo) {
        _estado.update { it.copy(articulos = it.articulos - articulo) }
        ejecutar(avisoAlTerminar = "\"${articulo.nombre}\" quitado del inventario") {
            repositorio.borrar(articulo)
        }
    }

    fun apuntarEnListaCompra(articulo: Articulo) {
        if (_estado.value.estaEnListaCompra(articulo)) return
        ejecutar(avisoAlTerminar = "\"${articulo.nombre}\" apuntado en la lista de la compra") {
            repositorio.apuntarEnListaCompra(articulo)
        }
    }

    fun apuntarTodoLoQueFalta() {
        val actual = _estado.value
        val pendientes = actual.articulos.filter { it.hayQueReponer && !actual.estaEnListaCompra(it) }
        if (pendientes.isEmpty()) {
            _avisos.tryEmit("Todo lo que falta ya está en la lista")
            return
        }
        ejecutar(avisoAlTerminar = "${pendientes.size} producto(s) apuntados en la lista de la compra") {
            pendientes.forEach { repositorio.apuntarEnListaCompra(it) }
        }
    }

    // El cambio se pinta al momento y luego se confirma contra el servidor;
    // si falla, la recarga devuelve la pantalla a lo que hay de verdad.
    private fun guardarCambio(cambiado: Articulo) {
        _estado.update { actual ->
            actual.copy(articulos = actual.articulos.map { if (it.id == cambiado.id) cambiado else it })
        }
        ejecutar { repositorio.actualizar(cambiado) }
    }

    private fun ejecutar(avisoAlTerminar: String? = null, accion: suspend () -> Unit) {
        cambiosLocales++
        escriturasEnCurso++
        viewModelScope.launch {
            try {
                accion()
                avisoAlTerminar?.let { _avisos.tryEmit(it) }
            } catch (e: Exception) {
                _avisos.tryEmit(describir(e))
            } finally {
                escriturasEnCurso--
            }
            recargar()
        }
    }

    private fun describir(e: Exception): String = when {
        e is ErrorSupabase && e.codigo == 404 ->
            "La base de datos no está al día: falta ejecutar el último schema.sql"
        e is ErrorSupabase && e.codigo == 400 ->
            "La base de datos no está al día: falta ejecutar el último schema.sql"
        e is ErrorSupabase && e.codigo == 409 ->
            "Ya hay un artículo con ese nombre"
        e is ErrorSupabase -> "El servidor ha rechazado el cambio (${e.codigo})"
        else -> "Sin conexión con el servidor"
    }

    private companion object {
        const val INTERVALO_SONDEO_MS = 5_000L
    }
}
