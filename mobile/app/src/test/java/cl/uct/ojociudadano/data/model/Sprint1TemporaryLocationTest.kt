package cl.uct.ojociudadano.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class Sprint1TemporaryLocationTest {

    @Test
    fun `provides the controlled Temuco location required for Sprint 1`() {
        val location = Sprint1TemporaryLocation.incidentLocation

        assertEquals(-38.7359, location.latitude, 0.0)
        assertEquals(-72.5904, location.longitude, 0.0)
        assertEquals("Ubicación de prueba Sprint 1", location.referenceAddress)
        assertEquals("Temuco", location.commune)
        assertEquals("La Araucanía", location.region)
        assertEquals("Centro", location.sector)
    }
}
