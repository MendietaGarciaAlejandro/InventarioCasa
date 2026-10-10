package com.example.inventariocasa.datos

import com.example.inventariocasa.BuildConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ErrorSupabase(val codigo: Int, cuerpo: String) : IOException("Supabase respondió $codigo: $cuerpo")

/**
 * Acceso directo a la API REST de Supabase (PostgREST). Sin base de datos local a propósito:
 * el inventario se gestiona en casa, con wifi, y así no hay nada que sincronizar.
 * Todas las llamadas son bloqueantes; quien las usa decide el hilo.
 */
class ClienteSupabase {

    private val urlBase = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val clave = BuildConfig.SUPABASE_ANON_KEY

    val estaConfigurado: Boolean get() = urlBase.isNotBlank() && clave.isNotBlank()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val json = "application/json".toMediaType()

    private val articulos: JsonAdapter<List<ArticuloDto>> = listaDe(ArticuloDto::class.java)
    private val productosLista: JsonAdapter<List<ProductoListaDto>> = listaDe(ProductoListaDto::class.java)
    private val historial: JsonAdapter<List<HistorialDto>> = listaDe(HistorialDto::class.java)
    private val codigos: JsonAdapter<List<CodigoBarrasDto>> = listaDe(CodigoBarrasDto::class.java)

    // serializeNulls: al quitar la ubicación hay que mandar "location": null, no omitirla
    private val campos: JsonAdapter<Map<String, Any?>> = moshi
        .adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
        .serializeNulls()

    private fun <T> listaDe(tipo: Class<T>): JsonAdapter<List<T>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, tipo))

    // ── Inventario ───────────────────────────────────────────────────────────

    fun listarInventario(): List<ArticuloDto> {
        val url = url("inventory") {
            addQueryParameter("select", "*")
            addQueryParameter("order", "name.asc")
        }
        return articulos.fromJson(ejecutar(peticion(url).get().build())).orEmpty()
    }

    /** Devuelve el id que le ha puesto la base de datos, que hace falta para colgarle un código. */
    fun crearArticulo(valores: Map<String, Any?>): String? {
        val cuerpo = campos.toJson(valores).toRequestBody(json)
        val peticion = peticion(url("inventory"))
            .addHeader("Prefer", "return=representation")
            .post(cuerpo)
            .build()
        return articulos.fromJson(ejecutar(peticion)).orEmpty().firstOrNull()?.id
    }

    fun actualizarArticulo(id: String, valores: Map<String, Any?>) {
        val url = url("inventory") { addQueryParameter("id", "eq.$id") }
        val cuerpo = campos.toJson(valores).toRequestBody(json)
        ejecutar(peticion(url).patch(cuerpo).build())
    }

    fun borrarArticulo(id: String) {
        val url = url("inventory") { addQueryParameter("id", "eq.$id") }
        ejecutar(peticion(url).delete().build())
    }

    // ── Códigos de barras ────────────────────────────────────────────────────

    fun idArticuloDeCodigo(codigo: String): String? {
        val url = url("inventory_barcodes") {
            addQueryParameter("select", "barcode,inventory_id")
            addQueryParameter("barcode", "eq.$codigo")
        }
        return codigos.fromJson(ejecutar(peticion(url).get().build())).orEmpty().firstOrNull()?.idArticulo
    }

    /** Si el código ya estaba colgado de otro artículo, pasa a este (la última palabra manda). */
    fun vincularCodigo(codigo: String, idArticulo: String) {
        val url = url("inventory_barcodes") { addQueryParameter("on_conflict", "barcode") }
        val cuerpo = campos.toJson(mapOf("barcode" to codigo, "inventory_id" to idArticulo)).toRequestBody(json)
        val peticion = peticion(url)
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(cuerpo)
            .build()
        ejecutar(peticion)
    }

    // ── Lista de la compra (tablas de la otra app) ───────────────────────────

    fun nombresEnListaCompra(): List<String> {
        val url = url("products") { addQueryParameter("select", "name") }
        return productosLista.fromJson(ejecutar(peticion(url).get().build())).orEmpty().map { it.nombre }
    }

    fun buscarEnHistorial(nombre: String): HistorialDto? {
        val url = url("product_history") {
            addQueryParameter("select", "price,quantity")
            addQueryParameter("name", "eq.$nombre")
            addQueryParameter("limit", "1")
        }
        return historial.fromJson(ejecutar(peticion(url).get().build())).orEmpty().firstOrNull()
    }

    /**
     * Apunta un producto en la lista de la compra. Si ya estaba (mismo nombre) no lo toca:
     * "ignore-duplicates" evita pisar la cantidad o el check que tenga puestos.
     */
    fun apuntarEnListaCompra(nombre: String, categoria: String, precio: Double, cantidad: Int) {
        val url = url("products") { addQueryParameter("on_conflict", "name") }
        val cuerpo = campos.toJson(
            mapOf(
                "name" to nombre,
                "category" to categoria,
                "price" to precio,
                "quantity" to cantidad,
                "is_checked" to false
            )
        ).toRequestBody(json)
        val peticion = peticion(url)
            .addHeader("Prefer", "resolution=ignore-duplicates")
            .post(cuerpo)
            .build()
        ejecutar(peticion)
    }

    // ── Detalles de HTTP ─────────────────────────────────────────────────────

    private fun url(tabla: String, configurar: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        "$urlBase/rest/v1/$tabla".toHttpUrl().newBuilder().apply(configurar).build()

    private fun peticion(url: HttpUrl): Request.Builder = Request.Builder()
        .url(url)
        .addHeader("apikey", clave)
        .addHeader("Authorization", "Bearer $clave")
        .addHeader("Content-Type", "application/json")

    private fun ejecutar(peticion: Request): String =
        http.newCall(peticion).execute().use { respuesta ->
            val cuerpo = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) throw ErrorSupabase(respuesta.code, cuerpo)
            cuerpo.ifBlank { "[]" }
        }
}
