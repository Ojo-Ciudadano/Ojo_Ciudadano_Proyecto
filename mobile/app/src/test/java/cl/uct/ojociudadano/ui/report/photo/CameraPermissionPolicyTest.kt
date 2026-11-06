package cl.uct.ojociudadano.ui.report.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPermissionPolicyTest {

    @Test
    fun `requests the permission while it is not granted`() {
        assertTrue(CameraPermissionPolicy.needsRequest(isGranted = false))
    }

    @Test
    fun `does not request the permission when it is already granted`() {
        assertFalse(CameraPermissionPolicy.needsRequest(isGranted = true))
    }

    @Test
    fun `explains that the permission is required when it can be requested again`() {
        assertEquals(
            PhotoCaptureErrors.PERMISSION_REQUIRED,
            CameraPermissionPolicy.denialMessage(shouldShowRationale = true)
        )
    }

    @Test
    fun `points to the system settings when the permission is blocked`() {
        assertEquals(
            PhotoCaptureErrors.PERMISSION_PERMANENTLY_DENIED,
            CameraPermissionPolicy.denialMessage(shouldShowRationale = false)
        )
    }
}
