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

    class TestApplication : Application()

    private val mockRepository = RandomUserRepository(api)
    private val testApplication = TestApplication()

    @Test
    fun `create returns SceneViewModel instance`() {
        val factory = SceneViewModelFactory(testApplication, mockRepository)

        val viewModel = factory.create(SceneViewModel::class.java)

        assertTrue(viewModel is SceneViewModel)
    }

    @Test
    fun `create throws IllegalArgumentException for unknown ViewModel`() {
        val factory = SceneViewModelFactory(testApplication, mockRepository)

        try {
            factory.create(UnknownViewModel::class.java)
            fail("Expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unknown ViewModel class") == true)
        }
    }

    class UnknownViewModel : ViewModel()
}
