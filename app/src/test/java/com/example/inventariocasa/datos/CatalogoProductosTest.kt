package com.example.inventariocasa.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogoProductosTest {

    // Respuesta real de Open Food Facts para un cartón de leche (8480000109088), recortada
    private val leche = """
        {"code":"8480000109088","product":{"brands":"Hacendado","categories_tags":["en:dairies"],
        "product_name":"Leche fresca semidesnatada","product_name_es":"Leche fresca semidesnatada",
        "quantity":"1 l"},"status":1,"status_verbose":"product found"}
    """.trimIndent()

    @Test
    fun `saca nombre, marca y envase de un producto conocido`() {
        val producto = CatalogoProductos.interpretar(leche)!!
        assertEquals("Leche fresca semidesnatada", producto.nombre)
        assertEquals("Hacendado · 1 l", producto.detalle)
    }

    @Test
    fun `un codigo que el catalogo no conoce no propone nada`() {
        val json = """{"code":"00000000","status":0,"status_verbose":"no code or invalid code"}"""
        assertNull(CatalogoProductos.interpretar(json))
    }

    @Test
    fun `prefiere el nombre en espanol y se queda con la primera marca`() {
        val json = """
            {"status":1,"product":{"product_name":"Kelloggs corn flakes the original",
            "product_name_es":"corn Flakes","brands":"Kellogg's, Kellogg España"}}
        """.trimIndent()
        val producto = CatalogoProductos.interpretar(json)!!
        assertEquals("Corn Flakes", producto.nombre)
        assertEquals("Kellogg's", producto.detalle)
    }

    @Test
    fun `un producto sin nombre no sirve como propuesta`() {
        val json = """{"status":1,"product":{"product_name":"","brands":"Marca"}}"""
        assertNull(CatalogoProductos.interpretar(json))
    }

    @Test
    fun `una respuesta que no es json no rompe nada`() {
        assertNull(CatalogoProductos.interpretar("<html>503</html>"))
    }
}
