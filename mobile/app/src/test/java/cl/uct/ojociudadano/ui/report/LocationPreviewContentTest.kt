package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationPreviewContentTest {

    @Test
    fun `valid gps location exposes formatted coordinates source and address`() {
        val preview = CreateReportFormState(
            location = LocationResult.Success(
                latitude = -38.7359,
                longitude = -72.5904
            ),
            locationSource = LocationSelectionSource.DEVICE,
            referenceAddress = "Av. Alemania 123",
            commune = "Temuco",
            region = "La Araucanía"
        ).locationPreviewContent()

        requireNotNull(preview)
        assertEquals(GeoCoordinate(-38.7359, -72.5904), preview.coordinate)
        assertEquals(
            "Latitud: -38.735900\nLongitud: -72.590400",
            preview.coordinatesLabel
        )
        assertEquals(
            "Av. Alemania 123 · Temuco · La Araucanía",
            preview.addressLabel
        )
        assertEquals("Obtenida por GPS", preview.sourceLabel)
    }

    @Test
    fun `manual selection identifies the map as the origin`() {
        val preview = CreateReportFormState(
            location = LocationResult.Success(-38.7396, -72.5984),
            locationSource = LocationSelectionSource.MANUAL
        ).locationPreviewContent()

        requireNotNull(preview)
        assertEquals("Seleccionada en el mapa", preview.sourceLabel)
    }

    @Test
    fun `preview is absent without a successful location`() {
        assertNull(
            CreateReportFormState(location = LocationResult.NotAvailable)
                .locationPreviewContent()
        )
        assertNull(
            CreateReportFormState(
                location = LocationResult.MissingPermissions
            ).locationPreviewContent()
        )
        assertNull(
            CreateReportFormState(location = LocationResult.Error("GPS no disponible"))
                .locationPreviewContent()
        )
    }

    @Test
    fun `preview ignores invalid coordinates even if the location says success`() {
        assertNull(
            CreateReportFormState(
                location = LocationResult.Success(latitude = 100.0, longitude = -72.5904)
            ).locationPreviewContent()
        )
    }

    @Test
    fun `blank address fields fall back to a pending reference label`() {
        assertEquals(
            PENDING_ADDRESS_LABEL,
            locationAddressLabel(
                referenceAddress = "   ",
                commune = "",
                region = ""
            )
        )
    }

    @Test
    fun `address label keeps only the available fields in order`() {
        assertEquals(
            "Av. Alemania 123",
            locationAddressLabel("Av. Alemania 123", "", "")
        )
        assertEquals(
            "Temuco · La Araucanía",
            locationAddressLabel("", "Temuco", "La Araucanía")
        )
    }
}
