package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.com.jairocpd.ar_app.data.repository.RandomUserRepository

class SceneViewModelFactory(
    private val application: Application,
    private val repository: RandomUserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SceneViewModel::class.java)) {
            return SceneViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
