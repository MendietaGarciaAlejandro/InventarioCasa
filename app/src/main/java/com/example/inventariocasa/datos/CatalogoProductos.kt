package com.example.inventariocasa.datos

import com.example.inventariocasa.modelo.normalizarNombre
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Lo que el catálogo sabe de un código: un nombre que proponer y algo de contexto. */
data class ProductoCatalogo(val nombre: String, val detalle: String?)

data class RespuestaCatalogoDto(
    val status: Int = 0,
    @Json(name = "product") val producto: ProductoCatalogoDto? = null
)

data class ProductoCatalogoDto(
    @Json(name = "product_name_es") val nombreEnEspanol: String? = null,
    @Json(name = "product_name") val nombre: String? = null,
    @Json(name = "brands") val marcas: String? = null,
    @Json(name = "quantity") val envase: String? = null
) {
    fun aModelo(): ProductoCatalogo? {
        val elegido = listOf(nombreEnEspanol, nombre).firstOrNull { !it.isNullOrBlank() } ?: return null
        // "brands" llega como lista separada por comas; la primera suele ser la comercial
        val marca = marcas?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val detalle = listOfNotNull(marca, envase?.trim()?.takeIf { it.isNotEmpty() })
            .joinToString(" · ")
            .ifBlank { null }
        return ProductoCatalogo(nombre = normalizarNombre(elegido), detalle = detalle)
    }
}

/**
 * Un código de barras es solo un número: no dice qué producto es. Para la primera vez que se
 * escanea algo se pregunta a Open Food Facts, una base de datos abierta mantenida por
 * voluntarios. Si no lo conoce (o no hay red) no pasa nada: el nombre se escribe a mano.
 */
class CatalogoProductos {

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun buscar(codigo: String): ProductoCatalogo? {
        val peticion = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$codigo.json?fields=$CAMPOS")
            // Open Food Facts pide identificar la aplicación que consulta
            .addHeader("User-Agent", "InventarioCasa/1.0 (Android; uso personal)")
            .get()
            .build()
        return http.newCall(peticion).execute().use { respuesta ->
            if (!respuesta.isSuccessful) null else interpretar(respuesta.body?.string().orEmpty())
        }
    }

    companion object {
        private const val CAMPOS = "product_name,product_name_es,brands,quantity"

        private val respuestas = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(RespuestaCatalogoDto::class.java)

        fun interpretar(json: String): ProductoCatalogo? {
            val respuesta = runCatching { respuestas.fromJson(json) }.getOrNull() ?: return null
            if (respuesta.status != 1) return null
            return respuesta.producto?.aModelo()
        }
    }
}
