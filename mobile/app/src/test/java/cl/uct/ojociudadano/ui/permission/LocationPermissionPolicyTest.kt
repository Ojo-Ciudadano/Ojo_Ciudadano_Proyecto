package cl.uct.ojociudadano.ui.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionPolicyTest {

    @Test
    fun `accepts precise location permission`() {
        assertTrue(
            LocationPermissionPolicy.isGranted(
                fineGranted = true,
                coarseGranted = false
            )
        )
    }

    @Test
    fun `accepts approximate location permission`() {
        assertTrue(
            LocationPermissionPolicy.isGranted(
                fineGranted = false,
                coarseGranted = true
            )
        )
    }

    @Test
    fun `requires a request when neither permission was granted`() {
        assertTrue(
            LocationPermissionPolicy.needsRequest(
                fineGranted = false,
                coarseGranted = false
            )
        )
    }

    @Test
    fun `does not request permission when it is already granted`() {
        assertFalse(
            LocationPermissionPolicy.needsRequest(
                fineGranted = true,
                coarseGranted = true
            )
        )
    }
}
