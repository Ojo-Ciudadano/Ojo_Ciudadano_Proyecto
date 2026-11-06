package cl.uct.ojociudadano.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.platform.app.InstrumentationRegistry
import cl.uct.ojociudadano.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandResourcesTest {
    @Test fun iconoAdaptativoYTemaDeInicioUtilizanLosRecursosOficiales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(R.mipmap.ic_launcher, info.icon)
        val icon = context.packageManager.getApplicationIcon(info)
        assertTrue(icon is AdaptiveIconDrawable)
        val bitmap = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 432, 432)
        icon.draw(Canvas(bitmap))
        var redPixels = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val color = bitmap.getPixel(x, y)
            if (android.graphics.Color.red(color) > 200 && android.graphics.Color.green(color) < 80) redPixels++
        }
        assertTrue(redPixels > 1000)
        File(context.getExternalFilesDir(null), "launcher-icon.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val theme = context.resources.newTheme().apply { applyStyle(R.style.Theme_OjoCiudadano_Starting, true) }
        val values = theme.obtainStyledAttributes(intArrayOf(
            androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon,
            androidx.core.splashscreen.R.attr.postSplashScreenTheme
        ))
        try {
            assertEquals(R.drawable.ic_splash_logo, values.getResourceId(0, 0))
            assertEquals(R.style.Theme_OjoCiudadano, values.getResourceId(1, 0))
        } finally { values.recycle() }
    }
}
