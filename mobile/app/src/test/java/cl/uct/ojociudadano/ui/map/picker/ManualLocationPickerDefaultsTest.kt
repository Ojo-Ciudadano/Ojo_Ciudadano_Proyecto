package cl.uct.ojociudadano.ui.map.picker

import cl.uct.ojociudadano.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ManualLocationPickerDefaultsTest {

    @Test
    fun `valid gps location becomes the initial center`() {
        val coordinate = ManualLocationPickerDefaults.initialCoordinate(
            LocationResult.Success(latitude = -33.4489, longitude = -70.6693)
        )

        assertEquals(-33.4489, coordinate.latitude, 0.0)
        assertEquals(-70.6693, coordinate.longitude, 0.0)
    }

    @Test
    fun `invalid gps location falls back to the Araucania center`() {
        val coordinate = ManualLocationPickerDefaults.initialCoordinate(
            LocationResult.Success(latitude = 100.0, longitude = -72.5904)
        )

        assertEquals(ManualLocationPickerDefaults.ARAUCANIA_CENTER, coordinate)
    }

    @Test
    fun `missing or denied location falls back to the Araucania center`() {
        assertEquals(
            ManualLocationPickerDefaults.ARAUCANIA_CENTER,
            ManualLocationPickerDefaults.initialCoordinate(LocationResult.NotAvailable)
        )
        assertEquals(
            ManualLocationPickerDefaults.ARAUCANIA_CENTER,
            ManualLocationPickerDefaults.initialCoordinate(LocationResult.MissingPermissions)
        )
        assertEquals(
            ManualLocationPickerDefaults.ARAUCANIA_CENTER,
            ManualLocationPickerDefaults.initialCoordinate(LocationResult.Error("GPS"))
        )
    }
}
