package cl.uct.ojociudadano.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCoordinateTest {

    @Test
    fun `coordinates inside the geographic range are valid`() {
        assertTrue(GeoCoordinate(-38.7359, -72.5904).isValid)
        assertTrue(GeoCoordinate(90.0, 180.0).isValid)
        assertTrue(GeoCoordinate(-90.0, -180.0).isValid)
        assertTrue(GeoCoordinate(0.0, 0.0).isValid)
    }

    @Test
    fun `coordinates outside latitude or longitude ranges are invalid`() {
        assertFalse(GeoCoordinate(90.0001, 0.0).isValid)
        assertFalse(GeoCoordinate(-90.0001, 0.0).isValid)
        assertFalse(GeoCoordinate(0.0, 180.0001).isValid)
        assertFalse(GeoCoordinate(0.0, -180.0001).isValid)
    }

    @Test
    fun `non finite coordinates are invalid`() {
        assertFalse(GeoCoordinate(Double.NaN, 0.0).isValid)
        assertFalse(GeoCoordinate(0.0, Double.NaN).isValid)
        assertFalse(GeoCoordinate(Double.POSITIVE_INFINITY, 0.0).isValid)
        assertFalse(GeoCoordinate(0.0, Double.NEGATIVE_INFINITY).isValid)
    }

    @Test
    fun `shared validation matches both coordinate entry points`() {
        assertTrue(isValidGeographicCoordinate(-38.7359, -72.5904))
        assertFalse(isValidGeographicCoordinate(200.0, -72.5904))
        assertFalse(isValidGeographicCoordinate(-38.7359, 200.0))
        assertFalse(isValidGeographicCoordinate(Double.NaN, -72.5904))
    }
}
