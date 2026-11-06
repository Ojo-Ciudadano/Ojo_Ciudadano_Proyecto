package cl.uct.ojociudadano.navigation

import org.junit.Assert.assertNull
import org.junit.Test

class AppRoutesTest {

    @Test
    fun `blank and missing identifiers cannot build a detail route`() {
        assertNull(AppRoutes.incidentDetail(null))
        assertNull(AppRoutes.incidentDetail(""))
        assertNull(AppRoutes.incidentDetail("   "))
    }
}
