package cl.uct.ojociudadano.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidGeocoderReverseGeocodingClientTest {

    @Test
    fun `maps street number commune and region from a geocoder address`() {
        val address = reverseGeocodedAddressFrom(
            thoroughfare = "  Av. Alemania ",
            subThoroughfare = " 123 ",
            featureName = "Edificio",
            addressLine = "Av. Alemania 123, Temuco",
            locality = " Temuco ",
            subAdminArea = " Cautín ",
            adminArea = " La Araucanía "
        )

        assertEquals("Av. Alemania 123", address.referenceAddress)
        assertEquals("Temuco", address.commune)
        assertEquals("La Araucanía", address.region)
    }

    @Test
    fun `falls back to feature name or address line when there is no street`() {
        val featureAddress = reverseGeocodedAddressFrom(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = "Plaza de Armas",
            addressLine = "Plaza de Armas, Temuco",
            locality = "Temuco",
            subAdminArea = null,
            adminArea = "La Araucanía"
        )
        val lineAddress = reverseGeocodedAddressFrom(
            thoroughfare = " ",
            subThoroughfare = null,
            featureName = "",
            addressLine = "Camino rural sin número",
            locality = null,
            subAdminArea = "Cautín",
            adminArea = "La Araucanía"
        )

        assertEquals("Plaza de Armas", featureAddress.referenceAddress)
        assertEquals("Camino rural sin número", lineAddress.referenceAddress)
        assertEquals("Cautín", lineAddress.commune)
    }

    @Test
    fun `leaves unavailable fields empty instead of inventing values`() {
        val address = reverseGeocodedAddressFrom(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = null,
            addressLine = null,
            locality = null,
            subAdminArea = null,
            adminArea = null
        )

        assertNull(address.referenceAddress)
        assertNull(address.commune)
        assertNull(address.region)
    }
}
