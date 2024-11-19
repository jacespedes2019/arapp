package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.lifecycle.ViewModel
import co.com.jairocpd.ar_app.config.RetrofitInstance.api
import co.com.jairocpd.ar_app.data.repository.RandomUserRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock

class SceneViewModelFactoryTest {

    // Subclase personalizada de Application
    class TestApplication : Application()

    private val mockRepository = RandomUserRepository(api)
    private val testApplication = TestApplication()

    @Test
    fun `create returns SceneViewModel instance`() {
        val factory = SceneViewModelFactory(testApplication, mockRepository)

        // Crea el ViewModel usando el factory
        val viewModel = factory.create(SceneViewModel::class.java)

        // Verifica que sea una instancia de SceneViewModel
        assertTrue(viewModel is SceneViewModel)
    }

    @Test
    fun `create throws IllegalArgumentException for unknown ViewModel`() {
        val factory = SceneViewModelFactory(testApplication, mockRepository)

        try {
            // Intenta crear un ViewModel con una clase desconocida
            factory.create(UnknownViewModel::class.java)
            fail("Expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            // Verifica que se lanzó la excepción esperada
            assertTrue(e.message?.contains("Unknown ViewModel class") == true)
        }
    }

    // Clase ficticia para probar el caso de excepción
    class UnknownViewModel : ViewModel()
}
