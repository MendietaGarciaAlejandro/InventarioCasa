package com.example.inventariocasa.datos

import com.example.inventariocasa.modelo.Articulo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Foto del momento: lo que hay en casa y lo que ya está apuntado para comprar. */
data class FotoInventario(
    val articulos: List<Articulo>,
    val nombresEnListaCompra: Set<String>
)

class RepositorioInventario(
    private val cliente: ClienteSupabase = ClienteSupabase(),
    private val catalogo: CatalogoProductos = CatalogoProductos()
) {

    val estaConfigurado: Boolean get() = cliente.estaConfigurado

    suspend fun cargar(): FotoInventario = withContext(Dispatchers.IO) {
        FotoInventario(
            articulos = cliente.listarInventario().map { it.aModelo() },
            nombresEnListaCompra = cliente.nombresEnListaCompra().map { it.lowercase() }.toSet()
        )
    }

    suspend fun crear(articulo: Articulo): String? = withContext(Dispatchers.IO) {
        cliente.crearArticulo(articulo.aCampos())
    }

    suspend fun idArticuloDeCodigo(codigo: String): String? = withContext(Dispatchers.IO) {
        cliente.idArticuloDeCodigo(codigo)
    }

    suspend fun vincularCodigo(codigo: String, idArticulo: String) = withContext(Dispatchers.IO) {
        cliente.vincularCodigo(codigo, idArticulo)
    }

    // El catálogo es una ayuda, no un requisito: si falla, se sigue sin propuesta de nombre
    suspend fun consultarCatalogo(codigo: String): ProductoCatalogo? = withContext(Dispatchers.IO) {
        runCatching { catalogo.buscar(codigo) }.getOrNull()
    }

    suspend fun actualizar(articulo: Articulo) = withContext(Dispatchers.IO) {
        val id = articulo.id ?: return@withContext
        cliente.actualizarArticulo(id, articulo.aCampos())
    }

    suspend fun borrar(articulo: Articulo) = withContext(Dispatchers.IO) {
        val id = articulo.id ?: return@withContext
        cliente.borrarArticulo(id)
    }

    /**
     * Pasa un artículo a la lista de la compra con el precio y la cantidad que la otra app
     * recuerda de él, para que allí aparezca igual que si lo hubieran escrito a mano.
     */
    suspend fun apuntarEnListaCompra(articulo: Articulo) = withContext(Dispatchers.IO) {
        val habitual = cliente.buscarEnHistorial(articulo.nombre)
        cliente.apuntarEnListaCompra(
            nombre = articulo.nombre,
            categoria = articulo.categoria,
            precio = habitual?.precio ?: 0.0,
            cantidad = habitual?.cantidad?.coerceAtLeast(1) ?: 1
        )
    }
}
