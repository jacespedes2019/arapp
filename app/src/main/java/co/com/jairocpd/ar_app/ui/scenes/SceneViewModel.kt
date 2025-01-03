package co.com.jairocpd.ar_app.ui.scenes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import co.com.jairocpd.ar_app.data.repository.RandomUserRepository
import co.com.jairocpd.ar_app.domain.model.Nodes
import co.com.jairocpd.ar_app.domain.model.Cube
import co.com.jairocpd.ar_app.domain.model.MaterialNode
import co.com.jairocpd.ar_app.domain.model.RandomUser
import co.com.jairocpd.ar_app.util.MaterialProperties
import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingFailureReason
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class SceneViewModel(application: Application, private val repository: RandomUserRepository) : AndroidViewModel(application) {
    private val _selection = MutableLiveData<KClass<out Nodes>>(Cube::class)
    val selection: LiveData<KClass<out Nodes>> = _selection

    private val _nodes = MutableLiveData<List<Nodes>>(emptyList())
    val nodes: LiveData<List<Nodes>> = _nodes

    private val _selectedNode = MutableLiveData<Nodes?>(null)
    val selectedNode: LiveData<Nodes?> = _selectedNode

    private val _trackingState = MutableLiveData<TrackingState?>(null)
    val trackingState: LiveData<TrackingState?> = _trackingState

    private val _trackingFailureReason = MutableLiveData<TrackingFailureReason?>(null)
    val trackingFailureReason: LiveData<TrackingFailureReason?> = _trackingFailureReason

    private val _isCubePlaced = MutableLiveData(false)
    val isCubePlaced: LiveData<Boolean> = _isCubePlaced

    private val _currentMaterialProperties = MutableLiveData(MaterialProperties.DEFAULT)
    val currentMaterialProperties: LiveData<MaterialProperties> = _currentMaterialProperties

    private val _randomUser = MutableLiveData<RandomUser?>()
    val randomUser: LiveData<RandomUser?> = _randomUser

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage


    fun fetchRandomUser() {
        viewModelScope.launch {
            val result = repository.fetchRandomUser()
            result.onSuccess { user ->
                _randomUser.value = user
                _errorMessage.value = null
            }.onFailure { exception ->
                _randomUser.value = null
                _errorMessage.value = exception.message
            }
        }
    }

    // Node operations
    fun addNode(node: Nodes) {
        val currentNodes = _nodes.value.orEmpty().toMutableList()
        currentNodes.add(node)
        _nodes.value = currentNodes
    }


    fun clearAllNodes() {
        _nodes.value = emptyList()
        _selectedNode.value = null
        _isCubePlaced.value = false
    }

    
    fun setCubePlaced(placed: Boolean) {
        _isCubePlaced.value = placed
    }

}