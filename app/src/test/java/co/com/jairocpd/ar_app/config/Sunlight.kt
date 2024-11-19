import android.content.SharedPreferences
import android.view.MenuItem
import co.com.jairocpd.ar_app.config.Settings
import com.google.ar.sceneform.ArSceneView
import org.junit.Test
import org.mockito.Mockito.*

class SunlightTest {

    @Test
    fun `applyTo updates the MenuItem checked state`() {
        val prefs = mock(SharedPreferences::class.java)
        val menuItem = mock(MenuItem::class.java)

        `when`(prefs.getBoolean("sunlight", true)).thenReturn(true)

        val sunlight = Settings.Sunlight(true, "sunlight", prefs)

        sunlight.applyTo(menuItem)

        verify(menuItem).isChecked = true
    }
}
