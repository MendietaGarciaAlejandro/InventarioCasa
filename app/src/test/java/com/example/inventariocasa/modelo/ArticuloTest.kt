package com.example.inventariocasa.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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

    // ── Caducidad ────────────────────────────────────────────────────────────

    private val hoy = LocalDate.of(2026, 10, 10)

    private fun queCaduca(fecha: LocalDate) = leche.copy(caducidad = fecha)

    @Test
    fun `sin fecha no hay aviso de caducidad`() {
        assertNull(leche.avisoCaducidad(hoy))
        assertFalse(leche.caducaPronto(hoy))
    }

    @Test
    fun `dentro de la semana avisa con los dias que quedan`() {
        val aviso = queCaduca(hoy.plusDays(3)).avisoCaducidad(hoy)!!
        assertEquals("Caduca en 3 días", aviso.texto)
        assertTrue(aviso.urgente)
    }

    @Test
    fun `hoy y manana tienen texto propio`() {
        assertEquals("Caduca hoy", queCaduca(hoy).avisoCaducidad(hoy)!!.texto)
        assertEquals("Caduca mañana", queCaduca(hoy.plusDays(1)).avisoCaducidad(hoy)!!.texto)
    }

    @Test
    fun `lo caducado sigue contando como urgente`() {
        val aviso = queCaduca(hoy.minusDays(2)).avisoCaducidad(hoy)!!
        assertEquals("Caducó hace 2 días", aviso.texto)
        assertTrue(queCaduca(hoy.minusDays(2)).caducaPronto(hoy))
    }

    @Test
    fun `a mas de una semana se ensena la fecha sin urgencia`() {
        val aviso = queCaduca(LocalDate.of(2026, 11, 5)).avisoCaducidad(hoy)!!
        assertEquals("Caduca el 05/11/2026", aviso.texto)
        assertFalse(aviso.urgente)
    }

    @Test
    fun `el limite del aviso son siete dias justos`() {
        assertTrue(queCaduca(hoy.plusDays(7)).caducaPronto(hoy))
        assertFalse(queCaduca(hoy.plusDays(8)).caducaPronto(hoy))
    }

    @Test
    fun `al acabarse se olvida la fecha de caducidad`() {
        val conFecha = queCaduca(hoy.plusDays(3))
        assertNull(conFecha.conCantidad(0).caducidad)
        assertNull(conFecha.conEstado(EstadoArticulo.NO_HAY).caducidad)
        assertEquals(hoy.plusDays(3), conFecha.conCantidad(1).caducidad)
    }
}
