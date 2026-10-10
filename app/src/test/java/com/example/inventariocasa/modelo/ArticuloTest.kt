package com.example.inventariocasa.modelo

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticuloTest {

    private val leche = Articulo(nombre = "Leche", cantidad = 2, estado = EstadoArticulo.HAY)

    @Test
    fun `al quedarse a cero pasa a no hay`() {
        val resultado = leche.conCantidad(0)
        assertEquals(EstadoArticulo.NO_HAY, resultado.estado)
    }

    @Test
    fun `la cantidad nunca baja de cero`() {
        assertEquals(0, leche.conCantidad(-3).cantidad)
    }

    @Test
    fun `al reponer algo que no habia vuelve a hay`() {
        val agotada = leche.conCantidad(0)
        assertEquals(EstadoArticulo.HAY, agotada.conCantidad(1).estado)
    }

    @Test
    fun `subir la cantidad no borra el aviso de queda poco`() {
        val poca = leche.conEstado(EstadoArticulo.QUEDA_POCO)
        assertEquals(EstadoArticulo.QUEDA_POCO, poca.conCantidad(3).estado)
    }

    @Test
    fun `marcar no hay pone la cantidad a cero`() {
        assertEquals(0, leche.conEstado(EstadoArticulo.NO_HAY).cantidad)
    }

    @Test
    fun `marcar hay sobre algo agotado deja al menos una unidad`() {
        val agotada = leche.conEstado(EstadoArticulo.NO_HAY)
        assertEquals(1, agotada.conEstado(EstadoArticulo.HAY).cantidad)
    }

    @Test
    fun `el nombre se guarda con la primera en mayuscula y sin espacios sobrantes`() {
        assertEquals("Leche entera", normalizarNombre("  leche entera "))
    }

    @Test
    fun `un estado desconocido de la api se trata como hay`() {
        assertEquals(EstadoArticulo.HAY, EstadoArticulo.desdeApi("raro"))
        assertEquals(EstadoArticulo.QUEDA_POCO, EstadoArticulo.desdeApi("low"))
    }
}
