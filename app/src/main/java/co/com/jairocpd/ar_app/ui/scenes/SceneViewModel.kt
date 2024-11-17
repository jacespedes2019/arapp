package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import co.com.jairocpd.ar_app.domain.model.Cube
import co.com.jairocpd.ar_app.domain.model.Nodes
import kotlin.reflect.KClass

class SceneViewModel(application: Application) : AndroidViewModel(application) {

    val selection = MutableLiveData<KClass<out Nodes>>(Cube::class)

}
