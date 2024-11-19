package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import co.com.jairocpd.ar_app.config.RetrofitInstance.api
import co.com.jairocpd.ar_app.data.repository.RandomUserRepository
import co.com.jairocpd.ar_app.domain.model.Nodes
import co.com.jairocpd.ar_app.domain.model.RandomUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SceneViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    class TestApplication : Application()

    private lateinit var testApplication: TestApplication
    private val mockRepository = RandomUserRepository(api)
    private lateinit var viewModel: SceneViewModel

    @Before
    fun setUp() {
        testApplication = TestApplication()
        viewModel = SceneViewModel(testApplication, mockRepository)
    }

    @Test
    fun `setCubePlaced updates isCubePlaced LiveData`() {
        val observer = mock<Observer<Boolean>>()
        viewModel.isCubePlaced.observeForever(observer)

        viewModel.setCubePlaced(true)

        verify(observer).onChanged(true)
        assertTrue(viewModel.isCubePlaced.value ?: false)

        viewModel.isCubePlaced.removeObserver(observer)
    }
}
